package com.slotforge.common.exception;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.MismatchedInputException;

/** Every API failure leaves as RFC 9457 problem JSON with a stable "code". */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    record FieldViolation(String field, String message) {
    }

    @ExceptionHandler(ApiException.class)
    public ProblemDetail handleApiException(ApiException ex) {
        return problem(ex.getStatus(), ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleConstraintFailure(DataIntegrityViolationException ex) {
        log.warn("Database constraint failed, SQL state {}", ConstraintErrorTranslator.sqlState(ex));
        ConstraintErrorTranslator.Translation error = ConstraintErrorTranslator.translate(ex);
        return problem(error.status(), error.code(), error.message());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleStaleUpdate(OptimisticLockingFailureException ex) {
        return problem(HttpStatus.CONFLICT, "STALE_VERSION",
                "This record was changed by someone else. Reload it and try again.");
    }

    @ExceptionHandler({DataAccessResourceFailureException.class, CannotCreateTransactionException.class})
    public ProblemDetail handleDatabaseUnavailable(Exception ex) {
        log.error("Database unavailable", ex);
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "DATABASE_UNAVAILABLE", "The database is not available right now.");
    }

    // Method security throws inside the controller, so without this the catch-all below would turn it into a 500.
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        return problem(HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have access to this.");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Something went wrong on the server.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldViolation> violations = ex.getBindingResult().getAllErrors().stream()
                .map(error -> new FieldViolation(
                        error instanceof FieldError field ? field.getField() : error.getObjectName(),
                        error.getDefaultMessage()))
                .toList();

        ProblemDetail body = ex.getBody();
        body.setTitle("Validation failed");
        body.setDetail(violations.size() == 1 ? "1 field is invalid" : violations.size() + " fields are invalid");
        body.setProperty("fieldErrors", violations);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    // A body that cannot be read gets the same shape as one that fails validation. When Jackson knows which
    // value was wrong, such as subjects[1].semesterNo, that field is named so the client can show it in place.
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldViolation> violations = unreadableField(ex).map(List::of).orElse(List.of());
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status,
                violations.isEmpty() ? "The request body could not be read." : "1 field is invalid");
        body.setTitle("Validation failed");
        body.setProperty("fieldErrors", violations);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, statusCode, request);
        if (response != null && response.getBody() instanceof ProblemDetail problem) {
            problem.setProperty("code", defaultCode(statusCode));
        }
        return response;
    }

    private static Optional<FieldViolation> unreadableField(Throwable ex) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof MismatchedInputException mismatch && !mismatch.getPath().isEmpty()) {
                return Optional.of(new FieldViolation(fieldPath(mismatch.getPath()), expected(mismatch.getTargetType())));
            }
        }
        return Optional.empty();
    }

    private static String fieldPath(List<JacksonException.Reference> references) {
        StringBuilder path = new StringBuilder();
        for (JacksonException.Reference reference : references) {
            if (reference.getPropertyName() != null) {
                if (!path.isEmpty()) path.append('.');
                path.append(reference.getPropertyName());
            } else if (reference.getIndex() >= 0) {
                path.append('[').append(reference.getIndex()).append(']');
            }
        }
        return path.toString();
    }

    private static String expected(Class<?> type) {
        if (type == null) return "has the wrong type";
        if (type.isEnum()) {
            return "must be one of " + Arrays.stream(type.getEnumConstants()).map(Object::toString)
                    .collect(Collectors.joining(", "));
        }
        if (type == Integer.class || type == int.class || type == Long.class || type == long.class) {
            return "must be a whole number";
        }
        if (type == Boolean.class || type == boolean.class) return "must be true or false";
        if (type == String.class) return "must be text";
        return "has the wrong type";
    }

    private static String defaultCode(HttpStatusCode status) {
        return status.value() == 400 ? "VALIDATION_FAILED" : HttpStatus.valueOf(status.value()).name();
    }

    private static ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty("code", code);
        return problem;
    }
}

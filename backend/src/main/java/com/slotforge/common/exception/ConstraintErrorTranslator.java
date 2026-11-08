package com.slotforge.common.exception;

import java.sql.SQLException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

/** Turns a database constraint failure into a safe error. Database messages can contain row values. */
final class ConstraintErrorTranslator {

    record Translation(HttpStatus status, String code, String message) {
    }

    private ConstraintErrorTranslator() {
    }

    static Translation translate(DataIntegrityViolationException ex) {
        return switch (sqlState(ex)) {
            case "23505" -> new Translation(HttpStatus.CONFLICT, "DUPLICATE",
                    "A record with these details already exists.");
            case "23503" -> new Translation(HttpStatus.CONFLICT, "IN_USE",
                    "This record is linked to other records.");
            case "23502", "23514" -> new Translation(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                    "These values are not allowed.");
            default -> new Translation(HttpStatus.CONFLICT, "CONFLICT",
                    "This change conflicts with existing data.");
        };
    }

    static String sqlState(DataIntegrityViolationException ex) {
        return ex.getMostSpecificCause() instanceof SQLException sql && sql.getSQLState() != null
                ? sql.getSQLState()
                : "unknown";
    }
}

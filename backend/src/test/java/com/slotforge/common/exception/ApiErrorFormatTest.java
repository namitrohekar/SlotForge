package com.slotforge.common.exception;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;

import com.slotforge.auth.repository.AppUserRepository;
import com.slotforge.auth.security.SecurityConfig;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(ApiErrorFormatTest.SampleController.class)
@Import({ApiErrorFormatTest.SampleController.class, SecurityConfig.class})
class ApiErrorFormatTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AppUserRepository users;

    @Test
    void validationFailureListsEveryField() throws Exception {
        mvc.perform(post("/sample").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "", "capacity": 0}"""))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("2 fields are invalid"))
                .andExpect(jsonPath("$.instance").value("/sample"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(2))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'capacity')].message")
                        .value("must be greater than 0"));
    }

    @Test
    void singleInvalidFieldReadsNaturally() throws Exception {
        mvc.perform(post("/sample").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Hall A", "capacity": 0}"""))
                .andExpect(jsonPath("$.detail").value("1 field is invalid"));
    }

    @Test
    void malformedJsonIsAValidationFailure() throws Exception {
        mvc.perform(post("/sample").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(0));
    }

    @Test
    void aValueOfTheWrongTypeNamesItsField() throws Exception {
        mvc.perform(post("/sample").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Hall A", "capacity": "many"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.detail").value("1 field is invalid"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("capacity"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("must be a whole number"));
    }

    @Test
    void missingRecordIsNotFound() throws Exception {
        mvc.perform(get("/sample/missing"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value("Hall 7 was not found"));
    }

    @Test
    void conflictKeepsItsOwnCode() throws Exception {
        mvc.perform(get("/sample/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_LOCKED"));
    }

    @Test
    void brokenBusinessRuleIsUnprocessable() throws Exception {
        mvc.perform(get("/sample/rule"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("LAST_ADMIN"));
    }

    @Test
    void unknownPathIsNotFound() throws Exception {
        mvc.perform(get("/no-such-path"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void wrongMethodKeepsTheFormat() throws Exception {
        mvc.perform(delete("/sample/missing").with(csrf()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void databaseConstraintFailureHidesRowValues() throws Exception {
        mvc.perform(get("/sample/duplicate"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE"))
                .andExpect(content().string(not(containsString("private@example.test"))));
    }

    @Test
    void staleUpdateAsksTheUserToReload() throws Exception {
        mvc.perform(get("/sample/stale"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STALE_VERSION"));
    }

    @Test
    void unexpectedFailureRevealsNothing() throws Exception {
        mvc.perform(get("/sample/crash"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(content().string(not(containsString("internal-detail"))));
    }

    record SampleRequest(@NotBlank String name, @Positive int capacity) {
    }

    @RestController
    static class SampleController {

        @PostMapping("/sample")
        void create(@Valid @RequestBody SampleRequest request) {
        }

        @GetMapping("/sample/missing")
        void missing() {
            throw ApiException.notFound("Hall", 7);
        }

        @GetMapping("/sample/conflict")
        void conflict() {
            throw ApiException.conflict("SESSION_LOCKED", "This session is locked.");
        }

        @GetMapping("/sample/rule")
        void rule() {
            throw ApiException.unprocessable("LAST_ADMIN", "The last administrator cannot be removed.");
        }

        @GetMapping("/sample/duplicate")
        void duplicate() {
            throw new DataIntegrityViolationException("insert failed",
                    new SQLException("Key (email)=(private@example.test) already exists", "23505"));
        }

        @GetMapping("/sample/stale")
        void stale() {
            throw new OptimisticLockingFailureException("row changed");
        }

        @GetMapping("/sample/crash")
        void crash() {
            throw new IllegalStateException("internal-detail");
        }
    }
}

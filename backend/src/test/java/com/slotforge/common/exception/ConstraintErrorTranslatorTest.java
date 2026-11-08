package com.slotforge.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

class ConstraintErrorTranslatorTest {

    @ParameterizedTest
    @CsvSource({
            "23505, CONFLICT, DUPLICATE",
            "23503, CONFLICT, IN_USE",
            "23502, BAD_REQUEST, VALIDATION_FAILED",
            "23514, BAD_REQUEST, VALIDATION_FAILED",
            "23P01, CONFLICT, CONFLICT",
    })
    void mapsPostgresqlStates(String sqlState, HttpStatus status, String code) {
        var error = ConstraintErrorTranslator.translate(
                new DataIntegrityViolationException("failed", new SQLException("detail", sqlState)));

        assertThat(error.status()).isEqualTo(status);
        assertThat(error.code()).isEqualTo(code);
    }

    @Test
    void failureWithoutSqlStateStillGivesAnError() {
        var error = ConstraintErrorTranslator.translate(new DataIntegrityViolationException("failed"));

        assertThat(error.code()).isEqualTo("CONFLICT");
    }
}

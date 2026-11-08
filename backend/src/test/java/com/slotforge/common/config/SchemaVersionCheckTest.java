package com.slotforge.common.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.slotforge.common.health.HealthResponse;
import com.slotforge.common.health.HealthService;
import org.junit.jupiter.api.Test;

class SchemaVersionCheckTest {

    private SchemaVersionCheck check(int actual, int required) {
        HealthService health = mock(HealthService.class);
        when(health.read()).thenReturn(new HealthResponse("UP", "slotforge", "18.6", actual, required));
        return new SchemaVersionCheck(health, new SlotForgeProperties("origin", new SlotForgeProperties.Schema(required)));
    }

    @Test
    void passesWhenTheDatabaseMatches() {
        assertThatCode(() -> check(3, 3).run(null)).doesNotThrowAnyException();
    }

    @Test
    void passesWhenTheDatabaseIsAhead() {
        assertThatCode(() -> check(9, 3).run(null)).doesNotThrowAnyException();
    }

    @Test
    void stopsStartupAndNamesTheNextScriptWhenBehind() {
        assertThatThrownBy(() -> check(1, 3).run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("schema version 1")
                .hasMessageContaining("needs 3")
                .hasMessageContaining("V002");
    }
}

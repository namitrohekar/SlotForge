package com.slotforge.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

class DatabaseTargetTest extends PostgresIntegrationTest {

    @Autowired
    private JdbcClient jdbc;

    @Test
    void usesTheTestDatabaseOnPostgresql() {
        assertThat(jdbc.sql("SELECT current_database()").query(String.class).single())
                .isEqualTo("slotforge_test");
        assertThat(jdbc.sql("SELECT version()").query(String.class).single())
                .startsWith("PostgreSQL");
    }

    @Test
    void rebuildsTheSchemaFromEveryMigrationScript() throws IOException {
        long scripts;
        try (Stream<Path> files = Files.list(Path.of("../database/migrations"))) {
            scripts = files.filter(file -> file.toString().endsWith(".sql")).count();
        }

        assertThat(jdbc.sql("SELECT count(*) FROM schema_version").query(Long.class).single())
                .isEqualTo(scripts);
    }

    @Test
    void hasNoEmbeddedDatabaseOnTheClasspath() {
        assertThatThrownBy(() -> Class.forName("org.h2.Driver"))
                .isInstanceOf(ClassNotFoundException.class);
    }
}

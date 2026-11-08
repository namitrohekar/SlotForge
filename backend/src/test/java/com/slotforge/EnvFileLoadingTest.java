package com.slotforge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.PropertySource;

/**
 * Local PostgreSQL uses trust authentication, so a working connection proves nothing about the password.
 * This reads Spring's environment directly, with no beans and no database.
 */
class EnvFileLoadingTest {

    @Test
    void databasePasswordComesFromTheEnvFile() {
        assumeTrue(Files.exists(Path.of(".env")), "backend/.env does not exist on this machine");

        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(NoBeans.class)
                .web(WebApplicationType.NONE)
                .bannerMode(Banner.Mode.OFF)
                .logStartupInfo(false)
                .run()) {
            PropertySource<?> envFile = context.getEnvironment().getPropertySources().stream()
                    .filter(source -> source.getName().contains(".env"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("backend/.env was not imported"));

            assertThat(envFile.getProperty("SLOTFORGE_DB_PASSWORD")).isNotNull();
            assertThat(context.getEnvironment().getProperty("spring.datasource.password"))
                    .isEqualTo(envFile.getProperty("SLOTFORGE_DB_PASSWORD"));
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class NoBeans {
    }
}

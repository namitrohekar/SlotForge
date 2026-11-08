package com.slotforge.support;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Base class for tests that use real PostgreSQL. Run them with ./mvnw test -Pdb-tests */
@Tag("db")
@SpringBootTest
@ActiveProfiles("test")
public abstract class PostgresIntegrationTest {
}

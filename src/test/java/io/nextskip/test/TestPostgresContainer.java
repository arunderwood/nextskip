package io.nextskip.test;

import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Singleton PostgreSQL container shared across all integration tests.
 *
 * <p>Using a singleton container dramatically improves test suite performance
 * by starting the database once and reusing it, rather than starting a new
 * container for each test class.
 *
 * <p>This follows the recommended Testcontainers pattern for JUnit 5.
 *
 * @see <a href="https://java.testcontainers.org/test_framework_integration/manual_lifecycle_control/#singleton-containers">
 *     Testcontainers Singleton Pattern</a>
 */
public final class TestPostgresContainer {

    private static final PostgreSQLContainer INSTANCE;

    static {
        INSTANCE = new PostgreSQLContainer("postgres:17")
                .withDatabaseName("nextskip_test")
                .withUsername("test")
                .withPassword("test");
        INSTANCE.start();
    }

    private TestPostgresContainer() {
        // Utility class - no instantiation
    }

    /**
     * Returns the shared PostgreSQL container instance.
     *
     * @return the singleton container (already started)
     */
    public static PostgreSQLContainer getInstance() {
        return INSTANCE;
    }
}

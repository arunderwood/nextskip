package io.nextskip.test;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base class for integration tests requiring a database.
 *
 * <p>Uses a singleton PostgreSQL container shared across all tests for performance.
 * Subclasses get automatic datasource configuration without any boilerplate.
 *
 * <p>Usage:
 * <pre>{@code
 * @SpringBootTest
 * class MyIntegrationTest extends AbstractIntegrationTest {
 *     @Test
 *     void myTest() { ... }
 * }
 * }</pre>
 */
@ActiveProfiles("test")
@SuppressWarnings({
        "PMD.AbstractClassWithoutAbstractMethod", // Abstract for inheritance-based config propagation
        "PMD.CloseResource" // Singleton container intentionally stays open for all tests
})
public abstract class AbstractIntegrationTest {

    /** Standard prefix for test data identifiers to distinguish from production data. */
    public static final String TEST_PREFIX = "TEST_";

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        var postgres = TestPostgresContainer.getInstance();
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    /**
     * Creates a test identifier with the standard TEST_ prefix.
     *
     * <p>Use this when creating test data to ensure consistency and easy identification.
     *
     * @param baseName the base identifier name
     * @return the prefixed test identifier (e.g., "TEST_myId")
     */
    protected static String testId(String baseName) {
        return TEST_PREFIX + baseName;
    }
}

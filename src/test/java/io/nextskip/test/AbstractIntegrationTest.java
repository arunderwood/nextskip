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

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        var postgres = TestPostgresContainer.getInstance();
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}

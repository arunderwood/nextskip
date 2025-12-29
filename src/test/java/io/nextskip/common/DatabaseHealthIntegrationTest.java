package io.nextskip.common;

import io.nextskip.common.model.DatabaseHealth;
import io.nextskip.common.repository.DatabaseHealthRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for database connectivity using Testcontainers.
 *
 * <p>Verifies:
 * <ul>
 *   <li>PostgreSQL container starts successfully</li>
 *   <li>Spring Boot connects to the database</li>
 *   <li>Liquibase migrations run on startup</li>
 *   <li>JPA repository operations work correctly</li>
 * </ul>
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class DatabaseHealthIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17")
            .withDatabaseName("nextskip_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private DataSource dataSource;

    @Autowired
    private DatabaseHealthRepository healthRepository;

    @Test
    void testDatabaseConnection_IsValid() throws SQLException {
        // When: Obtain connection from datasource
        try (Connection connection = dataSource.getConnection()) {
            // Then: Connection should be valid
            assertTrue(connection.isValid(5), "Database connection should be valid");
            assertFalse(connection.isClosed(), "Connection should not be closed");
        }
    }

    @Test
    void testLiquibaseMigration_CreatesHealthTable() {
        // When: Query the health table (created by Liquibase migration)
        var healthEntries = healthRepository.findAll();

        // Then: Should have the initial entry from migration
        assertFalse(healthEntries.isEmpty(),
                "Health table should have initial entry from Liquibase migration");

        var initialEntry = healthEntries.get(0);
        assertEquals("INITIALIZED", initialEntry.getStatus(),
                "Initial entry should have INITIALIZED status");
    }

    @Test
    void testRepository_CanInsertAndRetrieve() {
        // Given: A new health check entry
        var healthCheck = new DatabaseHealth("TEST_CHECK");

        // When: Save to database
        var saved = healthRepository.save(healthCheck);

        // Then: Should be persisted with generated ID
        assertNotNull(saved.getId(), "Saved entity should have generated ID");

        // And: Should be retrievable
        var retrieved = healthRepository.findById(saved.getId());
        assertTrue(retrieved.isPresent(), "Should find saved entity by ID");
        assertEquals("TEST_CHECK", retrieved.get().getStatus());
    }

    @Test
    void testRepository_FindMostRecent_ReturnsLatest() {
        // Given: Multiple health entries
        healthRepository.save(new DatabaseHealth("FIRST"));
        healthRepository.save(new DatabaseHealth("SECOND"));
        var latest = healthRepository.save(new DatabaseHealth("LATEST"));

        // When: Find most recent
        var mostRecent = healthRepository.findTopByOrderByCheckTimeDesc();

        // Then: Should return the latest entry
        assertTrue(mostRecent.isPresent());
        assertEquals(latest.getId(), mostRecent.get().getId());
    }
}

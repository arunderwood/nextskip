package io.nextskip.test;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;

/**
 * Base class for persistence integration tests.
 *
 * <p>Provides consistent test isolation through:
 * <ul>
 *   <li>{@code @BeforeEach} cleanup of all repositories returned by {@link #getRepositoriesToClean()}</li>
 *   <li>EntityManager cache clearing for accurate state verification</li>
 * </ul>
 *
 * <p>Why this is needed:
 * <ul>
 *   <li>{@code @Transactional} rollback alone fails with {@code saveAndFlush()}</li>
 *   <li>Singleton TestContainers means data can persist between test classes</li>
 *   <li>EntityManager cache can mask actual database state</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>{@code
 * @SpringBootTest
 * @Transactional
 * class MyEntityIntegrationTest extends AbstractPersistenceTest {
 *
 *     @Autowired
 *     private MyRepository repository;
 *
 *     @Override
 *     protected Collection<JpaRepository<?, ?>> getRepositoriesToClean() {
 *         return List.of(repository);
 *     }
 *
 *     @Test
 *     void testSomething() {
 *         // Repository is cleaned before this runs
 *     }
 * }
 * }</pre>
 */
@SpringBootTest
@Transactional
public abstract class AbstractPersistenceTest extends AbstractIntegrationTest {

    @Autowired
    protected EntityManager entityManager;

    /**
     * Returns the repositories that should be cleaned before each test.
     *
     * <p>Override this method to specify which repositories need cleanup.
     * Repositories are cleaned in iteration order, so for foreign key constraints,
     * return dependent tables first (child before parent).
     *
     * @return collection of repositories to clean, or empty if no cleanup needed
     */
    protected Collection<JpaRepository<?, ?>> getRepositoriesToClean() {
        return Collections.emptyList();
    }

    /**
     * Cleans all repositories before each test.
     *
     * <p>Calls {@link #getRepositoriesToClean()} and deletes all data from each repository,
     * then clears the persistence context to ensure fresh reads.
     */
    @BeforeEach
    void cleanupBeforeTest() {
        getRepositoriesToClean().forEach(JpaRepository::deleteAll);
        clearPersistenceContext();
    }

    /**
     * Clears the persistence context to ensure fresh reads from database.
     *
     * <p>Call this after {@code saveAndFlush()} operations when you need
     * to verify the actual database state rather than cached entities.
     */
    protected void clearPersistenceContext() {
        entityManager.flush();
        entityManager.clear();
    }
}

package io.nextskip.common.repository;

import io.nextskip.common.model.DatabaseHealth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for database health checks.
 *
 * <p>Walking skeleton repository to verify JPA and database connectivity.
 */
@Repository
public interface DatabaseHealthRepository extends JpaRepository<DatabaseHealth, Long> {

    /**
     * Find the most recent health check entry.
     *
     * @return the most recent health check, or empty if none exists
     */
    Optional<DatabaseHealth> findTopByOrderByCheckTimeDesc();
}

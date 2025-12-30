package io.nextskip.common.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Database health check entity used to verify database connectivity.
 *
 * <p>This is a walking skeleton entity - it exists solely to validate
 * that the database connection, JPA, and Liquibase are working correctly.
 * It may be removed or repurposed once real entities are introduced.
 */
@Entity
@Table(name = "database_health")
public class DatabaseHealth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "check_time", nullable = false)
    private Instant checkTime = Instant.now();

    @Column(name = "status", nullable = false, length = 50)
    private String status = "OK";

    /**
     * Default constructor required by JPA.
     */
    protected DatabaseHealth() {
        // JPA requires no-arg constructor
    }

    /**
     * Creates a new health check entry with the specified status.
     *
     * @param status the health check status
     */
    public DatabaseHealth(String status) {
        this.status = status;
        this.checkTime = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Instant getCheckTime() {
        return checkTime;
    }

    public String getStatus() {
        return status;
    }
}

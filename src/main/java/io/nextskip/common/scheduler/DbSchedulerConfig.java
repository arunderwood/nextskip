package io.nextskip.common.scheduler;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.task.OnStartup;
import com.github.kagkarlsson.scheduler.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.List;

/**
 * Manual db-scheduler configuration for Spring Boot 4 compatibility.
 *
 * <p>The db-scheduler-spring-boot-starter 16.x autoconfiguration is not compatible
 * with Spring Boot 4.x due to javax.sql.DataSource vs jakarta.sql.DataSource issues.
 * This configuration manually creates the Scheduler bean until the upstream fix is released.
 *
 * <p>TODO: Remove when db-scheduler releases Spring Boot 4 compatible starter.
 *
 * @see <a href="https://github.com/kagkarlsson/db-scheduler/issues/736">Issue #736</a>
 * @see <a href="https://github.com/kagkarlsson/db-scheduler/pull/771">PR #771</a>
 */
@Configuration
@ConditionalOnProperty(value = "db-scheduler.enabled", havingValue = "true")
public class DbSchedulerConfig {

    private static final Logger LOG = LoggerFactory.getLogger(DbSchedulerConfig.class);

    /**
     * Creates the Scheduler bean with manual configuration.
     *
     * <p>Mirrors the configuration from application.yml db-scheduler.* properties.
     *
     * @param dataSource the application datasource
     * @param tasks all Task beans registered in the application context
     * @return the configured Scheduler instance
     */
    @Bean
    public Scheduler scheduler(DataSource dataSource, List<Task<?>> tasks) {
        LOG.info("Creating db-scheduler with {} tasks (Spring Boot 4 workaround)", tasks.size());

        DataSource txAwareDataSource = configureDataSource(dataSource);

        // Separate regular tasks from OnStartup tasks (RecurringTasks implement OnStartup)
        List<Task<?>> regularTasks = tasks.stream()
                .filter(task -> !(task instanceof OnStartup))
                .toList();

        // RecurringTask implements both Task<?> and OnStartup
        // We need to pass them as varargs to satisfy the type constraints
        Object[] startupTasksArray = tasks.stream()
                .filter(OnStartup.class::isInstance)
                .toArray();

        LOG.debug("Regular tasks: {}, Startup tasks: {}", regularTasks.size(), startupTasksArray.length);

        // Note: Do NOT use registerShutdownHook() in Spring-managed environment.
        // SchedulerLifecycle handles graceful shutdown via Spring's lifecycle.
        // JVM shutdown hooks fire AFTER Spring closes the datasource, causing errors.
        var builder = Scheduler.create(txAwareDataSource, regularTasks)
                .threads(2)                              // Match application.yml: db-scheduler.threads
                .pollingInterval(Duration.ofSeconds(10)) // Match application.yml: db-scheduler.polling-interval
                .heartbeatInterval(Duration.ofMinutes(5)) // Match application.yml: db-scheduler.heartbeat-interval
                .shutdownMaxWait(Duration.ofSeconds(30)); // Match application.yml: db-scheduler.shutdown-max-wait

        // Add startup tasks using varargs method
        for (Object task : startupTasksArray) {
            if (task instanceof com.github.kagkarlsson.scheduler.task.helper.RecurringTask<?> recurringTask) {
                builder.startTasks(recurringTask);
            }
        }

        return builder.build();
    }

    private DataSource configureDataSource(DataSource existingDataSource) {
        if (existingDataSource instanceof TransactionAwareDataSourceProxy) {
            LOG.debug("Using already transaction-aware DataSource");
            return existingDataSource;
        }
        LOG.debug("Wrapping DataSource in TransactionAwareDataSourceProxy");
        return new TransactionAwareDataSourceProxy(existingDataSource);
    }

    /**
     * Manages the Scheduler lifecycle - starts on context refresh, stops on shutdown.
     */
    @Component
    static class SchedulerLifecycle implements AutoCloseable {

        private static final Logger LOG = LoggerFactory.getLogger(SchedulerLifecycle.class);

        private final Scheduler scheduler;

        SchedulerLifecycle(Scheduler scheduler) {
            this.scheduler = scheduler;
        }

        @EventListener(ContextRefreshedEvent.class)
        public void start() {
            if (!scheduler.getSchedulerState().isStarted()
                    && !scheduler.getSchedulerState().isShuttingDown()) {
                scheduler.start();
                LOG.info("db-scheduler started successfully");
            }
        }

        @Override
        public void close() {
            scheduler.stop();
        }
    }
}

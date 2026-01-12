package io.nextskip;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * NextSkip - Amateur Radio Activity Dashboard
 *
 * Main application class for the NextSkip platform, which provides
 * real-time situational awareness for amateur radio operators across
 * HF propagation, satellite passes, POTA/SOTA activations, contests,
 * and real-time band activity.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableCaching
@EnableScheduling
public class NextSkipApplication {

    public static void main(String[] args) {
        SpringApplication.run(NextSkipApplication.class, args);
    }
}

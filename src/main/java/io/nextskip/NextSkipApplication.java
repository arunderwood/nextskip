package io.nextskip;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * NextSkip - Amateur Radio Activity Dashboard
 *
 * Main application class for the NextSkip platform, which provides
 * real-time situational awareness for amateur radio operators across
 * HF propagation, satellite passes, POTA/SOTA activations, contests,
 * and real-time band activity.
 */
@SpringBootApplication
@EnableCaching
public class NextSkipApplication {

    public static void main(String[] args) {
        SpringApplication.run(NextSkipApplication.class, args);
    }
}

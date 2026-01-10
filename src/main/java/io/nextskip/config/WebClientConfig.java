package io.nextskip.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for WebClient and JSON processing.
 */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .defaultHeader(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate");
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}

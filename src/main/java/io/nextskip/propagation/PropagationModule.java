package io.nextskip.propagation;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Propagation Module configuration.
 *
 * This module handles:
 * - Solar indices (SFI, K-index, A-index) from NOAA SWPC
 * - Band conditions from HamQSL
 * - Propagation forecasting and analysis
 *
 * External Dependencies:
 * - NOAA Space Weather Prediction Center (SWPC)
 * - HamQSL.com XML API
 */
@Configuration
@ComponentScan(basePackages = "io.nextskip.propagation")
public class PropagationModule {
    // Module marker class - configuration is handled by component scanning
}

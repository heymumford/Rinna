/*
 * Logging utilities for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.util.logging.LogManager;

/**
 * Utility class to bridge Java Util Logging (JUL) to SLF4J.
 * This is needed for third-party libraries or legacy code that uses JUL.
 */
public final class LoggingBridge {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingBridge.class);
    private static boolean initialized = false;
    
    private LoggingBridge() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Initializes the JUL to SLF4J bridge.
     * This should be called once at application startup.
     */
    public static synchronized void installJulBridge() {
        if (initialized) {
            return;
        }
        
        try {
            // Remove existing handlers attached to JUL root logger
            LogManager.getLogManager().reset();
            
            // Install SLF4J bridge handler
            SLF4JBridgeHandler.install();
            
            initialized = true;
            LOGGER.info("JUL to SLF4J bridge installed successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to install JUL to SLF4J bridge", e);
        }
    }
}
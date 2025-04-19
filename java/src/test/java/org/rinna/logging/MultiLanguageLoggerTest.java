/*
 * Test class for the multi-language logging system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.logging;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Test class for MultiLanguageLogger.
 * These tests demonstrate how to use the multi-language logging system.
 */
@Tag("unit")
public class MultiLanguageLoggerTest {
    
    @BeforeAll
    public static void setUp() {
        // Initialize the multi-language logging system
        MultiLanguageLogger.initialize();
    }
    
    @Test
    void testJavaLogging() {
        // Get a logger
        MultiLanguageLogger logger = MultiLanguageLogger.getLogger(MultiLanguageLoggerTest.class);
        
        // Log messages at different levels
        logger.trace("This is a trace message from Java");
        logger.debug("This is a debug message from Java");
        logger.info("This is an info message from Java");
        logger.warn("This is a warning message from Java");
        logger.error("This is an error message from Java");
        
        // Log with context fields
        String requestId = UUID.randomUUID().toString();
        logger.withField("request_id", requestId)
              .info("Processing request");
        
        // Log with multiple context fields
        Map<String, String> fields = new HashMap<>();
        fields.put("user_id", "user-123");
        fields.put("action", "login");
        logger.withFields(fields)
              .info("User logged in");
        
        // Log with format and arguments
        logger.info("User {} performed action {}", "john.doe", "LOGIN");
    }
    
    @Test
    void testCrossLanguageLogging() {
        // Get a logger
        MultiLanguageLogger logger = MultiLanguageLogger.getLogger(MultiLanguageLoggerTest.class);
        
        // Log to Java
        logger.info("This message is logged through Java");
        
        // Log to Python
        logger.logPython("info", "This message is logged through Python");
        
        // Log to Bash
        logger.logBash("info", "This message is logged through Bash");
        
        // Log to Go
        logger.logGo("info", "This message is logged through Go");
        
        // Log with context fields across languages
        String traceId = UUID.randomUUID().toString();
        MultiLanguageLogger contextLogger = logger.withField("trace_id", traceId);
        
        contextLogger.info("Java log with trace ID");
        contextLogger.logPython("info", "Python log with trace ID");
        contextLogger.logBash("info", "Bash log with trace ID");
        contextLogger.logGo("info", "Go log with trace ID");
    }
}
package org.rinna.logging;

/**
 * Simple standalone test for the multi-language logging system.
 */
public class LoggingSystemTest {
    
    public static void main(String[] args) {
        // Initialize the multi-language logging system
        MultiLanguageLogger.initialize();
        
        // Get a logger
        MultiLanguageLogger logger = MultiLanguageLogger.getLogger(LoggingSystemTest.class);
        
        // Test Java logging
        logger.info("=== Testing Java Logging ===");
        logger.info("This is a Java info message");
        logger.debug("This is a Java debug message");
        logger.withField("request_id", "12345").info("Java message with request ID");
        
        // Test Bash logging
        logger.info("\n=== Testing Bash Logging ===");
        logger.logBash("INFO", "This is a Bash info message");
        logger.withField("request_id", "12345").logBash("INFO", "Bash message with request ID");
        
        // Test Python logging
        logger.info("\n=== Testing Python Logging ===");
        logger.logPython("INFO", "This is a Python info message");
        logger.withField("request_id", "12345").logPython("INFO", "Python message with request ID");
        
        // Test Go logging
        logger.info("\n=== Testing Go Logging ===");
        logger.logGo("INFO", "This is a Go info message");
        logger.withField("request_id", "12345").logGo("INFO", "Go message with request ID");
        
        // Test combined logging
        logger.info("\n=== Testing Combined Logging ===");
        String message = "Same message across all languages";
        
        logger.info(message); // Java
        logger.logBash("INFO", message); // Bash
        logger.logPython("INFO", message); // Python
        logger.logGo("INFO", message); // Go
        
        logger.info("All logging tests completed successfully!");
    }
}
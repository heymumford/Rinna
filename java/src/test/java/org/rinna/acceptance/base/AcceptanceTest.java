package org.rinna.acceptance.base;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * Base class for acceptance tests.
 * 
 * Acceptance tests:
 * - Verify the system meets business requirements
 * - Test complete end-to-end workflows
 * - Typically run more slowly and require more setup
 * - Run with a complete system for realistic testing
 */
@Tag("acceptance")
@Tag("bdd")
@Execution(ExecutionMode.SAME_THREAD)
@TestInstance(Lifecycle.PER_CLASS)
public abstract class AcceptanceTest extends BaseTest {
    
    /**
     * Sets up the complete system environment for acceptance testing.
     * This may include initializing the database, setting up test data, etc.
     */
    protected void setupAcceptanceEnvironment() {
        logger.info("Setting up acceptance test environment");
        // Implement environment setup here
    }
    
    /**
     * Cleans up the environment after acceptance testing.
     * This ensures that tests don't interfere with each other.
     */
    protected void cleanupAcceptanceEnvironment() {
        logger.info("Cleaning up acceptance test environment");
        // Implement environment cleanup here
    }
}

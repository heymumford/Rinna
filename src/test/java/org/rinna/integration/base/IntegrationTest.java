package org.rinna.integration.base;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * Base class for integration tests.
 * 
 * Integration tests:
 * - Test integration between modules or external dependencies
 * - May require more complex setup/teardown
 * - Should use real implementations where practical
 * - May need careful management of resources
 */
@Tag("integration")
@Execution(ExecutionMode.SAME_THREAD)
@TestInstance(Lifecycle.PER_CLASS)
public abstract class IntegrationTest extends BaseTest {
    
    /**
     * Sets up resources needed for integration testing.
     * Should be overridden by subclasses to set up specific integration points.
     */
    protected void setupIntegrationResources() {
        logger.info("Setting up integration test resources");
        // Override with specific setup code
    }
    
    /**
     * Cleans up resources after integration testing.
     * Should be overridden by subclasses to clean up specific integration points.
     */
    protected void cleanupIntegrationResources() {
        logger.info("Cleaning up integration test resources");
        // Override with specific cleanup code
    }
}

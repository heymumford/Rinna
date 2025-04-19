package org.rinna.component.base;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * Base class for component tests.
 * 
 * Component tests:
 * - Test behavior of components that work together
 * - Use real implementations for in-module dependencies
 * - Use mocks for external dependencies
 * - May be run in parallel but with more careful isolation
 */
@Tag("component")
@Execution(ExecutionMode.CONCURRENT)
@TestInstance(Lifecycle.PER_CLASS)
public abstract class ComponentTest extends BaseTest {
    
    /**
     * Sets up the component test context.
     * This should be used to create real instances of in-module dependencies.
     */
    protected void setupComponentContext() {
        logger.info("Setting up component test context");
        // Implement component-specific setup here
    }
}

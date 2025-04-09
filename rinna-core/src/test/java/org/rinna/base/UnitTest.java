package org.rinna.base;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * Base class for unit tests.
 * 
 * Unit tests:
 * - Test individual units of code in isolation
 * - Should be fast and focused
 * - Use mocks for dependencies
 * - Should run in parallel for speed
 */
@Tag("unit")
@Execution(ExecutionMode.CONCURRENT)
@TestInstance(Lifecycle.PER_METHOD)
public abstract class UnitTest extends BaseTest {
    
    /**
     * Creates a mock dependency for unit testing.
     * 
     * @param <T> Type of dependency to mock
     * @param clazz Class of dependency
     * @return Mocked instance
     */
    protected <T> T createMock(Class<T> clazz) {
        // In a real implementation this would create a mock using Mockito
        // For now just logging the intent
        logger.info("Creating mock of {}", clazz.getSimpleName());
        return null;
    }
}
package org.rinna.base;

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
 * - Test within module boundaries
 * - Use real implementations for in-module dependencies
 * - Mock external dependencies
 */
@Tag("component")
@Execution(ExecutionMode.CONCURRENT)
@TestInstance(Lifecycle.PER_METHOD)
public abstract class ComponentTest extends BaseTest {
}

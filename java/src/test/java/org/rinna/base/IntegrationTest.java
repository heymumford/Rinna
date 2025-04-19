package org.rinna.base;

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
 * - Use real implementations where practical
 * - May require more complex setup/teardown
 */
@Tag("integration")
@Execution(ExecutionMode.SAME_THREAD)
@TestInstance(Lifecycle.PER_CLASS)
public abstract class IntegrationTest extends BaseTest {
}

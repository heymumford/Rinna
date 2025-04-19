package org.rinna.base;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * Base class for performance tests.
 * 
 * Performance tests:
 * - Verify system performance meets requirements
 * - Test throughput, response time, resource usage
 * - Should be isolated from other tests
 */
@Tag("performance")
@Execution(ExecutionMode.SAME_THREAD)
@TestInstance(Lifecycle.PER_CLASS)
public abstract class PerformanceTest extends BaseTest {
}

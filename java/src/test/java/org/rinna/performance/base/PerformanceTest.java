package org.rinna.performance.base;

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
 * - Require special environment setup and measurement tools
 * - Should be run in isolation for accurate measurement
 */
@Tag("performance")
@Execution(ExecutionMode.SAME_THREAD)
@TestInstance(Lifecycle.PER_CLASS)
public abstract class PerformanceTest extends BaseTest {
    
    // Threshold values for performance expectations
    protected static final long ACCEPTABLE_RESPONSE_TIME_MS = 500;
    protected static final long ACCEPTABLE_THROUGHPUT_OPS = 100;
    
    /**
     * Sets up the performance testing environment.
     * This may include configuring profiling tools, warming up the JVM, etc.
     */
    protected void setupPerformanceEnvironment() {
        logger.info("Setting up performance test environment");
        // Implement environment setup here
    }
    
    /**
     * Runs a measurement cycle for performance testing.
     * 
     * @param iterations Number of iterations to run
     * @param runnable Code to measure
     * @return Average execution time in milliseconds
     */
    protected double measureAverageExecutionTime(int iterations, Runnable runnable) {
        // Warm-up phase
        for (int i = 0; i < 5; i++) {
            runnable.run();
        }
        
        // Measurement phase
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            runnable.run();
        }
        long endTime = System.nanoTime();
        
        double averageExecutionTimeMs = (endTime - startTime) / (iterations * 1_000_000.0);
        logger.info("Average execution time over {} iterations: {:.2f} ms", iterations, averageExecutionTimeMs);
        
        return averageExecutionTimeMs;
    }
}

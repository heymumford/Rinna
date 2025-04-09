package org.rinna.performance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Simple performance test with a Tag to demonstrate our test categorization system.
 */
@Tag("performance")
public class PerformanceTaggedTestPerformanceTest {

    @Test
    @DisplayName("Simple performance test to demonstrate tagging")
    void testSimplePerformanceTest() {
        // Just a simple test that always passes
        assertTrue(true, "This test should always pass");
    }
}

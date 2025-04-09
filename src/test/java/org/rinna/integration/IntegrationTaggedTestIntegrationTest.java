package org.rinna.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Simple integration test with a Tag to demonstrate our test categorization system.
 */
@Tag("integration")
public class IntegrationTaggedTestIntegrationTest {

    @Test
    @DisplayName("Simple integration test to demonstrate tagging")
    void testSimpleIntegrationTest() {
        // Just a simple test that always passes
        assertTrue(true, "This test should always pass");
    }
}

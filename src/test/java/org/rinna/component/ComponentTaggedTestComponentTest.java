package org.rinna.component;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Simple component test with a Tag to demonstrate our test categorization system.
 */
@Tag("component")
public class ComponentTaggedTestComponentTest {

    @Test
    @DisplayName("Simple component test to demonstrate tagging")
    void testSimpleComponentTest() {
        // Just a simple test that always passes
        assertTrue(true, "This test should always pass");
    }
}

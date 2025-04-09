package org.rinna.acceptance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Simple acceptance test with a Tag to demonstrate our test categorization system.
 */
@Tag("acceptance")
@Tag("bdd")
public class AcceptanceTaggedTestAcceptanceTest {

    @Test
    @DisplayName("Simple acceptance test to demonstrate tagging")
    void testSimpleAcceptanceTest() {
        // Just a simple test that always passes
        assertTrue(true, "This test should always pass");
    }
}

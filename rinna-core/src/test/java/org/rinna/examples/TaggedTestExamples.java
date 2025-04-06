package org.rinna.examples;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This class demonstrates the tagging approach for Rinna's test categories.
 * See docs/testing/TESTING_STRATEGY.md for full details.
 */
public class TaggedTestExamples {

    @Test
    @Tag("unit")
    @DisplayName("Example of a fast unit test")
    void unitTestExample() {
        // A unit test focuses on a single unit in isolation
        // Dependencies should be mocked or stubbed
        assertTrue(true, "Unit tests should run very quickly");
    }

    @Test
    @Tag("component")
    @DisplayName("Example of a component test")
    void componentTestExample() {
        // A component test validates the behavior of related classes
        // within a component/module
        assertTrue(true, "Component tests test related classes working together");
    }

    @Test
    @Tag("integration")
    @DisplayName("Example of an integration test")
    void integrationTestExample() {
        // An integration test verifies interactions between
        // different modules or with external dependencies
        assertTrue(true, "Integration tests verify module boundaries");
    }

    @Test
    @Tag("acceptance")
    @Tag("bdd")
    @DisplayName("Example of an acceptance test")
    void acceptanceTestExample() {
        // Acceptance tests verify the system meets business requirements
        // They often work through the public API or user interface
        assertTrue(true, "Acceptance tests validate end-to-end workflows");
    }

    @Test
    @Tag("performance")
    @DisplayName("Example of a performance test")
    void performanceTestExample() {
        // Performance tests verify the system meets performance requirements
        // They should run only before releases or in dedicated performance environments
        assertTrue(true, "Performance tests verify system throughput and response time");
    }
}
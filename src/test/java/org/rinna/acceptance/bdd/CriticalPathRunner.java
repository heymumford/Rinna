/*
 * BDD test runner for critical path feature
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.acceptance.bdd;

import org.junit.runner.RunWith;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;

/**
 * Cucumber test runner for critical path feature.
 */
@RunWith(Cucumber.class)
@CucumberOptions(
    features = "src/test/resources/features/critical-path.feature",
    glue = {"org.rinna.bdd"},
    plugin = {
        "pretty",
        "html:target/cucumber-reports/critical-path.html"
    },
    monochrome = true
)
@Tag("acceptance")
public class CriticalPathRunner {
    // This class is empty as it's just a test runner
}

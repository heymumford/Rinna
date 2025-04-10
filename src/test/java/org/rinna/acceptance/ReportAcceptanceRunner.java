/**
 * Acceptance test runner for Report features
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna.acceptance;

import org.junit.jupiter.api.Tag;
import org.junit.runner.RunWith;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;

/**
 * Runner for report acceptance tests.
 */
@RunWith(Cucumber.class)
@CucumberOptions(
    features = "src/test/resources/features/report-generation.feature",
    glue = {"org.rinna.acceptance.steps"},
    plugin = {
        "pretty",
        "html:target/cucumber-reports/report-acceptance",
        "json:target/cucumber-reports/report-acceptance.json"
    }
)
@Tag("acceptance")
public class ReportAcceptanceRunner {
    // This class acts as a runner for Cucumber tests
}
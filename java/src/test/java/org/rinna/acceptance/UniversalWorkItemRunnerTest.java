/**
 * Cucumber runner for Rinna Universal Work Item (RUWI) acceptance tests
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna.acceptance;

import org.junit.runner.RunWith;
import org.rinna.base.AcceptanceTest;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;

/**
 * Runner for the Rinna Universal Work Item (RUWI) acceptance tests.
 */
@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/features/universal-work-item.feature",
        glue = {"org.rinna.acceptance.steps"},
        plugin = {"pretty", "html:target/cucumber-reports/universal-work-item.html"},
        tags = "@ruwi"
)
public class UniversalWorkItemRunnerTest extends AcceptanceTest {
    // This class is intentionally empty. Its sole purpose is to serve as a runner for the Cucumber tests.
}
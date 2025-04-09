/*
 * Runner for statistics command BDD tests
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna.cli.bdd;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

/**
 * Runner for statistics command BDD tests.
 */
@RunWith(Cucumber.class)
@CucumberOptions(
    features = "src/test/resources/features/stats-commands.feature",
    glue = {"org.rinna.cli.bdd"},
    plugin = {
        "pretty",
        "html:target/cucumber-reports/stats-commands-report.html",
        "json:target/cucumber-reports/stats-commands-report.json"
    },
    monochrome = true
)
public class StatsCommandRunner {
    // This class serves as a runner for the statistics command BDD tests
}
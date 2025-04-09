/*
 * Runner for critical path command BDD tests
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
 * Runner for critical path command BDD tests.
 */
@RunWith(Cucumber.class)
@CucumberOptions(
    features = "src/test/resources/features/critical-path-commands.feature",
    glue = {"org.rinna.cli.bdd"},
    plugin = {
        "pretty",
        "html:target/cucumber-reports/critical-path-commands-report.html",
        "json:target/cucumber-reports/critical-path-commands-report.json"
    },
    monochrome = true
)
public class CriticalPathCommandRunner {
    // This class serves as a runner for the critical path command BDD tests
}
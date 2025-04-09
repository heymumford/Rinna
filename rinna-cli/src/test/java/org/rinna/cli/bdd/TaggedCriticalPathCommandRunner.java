/*
 * Tagged runner for critical path command BDD tests
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
 * Tagged runner for critical path command BDD tests.
 * This runner executes tests with specific tags configured in the maven profile.
 */
@RunWith(Cucumber.class)
@CucumberOptions(
    features = "src/test/resources/features/critical-path-commands.feature",
    glue = {"org.rinna.cli.bdd"},
    plugin = {
        "pretty",
        "html:target/cucumber-reports/critical-path-commands-tagged-report.html",
        "json:target/cucumber-reports/critical-path-commands-tagged-report.json"
    },
    monochrome = true,
    tags = "${cucumber.filter.tags}"
)
public class TaggedCriticalPathCommandRunner {
    // This class serves as a tagged runner for the critical path command BDD tests
}
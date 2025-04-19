/*
 * Tagged runner for item command BDD tests
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna.cli.bdd;

import org.junit.runner.RunWith;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;

/**
 * Tagged runner for item command BDD tests.
 * This runner executes tests with specific tags configured in the maven profile.
 */
@RunWith(Cucumber.class)
@CucumberOptions(
    features = "src/test/resources/features/item-commands.feature",
    glue = {"org.rinna.cli.bdd"},
    plugin = {
        "pretty",
        "html:target/cucumber-reports/item-commands-tagged-report.html",
        "json:target/cucumber-reports/item-commands-tagged-report.json"
    },
    monochrome = true,
    tags = "${cucumber.filter.tags}"
)
public class TaggedItemCommandRunner {
    // This class serves as a tagged runner for the item command BDD tests
}
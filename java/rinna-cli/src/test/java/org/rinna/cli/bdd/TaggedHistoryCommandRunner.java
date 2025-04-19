/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.bdd;

import org.junit.runner.RunWith;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;

/**
 * Tagged runner for history-commands.feature BDD tests,
 * allowing selective execution of tests with specific tags.
 */
@RunWith(Cucumber.class)
@CucumberOptions(
    features = "classpath:features/history-commands.feature",
    glue = {"org.rinna.cli.bdd"},
    plugin = {"pretty", "html:target/cucumber-reports/history-commands.html", 
              "json:target/cucumber-reports/history-commands.json"},
    tags = "@tag:history"
)
public class TaggedHistoryCommandRunner {
    // This class is intentionally empty. It's just a runner.
}
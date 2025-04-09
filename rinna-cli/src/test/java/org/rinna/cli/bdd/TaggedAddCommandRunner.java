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

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

/**
 * Runner for the Add Command BDD tests that are tagged with "tag:add".
 * This allows selective execution of these tests as part of a larger test suite.
 */
@RunWith(Cucumber.class)
@CucumberOptions(
    features = "classpath:features/add-commands.feature",
    glue = {"org.rinna.cli.bdd"},
    plugin = {"pretty", "html:target/cucumber-reports/add-commands-tagged.html"},
    tags = "@tag:add and not @Ignore"
)
public class TaggedAddCommandRunner {
    // This class is intentionally empty. It's just a runner.
}
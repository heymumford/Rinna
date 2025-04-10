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
 * Runner for the View Command BDD tests that are tagged with "tag:view".
 * This allows selective execution of these tests as part of a larger test suite.
 */
@RunWith(Cucumber.class)
@CucumberOptions(
    features = "classpath:features/view-commands.feature",
    glue = {"org.rinna.cli.bdd"},
    plugin = {"pretty", "html:target/cucumber-reports/view-commands-tagged.html"},
    tags = "@tag:view and not @Ignore"
)
public class TaggedViewCommandRunner {
    // This class is intentionally empty. It's just a runner.
}
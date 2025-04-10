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
 * Tagged runner for the Edit Command BDD tests.
 * This runner allows specific test selection using tags.
 */
@RunWith(Cucumber.class)
@CucumberOptions(
    features = "classpath:features/edit-commands.feature",
    glue = {"org.rinna.cli.bdd"},
    plugin = {"pretty", "html:target/cucumber-reports/edit-commands.html", 
              "json:target/cucumber-reports/edit-commands.json"},
    tags = "@tag:edit"
)
public class TaggedEditCommandRunner {
    // This class is intentionally empty. It's just a runner.
}

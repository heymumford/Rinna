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
 * Runner for the Edit Command BDD tests.
 */
@RunWith(Cucumber.class)
@CucumberOptions(
    features = "classpath:features/edit-commands.feature",
    glue = {"org.rinna.cli.bdd"},
    plugin = {"pretty", "html:target/cucumber-reports/edit-commands.html"},
    tags = "not @Ignore"
)
public class EditCommandRunner {
    // This class is intentionally empty. It's just a runner.
}

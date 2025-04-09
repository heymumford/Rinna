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

@RunWith(Cucumber.class)
@CucumberOptions(
    features = "src/test/resources/features/undo-commands.feature",
    glue = {"org.rinna.cli.bdd"},
    plugin = {"pretty", "html:target/cucumber-reports/undo-commands.html"},
    tags = "not @ignore"
)
public class UndoCommandRunner {
    // This class serves as an entry point for Cucumber tests
}
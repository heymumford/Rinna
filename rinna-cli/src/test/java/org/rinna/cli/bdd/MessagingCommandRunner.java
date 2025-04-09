/*
 * MessagingCommandRunner - Runner for messaging command BDD tests
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
 * Runner class for messaging command BDD tests
 */
@RunWith(Cucumber.class)
@CucumberOptions(
    features = "src/test/resources/features/messaging-commands.feature",
    glue = {"org.rinna.cli.bdd"},
    plugin = {"pretty", "html:target/cucumber-reports/messaging-commands.html"},
    tags = "@messaging",
    monochrome = true
)
public class MessagingCommandRunner {
    // This class is just a runner, so it doesn't need any code
}
/*
 * TaggedMessagingCommandRunner - Runner for tagged messaging command tests
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
 * Runner for selectively executing messaging command tests with tags.
 * This runner can be configured via system properties to execute specific tagged scenarios.
 * Example usage: mvn test -Dcucumber.filter.tags="@messaging and @smoke"
 */
@RunWith(Cucumber.class)
@CucumberOptions(
    features = "src/test/resources/features/messaging-commands.feature",
    glue = {"org.rinna.cli.bdd"},
    plugin = {
        "pretty", 
        "html:target/cucumber-reports/tagged-messaging-commands.html",
        "json:target/cucumber-reports/tagged-messaging-commands.json"
    },
    monochrome = true
)
public class TaggedMessagingCommandRunner {
    // This class is just a runner, so it doesn't need any code
}
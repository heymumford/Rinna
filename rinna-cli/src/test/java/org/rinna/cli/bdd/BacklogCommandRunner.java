package org.rinna.cli.bdd;

import org.junit.runner.RunWith;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;

/**
 * Runner for BacklogCommand BDD tests.
 */
@RunWith(Cucumber.class)
@CucumberOptions(
    features = "src/test/resources/features/backlog-commands.feature",
    glue = {"org.rinna.cli.bdd"},
    plugin = {"pretty", "summary", "html:target/cucumber-reports/backlog-commands.html"},
    monochrome = true,
    dryRun = false,
    strict = true
)
public class BacklogCommandRunner {
    // Runner class is empty
}
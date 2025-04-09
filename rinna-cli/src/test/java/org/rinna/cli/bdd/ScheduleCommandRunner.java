package org.rinna.cli.bdd;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

/**
 * Runner for ScheduleCommand BDD tests.
 */
@RunWith(Cucumber.class)
@CucumberOptions(
    features = "src/test/resources/features/schedule-commands.feature",
    glue = {"org.rinna.cli.bdd"},
    plugin = {"pretty", "summary", "html:target/cucumber-reports/schedule-commands.html"},
    monochrome = true,
    dryRun = false,
    strict = true
)
public class ScheduleCommandRunner {
    // Runner class is empty
}
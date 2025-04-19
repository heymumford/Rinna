package org.rinna.cli.bdd;

import org.junit.runner.RunWith;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;

/**
 * Runner for ScheduleCommand BDD tests with tags.
 * Use this runner to execute specific scenarios with tags.
 */
@RunWith(Cucumber.class)
@CucumberOptions(
    features = "src/test/resources/features/schedule-commands.feature",
    glue = {"org.rinna.cli.bdd"},
    tags = "@schedule",
    plugin = {"pretty", "summary", "html:target/cucumber-reports/tagged-schedule-commands.html"},
    monochrome = true,
    dryRun = false
)
public class TaggedScheduleCommandRunner {
    // Runner class is empty
}

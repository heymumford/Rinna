package org.rinna.cli.bdd;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

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
    dryRun = false,
    strict = true
)
public class TaggedScheduleCommandRunner {
    // Runner class is empty
}
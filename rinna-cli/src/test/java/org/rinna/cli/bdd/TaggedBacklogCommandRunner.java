package org.rinna.cli.bdd;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

/**
 * Runner for BacklogCommand BDD tests with tags.
 * Use this runner to execute specific scenarios with tags.
 */
@RunWith(Cucumber.class)
@CucumberOptions(
    features = "src/test/resources/features/backlog-commands.feature",
    glue = {"org.rinna.cli.bdd"},
    tags = "@backlog",
    plugin = {"pretty", "summary", "html:target/cucumber-reports/tagged-backlog-commands.html"},
    monochrome = true,
    dryRun = false,
    strict = true
)
public class TaggedBacklogCommandRunner {
    // Runner class is empty
}
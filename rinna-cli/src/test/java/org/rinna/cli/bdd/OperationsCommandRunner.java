package org.rinna.cli.bdd;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

/**
 * Runner for OperationsCommand BDD tests.
 */
@RunWith(Cucumber.class)
@CucumberOptions(
    features = "classpath:features/operations-commands.feature",
    glue = {"org.rinna.cli.bdd"},
    plugin = {"pretty", "html:target/cucumber-reports/operations-commands.html"},
    tags = "not @Ignore"
)
public class OperationsCommandRunner {
    // This class is intentionally empty, it's just a runner
}
package org.rinna.acceptance;

import org.junit.jupiter.api.Tag;
import org.junit.runner.RunWith;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;

/**
 * Runner for workflow acceptance tests.
 */
@RunWith(Cucumber.class)
@CucumberOptions(
    features = "src/test/resources/features/workflow.feature",
    glue = {"org.rinna.acceptance.steps"},
    plugin = {"pretty", "html:target/cucumber-reports/workflow-report.html"}
)
@Tag("acceptance")
@Tag("bdd")
public class WorkflowAcceptanceRunner {
    // This class serves as a runner for Cucumber BDD tests
}
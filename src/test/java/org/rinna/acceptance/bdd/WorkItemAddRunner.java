package org.rinna.acceptance.bdd;

import org.junit.runner.RunWith;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;

@RunWith(Cucumber.class)
@CucumberOptions(
    features = "src/test/resources/features/workitem-add.feature",
    glue = {"org.rinna.bdd"},
    plugin = {"pretty", "html:target/cucumber/workitem-add.html"}
)
@Tag("acceptance")
public class WorkItemAddRunner {
    // This class is intentionally empty. It's just a runner.
}

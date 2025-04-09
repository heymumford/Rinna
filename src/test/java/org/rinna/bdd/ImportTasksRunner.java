package org.rinna.bdd;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

/**
 * Runner for Import Tasks feature tests.
 */
@RunWith(Cucumber.class)
@CucumberOptions(
    features = "src/test/resources/features/import-tasks.feature",
    glue = {"org.rinna.bdd"},
    plugin = {
        "pretty",
        "html:target/cucumber/import-tasks",
        "json:target/cucumber/import-tasks.json"
    },
    tags = "@import or @bulk"
)
public class ImportTasksRunner {
    // Empty body - JUnit will run this with Cucumber
}
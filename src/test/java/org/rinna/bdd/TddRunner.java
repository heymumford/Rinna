package org.rinna.bdd;

import org.junit.runner.RunWith;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;

/**
 * Runner for TDD-related Cucumber features.
 */
@RunWith(Cucumber.class)
@CucumberOptions(
    features = {
        "src/test/resources/features/tdd-workflow.feature",
        "src/test/resources/features/tdd-engineering.feature"
    },
    glue = {"org.rinna.bdd"},
    plugin = {
        "pretty",
        "html:target/cucumber/tdd",
        "json:target/cucumber/tdd.json"
    },
    tags = "@tdd"
)
public class TddRunner {
    // Empty body - JUnit will run this with Cucumber
}
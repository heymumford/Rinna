package org.rinna.bdd;

import org.junit.runner.RunWith;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;

@RunWith(Cucumber.class)
@CucumberOptions(
    features = "src/test/resources/features/workitem-listing.feature",
    glue = {"org.rinna.bdd"},
    plugin = {"pretty", "html:target/cucumber/workitem-listing.html"}
)
public class WorkItemListingRunner {
    // This class is intentionally empty. It's just a runner.
}
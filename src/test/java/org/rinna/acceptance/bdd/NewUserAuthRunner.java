package org.rinna.acceptance.bdd;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
    features = "src/test/resources/features/new-user-auth.feature",
    glue = {"org.rinna.bdd"},
    plugin = {"pretty", "html:target/cucumber/new-user-auth.html"}
)
@Tag("acceptance")
public class NewUserAuthRunner {
    // This class is intentionally empty. It's just a runner.
}

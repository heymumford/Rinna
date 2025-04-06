package org.rinna.examples;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.cucumber.junit.platform.engine.Cucumber;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;

/**
 * Example of an acceptance test for Rinna.
 * This class uses Cucumber for BDD-style tests that define
 * user-level acceptance criteria.
 */
@Tag("acceptance")
@Tag("bdd")
@Cucumber
public class AcceptanceTestExample {

    // Test context to maintain state between steps
    private TestContext context = new TestContext();
    
    // Simple context class for this example
    static class TestContext {
        String workItemId;
        String workItemTitle;
        String errorMessage;
    }

    @Given("a new work item with title {string}")
    public void aNewWorkItemWithTitle(String title) {
        context.workItemTitle = title;
        // In a real implementation, you might initialize your test data here
    }

    @When("I create the work item")
    public void iCreateTheWorkItem() {
        try {
            // In a real implementation, you would call your service here
            if (context.workItemTitle != null && !context.workItemTitle.isEmpty()) {
                context.workItemId = "WI-" + System.currentTimeMillis();
            } else {
                throw new IllegalArgumentException("Work item title cannot be empty");
            }
        } catch (Exception e) {
            context.errorMessage = e.getMessage();
        }
    }

    @Then("the work item should be created successfully")
    public void theWorkItemShouldBeCreatedSuccessfully() {
        assert context.workItemId != null && context.workItemId.startsWith("WI-");
    }

    @Then("I should see an error about empty title")
    public void iShouldSeeAnErrorAboutEmptyTitle() {
        assert context.errorMessage != null && context.errorMessage.contains("empty");
    }
}

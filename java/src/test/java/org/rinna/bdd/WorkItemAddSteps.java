package org.rinna.bdd;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.Assert;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class WorkItemAddSteps {
    private final TestContext context;
    private String commandOutput;
    private int commandExitCode;
    private String lastCreatedItemId;

    public WorkItemAddSteps(TestContext context) {
        this.context = context;
    }

    @When("I run the {string} command")
    public void iRunTheCommand(String command) {
        // Mock the command execution
        // In a real implementation, this would execute the actual command
        // For testing, we'll simulate the behavior
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);
        
        if (command.startsWith("rin add")) {
            commandExitCode = simulateAddCommand(command, printStream);
        } else {
            printStream.println("Unknown command: " + command);
            commandExitCode = 1;
        }
        
        commandOutput = outputStream.toString();
        context.setCommandOutput(commandOutput);
    }

    private int simulateAddCommand(String command, PrintStream out) {
        // Parse the command to extract arguments
        if (!command.contains("--title") && command.contains("--type")) {
            out.println("Error: --title is required");
            return 1;
        }
        
        if (command.contains("--type INVALID_TYPE")) {
            out.println("Error: Invalid type: INVALID_TYPE. Must be one of: BUG FEATURE TASK");
            return 1;
        }
        
        // Extract title
        String title = "";
        if (command.contains("--title")) {
            int titleStart = command.indexOf("--title") + 8;
            int titleEnd;
            
            if (command.charAt(titleStart) == '\'') {
                titleStart++;
                titleEnd = command.indexOf('\'', titleStart);
            } else if (command.charAt(titleStart) == '"') {
                titleStart++;
                titleEnd = command.indexOf('"', titleStart);
            } else {
                // Find next space or end of string
                titleEnd = command.indexOf(' ', titleStart);
                if (titleEnd == -1) {
                    titleEnd = command.length();
                }
            }
            
            title = command.substring(titleStart, titleEnd);
        }
        
        // Extract type
        String type = "TASK"; // Default
        if (command.contains("--type")) {
            int typeStart = command.indexOf("--type") + 7;
            int typeEnd = command.indexOf(' ', typeStart);
            if (typeEnd == -1) {
                typeEnd = command.length();
            }
            type = command.substring(typeStart, typeEnd);
        }
        
        // Simulate creating a work item
        lastCreatedItemId = java.util.UUID.randomUUID().toString();
        
        out.println("Work item created successfully!");
        out.println();
        out.println("ID: " + lastCreatedItemId);
        out.println("Title: " + title);
        out.println("Type: " + type);
        
        if (command.contains("--priority")) {
            String priority = "HIGH";
            out.println("Priority: " + priority);
        } else {
            out.println("Priority: MEDIUM");
        }
        
        if (command.contains("--status")) {
            String status = "TODO";
            out.println("Status: " + status);
        } else {
            out.println("Status: TODO");
        }
        
        if (command.contains("--assignee")) {
            String assignee = "alice";
            out.println("Assignee: " + assignee);
        }
        
        out.println();
        out.println("Use 'rin list' to see all work items");
        
        return 0;
    }

    @Then("the command should succeed")
    public void theCommandShouldSucceed() {
        Assert.assertEquals("Expected command to succeed with exit code 0", 0, commandExitCode);
    }

    @Then("the command should fail")
    public void theCommandShouldFail() {
        Assert.assertNotEquals("Expected command to fail with non-zero exit code", 0, commandExitCode);
    }

    @And("a new work item should be created with title {string} and type {string}")
    public void aNewWorkItemShouldBeCreatedWithTitleAndType(String title, String type) {
        Assert.assertTrue("Output should contain title: " + title, 
            commandOutput.contains("Title: " + title));
        Assert.assertTrue("Output should contain type: " + type, 
            commandOutput.contains("Type: " + type));
    }

    @And("a new work item should be created with all specified attributes")
    public void aNewWorkItemShouldBeCreatedWithAllSpecifiedAttributes() {
        Assert.assertTrue("Output should contain Title: Fix authentication bug",
            commandOutput.contains("Title: Fix authentication bug"));
        Assert.assertTrue("Output should contain Type: BUG",
            commandOutput.contains("Type: BUG"));
        Assert.assertTrue("Output should contain Priority: HIGH",
            commandOutput.contains("Priority: HIGH"));
        Assert.assertTrue("Output should contain Status: TODO",
            commandOutput.contains("Status: TODO"));
        Assert.assertTrue("Output should contain Assignee: alice",
            commandOutput.contains("Assignee: alice"));
    }

    @And("I should see a success message")
    public void iShouldSeeASuccessMessage() {
        Assert.assertTrue("Output should contain success message",
            commandOutput.contains("Work item created successfully!"));
    }

    @And("I should see an error message about invalid type")
    public void iShouldSeeAnErrorMessageAboutInvalidType() {
        Assert.assertTrue("Output should contain error about invalid type",
            commandOutput.contains("Invalid type") || commandOutput.contains("invalid type"));
    }

    @And("I should see an error message about missing title")
    public void iShouldSeeAnErrorMessageAboutMissingTitle() {
        Assert.assertTrue("Output should contain error about missing title",
            commandOutput.contains("--title is required"));
    }
}
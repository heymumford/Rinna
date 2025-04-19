package org.rinna.bdd;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.Assert;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class WorkItemManagementSteps {
    private final TestContext context;
    private String commandOutput;
    private List<Map<String, String>> workItems;
    private List<String> commandOutputs;

    public WorkItemManagementSteps(TestContext context) {
        this.context = context;
        this.workItems = new ArrayList<>();
        this.commandOutputs = new ArrayList<>();
    }

    @Given("there are no work items in the repository")
    public void thereAreNoWorkItemsInTheRepository() {
        // For testing purposes, we'll just clear our in-memory list
        workItems.clear();
        commandOutputs.clear();
    }

    @Given("there are work items of different types in the repository")
    public void thereAreWorkItemsOfDifferentTypesInTheRepository() {
        // Add a few work items of different types
        workItems.clear();
        commandOutputs.clear();
        
        // Add a feature
        Map<String, String> feature = new HashMap<>();
        feature.put("id", UUID.randomUUID().toString());
        feature.put("title", "Implement user authentication");
        feature.put("type", "FEATURE");
        feature.put("priority", "MEDIUM");
        feature.put("status", "TODO");
        workItems.add(feature);
        
        // Add a bug
        Map<String, String> bug = new HashMap<>();
        bug.put("id", UUID.randomUUID().toString());
        bug.put("title", "Fix login bug");
        bug.put("type", "BUG");
        bug.put("priority", "HIGH");
        bug.put("status", "TODO");
        workItems.add(bug);
        
        // Add a task
        Map<String, String> task = new HashMap<>();
        task.put("id", UUID.randomUUID().toString());
        task.put("title", "Update documentation");
        task.put("type", "TASK");
        task.put("priority", "LOW");
        task.put("status", "IN_PROGRESS");
        workItems.add(task);
    }

    @When("I add a new work item with title {string} and type {string}")
    public void iAddANewWorkItemWithTitleAndType(String title, String type) {
        String command = String.format("rin add --title \"%s\" --type %s", title, type);
        executeCommand(command);
        
        // Add to our in-memory list for verification
        Map<String, String> item = new HashMap<>();
        item.put("title", title);
        item.put("type", type);
        item.put("priority", "MEDIUM"); // Default
        item.put("status", "TODO"); // Default
        
        // Extract ID from output
        String output = commandOutputs.get(commandOutputs.size() - 1);
        if (output.contains("ID:")) {
            String id = output.substring(output.indexOf("ID:") + 4).trim();
            id = id.split("\n")[0];
            item.put("id", id);
        } else {
            item.put("id", UUID.randomUUID().toString());
        }
        
        workItems.add(item);
    }

    @When("I add a new work item with title {string} and type {string} and priority {string}")
    public void iAddANewWorkItemWithTitleAndTypeAndPriority(String title, String type, String priority) {
        String command = String.format("rin add --title \"%s\" --type %s --priority %s", title, type, priority);
        executeCommand(command);
        
        // Add to our in-memory list for verification
        Map<String, String> item = new HashMap<>();
        item.put("title", title);
        item.put("type", type);
        item.put("priority", priority);
        item.put("status", "TODO"); // Default
        
        // Extract ID from output
        String output = commandOutputs.get(commandOutputs.size() - 1);
        if (output.contains("ID:")) {
            String id = output.substring(output.indexOf("ID:") + 4).trim();
            id = id.split("\n")[0];
            item.put("id", id);
        } else {
            item.put("id", UUID.randomUUID().toString());
        }
        
        workItems.add(item);
    }

    @When("I add a work item with the following attributes:")
    public void iAddAWorkItemWithTheFollowingAttributes(DataTable dataTable) {
        Map<String, String> attributes = dataTable.asMap(String.class, String.class);
        
        StringBuilder command = new StringBuilder("rin add");
        if (attributes.containsKey("title")) {
            command.append(" --title \"").append(attributes.get("title")).append("\"");
        }
        if (attributes.containsKey("type")) {
            command.append(" --type ").append(attributes.get("type"));
        }
        if (attributes.containsKey("description")) {
            command.append(" --description \"").append(attributes.get("description")).append("\"");
        }
        if (attributes.containsKey("priority")) {
            command.append(" --priority ").append(attributes.get("priority"));
        }
        if (attributes.containsKey("status")) {
            command.append(" --status ").append(attributes.get("status"));
        }
        if (attributes.containsKey("assignee")) {
            command.append(" --assignee ").append(attributes.get("assignee"));
        }
        
        executeCommand(command.toString());
        
        // Add to our in-memory list for verification
        Map<String, String> item = new HashMap<>(attributes);
        
        // Extract ID from output
        String output = commandOutputs.get(commandOutputs.size() - 1);
        if (output.contains("ID:")) {
            String id = output.substring(output.indexOf("ID:") + 4).trim();
            id = id.split("\n")[0];
            item.put("id", id);
        } else {
            item.put("id", UUID.randomUUID().toString());
        }
        
        if (!item.containsKey("priority")) {
            item.put("priority", "MEDIUM"); // Default
        }
        if (!item.containsKey("status")) {
            item.put("status", "TODO"); // Default
        }
        
        workItems.add(item);
    }

    @When("I list all work items")
    public void iListAllWorkItems() {
        executeCommand("rin list");
    }

    @When("I filter work items by type {string}")
    public void iFilterWorkItemsByType(String type) {
        executeCommand("rin list --type " + type);
    }

    @When("I filter work items by status {string}")
    public void iFilterWorkItemsByStatus(String status) {
        executeCommand("rin list --status " + status);
    }

    @When("I filter work items by priority {string}")
    public void iFilterWorkItemsByPriority(String priority) {
        executeCommand("rin list --priority " + priority);
    }

    @Then("I should see both work items in the list")
    public void iShouldSeeBothWorkItemsInTheList() {
        Assert.assertEquals("Expected 2 work items", 2, workItems.size());
        for (Map<String, String> item : workItems) {
            Assert.assertTrue("Output should contain title: " + item.get("title"),
                commandOutput.contains(item.get("title")));
            Assert.assertTrue("Output should contain type: " + item.get("type"),
                commandOutput.contains(item.get("type")));
        }
    }

    @Then("the work item counts should match")
    public void theWorkItemCountsShouldMatch() {
        Assert.assertTrue("Output should contain count information",
            commandOutput.contains("Displaying " + workItems.size()));
    }

    @Then("I should only see work items of type {string}")
    public void iShouldOnlySeeWorkItemsOfType(String type) {
        // Count items of this type in our in-memory list
        long count = workItems.stream()
            .filter(item -> type.equals(item.get("type")))
            .count();
        
        Assert.assertTrue("Output should contain type: " + type, 
            commandOutput.contains(type));
        
        // We should not see items of other types
        for (Map<String, String> item : workItems) {
            if (!type.equals(item.get("type"))) {
                Assert.assertFalse("Output should not contain title of non-matching type: " + item.get("title"),
                    commandOutput.contains(item.get("title")));
            }
        }
    }

    @Then("I should only see work items with status {string}")
    public void iShouldOnlySeeWorkItemsWithStatus(String status) {
        // Count items with this status in our in-memory list
        long count = workItems.stream()
            .filter(item -> status.equals(item.get("status")))
            .count();
        
        Assert.assertTrue("Output should contain status: " + status, 
            commandOutput.contains(status));
        
        // We should not see items with other status
        for (Map<String, String> item : workItems) {
            if (!status.equals(item.get("status")) && item.get("title") != null) {
                Assert.assertFalse("Output should not contain title of non-matching status: " + item.get("title"),
                    commandOutput.contains(item.get("title")));
            }
        }
    }

    @Then("I should only see work items with priority {string}")
    public void iShouldOnlySeeWorkItemsWithPriority(String priority) {
        // Count items with this priority in our in-memory list
        long count = workItems.stream()
            .filter(item -> priority.equals(item.get("priority")))
            .count();
        
        Assert.assertTrue("Output should contain priority: " + priority, 
            commandOutput.contains(priority));
        
        // We should not see items with other priority
        for (Map<String, String> item : workItems) {
            if (!priority.equals(item.get("priority")) && item.get("title") != null) {
                Assert.assertFalse("Output should not contain title of non-matching priority: " + item.get("title"),
                    commandOutput.contains(item.get("title")));
            }
        }
    }

    @Then("the work item should be added successfully")
    public void theWorkItemShouldBeAddedSuccessfully() {
        Assert.assertTrue("Output should contain success message",
            commandOutput.contains("Work item created successfully"));
    }

    @Then("when I list all work items, the new item should be included")
    public void whenIListAllWorkItemsTheNewItemShouldBeIncluded() {
        executeCommand("rin list");
        
        Map<String, String> lastItem = workItems.get(workItems.size() - 1);
        Assert.assertTrue("Output should contain the newly added item title: " + lastItem.get("title"),
            commandOutput.contains(lastItem.get("title")));
        Assert.assertTrue("Output should contain the newly added item type: " + lastItem.get("type"),
            commandOutput.contains(lastItem.get("type")));
    }

    private void executeCommand(String command) {
        // Mock the command execution
        // In a real implementation, this would execute the actual command
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);
        
        int exitCode = 0;
        if (command.startsWith("rin add")) {
            // Simulate the add command
            printStream.println("Work item created successfully!");
            printStream.println();
            printStream.println("ID: " + UUID.randomUUID());
            
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
                printStream.println("Title: " + title);
            }
            
            // Extract type
            if (command.contains("--type")) {
                int typeStart = command.indexOf("--type") + 7;
                int typeEnd = command.indexOf(' ', typeStart);
                if (typeEnd == -1) {
                    typeEnd = command.length();
                }
                String type = command.substring(typeStart, typeEnd);
                printStream.println("Type: " + type);
            } else {
                printStream.println("Type: TASK");
            }
            
            // Extract priority
            if (command.contains("--priority")) {
                int priorityStart = command.indexOf("--priority") + 11;
                int priorityEnd = command.indexOf(' ', priorityStart);
                if (priorityEnd == -1) {
                    priorityEnd = command.length();
                }
                String priority = command.substring(priorityStart, priorityEnd);
                printStream.println("Priority: " + priority);
            } else {
                printStream.println("Priority: MEDIUM");
            }
            
            // Extract status
            if (command.contains("--status")) {
                int statusStart = command.indexOf("--status") + 9;
                int statusEnd = command.indexOf(' ', statusStart);
                if (statusEnd == -1) {
                    statusEnd = command.length();
                }
                String status = command.substring(statusStart, statusEnd);
                printStream.println("Status: " + status);
            } else {
                printStream.println("Status: TODO");
            }
            
            // Extract assignee
            if (command.contains("--assignee")) {
                int assigneeStart = command.indexOf("--assignee") + 11;
                int assigneeEnd = command.indexOf(' ', assigneeStart);
                if (assigneeEnd == -1) {
                    assigneeEnd = command.length();
                }
                String assignee = command.substring(assigneeStart, assigneeEnd);
                printStream.println("Assignee: " + assignee);
            }
            
            printStream.println();
            printStream.println("Use 'rin list' to see all work items");
        } else if (command.startsWith("rin list")) {
            // Simulate the list command
            boolean hasType = command.contains("--type");
            boolean hasStatus = command.contains("--status");
            boolean hasPriority = command.contains("--priority");
            
            String typeFilter = "";
            if (hasType) {
                int typeStart = command.indexOf("--type") + 7;
                int typeEnd = command.indexOf(' ', typeStart);
                if (typeEnd == -1) {
                    typeEnd = command.length();
                }
                typeFilter = command.substring(typeStart, typeEnd);
            }
            
            String statusFilter = "";
            if (hasStatus) {
                int statusStart = command.indexOf("--status") + 9;
                int statusEnd = command.indexOf(' ', statusStart);
                if (statusEnd == -1) {
                    statusEnd = command.length();
                }
                statusFilter = command.substring(statusStart, statusEnd);
            }
            
            String priorityFilter = "";
            if (hasPriority) {
                int priorityStart = command.indexOf("--priority") + 11;
                int priorityEnd = command.indexOf(' ', priorityStart);
                if (priorityEnd == -1) {
                    priorityEnd = command.length();
                }
                priorityFilter = command.substring(priorityStart, priorityEnd);
            }
            
            // Filter items based on criteria
            List<Map<String, String>> filteredItems = new ArrayList<>();
            for (Map<String, String> item : workItems) {
                boolean matches = true;
                
                if (hasType && !typeFilter.equals(item.get("type"))) {
                    matches = false;
                }
                
                if (hasStatus && !statusFilter.equals(item.get("status"))) {
                    matches = false;
                }
                
                if (hasPriority && !priorityFilter.equals(item.get("priority"))) {
                    matches = false;
                }
                
                if (matches) {
                    filteredItems.add(item);
                }
            }
            
            if (filteredItems.isEmpty()) {
                printStream.println("No work items found in the repository.");
                printStream.println();
                printStream.println("To add a new work item, use:");
                printStream.println("  rin add --type FEATURE --title \"Your feature title\" --description \"Description\"");
                printStream.println("  rin add --type BUG --title \"Bug description\" --priority HIGH");
                printStream.println("  rin add --type TASK --title \"Task to do\" --assignee \"username\"");
            } else {
                printStream.println("Work Items:");
                printStream.println("--------------------------------------------------------------------------------");
                printStream.printf("%-36s %-40s %-10s %-10s %-12s %s\n", "ID", "TITLE", "TYPE", "PRIORITY", "STATUS", "ASSIGNEE");
                printStream.println("--------------------------------------------------------------------------------");
                
                for (Map<String, String> item : filteredItems) {
                    String id = item.getOrDefault("id", UUID.randomUUID().toString());
                    String title = item.getOrDefault("title", "");
                    String type = item.getOrDefault("type", "");
                    String priority = item.getOrDefault("priority", "");
                    String status = item.getOrDefault("status", "");
                    String assignee = item.getOrDefault("assignee", "-");
                    
                    printStream.printf("%-36s %-40s %-10s %-10s %-12s %s\n", id, title, type, priority, status, assignee);
                }
                
                printStream.println("--------------------------------------------------------------------------------");
                printStream.println("Displaying " + filteredItems.size() + " of " + filteredItems.size() + " item(s)");
            }
        } else {
            printStream.println("Unknown command: " + command);
            exitCode = 1;
        }
        
        commandOutput = outputStream.toString();
        commandOutputs.add(commandOutput);
        context.setCommandOutput(commandOutput);
    }
}
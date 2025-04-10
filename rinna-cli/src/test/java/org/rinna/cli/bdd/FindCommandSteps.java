package org.rinna.cli.bdd;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.rinna.cli.command.FindCommand;
import org.rinna.cli.service.*;
import org.rinna.domain.model.*;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Step definitions for find-commands.feature.
 */
public class FindCommandSteps {

    private AutoCloseable closeable;
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final List<WorkItem> workItems = new ArrayList<>();
    private FindCommand findCommand;
    private int exitCode;

    @Mock
    private ServiceManager mockServiceManager;
    
    @Mock
    private ItemService mockItemService;
    
    @Mock
    private MetadataService mockMetadataService;

    @Mock
    private AuthorizationService mockAuthorizationService;

    @Before
    public void setup() {
        closeable = MockitoAnnotations.openMocks(this);
        System.setOut(new PrintStream(outputStream));
        
        when(mockServiceManager.getItemService()).thenReturn(mockItemService);
        when(mockServiceManager.getMetadataService()).thenReturn(mockMetadataService);
        when(mockServiceManager.getAuthorizationService()).thenReturn(mockAuthorizationService);
        when(mockAuthorizationService.hasPermission(anyString())).thenReturn(true);
    }

    @After
    public void tearDown() throws Exception {
        System.setOut(originalOut);
        if (closeable != null) {
            closeable.close();
        }
    }

    @Given("the user is authenticated")
    public void theUserIsAuthenticated() {
        when(mockAuthorizationService.isAuthenticated()).thenReturn(true);
    }

    @Given("the following work items exist in the system:")
    public void theFollowingWorkItemsExistInTheSystem(DataTable dataTable) {
        List<List<String>> rows = dataTable.asLists();
        
        // Skip header row
        for (int i = 1; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            String id = row.get(0);
            String type = row.get(1);
            String title = row.get(2);
            String state = row.get(3);
            String priority = row.get(4);
            String assignee = row.get(5);
            String createdDate = row.get(6);
            String modifiedDate = row.get(7);
            
            DefaultWorkItem workItem = new DefaultWorkItem(id, title);
            workItem.setType(WorkItemType.valueOf(type.toUpperCase()));
            workItem.setState(WorkflowState.valueOf(state.replaceAll("\\s+", "_").toUpperCase()));
            workItem.setPriority(Priority.valueOf(priority.toUpperCase()));
            workItem.setAssignee(assignee);
            
            // Parse dates
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDateTime created = LocalDateTime.parse(createdDate, formatter).atStartOfDay();
            LocalDateTime modified = LocalDateTime.parse(modifiedDate, formatter).atStartOfDay();
            
            // Set creation and modification dates
            workItem.setCreatedAt(created.atZone(ZoneId.systemDefault()).toInstant());
            workItem.setUpdatedAt(modified.atZone(ZoneId.systemDefault()).toInstant());
            
            workItems.add(workItem);
        }
        
        when(mockItemService.findItems(any())).thenReturn(workItems);
    }

    @When("the user executes find command with name pattern {string}")
    public void theUserExecutesFindCommandWithNamePattern(String pattern) {
        findCommand = new FindCommand(mockServiceManager);
        findCommand.setNamePattern(pattern);
        exitCode = findCommand.call();
    }

    @When("the user executes find command with type {string}")
    public void theUserExecutesFindCommandWithType(String type) {
        findCommand = new FindCommand(mockServiceManager);
        findCommand.setType(type);
        exitCode = findCommand.call();
    }

    @When("the user executes find command with state {string}")
    public void theUserExecutesFindCommandWithState(String state) {
        findCommand = new FindCommand(mockServiceManager);
        findCommand.setState(state);
        exitCode = findCommand.call();
    }

    @When("the user executes find command with priority {string}")
    public void theUserExecutesFindCommandWithPriority(String priority) {
        findCommand = new FindCommand(mockServiceManager);
        findCommand.setPriority(priority);
        exitCode = findCommand.call();
    }

    @When("the user executes find command with assignee {string}")
    public void theUserExecutesFindCommandWithAssignee(String assignee) {
        findCommand = new FindCommand(mockServiceManager);
        findCommand.setAssignee(assignee);
        exitCode = findCommand.call();
    }

    @When("the user executes find command with created after {string}")
    public void theUserExecutesFindCommandWithCreatedAfter(String dateString) {
        findCommand = new FindCommand(mockServiceManager);
        findCommand.setCreatedAfter(dateString);
        exitCode = findCommand.call();
    }

    @When("the user executes find command with updated before {string}")
    public void theUserExecutesFindCommandWithUpdatedBefore(String dateString) {
        findCommand = new FindCommand(mockServiceManager);
        findCommand.setUpdatedBefore(dateString);
        exitCode = findCommand.call();
    }

    @When("the user executes find command with type {string} and state {string}")
    public void theUserExecutesFindCommandWithTypeAndState(String type, String state) {
        findCommand = new FindCommand(mockServiceManager);
        findCommand.setType(type);
        findCommand.setState(state);
        exitCode = findCommand.call();
    }

    @When("the user executes find command with name pattern {string} and JSON output")
    public void theUserExecutesFindCommandWithNamePatternAndJsonOutput(String pattern) {
        findCommand = new FindCommand(mockServiceManager);
        findCommand.setNamePattern(pattern);
        findCommand.setJsonOutput(true);
        exitCode = findCommand.call();
    }

    @When("the user executes find command with type {string} and count only option")
    public void theUserExecutesFindCommandWithTypeAndCountOnlyOption(String type) {
        findCommand = new FindCommand(mockServiceManager);
        findCommand.setType(type);
        findCommand.setCountOnly(true);
        exitCode = findCommand.call();
    }

    @When("the user executes find command with help option")
    public void theUserExecutesFindCommandWithHelpOption() {
        findCommand = new FindCommand(mockServiceManager);
        findCommand.setHelp(true);
        exitCode = findCommand.call();
    }

    @Then("the command should succeed")
    public void theCommandShouldSucceed() {
        Assertions.assertEquals(0, exitCode);
    }

    @Then("the command should fail")
    public void theCommandShouldFail() {
        Assertions.assertNotEquals(0, exitCode);
    }

    @Then("the output should contain {string}")
    public void theOutputShouldContain(String expectedText) {
        String output = outputStream.toString();
        Assertions.assertTrue(output.contains(expectedText), 
                "Expected output to contain: " + expectedText + " but got: " + output);
    }

    @Then("the output should contain {string} and {string}")
    public void theOutputShouldContainAnd(String text1, String text2) {
        String output = outputStream.toString();
        Assertions.assertTrue(output.contains(text1), 
                "Expected output to contain: " + text1 + " but got: " + output);
        Assertions.assertTrue(output.contains(text2), 
                "Expected output to contain: " + text2 + " but got: " + output);
    }
    
    @Then("the output should contain valid JSON with items {string} and {string}")
    public void theOutputShouldContainValidJsonWithItems(String item1, String item2) {
        String output = outputStream.toString();
        Assertions.assertTrue(output.contains(item1), 
                "Expected output to contain: " + item1 + " but got: " + output);
        Assertions.assertTrue(output.contains(item2), 
                "Expected output to contain: " + item2 + " but got: " + output);
        // Simple JSON validation - contains array brackets
        Assertions.assertTrue(output.contains("[") && output.contains("]"), 
                "Expected output to be JSON format but got: " + output);
    }

    @Then("the output should not contain {string}")
    public void theOutputShouldNotContain(String text) {
        String output = outputStream.toString();
        Assertions.assertFalse(output.contains(text), 
                "Expected output to not contain: " + text + " but got: " + output);
    }

    @Then("the output should not contain {string}, {string}, and {string}")
    public void theOutputShouldNotContainMultiple(String text1, String text2, String text3) {
        String output = outputStream.toString();
        Assertions.assertFalse(output.contains(text1), 
                "Expected output to not contain: " + text1 + " but got: " + output);
        Assertions.assertFalse(output.contains(text2), 
                "Expected output to not contain: " + text2 + " but got: " + output);
        Assertions.assertFalse(output.contains(text3), 
                "Expected output to not contain: " + text3 + " but got: " + output);
    }

    @Then("the output should not contain {string} and {string}")
    public void theOutputShouldNotContainTwo(String text1, String text2) {
        String output = outputStream.toString();
        Assertions.assertFalse(output.contains(text1), 
                "Expected output to not contain: " + text1 + " but got: " + output);
        Assertions.assertFalse(output.contains(text2), 
                "Expected output to not contain: " + text2 + " but got: " + output);
    }

    @Then("the output should not contain {string}, {string}, {string}, and {string}")
    public void theOutputShouldNotContainFour(String text1, String text2, String text3, String text4) {
        String output = outputStream.toString();
        Assertions.assertFalse(output.contains(text1), 
                "Expected output to not contain: " + text1 + " but got: " + output);
        Assertions.assertFalse(output.contains(text2), 
                "Expected output to not contain: " + text2 + " but got: " + output);
        Assertions.assertFalse(output.contains(text3), 
                "Expected output to not contain: " + text3 + " but got: " + output);
        Assertions.assertFalse(output.contains(text4), 
                "Expected output to not contain: " + text4 + " but got: " + output);
    }

    @Then("the output should not contain item details")
    public void theOutputShouldNotContainItemDetails() {
        String output = outputStream.toString();
        Assertions.assertFalse(output.contains("Title:"), 
                "Expected output to not contain item details, but found 'Title:' in: " + output);
        Assertions.assertFalse(output.contains("State:"), 
                "Expected output to not contain item details, but found 'State:' in: " + output);
    }

    @Then("the output should contain an error message about invalid date format")
    public void theOutputShouldContainAnErrorMessageAboutInvalidDateFormat() {
        String output = outputStream.toString();
        Assertions.assertTrue(output.contains("Invalid date format") || 
                              output.contains("date format") || 
                              output.contains("parse") || 
                              output.contains("format"), 
                "Expected error about invalid date format in: " + output);
    }

    @Then("the output should contain an error message about invalid priority")
    public void theOutputShouldContainAnErrorMessageAboutInvalidPriority() {
        String output = outputStream.toString();
        Assertions.assertTrue(output.contains("Invalid priority") || 
                              output.contains("valid priority") || 
                              output.contains("Unknown priority"), 
                "Expected error about invalid priority in: " + output);
    }

    @Then("the output should contain usage instructions for the find command")
    public void theOutputShouldContainUsageInstructionsForTheFindCommand() {
        String output = outputStream.toString();
        Assertions.assertTrue(output.contains("Usage:") && 
                              output.contains("find") && 
                              output.contains("Options:"), 
                "Expected usage instructions in: " + output);
    }

    @Then("the output should list all available options")
    public void theOutputShouldListAllAvailableOptions() {
        String output = outputStream.toString();
        
        // Check for important options
        List<String> expectedOptions = Arrays.asList(
            "--name", "--type", "--state", "--priority", "--assignee",
            "--created-after", "--updated-before", "--json", "--count-only", "--help"
        );
        
        for (String option : expectedOptions) {
            Assertions.assertTrue(output.contains(option), 
                    "Expected output to contain option: " + option + " but got: " + output);
        }
    }

    @Then("the MetadataService should record the operation")
    public void theMetadataServiceShouldRecordTheOperation() {
        verify(mockMetadataService, times(1)).recordOperation(anyString(), anyString(), any());
    }

    @Then("the MetadataService should not record any operation")
    public void theMetadataServiceShouldNotRecordAnyOperation() {
        verify(mockMetadataService, never()).recordOperation(anyString(), anyString(), any());
    }
}
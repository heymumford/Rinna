package org.rinna.cli.bdd;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.rinna.cli.command.OperationsCommand;
import org.rinna.cli.service.AuthorizationService;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.domain.model.OperationRecord;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Step definitions for operations-commands.feature.
 */
public class OperationsCommandSteps {

    private AutoCloseable closeable;
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final List<OperationRecord> operations = new ArrayList<>();
    private OperationsCommand operationsCommand;
    private int exitCode;

    @Mock
    private ServiceManager mockServiceManager;
    
    @Mock
    private MetadataService mockMetadataService;

    @Mock
    private AuthorizationService mockAuthorizationService;

    @Before
    public void setup() {
        closeable = MockitoAnnotations.openMocks(this);
        System.setOut(new PrintStream(outputStream));
        
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

    @Given("no operations exist in the system")
    public void noOperationsExistInTheSystem() {
        operations.clear();
        when(mockMetadataService.getRecentOperations(anyInt())).thenReturn(operations);
    }
    
    @Given("the user is not authorized to view operations")
    public void theUserIsNotAuthorizedToViewOperations() {
        when(mockAuthorizationService.hasPermission(anyString())).thenReturn(false);
    }

    @Given("the following operations exist in the system:")
    public void theFollowingOperationsExistInTheSystem(DataTable dataTable) {
        List<Map<String, String>> rows = dataTable.asMaps();
        
        for (Map<String, String> row : rows) {
            String operationId = row.get("Operation ID");
            String type = row.get("Type");
            String status = row.get("Status");
            String startTimeStr = row.get("Start Time");
            String endTimeStr = row.get("End Time");
            String parametersStr = row.get("Parameters");
            String resultsStr = row.get("Results");
            String errorDetails = row.get("Error Details");
            
            // Parse times
            Instant startTime = Instant.parse(startTimeStr);
            Instant endTime = endTimeStr != null && !endTimeStr.isEmpty() ? Instant.parse(endTimeStr) : null;
            
            // Parse parameters and results as Maps
            Map<String, Object> parameters = parseJsonToMap(parametersStr);
            Map<String, Object> results = parseJsonToMap(resultsStr);
            
            // Create and add the operation record
            OperationRecord operationRecord = new OperationRecord(
                operationId, type, status, startTime, endTime, parameters, results, errorDetails);
            operations.add(operationRecord);
        }
        
        when(mockMetadataService.getRecentOperations(anyInt())).thenReturn(operations);
    }

    @When("the user executes the operations command")
    public void theUserExecutesTheOperationsCommand() {
        operationsCommand = new OperationsCommand(mockServiceManager);
        exitCode = operationsCommand.call();
    }

    @When("the user executes the operations command with limit {int}")
    public void theUserExecutesTheOperationsCommandWithLimit(int limit) {
        operationsCommand = new OperationsCommand(mockServiceManager);
        operationsCommand.setLimit(limit);
        exitCode = operationsCommand.call();
    }

    @When("the user executes the operations command with filter {string}")
    public void theUserExecutesTheOperationsCommandWithFilter(String filter) {
        operationsCommand = new OperationsCommand(mockServiceManager);
        operationsCommand.setFilter(filter);
        exitCode = operationsCommand.call();
    }

    @When("the user executes the operations command with JSON output")
    public void theUserExecutesTheOperationsCommandWithJsonOutput() {
        operationsCommand = new OperationsCommand(mockServiceManager);
        operationsCommand.setJsonOutput(true);
        exitCode = operationsCommand.call();
    }

    @When("the user executes the operations command with verbose flag")
    public void theUserExecutesTheOperationsCommandWithVerboseFlag() {
        operationsCommand = new OperationsCommand(mockServiceManager);
        operationsCommand.setVerbose(true);
        exitCode = operationsCommand.call();
    }

    @When("the user executes the operations command with recent {int}")
    public void theUserExecutesTheOperationsCommandWithRecent(int recent) {
        operationsCommand = new OperationsCommand(mockServiceManager);
        operationsCommand.setRecent(recent);
        exitCode = operationsCommand.call();
    }

    @When("the user executes the operations command with help option")
    public void theUserExecutesTheOperationsCommandWithHelpOption() {
        operationsCommand = new OperationsCommand(mockServiceManager);
        operationsCommand.setHelp(true);
        exitCode = operationsCommand.call();
    }

    @Then("the command should succeed")
    public void theCommandShouldSucceed() {
        Assertions.assertEquals(0, exitCode);
    }

    @Then("the command should fail")
    public void theCommandShouldFail() {
        Assertions.assertNotEquals(0, exitCode);
    }

    @Then("the output should show {int} operations")
    public void theOutputShouldShowOperations(int count) {
        String output = outputStream.toString();
        int occurrences = 0;
        int index = 0;
        
        // For JSON output we count objects
        if (output.contains("[{")) {
            occurrences = countJsonObjects(output);
        } else {
            // For text output we count "Operation ID:" occurrences
            while ((index = output.indexOf("Operation ID:", index)) != -1) {
                occurrences++;
                index += "Operation ID:".length();
            }
        }
        
        Assertions.assertEquals(count, occurrences, 
                "Expected " + count + " operations but found " + occurrences);
    }

    @Then("the output should include operation details")
    public void theOutputShouldIncludeOperationDetails() {
        String output = outputStream.toString();
        Assertions.assertTrue(output.contains("Operation ID:"), 
                "Output should include operation IDs");
        Assertions.assertTrue(output.contains("Type:"), 
                "Output should include operation types");
        Assertions.assertTrue(output.contains("Status:"), 
                "Output should include operation status");
    }

    @Then("the output should show only operations of type {string}")
    public void theOutputShouldShowOnlyOperationsOfType(String type) {
        String output = outputStream.toString();
        Assertions.assertTrue(output.contains(type), 
                "Output should contain operations of type " + type);
        
        // Check that other types aren't present
        List<String> otherTypes = List.of("ADD_ITEM", "UPDATE_ITEM", "VIEW_ITEM", "DELETE_ITEM", "LIST_ITEMS");
        for (String otherType : otherTypes) {
            if (!otherType.equals(type)) {
                // We need to check this more carefully since types might be substrings of each other
                int index = output.indexOf("Type: " + otherType);
                Assertions.assertEquals(-1, index, 
                        "Output should not contain operations of type " + otherType);
            }
        }
    }

    @Then("the output should contain valid JSON")
    public void theOutputShouldContainValidJson() {
        String output = outputStream.toString();
        Assertions.assertTrue(output.trim().startsWith("[") && output.trim().endsWith("]"), 
                "Output should be a JSON array");
        Assertions.assertTrue(output.contains("{") && output.contains("}"), 
                "Output should contain JSON objects");
    }

    @Then("the JSON should include all operation fields")
    public void theJsonShouldIncludeAllOperationFields() {
        String output = outputStream.toString();
        Assertions.assertTrue(output.contains("\"operationId\""), 
                "JSON should include operationId field");
        Assertions.assertTrue(output.contains("\"operationType\""), 
                "JSON should include operationType field");
        Assertions.assertTrue(output.contains("\"status\""), 
                "JSON should include status field");
        Assertions.assertTrue(output.contains("\"startTime\""), 
                "JSON should include startTime field");
    }

    @Then("the output should include operation parameters")
    public void theOutputShouldIncludeOperationParameters() {
        String output = outputStream.toString();
        Assertions.assertTrue(output.contains("Parameters:"), 
                "Output should include parameters section");
        Assertions.assertTrue(output.contains("title"), 
                "Output should include parameter names");
    }

    @Then("the output should include operation results")
    public void theOutputShouldIncludeOperationResults() {
        String output = outputStream.toString();
        Assertions.assertTrue(output.contains("Results:"), 
                "Output should include results section");
        Assertions.assertTrue(output.contains("success") || output.contains("count") || output.contains("id"), 
                "Output should include result values");
    }

    @Then("the output should include error details for failed operations")
    public void theOutputShouldIncludeErrorDetailsForFailedOperations() {
        String output = outputStream.toString();
        Assertions.assertTrue(output.contains("Error Details:"), 
                "Output should include error details section");
        Assertions.assertTrue(output.contains("Item locked"), 
                "Output should include error message details");
    }

    @Then("the output should indicate no matching operations found")
    public void theOutputShouldIndicateNoMatchingOperationsFound() {
        String output = outputStream.toString();
        Assertions.assertTrue(output.contains("No operations found") || 
                             output.contains("No matching operations"), 
                "Output should indicate no matching operations");
    }

    @Then("the output should indicate no operations found")
    public void theOutputShouldIndicateNoOperationsFound() {
        String output = outputStream.toString();
        Assertions.assertTrue(output.contains("No operations found"), 
                "Output should indicate no operations found");
    }

    @Then("the output should contain usage instructions")
    public void theOutputShouldContainUsageInstructions() {
        String output = outputStream.toString();
        Assertions.assertTrue(output.contains("Usage:"), 
                "Output should contain usage instructions");
        Assertions.assertTrue(output.contains("operations"), 
                "Output should mention the command name");
    }

    @Then("the output should list all available options")
    public void theOutputShouldListAllAvailableOptions() {
        String output = outputStream.toString();
        Assertions.assertTrue(output.contains("Options:"), 
                "Output should list options");
        
        // Check for important options
        List<String> expectedOptions = List.of("--limit", "--filter", "--json", "--verbose", "--recent", "--help");
        for (String option : expectedOptions) {
            Assertions.assertTrue(output.contains(option), 
                    "Output should include option: " + option);
        }
    }

    @Then("the output should contain an error message about permissions")
    public void theOutputShouldContainAnErrorMessageAboutPermissions() {
        String output = outputStream.toString();
        String error = outputStream.toString();
        Assertions.assertTrue(error.contains("permission") || error.contains("authorized"), 
                "Error should mention permission issues");
    }

    @Then("the MetadataService should record the operation")
    public void theMetadataServiceShouldRecordTheOperation() {
        verify(mockMetadataService).recordOperation(eq("command.operations"), eq("LIST_OPERATIONS"), any());
    }

    @Then("the MetadataService should record the operation with limit parameter")
    public void theMetadataServiceShouldRecordTheOperationWithLimitParameter() {
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(mockMetadataService).recordOperation(eq("command.operations"), eq("LIST_OPERATIONS"), captor.capture());
        
        Map<String, Object> params = captor.getValue();
        Assertions.assertTrue(params.containsKey("limit"), 
                "Operation parameters should include limit");
    }

    @Then("the MetadataService should record the operation with filter parameter")
    public void theMetadataServiceShouldRecordTheOperationWithFilterParameter() {
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(mockMetadataService).recordOperation(eq("command.operations"), eq("LIST_OPERATIONS"), captor.capture());
        
        Map<String, Object> params = captor.getValue();
        Assertions.assertTrue(params.containsKey("filter"), 
                "Operation parameters should include filter");
    }

    @Then("the MetadataService should record the operation with format parameter")
    public void theMetadataServiceShouldRecordTheOperationWithFormatParameter() {
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(mockMetadataService).recordOperation(eq("command.operations"), eq("LIST_OPERATIONS"), captor.capture());
        
        Map<String, Object> params = captor.getValue();
        Assertions.assertTrue(params.containsKey("format"), 
                "Operation parameters should include format");
        Assertions.assertEquals("json", params.get("format"), 
                "Format parameter should be 'json'");
    }

    @Then("the MetadataService should record the operation with verbose parameter")
    public void theMetadataServiceShouldRecordTheOperationWithVerboseParameter() {
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(mockMetadataService).recordOperation(eq("command.operations"), eq("LIST_OPERATIONS"), captor.capture());
        
        Map<String, Object> params = captor.getValue();
        Assertions.assertTrue(params.containsKey("verbose"), 
                "Operation parameters should include verbose");
        Assertions.assertEquals(true, params.get("verbose"), 
                "Verbose parameter should be true");
    }
    
    @Then("the MetadataService should record the operation with recent parameter")
    public void theMetadataServiceShouldRecordTheOperationWithRecentParameter() {
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(mockMetadataService).recordOperation(eq("command.operations"), eq("LIST_OPERATIONS"), captor.capture());
        
        Map<String, Object> params = captor.getValue();
        Assertions.assertTrue(params.containsKey("recent"), 
                "Operation parameters should include recent");
    }

    @Then("the MetadataService should not record any operation")
    public void theMetadataServiceShouldNotRecordAnyOperation() {
        verify(mockMetadataService, never()).recordOperation(anyString(), anyString(), any());
    }

    // Helper method to parse JSON string to Map
    private Map<String, Object> parseJsonToMap(String json) {
        if (json == null || json.isEmpty()) {
            return new HashMap<>();
        }
        
        Map<String, Object> result = new HashMap<>();
        
        // Simple JSON parsing for this test
        String content = json.trim();
        if (content.startsWith("{") && content.endsWith("}")) {
            content = content.substring(1, content.length() - 1);
            
            if (!content.isEmpty()) {
                // Split by commas, but handle quotes
                String[] pairs = content.split(",");
                for (String pair : pairs) {
                    String[] keyValue = pair.split(":");
                    if (keyValue.length == 2) {
                        String key = keyValue[0].trim().replace("\"", "");
                        String value = keyValue[1].trim().replace("\"", "");
                        result.put(key, value);
                    }
                }
            }
        }
        
        return result;
    }
    
    // Helper method to count JSON objects in a string
    private int countJsonObjects(String json) {
        int count = 0;
        int index = 0;
        
        while ((index = json.indexOf("{", index)) != -1) {
            count++;
            index += 1;
        }
        
        return count;
    }
}
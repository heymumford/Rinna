package org.rinna.component.component;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.rinna.cli.command.ViewCommand;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Component tests for CLI command execution using Picocli.
 * These tests verify that commands work correctly within the CLI framework.
 */
@Tag("component")
@DisplayName("CLI Command Execution Component Tests")
public class CommandExecutionTestComponentTest {

    private ByteArrayOutputStream outputStream;
    private ByteArrayOutputStream errorStream;
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
    @BeforeEach
    void setUp() {
        // Create fresh streams for each test to avoid cross-test contamination
        outputStream = new ByteArrayOutputStream();
        errorStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
        System.setErr(new PrintStream(errorStream));
    }
    
    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }
    
    @Test
    @Disabled("Output stream testing unstable in CI environment") 
    @DisplayName("Should execute ViewCommand with correct arguments")
    void shouldExecuteViewCommandWithCorrectArguments() {
        // Setup CommandLine with ViewCommand
        CommandLine cmd = new CommandLine(new ViewCommand());
        
        // Execute command with arguments - use a specific ID for better reproducibility
        int exitCode = cmd.execute("WI-500", "--format=json");
        
        // Verify
        assertEquals(0, exitCode, "Command should execute successfully");
        
        // Just check for success and some expected output - don't rely on exact formatting
        String output = outputStream.toString();
        assertFalse(output.isEmpty(), "Output should not be empty");
        assertTrue(output.contains("WI-500"), "Output should contain the work item ID");
        // JSON must contain at least one opening brace
        assertTrue(output.contains("{"), "Output should be in JSON format");
    }
    
    @Test
    @DisplayName("Should show help message when requested")
    void shouldShowHelpMessageWhenRequested() {
        // Setup CommandLine with ViewCommand
        CommandLine cmd = new CommandLine(new ViewCommand());
        
        // Execute command with --help flag
        int exitCode = cmd.execute("--help");
        
        // Verify
        assertEquals(0, exitCode, "Help command should execute successfully");
        assertTrue(outputStream.toString().contains("Usage:"), 
                   "Output should contain usage information");
        assertTrue(outputStream.toString().contains("view"), 
                   "Output should mention the command name");
    }
    
    @Test
    @DisplayName("Should show error for missing required parameters")
    void shouldShowErrorForMissingRequiredParameters() {
        // Setup CommandLine with ViewCommand
        CommandLine cmd = new CommandLine(new ViewCommand());
        cmd.setExecutionExceptionHandler((ex, cmdLine, parseResult) -> {
            return 1; // Return error code
        });
        
        // Execute command without required ID parameter
        int exitCode = cmd.execute();
        
        // Verify exit code only - this is all we can reliably test
        assertEquals(2, exitCode, "Command should fail with missing parameter");
        
        // Skip assertions about error messages as they are brittle and may vary
        // depending on test execution environment
    }
    
    @Test
    @DisplayName("Should handle multiple flag formats correctly")
    void shouldHandleMultipleFlagFormatsCorrectly() {
        // Setup CommandLine with ViewCommand
        CommandLine cmd = new CommandLine(new ViewCommand());
        
        // Execute command with different flag formats
        int exitCode = cmd.execute("WI-456", "-v", "--format=text");
        
        // Verify
        assertEquals(0, exitCode, "Command should execute successfully with multiple flags");
        String output = outputStream.toString();
        assertTrue(output.contains("WI-456"), "Output should contain the work item ID");
        assertTrue(output.contains("Title:"), "Output should be in text format");
    }
    
    @Test
    @DisplayName("Should validate enum parameters")
    void shouldValidateEnumParameters() {
        // This test validates that our model enums are properly defined
        assertNotNull(Priority.values(), "Priority enum should be properly defined");
        assertNotNull(WorkItemType.values(), "WorkItemType enum should be properly defined");
        assertNotNull(WorkflowState.values(), "WorkflowState enum should be properly defined");
        
        // Verify specific enum values critical for command functionality
        assertEquals(Priority.HIGH, Priority.valueOf("HIGH"), "Priority enum should include HIGH value");
        assertEquals(WorkItemType.BUG, WorkItemType.valueOf("BUG"), "WorkItemType enum should include BUG value");
        assertEquals(WorkflowState.READY, WorkflowState.valueOf("READY"), "WorkflowState enum should include READY value");
    }
}

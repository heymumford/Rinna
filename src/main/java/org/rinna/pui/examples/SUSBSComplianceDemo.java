package org.rinna.pui.examples;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.rinna.pui.cli.CommandGenerator;
import org.rinna.pui.cli.ShellCommandBridge;
import org.rinna.pui.cli.ShellEscapeHandler;
import org.rinna.pui.component.BoxLayout;
import org.rinna.pui.component.Button;
import org.rinna.pui.component.CommandHistory;
import org.rinna.pui.component.CommandLineComponent;
import org.rinna.pui.component.Container;
import org.rinna.pui.component.Label;
import org.rinna.pui.component.List;
import org.rinna.pui.component.TextBox;

/**
 * Demonstration of SUSBS (Standardized Utility Shell-Based Solution) compliance
 * in PUI components.
 */
public class SUSBSComplianceDemo {
    
    private static ShellCommandBridge commandBridge;
    private static CommandGenerator commandGenerator;
    private static CommandHistory commandHistory;
    private static CommandLineComponent commandLine;
    private static List operationList;
    private static TextBox scriptPathInput;
    private static Container mainContainer;
    private static boolean isRunning = true;
    private static Map<String, Map<String, String>> savedOperations = new HashMap<>();
    
    public static void main(String[] args) {
        // Initialize components
        setupComponents();
        
        // Main event loop
        while (isRunning) {
            // Render UI
            mainContainer.render();
            
            // Process input
            int key = readKey();
            if (key == 'q') {
                isRunning = false;
            } else {
                mainContainer.handleKeyInput(key);
            }
            
            // Brief pause
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Restore terminal
        System.out.print("\033[?25h"); // Show cursor
        System.out.print("\033[0m");   // Reset attributes
        System.out.println("\nSUSBS Compliance Demo ended.");
    }
    
    /**
     * Set up all UI components.
     */
    private static void setupComponents() {
        // Clear screen and hide cursor
        System.out.print("\033[2J");
        System.out.print("\033[H");
        System.out.print("\033[?25l");
        
        // Initialize command bridge and generator
        commandBridge = new ShellCommandBridge();
        commandGenerator = new CommandGenerator(commandBridge);
        
        // Create main container
        mainContainer = new Container();
        mainContainer.setPosition(1, 1);
        
        // Add title
        Label titleLabel = new Label("Rinna SUSBS Compliance Demo");
        titleLabel.setPosition(2, 1);
        mainContainer.addComponent(titleLabel);
        
        // Create operation area
        Label operationsLabel = new Label("Available Operations:");
        operationsLabel.setPosition(2, 3);
        mainContainer.addComponent(operationsLabel);
        
        operationList = new List();
        operationList.setPosition(2, 4);
        operationList.setDimensions(40, 10);
        operationList.addItem("workitem.add - Add a work item");
        operationList.addItem("workitem.list - List work items");
        operationList.addItem("workitem.view - View a work item");
        operationList.addItem("workflow.transition - Change workflow state");
        operationList.addItem("stats.dashboard - Show statistics dashboard");
        operationList.addItem("dependency.list - List dependencies");
        operationList.addItem("search.global - Search for work items");
        operationList.setItemSelectedListener(SUSBSComplianceDemo::handleOperationSelected);
        mainContainer.addComponent(operationList);
        
        // Create command history component
        commandHistory = new CommandHistory();
        commandHistory.setPosition(45, 4);
        commandHistory.setDimensions(50, 10);
        commandHistory.setCommandSelectedListener(cmd -> {
            if (commandLine != null) {
                commandLine.executeCommand(cmd, false);
            }
        });
        mainContainer.addComponent(commandHistory);
        
        // Create command line
        commandLine = new CommandLineComponent(commandBridge);
        commandLine.setPosition(2, 16);
        commandLine.setWidth(93);
        commandLine.setCommandExecutionListener(SUSBSComplianceDemo::handleCommandExecution);
        mainContainer.addComponent(commandLine);
        
        // Create shell escape button
        Button shellEscapeButton = new Button("Shell Escape");
        shellEscapeButton.setPosition(2, 18);
        shellEscapeButton.setActionListener(() -> {
            ShellEscapeHandler shellEscapeHandler = new ShellEscapeHandler();
            shellEscapeHandler.setCommandHistoryConsumer(commands -> {
                for (String cmd : commands) {
                    commandHistory.addCommand("!" + cmd);
                }
            });
            shellEscapeHandler.escapeToShell();
        });
        mainContainer.addComponent(shellEscapeButton);
        
        // Create script generation components
        Label scriptLabel = new Label("Generate Shell Script:");
        scriptLabel.setPosition(2, 20);
        mainContainer.addComponent(scriptLabel);
        
        scriptPathInput = new TextBox();
        scriptPathInput.setPosition(25, 20);
        scriptPathInput.setWidth(40);
        scriptPathInput.setValue("/tmp/rinna_demo_script.sh");
        mainContainer.addComponent(scriptPathInput);
        
        Button generateScriptButton = new Button("Generate");
        generateScriptButton.setPosition(70, 20);
        generateScriptButton.setActionListener(SUSBSComplianceDemo::generateScript);
        mainContainer.addComponent(generateScriptButton);
        
        // Add help text
        BoxLayout helpBox = new BoxLayout();
        helpBox.setPosition(2, 22);
        helpBox.setDimensions(93, 4);
        helpBox.setTitle("Shell Integration Features");
        helpBox.addComponent(new Label("- Enter commands at the prompt above"));
        helpBox.addComponent(new Label("- Use ! prefix for direct shell commands (e.g., !ls)"));
        helpBox.addComponent(new Label("- Shell escape provides full shell access"));
        helpBox.addComponent(new Label("- Generate shell scripts from recorded operations"));
        mainContainer.addComponent(helpBox);
        
        // Add exit instructions
        Label exitLabel = new Label("Press 'q' to exit");
        exitLabel.setPosition(2, 27);
        mainContainer.addComponent(exitLabel);
    }
    
    /**
     * Handle command execution from the command line.
     * 
     * @param command The executed command
     */
    private static void handleCommandExecution(String command) {
        // Add to history
        commandHistory.addCommand(command);
        
        // Check for special commands
        if (command.equals("clear")) {
            commandHistory.clearHistory();
            return;
        }
        
        // Process as regular command
        System.out.println("\nExecuting: " + command);
        
        // In a real implementation, we would parse the command and execute
        // the appropriate operation through the command bridge
        // For this demo, we'll just record it for script generation
        
        if (command.contains(" ")) {
            String operation = command.substring(0, command.indexOf(" "));
            Map<String, String> params = new HashMap<>();
            params.put("args", command.substring(command.indexOf(" ") + 1));
            savedOperations.put(command, params);
        } else {
            savedOperations.put(command, new HashMap<>());
        }
    }
    
    /**
     * Handle selection of an operation from the list.
     * 
     * @param selectedItem The selected operation
     */
    private static void handleOperationSelected(String selectedItem) {
        if (selectedItem == null) {
            return;
        }
        
        String operation = selectedItem.substring(0, selectedItem.indexOf(" - "));
        
        // Generate sample command based on operation
        Map<String, String> sampleParams = generateSampleParams(operation);
        String command = commandBridge.generateCommand(operation, sampleParams);
        
        // Execute the command
        if (commandLine != null) {
            commandLine.executeCommand(command, true);
        }
    }
    
    /**
     * Generate sample parameters for an operation.
     * 
     * @param operation The operation identifier
     * @return Map of sample parameters
     */
    private static Map<String, String> generateSampleParams(String operation) {
        Map<String, String> params = new HashMap<>();
        
        switch (operation) {
            case "workitem.add":
                params.put("type", "task");
                params.put("title", "Sample task");
                params.put("priority", "medium");
                break;
                
            case "workitem.list":
                params.put("filter", "--state=IN_PROGRESS");
                break;
                
            case "workitem.view":
                params.put("id", "WI-123");
                break;
                
            case "workflow.transition":
                params.put("id", "WI-123");
                params.put("state", "IN_REVIEW");
                break;
                
            case "stats.dashboard":
                // No parameters needed
                break;
                
            case "dependency.list":
                params.put("id", "WI-123");
                break;
                
            case "search.global":
                params.put("query", "priority");
                break;
                
            default:
                // No parameters for unknown operations
                break;
        }
        
        return params;
    }
    
    /**
     * Generate a shell script from recorded operations.
     */
    private static void generateScript() {
        String scriptPath = scriptPathInput.getValue();
        if (scriptPath == null || scriptPath.isEmpty()) {
            System.out.println("\nError: Please enter a valid script path");
            return;
        }
        
        try {
            // Convert saved operations to format expected by CommandGenerator
            java.util.List<Map.Entry<String, Map<String, String>>> operations = new ArrayList<>();
            
            // Add header comment
            Map<String, String> headerParams = new HashMap<>();
            headerParams.put("comment", "Operations recorded in Rinna PUI");
            operations.add(Map.entry("comment", headerParams));
            
            operations.add(Map.entry("blank", new HashMap<>()));
            
            // Add all recorded operations
            for (Map.Entry<String, Map<String, String>> entry : savedOperations.entrySet()) {
                String command = entry.getKey();
                Map<String, String> params = new HashMap<>();
                params.put("command", command);
                operations.add(Map.entry("shell", params));
            }
            
            // Generate the script
            commandGenerator.clear();
            for (Map.Entry<String, Map<String, String>> operation : operations) {
                if (operation.getKey().equals("comment")) {
                    commandGenerator.addComment(operation.getValue().get("comment"));
                } else if (operation.getKey().equals("blank")) {
                    commandGenerator.addBlankLine();
                } else {
                    commandGenerator.addOperation(operation.getKey(), operation.getValue());
                }
            }
            
            // Save the script
            Path path = Paths.get(scriptPath);
            Files.writeString(path, commandGenerator.generateScript());
            
            // Make executable
            Path file = Paths.get(scriptPath);
            file.toFile().setExecutable(true);
            
            System.out.println("\nScript generated successfully: " + scriptPath);
            
        } catch (IOException e) {
            System.out.println("\nError generating script: " + e.getMessage());
        }
    }
    
    /**
     * Read a key from the terminal.
     * 
     * @return The key code
     */
    private static int readKey() {
        try {
            if (System.in.available() > 0) {
                return System.in.read();
            }
        } catch (IOException e) {
            // Ignore
        }
        return -1;
    }
}
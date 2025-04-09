package org.rinna.pui.examples;

import org.rinna.pui.cli.CommandMirror;
import org.rinna.pui.cli.ShellIntegrationLayer;
import org.rinna.pui.component.BoxLayout;
import org.rinna.pui.component.Button;
import org.rinna.pui.component.Container;
import org.rinna.pui.component.Label;
import org.rinna.pui.component.List;
import org.rinna.pui.component.TextBox;

import java.util.HashMap;
import java.util.Map;

/**
 * Demonstration of SUSBS-compliant command mirroring in PUI.
 * Shows how PUI operations map directly to shell commands and vice versa.
 */
public class CommandMirrorDemo {
    
    private static ShellIntegrationLayer shellIntegration;
    private static Container mainContainer;
    private static List operationsList;
    private static TextBox resultTextBox;
    private static TextBox shellCommandTextBox;
    private static TextBox puiOperationTextBox;
    private static TextBox parameterTextBox;
    private static boolean isRunning = true;
    
    public static void main(String[] args) {
        // Initialize shell integration layer
        shellIntegration = new ShellIntegrationLayer();
        
        // Set up UI
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
        System.out.println("\nCommand Mirror Demo ended.");
    }
    
    /**
     * Set up all UI components.
     */
    private static void setupComponents() {
        // Clear screen and hide cursor
        System.out.print("\033[2J");
        System.out.print("\033[H");
        System.out.print("\033[?25l");
        
        // Create main container
        mainContainer = new Container();
        mainContainer.setPosition(1, 1);
        
        // Add title
        Label titleLabel = new Label("SUSBS-Compliant Command Mirroring Demo");
        titleLabel.setPosition(2, 1);
        mainContainer.addComponent(titleLabel);
        
        // Add info
        Label infoLabel = new Label("This demo shows how PUI operations map directly to shell commands and vice versa");
        infoLabel.setPosition(2, 2);
        mainContainer.addComponent(infoLabel);
        
        // Left column: List of supported operations
        BoxLayout operationsBox = new BoxLayout();
        operationsBox.setPosition(2, 4);
        operationsBox.setDimensions(40, 20);
        operationsBox.setTitle("Supported Operations");
        
        operationsList = new List();
        operationsList.setWidth(36);
        operationsList.setHeight(18);
        
        // Add supported operations to the list
        for (String operation : shellIntegration.getSupportedOperations()) {
            String template = shellIntegration.getShellTemplate(operation);
            operationsList.addItem(operation + " → rin " + template);
        }
        
        operationsList.setItemSelectedListener(CommandMirrorDemo::operationSelected);
        
        operationsBox.addComponent(operationsList);
        mainContainer.addComponent(operationsBox);
        
        // Right column: PUI to Shell and Shell to PUI conversion
        BoxLayout conversionBox = new BoxLayout();
        conversionBox.setPosition(44, 4);
        conversionBox.setDimensions(56, 12);
        conversionBox.setTitle("Command Mirroring");
        
        // PUI to Shell section
        Label puiToShellLabel = new Label("PUI to Shell Conversion:");
        conversionBox.addComponent(puiToShellLabel);
        
        Label operationLabel = new Label("PUI Operation:");
        conversionBox.addComponent(operationLabel);
        
        puiOperationTextBox = new TextBox();
        puiOperationTextBox.setWidth(50);
        puiOperationTextBox.setValue("workitem.list");
        conversionBox.addComponent(puiOperationTextBox);
        
        Label paramsLabel = new Label("Parameters (JSON format):");
        conversionBox.addComponent(paramsLabel);
        
        parameterTextBox = new TextBox();
        parameterTextBox.setWidth(50);
        parameterTextBox.setValue("{\"filter\": \"--state=IN_PROGRESS\"}");
        conversionBox.addComponent(parameterTextBox);
        
        Button convertToShellButton = new Button("Convert to Shell Command");
        convertToShellButton.setActionListener(CommandMirrorDemo::convertToShell);
        conversionBox.addComponent(convertToShellButton);
        
        // Shell to PUI section
        BoxLayout shellToPuiBox = new BoxLayout();
        shellToPuiBox.setPosition(44, 17);
        shellToPuiBox.setDimensions(56, 7);
        shellToPuiBox.setTitle("Shell to PUI Conversion");
        
        Label shellCommandLabel = new Label("Shell Command:");
        shellToPuiBox.addComponent(shellCommandLabel);
        
        shellCommandTextBox = new TextBox();
        shellCommandTextBox.setWidth(50);
        shellCommandTextBox.setValue("rin list --state=IN_PROGRESS");
        shellToPuiBox.addComponent(shellCommandTextBox);
        
        Button convertToPuiButton = new Button("Convert to PUI Operation");
        convertToPuiButton.setActionListener(CommandMirrorDemo::convertToPui);
        shellToPuiBox.addComponent(convertToPuiButton);
        
        mainContainer.addComponent(conversionBox);
        mainContainer.addComponent(shellToPuiBox);
        
        // Results box
        BoxLayout resultsBox = new BoxLayout();
        resultsBox.setPosition(2, 25);
        resultsBox.setDimensions(98, 5);
        resultsBox.setTitle("Results");
        
        resultTextBox = new TextBox();
        resultTextBox.setWidth(94);
        resultTextBox.setEditable(false);
        resultsBox.addComponent(resultTextBox);
        
        mainContainer.addComponent(resultsBox);
        
        // Add exit instructions
        Label exitLabel = new Label("Press 'q' to exit");
        exitLabel.setPosition(2, 31);
        mainContainer.addComponent(exitLabel);
    }
    
    /**
     * Handle selection of an operation from the list.
     * 
     * @param selectedItem The selected operation
     */
    private static void operationSelected(String selectedItem) {
        if (selectedItem != null) {
            // Extract the operation part
            String operation = selectedItem.substring(0, selectedItem.indexOf(" →"));
            puiOperationTextBox.setValue(operation);
            
            // Generate example parameter values
            Map<String, String> params = generateExampleParams(operation);
            
            // Convert to JSON-like format
            StringBuilder paramsJson = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (!first) {
                    paramsJson.append(", ");
                }
                paramsJson.append("\"").append(entry.getKey()).append("\": \"")
                        .append(entry.getValue()).append("\"");
                first = false;
            }
            paramsJson.append("}");
            
            parameterTextBox.setValue(paramsJson.toString());
            
            // Preview the shell command
            convertToShell();
        }
    }
    
    /**
     * Generate example parameters for an operation.
     * 
     * @param operation The operation identifier
     * @return Map of example parameters
     */
    private static Map<String, String> generateExampleParams(String operation) {
        Map<String, String> params = new HashMap<>();
        
        switch (operation) {
            case "workitem.add":
                params.put("type", "task");
                params.put("title", "Example task");
                params.put("priority", "medium");
                break;
                
            case "workitem.list":
                params.put("filter", "--state=IN_PROGRESS");
                break;
                
            case "workitem.view":
                params.put("id", "WI-123");
                break;
                
            case "workitem.update":
                params.put("id", "WI-123");
                params.put("field", "priority");
                params.put("value", "high");
                break;
                
            case "workflow.transition":
                params.put("id", "WI-123");
                params.put("state", "IN_REVIEW");
                break;
                
            default:
                // For other operations, use empty params
                break;
        }
        
        return params;
    }
    
    /**
     * Convert PUI operation to shell command.
     */
    private static void convertToShell() {
        try {
            // Get the operation
            String operation = puiOperationTextBox.getValue();
            
            // Parse parameters from JSON
            String paramsJson = parameterTextBox.getValue();
            Map<String, String> params = parseJsonParams(paramsJson);
            
            // Generate shell command
            String shellCommand = shellIntegration.getShellEquivalent(operation, params);
            
            // Set result
            resultTextBox.setValue("PUI to Shell: " + operation + " → " + shellCommand);
            
            // Update shell command textbox for reverse conversion
            shellCommandTextBox.setValue(shellCommand);
            
        } catch (Exception e) {
            resultTextBox.setValue("Error: " + e.getMessage());
        }
    }
    
    /**
     * Convert shell command to PUI operation.
     */
    private static void convertToPui() {
        try {
            // Get the shell command
            String shellCommand = shellCommandTextBox.getValue();
            
            // Convert to PUI operation
            Map.Entry<String, Map<String, String>> puiOperation = shellIntegration.getPuiEquivalent(shellCommand);
            
            if (puiOperation != null) {
                // Format parameters as JSON
                StringBuilder paramsJson = new StringBuilder("{");
                boolean first = true;
                for (Map.Entry<String, String> entry : puiOperation.getValue().entrySet()) {
                    if (!first) {
                        paramsJson.append(", ");
                    }
                    paramsJson.append("\"").append(entry.getKey()).append("\": \"")
                            .append(entry.getValue()).append("\"");
                    first = false;
                }
                paramsJson.append("}");
                
                // Set result
                resultTextBox.setValue("Shell to PUI: " + shellCommand + " → " + 
                        puiOperation.getKey() + " with params " + paramsJson);
                
                // Update PUI operation textbox for reverse conversion
                puiOperationTextBox.setValue(puiOperation.getKey());
                parameterTextBox.setValue(paramsJson.toString());
                
            } else {
                resultTextBox.setValue("No PUI operation equivalent found for: " + shellCommand);
            }
            
        } catch (Exception e) {
            resultTextBox.setValue("Error: " + e.getMessage());
        }
    }
    
    /**
     * Parse parameters from JSON-like string.
     * 
     * @param json The JSON string
     * @return Map of parameters
     */
    private static Map<String, String> parseJsonParams(String json) {
        Map<String, String> params = new HashMap<>();
        
        // Simple JSON parsing for demo
        // Remove braces
        String content = json.trim();
        if (content.startsWith("{")) {
            content = content.substring(1);
        }
        if (content.endsWith("}")) {
            content = content.substring(0, content.length() - 1);
        }
        
        // Split by commas (not inside quotes)
        String[] pairs = content.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        
        for (String pair : pairs) {
            // Skip empty pairs
            if (pair.trim().isEmpty()) {
                continue;
            }
            
            // Split by colon
            String[] keyValue = pair.split(":");
            if (keyValue.length == 2) {
                // Extract key and value
                String key = keyValue[0].trim();
                String value = keyValue[1].trim();
                
                // Remove quotes
                if (key.startsWith("\"") && key.endsWith("\"")) {
                    key = key.substring(1, key.length() - 1);
                }
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                
                params.put(key, value);
            }
        }
        
        return params;
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
        } catch (java.io.IOException e) {
            // Ignore
        }
        return -1;
    }
}
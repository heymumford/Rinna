package org.rinna.pui.examples;

import org.rinna.pui.cli.ShellIntegrationLayer;
import org.rinna.pui.component.BoxLayout;
import org.rinna.pui.component.Button;
import org.rinna.pui.component.Container;
import org.rinna.pui.component.Label;
import org.rinna.pui.component.List;
import org.rinna.pui.component.ShellConsole;
import org.rinna.pui.component.TextBox;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Demonstration of the Shell Integration Layer for PUI operations.
 * Shows how PUI components can integrate with the shell in a SUSBS-compliant way.
 */
public class ShellIntegrationDemo {
    
    private static ShellIntegrationLayer shellIntegration;
    private static ShellConsole shellConsole;
    private static List commandHistoryList;
    private static Container mainContainer;
    private static boolean isRunning = true;
    private static TextBox scriptPathInput;
    
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
        System.out.println("\nShell Integration Demo ended.");
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
        Label titleLabel = new Label("Rinna Shell Integration Demo");
        titleLabel.setPosition(2, 1);
        mainContainer.addComponent(titleLabel);
        
        // Add info
        Label infoLabel = new Label("This demo shows how PUI components integrate with the shell via the ShellIntegrationLayer");
        infoLabel.setPosition(2, 2);
        mainContainer.addComponent(infoLabel);
        
        // Shell console
        shellConsole = new ShellConsole(shellIntegration);
        shellConsole.setPosition(2, 4);
        shellConsole.setDimensions(80, 20);
        shellConsole.setPrompt("rin> ");
        mainContainer.addComponent(shellConsole);
        
        // Command history list
        commandHistoryList = new List();
        commandHistoryList.setPosition(84, 4);
        commandHistoryList.setDimensions(40, 10);
        commandHistoryList.setTitle("Command History");
        
        // Add command history listener
        shellIntegration.addCommandExecutionListener(command -> {
            commandHistoryList.clearItems();
            for (String cmd : shellIntegration.getCommandHistory()) {
                commandHistoryList.addItem(cmd);
            }
        });
        
        // Initial history population
        for (String command : shellIntegration.getCommandHistory()) {
            commandHistoryList.addItem(command);
        }
        
        mainContainer.addComponent(commandHistoryList);
        
        // Script generation components
        BoxLayout scriptBox = new BoxLayout();
        scriptBox.setPosition(84, 15);
        scriptBox.setDimensions(40, 9);
        scriptBox.setTitle("Shell Script Generation");
        
        Label scriptLabel = new Label("Generate a shell script from your commands:");
        scriptBox.addComponent(scriptLabel);
        
        Label pathLabel = new Label("Script path:");
        scriptBox.addComponent(pathLabel);
        
        scriptPathInput = new TextBox();
        scriptPathInput.setWidth(30);
        scriptPathInput.setValue("/tmp/rinna_shell_script.sh");
        scriptBox.addComponent(scriptPathInput);
        
        Button generateButton = new Button("Generate Script");
        generateButton.setActionListener(() -> {
            String path = scriptPathInput.getValue();
            if (path != null && !path.isEmpty()) {
                boolean success = shellIntegration.generateScriptFromHistory(path, 0);
                if (success) {
                    try {
                        // Make executable
                        Files.setPosixFilePermissions(Paths.get(path), 
                                java.nio.file.attribute.PosixFilePermissions.fromString("rwxr-xr-x"));
                        shellConsole.addOutput("\nScript generated successfully at: " + path);
                    } catch (Exception e) {
                        shellConsole.addOutput("\nScript generated, but couldn't set permissions: " + e.getMessage());
                    }
                } else {
                    shellConsole.addOutput("\nFailed to generate script at: " + path);
                }
            } else {
                shellConsole.addOutput("\nPlease enter a valid script path");
            }
        });
        scriptBox.addComponent(generateButton);
        
        Button clearHistoryButton = new Button("Clear History");
        clearHistoryButton.setActionListener(() -> {
            shellIntegration.clearCommandHistory();
            commandHistoryList.clearItems();
            shellConsole.addOutput("\nCommand history cleared");
        });
        scriptBox.addComponent(clearHistoryButton);
        
        mainContainer.addComponent(scriptBox);
        
        // Environment variable display
        BoxLayout envBox = new BoxLayout();
        envBox.setPosition(2, 25);
        envBox.setDimensions(122, 4);
        envBox.setTitle("Environment Variables");
        
        // Add example environment variables
        Map<String, String> rinnaVars = new HashMap<>();
        rinnaVars.put("RINNA_HOME", System.getProperty("user.dir"));
        rinnaVars.put("RINNA_CONFIG", System.getProperty("user.dir") + "/config");
        rinnaVars.put("RINNA_SHELL_INTEGRATION", "enabled");
        shellIntegration.setEnvironmentVariables(rinnaVars);
        
        // Display environment variables
        StringBuilder envVarText = new StringBuilder();
        for (Map.Entry<String, String> entry : shellIntegration.getAllEnvironmentVariables().entrySet()) {
            if (entry.getKey().startsWith("RINNA_")) {
                envVarText.append(entry.getKey()).append("=").append(entry.getValue()).append("  ");
            }
        }
        Label envLabel = new Label(envVarText.toString());
        envBox.addComponent(envLabel);
        
        Label instructionsLabel = new Label("Try commands like: help, !ls, !pwd, workflow states");
        envBox.addComponent(instructionsLabel);
        
        mainContainer.addComponent(envBox);
        
        // Add exit instructions
        Label exitLabel = new Label("Press 'q' to exit");
        exitLabel.setPosition(2, 30);
        mainContainer.addComponent(exitLabel);
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
# Shell Integration for PUI Components

This document describes the comprehensive shell integration layer for Rinna's Pragmatic User Interface (PUI) components, providing a SUSBS-compliant interface for shell operations.

## Overview

The Shell Integration Layer provides a unified API for PUI components to interact with the shell environment in a standardized way. It builds on the SUSBS compliance features to create a seamless integration between the PUI and shell commands.

## Core Components

### 1. ShellIntegrationLayer

The central component that coordinates all shell-related functionality:

```java
// Create the shell integration layer
ShellIntegrationLayer shellIntegration = new ShellIntegrationLayer();

// Execute commands
String result = shellIntegration.executeCommand("list --state=IN_PROGRESS");
String shellResult = shellIntegration.executeShellCommand("ls -la");

// Execute commands asynchronously
CompletableFuture<String> future = shellIntegration.executeCommandAsync("stats dashboard");
future.thenAccept(output -> System.out.println(output));
```

Features:
- Command execution (both Rinna and shell commands)
- Command history tracking
- Environment variable management
- Shell script generation
- Process management
- Asynchronous command execution
- Command completion suggestions

### 2. ShellConsole Component

A full-featured terminal component for direct shell interaction:

```java
// Create a shell console with the integration layer
ShellConsole console = new ShellConsole(shellIntegration);
console.setDimensions(80, 24);
console.setPrompt("rin> ");

// Add to container
container.addComponent(console);
```

Features:
- Command entry with history navigation
- Tab completion
- Direct shell command execution with `!` prefix
- Shell session handling
- VT100 terminal emulation
- Keyboard shortcuts (Ctrl+A, Ctrl+E, Ctrl+L, etc.)

## Key Features

### Command Execution and Mirroring

The shell integration layer abstracts command execution across different contexts and implements SUSBS-compliant command mirroring:

```java
// Execute Rinna commands
shellIntegration.executeCommand("add task --title=\"New task\" --priority=high");

// Execute shell commands directly
shellIntegration.executeShellCommand("grep -r \"TODO\" .");

// Execute operations by ID with automatic shell command generation
Map<String, String> params = new HashMap<>();
params.put("title", "New bug");
params.put("priority", "high");
shellIntegration.executeOperation("workitem.add", params);

// Bidirectional command mirroring
String shellCommand = shellIntegration.getShellEquivalent("workitem.list", params);
Map.Entry<String, Map<String, String>> puiOperation = shellIntegration.getPuiEquivalent("rin list --state=IN_PROGRESS");
```

#### Command Mirroring

The `CommandMirror` class provides bidirectional mapping between PUI operations and shell commands:

```java
CommandMirror mirror = new CommandMirror();

// PUI to Shell conversion
Map<String, String> params = new HashMap<>();
params.put("id", "WI-123");
String shellCommand = mirror.puiToShell("workitem.view", params);
// Results in: "rin view WI-123"

// Shell to PUI conversion
Map.Entry<String, Map<String, String>> puiOperation = mirror.shellToPui("rin update WI-123 --priority=\"high\"");
// Results in: "workitem.update" with params {"id":"WI-123", "field":"priority", "value":"high"}
```

Benefits of command mirroring:
- Every PUI operation has a direct shell command equivalent
- Shell commands can be parsed back to PUI operations
- Parameters are automatically extracted and validated
- Consistent mapping ensures UI and CLI provide identical functionality

### Environment Variable Management

Manage environment variables for shell commands:

```java
// Set environment variables
shellIntegration.environmentVariable("RINNA_DEBUG", "true");

// Get environment variables
String debugMode = shellIntegration.environmentVariable("RINNA_DEBUG", null);

// Set multiple variables
Map<String, String> vars = new HashMap<>();
vars.put("RINNA_HOME", "/path/to/rinna");
vars.put("RINNA_CONFIG", "/path/to/config");
shellIntegration.setEnvironmentVariables(vars);
```

### Process Management

Start and manage long-running processes:

```java
// Start a process with output handling
String processId = shellIntegration.startProcess("tail -f logfile.log", 
    line -> System.out.println("Log: " + line));

// Stop a process
shellIntegration.stopProcess(processId);
```

### Script Generation

Generate shell scripts from command history or operations:

```java
// Generate from history
shellIntegration.generateScriptFromHistory("/path/to/script.sh", 0);

// Generate from specific operations
List<Map.Entry<String, Map<String, String>>> operations = new ArrayList<>();
// Add operations...
shellIntegration.generateScript("/path/to/script.sh", operations);
```

## Integration Examples

### Basic Command Execution

```java
ShellIntegrationLayer shell = new ShellIntegrationLayer();

// Execute a Rinna command
String result = shell.executeCommand("list --state=IN_PROGRESS");
System.out.println(result);

// Execute a shell command
String files = shell.executeShellCommand("ls -la");
System.out.println(files);
```

### Event-Based Integration

```java
ShellIntegrationLayer shell = new ShellIntegrationLayer();

// Add a command listener
shell.addCommandExecutionListener(command -> {
    System.out.println("Command executed: " + command);
});

// Add an environment change listener
shell.addEnvironmentChangeListener(changes -> {
    System.out.println("Environment variables changed:");
    changes.forEach((key, value) -> System.out.println(key + "=" + value));
});
```

### Asynchronous Command Execution

```java
ShellIntegrationLayer shell = new ShellIntegrationLayer();

// Execute a long-running command asynchronously
CompletableFuture<String> future = shell.executeCommandAsync("stats generate --type=full");

// Add completion handlers
future.thenAccept(result -> {
    System.out.println("Command completed with result:");
    System.out.println(result);
}).exceptionally(ex -> {
    System.err.println("Command failed: " + ex.getMessage());
    return null;
});
```

## Demos and Examples

Three comprehensive demos showcase the shell integration capabilities:

1. **SUSBS Compliance Demo** - Shows basic shell integration features
   ```bash
   ./run-susbs-compliance-demo.sh
   ```

2. **Shell Integration Demo** - Shows the comprehensive shell integration layer
   ```bash
   ./run-shell-integration-demo.sh
   ```

3. **Command Mirror Demo** - Shows bidirectional command mirroring
   ```bash
   ./run-command-mirror-demo.sh
   ```

These demos illustrate:
- Command execution and history tracking
- Shell script generation
- Environment variable management
- Shell console usage
- Process management
- Command completion
- Bidirectional command mirroring
- Parameter extraction and substitution

## Implementation Guidelines

When extending or using the shell integration layer:

1. **Use the Layer as a Facade** - Always go through the shell integration layer rather than directly calling shell commands

2. **Honor Environment Variables** - Check for and respect Rinna-specific environment variables

3. **Command Tracking** - Ensure all commands are properly tracked for history and script generation

4. **Error Handling** - Implement proper error handling for shell operations

5. **Keep State in Sync** - Ensure PUI state stays in sync with shell state

6. **Command Mirroring** - Map all PUI operations to equivalent shell commands

## Security Considerations

The shell integration layer implements several security measures:

- **Command Validation** - Validates commands before execution
- **Restricted Commands** - Prevents execution of dangerous commands
- **Environment Isolation** - Provides isolated environment for shell operations
- **Process Control** - Proper resource management for processes
- **Secure Script Generation** - Generates scripts with proper permissions and security checks

## References

- [SUSBS Standards](../../SUSBS_STANDARDS.md)
- [SUSBS Compliance in PUI](./susbs-compliance.md)
- [PUI Design Principles](../../PUI_DESIGN_PRINCIPLES.md)
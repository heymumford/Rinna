# CLI Command Error Handling Template

This document provides a standardized template for implementing error handling in CLI commands. Use this as a reference when implementing new commands or refactoring existing ones.

## Command Class Structure

```java
public class ExampleCommand implements Callable<Integer> {
    
    private final ServiceManager serviceManager;
    private final Scanner scanner;
    private final MetadataService metadataService;
    private final OperationTracker operationTracker;
    private final ErrorHandler errorHandler;
    private String format = "text";
    private boolean verbose = false;
    
    // Other command-specific fields
    private String operation;
    private String[] args = new String[0];
    
    /**
     * Creates a new ExampleCommand with the specified ServiceManager.
     */
    public ExampleCommand(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
        this.metadataService = serviceManager.getMetadataService();
        this.scanner = new Scanner(System.in, java.nio.charset.StandardCharsets.UTF_8.name());
        
        // Initialize utility instances
        this.operationTracker = new OperationTracker(metadataService);
        this.errorHandler = new ErrorHandler(metadataService);
    }
    
    // Setter methods
    
    public void setOperation(String operation) {
        this.operation = operation;
    }
    
    public void setArgs(String[] args) {
        this.args = args;
    }
    
    public void setFormat(String format) {
        this.format = format;
        this.errorHandler.outputFormat(format);
    }
    
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
        this.errorHandler.verbose(verbose);
    }
    
    @Override
    public Integer call() {
        // Configure the operation tracker with command details
        operationTracker
            .command("command-name")
            .operationType("OPERATION_TYPE")
            .param("operation", operation != null ? operation : "help")
            .param("argsCount", args.length)
            .param("format", format)
            .param("verbose", verbose);
        
        // Add first argument if available
        if (args.length > 0) {
            operationTracker.param("arg0", args[0]);
        }
        
        try {
            // Execute the main operation with tracking
            return operationTracker.execute(() -> {
                // Handle missing operation
                if (operation == null || operation.isEmpty()) {
                    displayHelp(operationTracker.start());
                    return 1;
                }
                
                // Get the required service
                SomeService service = serviceManager.getSomeService();
                if (service == null) {
                    String errorMessage = "Service is not available.";
                    return errorHandler.handleError(
                        operationTracker.start(),
                        "command-name", 
                        errorMessage,
                        new IllegalStateException(errorMessage),
                        ErrorHandler.Severity.SYSTEM
                    );
                }
                
                // Delegate to the appropriate operation
                String parentOperationId = operationTracker.start();
                int result;
                
                switch (operation) {
                    case "sub-operation1":
                        result = handleSubOperation1(service, parentOperationId);
                        break;
                    
                    case "sub-operation2":
                        result = handleSubOperation2(service, parentOperationId);
                        break;
                    
                    case "help":
                        displayHelp(parentOperationId);
                        result = 0;
                        break;
                    
                    default:
                        String errorMessage = "Unknown operation: " + operation;
                        displayHelp(parentOperationId);
                        return errorHandler.handleError(
                            parentOperationId,
                            "command-name",
                            errorMessage,
                            new IllegalArgumentException(errorMessage),
                            ErrorHandler.Severity.VALIDATION
                        );
                }
                
                // If operation was successful, record the result
                if (result == 0) {
                    Map<String, Object> resultData = new HashMap<>();
                    resultData.put("success", true);
                    resultData.put("operation", operation);
                    operationTracker.complete(parentOperationId, resultData);
                }
                
                return result;
            });
        } catch (Exception e) {
            // Handle any unexpected errors using the error handler
            return errorHandler.handleUnexpectedError(
                operationTracker.start(),
                "command-name", 
                e
            );
        }
    }
    
    /**
     * Handles sub-operation1.
     */
    private int handleSubOperation1(SomeService service, String parentOperationId) {
        // Create a sub-tracker for this operation
        OperationTracker subTracker = operationTracker
            .command("command-name-sub-operation1")
            .param("operation", "sub-operation1")
            .param("format", format)
            .parent(parentOperationId);
        
        try {
            // Execute the operation and return the result
            return subTracker.execute(() -> {
                // Get required input or parameters
                Map<String, String> options = parseOptions(args);
                
                // Validate input parameters
                if (!options.containsKey("required-param")) {
                    Map<String, String> validationErrors = new HashMap<>();
                    validationErrors.put("required-param", "This parameter is required");
                    return errorHandler.handleValidationError(
                        subTracker.start(),
                        "command-name-sub-operation1",
                        validationErrors
                    );
                }
                
                // Perform operation
                try {
                    boolean success = service.performOperation(options.get("required-param"));
                    if (success) {
                        // Output success message in appropriate format
                        outputSuccess("Operation completed successfully");
                        
                        // Return success
                        return 0;
                    } else {
                        // Handle operation failure
                        String errorMessage = "Operation failed";
                        return errorHandler.handleError(
                            subTracker.start(),
                            "command-name-sub-operation1",
                            errorMessage,
                            ErrorHandler.Severity.ERROR
                        );
                    }
                } catch (Exception e) {
                    // Handle expected errors
                    String errorMessage = "Error during operation: " + e.getMessage();
                    return errorHandler.handleError(
                        subTracker.start(),
                        "command-name-sub-operation1",
                        errorMessage,
                        e,
                        ErrorHandler.Severity.ERROR
                    );
                }
            });
        } catch (Exception e) {
            // Handle unexpected errors
            return errorHandler.handleUnexpectedError(
                subTracker.start(),
                "command-name-sub-operation1",
                e,
                ErrorHandler.Severity.SYSTEM
            );
        }
    }
    
    /**
     * Handles sub-operation2.
     */
    private int handleSubOperation2(SomeService service, String parentOperationId) {
        // Similar to handleSubOperation1, with appropriate error handling
        // ...
    }
    
    /**
     * Displays help information.
     */
    private void displayHelp(String operationId) {
        // Create operation parameters for tracking
        Map<String, Object> helpData = new HashMap<>();
        helpData.put("command", "command-name");
        helpData.put("action", "help");
        helpData.put("format", format);
        
        // Start tracking sub-operation
        String helpOperationId = metadataService.trackOperation("command-name-help", helpData);
        
        try {
            // Display help in appropriate format
            if ("json".equalsIgnoreCase(format)) {
                // JSON help output
                // ...
            } else {
                // Text help output
                // ...
            }
            
            // Track operation success
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("success", true);
            resultData.put("format", format);
            metadataService.completeOperation(helpOperationId, resultData);
        } catch (Exception e) {
            // Handle help display error
            metadataService.failOperation(helpOperationId, e);
            throw e; // Rethrow to be caught by caller
        }
    }
    
    /**
     * Parses command line arguments into a map of options.
     */
    private Map<String, String> parseOptions(String[] args) {
        Map<String, String> options = new HashMap<>();
        
        // Parse options from args
        // ...
        
        return options;
    }
    
    /**
     * Outputs a success message in either JSON or text format.
     */
    private void outputSuccess(String message) {
        if ("json".equalsIgnoreCase(format)) {
            Map<String, Object> successData = errorHandler.createSuccessResult("command-name", Map.of("message", message));
            System.out.println(OutputFormatter.toJson(successData, verbose));
        } else {
            System.out.println(message);
        }
    }
}
```

## Key Error Handling Patterns

### Validation Errors

For user input validation errors:

```java
Map<String, String> validationErrors = new HashMap<>();
validationErrors.put("param1", "Parameter cannot be empty");
validationErrors.put("param2", "Invalid value");
return errorHandler.handleValidationError(operationId, "command-name", validationErrors);
```

### Expected Errors

For expected errors that occur during normal operation:

```java
String errorMessage = "Error during operation: " + e.getMessage();
return errorHandler.handleError(
    operationId,
    "command-name",
    errorMessage,
    e,
    ErrorHandler.Severity.ERROR
);
```

### Unexpected Errors

For unexpected runtime errors:

```java
return errorHandler.handleUnexpectedError(
    operationId,
    "command-name",
    e,
    ErrorHandler.Severity.SYSTEM
);
```

### Interactive vs Non-interactive Mode

When a command supports both interactive and non-interactive (JSON) modes:

```java
if ("json".equalsIgnoreCase(format)) {
    // Return guidance for non-interactive mode
    Map<String, Object> resultData = new HashMap<>();
    resultData.put("result", "success");
    resultData.put("message", "Input required for operation");
    resultData.put("requiredParams", Arrays.asList("param1", "param2"));
    System.out.println(OutputFormatter.toJson(resultData, verbose));
    return 0;
} else {
    // Interactive mode
    System.out.print("Enter required information: ");
    String input = scanner.nextLine();
    // Process input
}
```

## Severity Levels

Use the appropriate severity level for each error:

- `VALIDATION` - For user input errors
- `WARNING` - For non-fatal issues that might affect operation
- `ERROR` - For fatal issues that prevent operation completion
- `SYSTEM` - For system-level errors (file system, network, etc.)
- `SECURITY` - For security-related issues

## Common Refactoring Patterns

When refactoring existing commands to use the standardized error handling:

1. Add the `ErrorHandler` field and initialize it in the constructor
2. Update setters for format and verbose to configure the ErrorHandler
3. Replace direct calls to `outputError` with `errorHandler.outputError` or one of the handling methods
4. Update operation tracking to use OperationTracker or hierarchical tracking
5. Update switch statements to use the standardized handleXYZ method for fallthrough cases
6. Replace all direct exception handling with the appropriate ErrorHandler method
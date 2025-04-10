# Operation Tracking Utilities

Rinna provides a comprehensive set of utilities for tracking CLI command operations, designed to reduce boilerplate code, standardize error handling, and optimize performance in high-volume scenarios.

## Overview

The operation tracking utilities consist of three main components:

1. **OperationTracker**: A fluent API for tracking operations with MetadataService
2. **MetadataOptimizer**: Performance optimizations for high-volume operation tracking
3. **ErrorHandler**: Standardized error handling with integrated operation tracking

These utilities work together to provide a consistent approach to operation tracking across all CLI commands, making it easier to implement robust and maintainable command handlers.

## OperationTracker

The `OperationTracker` class provides a fluent API for tracking operations with the MetadataService, reducing boilerplate code and standardizing error handling.

### Key Features

- Fluent interface for building operation parameters
- Automatic handling of operation completion or failure
- Support for hierarchical operations with parent-child relationships
- Lambda-based execution with automatic tracking

### Usage Example

```java
// Create a tracker with the MetadataService
OperationTracker tracker = new OperationTracker(metadataService);

// Configure and execute an operation
Map<String, Object> result = tracker
    .command("my-command")
    .operationType("READ")
    .param("itemId", "WI-123")
    .param("format", "json")
    .execute(() -> {
        // Your operation code here
        return myService.getItem("WI-123");
    });

// For nested operations
OperationTracker subTracker = tracker.createSubTracker("my-command-sub-operation");
subTracker
    .param("subParam", "value")
    .executeVoid(() -> {
        // Sub-operation code
        myService.processItem("WI-123");
    });
```

## MetadataOptimizer

The `MetadataOptimizer` class provides performance optimizations for high-volume operation tracking, including batch processing, caching, and asynchronous execution.

### Key Features

- Batch operation tracking to reduce overhead
- Parameter caching to avoid repeated object creation
- Asynchronous completion to avoid blocking
- Rate limiting to prevent overwhelming the service
- Background flushing of operation queues

### Usage Example

```java
// Create an optimizer with default settings
MetadataOptimizer optimizer = new MetadataOptimizer(metadataService);

// Use cached parameters
Map<String, Object> params = optimizer.getCachedParameters("list");
params.put("limit", 10);

// Start an operation with cached parameters
String opId = optimizer.startOperationWithCachedParams("list-command", "READ", "list");

// Complete an operation asynchronously
optimizer.completeOperationAsync(opId, result);

// Process a batch of items
List<String> items = getItemsToProcess();
int successCount = optimizer.processBatch(
    "batch-command",
    "PROCESS",
    items,
    item -> {
        Map<String, Object> itemParams = new HashMap<>();
        itemParams.put("item", item);
        return itemParams;
    },
    item -> {
        // Process each item
        processItem(item);
    }
);

// Ensure all operations are completed
optimizer.flushQueue();
optimizer.shutdown(5); // 5 second timeout
```

## ErrorHandler

The `ErrorHandler` class provides standardized error handling with integrated operation tracking, ensuring consistent error reporting and proper tracking of all errors.

### Key Features

- Consistent error reporting across commands
- Automatic tracking of errors in MetadataService
- Support for structured validation errors
- Format-aware output (text or JSON)
- Standardized success result handling

### Usage Example

```java
// Create an error handler
ErrorHandler errorHandler = new ErrorHandler(metadataService)
    .verbose(true)
    .outputFormat("json");

try {
    // Your command code here
    Map<String, Object> result = processCommand();
    
    // Handle success
    return errorHandler.handleSuccess(operationId, result);
} catch (ValidationException e) {
    // Handle validation errors
    Map<String, String> validationErrors = e.getValidationErrors();
    return errorHandler.handleValidationError(operationId, "my-command", validationErrors);
} catch (Exception e) {
    // Handle unexpected errors
    return errorHandler.handleUnexpectedError(operationId, "my-command", e);
}
```

## Best Practices

### Command Implementation Pattern

1. **Initialize the utilities in the constructor**:
   ```java
   public MyCommand(ServiceManager serviceManager) {
       this.serviceManager = serviceManager;
       this.metadataService = serviceManager.getMetadataService();
       this.tracker = new OperationTracker(metadataService);
       this.errorHandler = new ErrorHandler(metadataService);
   }
   ```

2. **Configure the tracker in the call method**:
   ```java
   public Integer call() {
       errorHandler.verbose(verbose).outputFormat(format);
       
       tracker.command("my-command")
           .param("operation", operation)
           .param("format", format);
       
       String operationId = tracker.start();
       
       try {
           // Command implementation
       } catch (Exception e) {
           return errorHandler.handleError(operationId, "my-command", e.getMessage(), e);
       }
   }
   ```

3. **Use hierarchical tracking for sub-operations**:
   ```java
   private int handleSubOperation(String parentOperationId) {
       OperationTracker subTracker = tracker.createSubTracker("my-command-sub");
       
       try {
           // Sub-operation implementation
       } catch (Exception e) {
           return errorHandler.handleError(parentOperationId, "my-command-sub", e.getMessage(), e);
       }
   }
   ```

### Performance Considerations

1. **Use batch processing for high-volume operations**:
   ```java
   MetadataOptimizer optimizer = new MetadataOptimizer(metadataService);
   optimizer.processBatch("bulk-command", "PROCESS", items, ...);
   ```

2. **Cache common parameter maps**:
   ```java
   Map<String, Object> listParams = new HashMap<>();
   listParams.put("operation", "list");
   optimizer.cacheParameters("list", listParams);
   ```

3. **Complete operations asynchronously when appropriate**:
   ```java
   optimizer.completeOperationAsync(operationId, result);
   ```

## Complete Example

See the `CommandExample` class for a complete example of how to implement a command using these utilities. It demonstrates:

1. Simple operations with error handling
2. Nested operations with hierarchical tracking
3. Batch operations with optimized performance
4. Standard help display with proper tracking

## Refactoring Example: AdminMonitorCommand

The `AdminMonitorCommand` has been refactored to use the new operation tracking utilities. This example demonstrates how to migrate an existing command to use these utilities:

### Before Refactoring

The original implementation had several issues:
- Repetitive code for tracking operations
- Inconsistent error handling
- Manual parameter creation and tracking
- No clear separation between operation logic and tracking

```java
private int handleDashboardOperation(MonitoringService monitoringService, String parentOperationId) {
    // Create operation parameters for tracking
    Map<String, Object> params = new HashMap<>();
    params.put("operation", "dashboard");
    params.put("format", format);
    
    // Start tracking sub-operation
    String dashboardOperationId = metadataService.trackOperation("admin-monitor-dashboard", params);
    
    try {
        String dashboard = monitoringService.getDashboard();
        
        if ("json".equalsIgnoreCase(format)) {
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("result", "success");
            resultData.put("operation", "dashboard");
            resultData.put("dashboard", dashboard);
            
            System.out.println(OutputFormatter.toJson(resultData, verbose));
        } else {
            System.out.println(dashboard);
        }
        
        // Track operation success
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("success", true);
        metadataService.completeOperation(dashboardOperationId, resultData);
        
        return 0;
    } catch (Exception e) {
        String errorMessage = "Error displaying dashboard: " + e.getMessage();
        outputError(errorMessage);
        if (verbose) {
            e.printStackTrace();
        }
        metadataService.failOperation(dashboardOperationId, e);
        return 1;
    }
}
```

### After Refactoring

The refactored implementation using operation tracking utilities:
- Uses fluent API for parameter building
- Automatically handles operation completion and failure
- Provides consistent error handling
- Focuses on the core operation logic

```java
private int handleDashboardOperation(MonitoringService monitoringService, String parentOperationId) {
    // Create a sub-tracker for the dashboard operation
    OperationTracker dashboardTracker = operationTracker
        .command("admin-monitor-dashboard")
        .param("operation", "dashboard")
        .param("format", format)
        .parent(parentOperationId);
    
    try {
        // Execute the dashboard operation and return the result
        return dashboardTracker.execute(() -> {
            // Get the dashboard data
            String dashboard = monitoringService.getDashboard();
            
            // Display the dashboard in the appropriate format
            if ("json".equalsIgnoreCase(format)) {
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("result", "success");
                resultData.put("operation", "dashboard");
                resultData.put("dashboard", dashboard);
                
                System.out.println(OutputFormatter.toJson(resultData, verbose));
            } else {
                System.out.println(dashboard);
            }
            
            // Return success result map for operation tracking
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("success", true);
            
            return 0;  // Success exit code
        });
    } catch (Exception e) {
        // Handle error using ErrorHandler for consistent error reporting
        String errorMessage = "Error displaying dashboard: " + e.getMessage();
        return errorHandler.handleError(parentOperationId, "dashboard", errorMessage, e);
    }
}
```

### Key Refactoring Benefits

1. **Code Reduction**: The refactored method has less boilerplate and focuses on the operation logic
2. **Error Handling**: Consistent error handling across all operations
3. **Self-Documenting**: The fluent API makes the code more readable and self-documenting
4. **Maintainability**: Easier to maintain and extend with new features
5. **Consistency**: All operations follow the same pattern for tracking and error handling

## Summary

By using these utilities, you can:

1. **Reduce Boilerplate**: Minimize repetitive code for operation tracking
2. **Standardize Error Handling**: Ensure consistent error reporting and tracking
3. **Optimize Performance**: Efficiently track high-volume operations
4. **Improve Code Quality**: Focus on command logic rather than tracking details

The operation tracking utilities are designed to work seamlessly with existing CLI commands and can be gradually adopted across the codebase.
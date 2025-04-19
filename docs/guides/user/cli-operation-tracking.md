# CLI Operation Tracking

The Rinna CLI provides comprehensive operation tracking capabilities that enhance auditability, traceability, and analytics. This document explains how operation tracking works and how to leverage it in both user scenarios and custom command development.

## Overview

Operation tracking in Rinna records detailed information about every CLI command execution, including:

- Command name and operation type
- Parameters and arguments
- Start and end timestamps
- Success or failure status
- Results or error details
- User and client information
- Hierarchical relationships between operations

This data enables advanced usage analytics, audit trails, troubleshooting, and performance optimization.

## Key Features

### Comprehensive Audit Trail

Every CLI command automatically tracks its execution, creating a detailed audit trail that includes:

- **Who**: Username and client information
- **What**: Command name, parameters, and results
- **When**: Precise timestamps for start and completion
- **How**: Success/failure status and execution details

### Hierarchical Operation Tracking

Operations are tracked in a hierarchical structure, where:

- Each command creates a parent operation
- Sub-operations are created for major steps
- Operations can be nested to any depth
- Child-parent relationships are preserved

This allows for detailed analysis of command execution flow and enables pinpointing specific issues in complex operations.

### Metadata Service Integration

All CLI commands integrate with the `MetadataService`, which provides:

- Standardized tracking across all commands
- Consistent parameter and result handling
- Unified error tracking and reporting
- Centralized operation history storage

## Using Operation Tracking

### Viewing Operation History

To view the history of recent operations:

```bash
# View recent operations
rin admin operations list

# View operations for a specific command
rin admin operations list --command add

# View operations of a specific type
rin admin operations list --type UPDATE

# Limit the number of operations shown
rin admin operations list --limit 10
```

### Analyzing Operation Details

To view detailed information about a specific operation:

```bash
# View details for a specific operation ID
rin admin operations detail OP-123456

# Export operation details to JSON
rin admin operations detail OP-123456 --format json
```

### Operation Analytics Dashboard

The operations analytics dashboard provides visual insights into command usage patterns:

```bash
# View the operations dashboard
rin admin operations dashboard

# View analytics for a specific time period
rin admin operations dashboard --from "2025-01-01" --to "2025-01-31"

# Filter analytics by command
rin admin operations dashboard --command add
```

### CLI Operations Monitor

For real-time monitoring of CLI operations, use the operations monitor:

```bash
# Start the operations monitor
./run-operations-monitor.sh
```

This launches the PUI-based operations monitor that shows real-time operation tracking with visualization of command execution, performance metrics, and status information.

## Operation Data Structure

Each operation includes the following information:

| Field | Description |
|-------|-------------|
| `id` | Unique identifier for the operation |
| `commandName` | Name of the command being executed |
| `operationType` | Type of operation (CREATE, READ, UPDATE, DELETE, etc.) |
| `parameters` | Map of input parameters and arguments |
| `startTime` | When the operation started |
| `endTime` | When the operation completed (or failed) |
| `status` | Current status (IN_PROGRESS, COMPLETED, FAILED) |
| `result` | Result data (for successful operations) |
| `errorMessage` | Error details (for failed operations) |
| `username` | User who executed the command |
| `clientInfo` | Client information (hostname, terminal, etc.) |

## Command-Specific Tracking Details

Different commands track specific relevant information:

### Work Item Commands

Work item commands track details such as:

- Work item IDs
- State transitions
- Priority changes
- Assignment changes
- Creation and modification details

Example output:

```
Operation: ADD-COMMAND (CREATE)
Parameters:
  title: "Implement payment gateway"
  type: FEATURE
  priority: HIGH
Result:
  id: WI-123
  created: true
  creationTime: 2025-04-09T14:30:22
Child Operations:
  - add-validation (VALIDATE)
  - add-creation (CREATE)
  - add-display (READ)
```

### Workflow Commands

Workflow commands track workflow-specific details:

- Current and target states
- Transition validation
- Comments and notes
- Transition timestamps
- Approval information

Example output:

```
Operation: WORKFLOW-COMMAND (UPDATE)
Parameters:
  itemId: WI-123
  targetState: IN_PROGRESS
  comment: "Starting implementation"
Result:
  success: true
  previousState: TODO
  newState: IN_PROGRESS
  transitionTime: 2025-04-09T15:45:12
Child Operations:
  - workflow-validate (VALIDATE)
  - workflow-transition (UPDATE)
  - workflow-display (READ)
```

### Search and Filtering Commands

Search commands track search criteria and results:

- Search terms and patterns
- Filter criteria
- Sort options
- Result counts
- Execution time

Example output:

```
Operation: FIND-COMMAND (SEARCH)
Parameters:
  type: BUG
  priority: HIGH
  assignee: johndoe
Result:
  matchCount: 5
  executionTime: 124ms
  filters: 3
```

### Server Management Commands

Server commands track service management details:

- Service actions (start, stop, restart)
- Service status information
- Port and PID information
- Configuration changes

Example output:

```
Operation: SERVER-COMMAND (MANAGE)
Parameters:
  action: start
  port: 8080
Result:
  success: true
  pid: 12345
  startTime: 2025-04-09T09:15:22
  port: 8080
```

### Bulk Operations

Bulk commands provide comprehensive hierarchical tracking:

- Filter criteria and matching counts
- Individual item operations
- Success and failure counts
- Detailed execution flow

Example output:

```
Operation: BULK-COMMAND (UPDATE)
Parameters:
  filter: { "text": "payment" }
  update: { "set-priority": "HIGH" }
Result:
  totalItems: 5
  updatedItems: 5
  failedItems: 0
Child Operations:
  - bulk-filter (SEARCH)
    - bulk-filter-method (SEARCH)
    - bulk-primary-filter (SEARCH)
  - bulk-update-apply (UPDATE)
    - bulk-apply-updates-method (UPDATE)
    - bulk-update-type-set-priority (UPDATE)
    - bulk-update-item (UPDATE) [x5]
      - bulk-update-item-priority (UPDATE) [x5]
  - bulk-result-display (READ)
```

## Statistics and Analytics

The MetadataService provides rich analytics capabilities:

### Command Usage Patterns

```bash
# View command usage statistics
rin admin operations stats --by command

# View usage trends over time
rin admin operations stats --by date --interval daily
```

### Performance Metrics

```bash
# View command performance metrics
rin admin operations stats --by performance

# View slowest commands
rin admin operations stats --by performance --sort duration --limit 10
```

### Error Analysis

```bash
# View error statistics
rin admin operations stats --by errors

# View most common errors
rin admin operations stats --by errors --sort count --limit 10
```

## Integrating with PUI Components

The operation tracking capabilities integrate with the Pragmatic User Interface (PUI) through the `ServiceBridge` class, allowing UI components to:

- Display real-time operation information
- Show operation history and details
- Visualize command usage patterns
- Monitor system activity

The `OperationsMonitorDemo` provides a comprehensive example of how to integrate operation tracking with PUI components.

## For Developers: Implementing Operation Tracking

When developing custom CLI commands, follow these guidelines to ensure proper operation tracking:

### Basic Tracking Pattern

```java
// Get the MetadataService from ServiceManager
MetadataService metadataService = serviceManager.getMetadataService();

// Create parameter map
Map<String, Object> params = new HashMap<>();
params.put("itemId", itemId);
params.put("format", format);

// Start tracking the operation
String operationId = metadataService.startOperation(
    "my-custom-command",  // Command name 
    "READ",               // Operation type
    params                // Parameters
);

try {
    // Execute command logic
    Object result = performOperation();
    
    // Complete the operation with result
    metadataService.completeOperation(operationId, result);
    
    return 0; // Success
} catch (Exception e) {
    // Track operation failure
    metadataService.failOperation(operationId, e);
    
    return 1; // Failure
}
```

### Hierarchical Tracking Pattern

For commands with multiple steps, use hierarchical tracking:

```java
// Start main command operation
String mainOpId = metadataService.startOperation("main-command", "UPDATE", mainParams);

try {
    // Track validation step
    String validateOpId = metadataService.startOperation("validate-step", "VALIDATE", validateParams);
    try {
        boolean valid = validateInput();
        metadataService.completeOperation(validateOpId, Map.of("valid", valid));
    } catch (Exception e) {
        metadataService.failOperation(validateOpId, e);
        throw e;
    }
    
    // Track execution step
    String execOpId = metadataService.startOperation("execute-step", "UPDATE", execParams);
    try {
        Object result = executeOperation();
        metadataService.completeOperation(execOpId, result);
    } catch (Exception e) {
        metadataService.failOperation(execOpId, e);
        throw e;
    }
    
    // Complete main operation
    metadataService.completeOperation(mainOpId, finalResult);
    return 0;
} catch (Exception e) {
    metadataService.failOperation(mainOpId, e);
    return 1;
}
```

### Best Practices

1. **Use Consistent Command Names**: Follow the pattern `command-name` for operations
2. **Use Standard Operation Types**: Stick to CREATE, READ, UPDATE, DELETE, EXECUTE, VALIDATE, SEARCH, MANAGE
3. **Track All Parameters**: Include all relevant command parameters in the tracking
4. **Handle Errors Properly**: Always call failOperation when exceptions occur
5. **Track Sub-Operations**: For complex commands, track each major step
6. **Include Results**: Add meaningful result data when completing operations
7. **Track Performance**: Include timing information when relevant
8. **Redact Sensitive Data**: Don't include passwords or tokens in parameters

## Related Documentation

- [Service Management](service-management.md) - Service architecture documentation
- [PUI Architecture](pui-architecture.md) - PUI framework documentation
- [Workflow Transition UI](workflow-transition-ui.md) - Example of PUI with operation tracking
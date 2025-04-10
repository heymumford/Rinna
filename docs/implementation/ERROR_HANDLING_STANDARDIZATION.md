# Error Handling Standardization

This document outlines the standardized approach to error handling across all CLI commands in the Rinna CLI system.

## Overview

The Rinna CLI implements a standardized error handling mechanism to ensure:

1. Consistent error reporting for users in both text and JSON output formats
2. Detailed operation tracking for all errors using the MetadataService
3. Appropriate error severity levels based on the type of error
4. Support for both interactive and non-interactive operations
5. Stacktrace handling based on verbosity settings
6. Format-aware error output with proper JSON formatting
7. Consistent exit codes and operational metadata

## Standard Pattern

All CLI commands should follow this standard pattern for error handling:

1. Use the `ErrorHandler` utility for all error reporting and tracking
2. Configure the `ErrorHandler` with the command's output format and verbosity settings
3. Implement parent-child operation tracking with proper hierarchy
4. Use standardized error categories and severity levels
5. Provide contextual information for errors when possible
6. Always track operations with the MetadataService

## ErrorHandler Categories

The `ErrorHandler` utility supports the following error categories:

1. **Validation Errors** - User input errors (`handleValidationError`)
2. **Expected Errors** - Anticipated errors during normal operation (`handleError`)
3. **Unexpected Errors** - Runtime exceptions and system errors (`handleUnexpectedError`)

## Implementation Details

### Command Setup

Every command should:

1. Initialize an `ErrorHandler` instance in the constructor
2. Configure it with the command's output format and verbosity

```java
// Initialize utility instances
this.errorHandler = new ErrorHandler(metadataService);

// Set format and verbosity
public void setFormat(String format) {
    this.format = format;
    this.errorHandler.outputFormat(format);
}

public void setVerbose(boolean verbose) {
    this.verbose = verbose;
    this.errorHandler.verbose(verbose);
}
```

### Error Handling Pattern

When handling errors, use the appropriate method:

```java
// For validation errors
Map<String, String> validationErrors = new HashMap<>();
validationErrors.put("paramName", "Invalid parameter value");
return errorHandler.handleValidationError(operationId, "command-name", validationErrors);

// For expected errors
return errorHandler.handleError(operationId, "command-name", 
    "Error message with context", exception);

// For unexpected errors
return errorHandler.handleUnexpectedError(operationId, "command-name", exception);
```

### Operation Tracking

Always track operations hierarchically:

```java
// Main operation tracker
String mainOperationId = metadataService.startOperation("command-name", "OPERATION_TYPE", params);

// Sub-operation tracker
String subOperationId = metadataService.trackOperation("command-name-sub-operation", subParams);

// Link sub-operation to parent
metadataService.trackOperationDetail(mainOperationId, "subOperation", subOperationId);
```

## Migration Steps

To migrate existing commands to the standardized approach:

1. Add the `ErrorHandler` field and initialization
2. Set format and verbosity configuration methods
3. Replace direct `System.err.println` with `errorHandler.outputError`
4. Replace custom error format handling with `ErrorHandler` methods
5. Update operation tracking to use the standardized approach
6. Use appropriate error categories based on error type

## Error Severity Levels

Error severity levels should be tracked as part of the operation details:

- **VALIDATION** - User input errors
- **WARNING** - Non-fatal issues that might affect operation
- **ERROR** - Fatal issues that prevent operation completion
- **SYSTEM** - System-level errors (file system, network, etc.)
- **SECURITY** - Security-related issues

## JSON Error Structure

All JSON error responses should follow this structure:

```json
{
  "result": "error",
  "command": "command-name",
  "message": "Human-readable error message",
  "severity": "ERROR|WARNING|VALIDATION|SYSTEM|SECURITY",
  "details": {
    "field": "Field with error (for validation errors)",
    "context": "Additional context",
    "code": "Error code if applicable"
  },
  "stackTrace": "Stack trace (only if verbose mode)"
}
```

## Command Standardization Checklist

For each command implementation:

- [ ] Initialize `ErrorHandler` in constructor
- [ ] Configure it with format and verbosity settings
- [ ] Use standard error categories
- [ ] Track operations hierarchically
- [ ] Provide contextual information
- [ ] Use consistent exit codes
- [ ] Handle both interactive and non-interactive modes

## Examples

### Reference Implementations

The following commands have been refactored to use the standardized error handling approach:

1. **AdminComplianceCommand**
   - First reference implementation of standardized error handling
   - Uses proper severity levels for different error cases
   - Implements hierarchical operation tracking
   - Provides detailed context for validation errors

2. **AdminAuditCommand**
   - Uses standardized error handling for audit operations
   - Implements severity-appropriate error handling
   - Provides comprehensive operation tracking
   - Handles both interactive and non-interactive modes

3. **AdminDiagnosticsCommand**
   - Implements standardized approach for system diagnostics
   - Uses proper severity levels for system-related operations
   - Handles complex error conditions with appropriate context
   - Provides detailed tracking for diagnostic sub-operations

4. **AdminRecoveryCommand**
   - Implements standardized approach for recovery operations
   - Uses hierarchical tracking for complex operation sequences
   - Handles interactive confirmation workflow with proper tracking
   - Implements proper parent-child operation relationships
   - Uses appropriate severity levels for recovery operations:
     - VALIDATION for missing/invalid parameters
     - ERROR for operation failures
     - SYSTEM for unexpected exceptions

## Testing

Each command should have test cases for:

1. Validation error handling
2. Expected error handling
3. Unexpected error handling
4. JSON and text output formats
5. Verbose mode behavior
6. Operation tracking integration
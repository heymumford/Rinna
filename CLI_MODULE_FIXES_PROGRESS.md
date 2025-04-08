# CLI Module Command Implementation Progress

This document tracks progress updating CLI commands to use the ViewCommand pattern with proper MetadataService integration.

## Command Update Status

| Command | Status | Date | Notes |
|---------|--------|------|-------|
| ViewCommand | ✅ Done | Prior | Reference implementation pattern |
| BugCommand | ✅ Done | Prior | Updated to match ViewCommand pattern |
| LoginCommand | ✅ Done | Prior | Updated to match ViewCommand pattern |
| LogoutCommand | ✅ Done | Prior | Updated to match ViewCommand pattern |
| GrepCommand | ✅ Done | 2025-04-08 | Added MetadataService integration and operation tracking |
| StatsCommand | ✅ Done | 2025-04-08 | Added MetadataService integration and operation tracking |
| MsgCommand | ✅ Done | 2025-04-08 | Added MetadataService integration and operation tracking |
| CriticalPathCommand | ✅ Done | Prior | Already using the ViewCommand pattern |
| AddCommand | ⏳ Pending | | |
| ListCommand | ⏳ Pending | | |
| UpdateCommand | ⏳ Pending | | |
| ImportCommand | ⏳ Pending | | |
| BacklogCommand | ⏳ Pending | | |
| UserAccessCommand | ⏳ Pending | | |
| LsCommand | ⏳ Pending | | |
| CatCommand | ⏳ Pending | | |
| FindCommand | ⏳ Pending | | |
| OperationsCommand | ⏳ Pending | | |
| DoneCommand | ⏳ Pending | | |
| HistoryCommand | ⏳ Pending | | |
| AdminCommand | ⏳ Pending | | |
| EditCommand | ⏳ Pending | | |
| UndoCommand | ⏳ Pending | | |
| ScheduleCommand | ⏳ Pending | | |
| ReportCommand | ⏳ Pending | | |
| CommentCommand | ⏳ Pending | | |
| TestCommand | ⏳ Pending | | |
| ServerCommand | ⏳ Pending | | |
| NotifyCommand | ⏳ Pending | | |
| WorkflowCommand | ⏳ Pending | | |
| BulkCommand | ⏳ Pending | | |

## Pattern Implementation Summary

The ViewCommand pattern includes the following key components:

1. **Constructor Consistency**: Both default constructor and service-injected constructor
   ```java
   public CommandName() {
       this(ServiceManager.getInstance());
   }
   
   public CommandName(ServiceManager serviceManager) {
       this.serviceManager = serviceManager;
       this.metadataService = serviceManager.getMetadataService();
       // Other service initialization
   }
   ```

2. **Operation Tracking**: Standard pattern for tracking command operations
   ```java
   // Start tracking with parameters
   Map<String, Object> params = new HashMap<>();
   params.put("param1", value1);
   String operationId = metadataService.startOperation("command-name", "OPERATION_TYPE", params);
   
   try {
       // Command implementation
       
       // Success tracking
       Map<String, Object> result = new HashMap<>();
       result.put("result_key", resultValue);
       metadataService.completeOperation(operationId, result);
       return 0;
   } catch (Exception e) {
       // Error tracking
       metadataService.failOperation(operationId, e);
       return 1;
   }
   ```

3. **Method Signatures**: For helper methods, include operation ID parameter
   ```java
   private int helperMethod(String param, String operationId) {
       // Method implementation
       
       // Success tracking
       Map<String, Object> result = new HashMap<>();
       result.put("result_key", resultValue);
       metadataService.completeOperation(operationId, result);
       return 0;
   }
   ```

4. **Common Parameters**: Standard parameters for configuration
   ```java
   private String format = "text"; // Output format
   private boolean verbose = false; // Verbose output flag
   ```

5. **Consistent Error Handling**: Common approach for error handling
   ```java
   try {
       // Command implementation
   } catch (Exception e) {
       System.err.println("Error: " + e.getMessage());
       if (verbose) {
           e.printStackTrace();
       }
       metadataService.failOperation(operationId, e);
       return 1;
   }
   ```

## Next Steps

1. Continue updating remaining CLI commands to follow the ViewCommand pattern
2. Improve unit tests for CLI commands to verify MetadataService integration
3. Create integration tests to verify command operation tracking
4. Update CLI documentation to reflect operation tracking capabilities
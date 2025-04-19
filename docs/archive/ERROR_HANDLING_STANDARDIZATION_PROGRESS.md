# Error Handling Standardization Progress

This document tracks the progress of refactoring CLI commands to use standardized error handling with the `ErrorHandler` utility.

## Completed Commands

| Command | Status | Notes |
|---------|--------|-------|
| AdminComplianceCommand | ✅ Done | First reference implementation with comprehensive severity levels |
| AdminAuditCommand | ✅ Done | Uses standardized error handling with appropriate severity levels |
| AdminDiagnosticsCommand | ✅ Done | Implements proper error handling for system operations |
| AdminRecoveryCommand | ✅ Done | Complex implementation with hierarchical operation tracking and interactive workflows |

## High-Priority Commands (Next)

| Command | Status | Notes |
|---------|--------|-------|
| ServerCommand | 🔄 Planned | Needs to handle service availability errors and process management |
| UpdateCommand | 🔄 Planned | Requires validation error handling with field-level details |

## Medium-Priority Commands

| Command | Status | Notes |
|---------|--------|-------|
| FindCommand | 🔄 Planned | Needs to handle search parameter validation and query errors |
| EditCommand | 🔄 Planned | Requires detailed validation error reporting |
| BulkCommand | 🔄 Planned | Complex validation with per-item error tracking |
| WorkflowCommand | 🔄 Planned | Needs to handle state transition errors with proper validation |

## Standard Commands

| Command | Status | Notes |
|---------|--------|-------|
| LoginCommand | 🔄 Planned | Authentication failure handling with security severity |
| LogoutCommand | 🔄 Planned | Simple implementation with minimal error cases |
| ListCommand | 🔄 Planned | Filter validation and result handling |
| ViewCommand | 🔄 Planned | Item not found handling and format conversion |
| AddCommand | 🔄 Planned | Complex validation with field-level details |
| BacklogCommand | 🔄 Planned | Needs prioritization error handling |
| DoneCommand | 🔄 Planned | State transition validation |
| HistoryCommand | 🔄 Planned | Date range validation and filtering errors |
| LsCommand | 🔄 Planned | Directory and pattern validation |
| CatCommand | 🔄 Planned | File not found and format errors |
| GrepCommand | 🔄 Planned | Pattern validation and search errors |
| StatsCommand | 🔄 Planned | Date range and metric validation |
| NotifyCommand | 🔄 Planned | Recipient validation and delivery errors |
| ReportCommand | 🔄 Planned | Template and format validation |
| TestCommand | 🔄 Planned | Test execution and validation errors |
| MsgCommand | 🔄 Planned | Recipient and message validation |
| BugCommand | 🔄 Planned | Bug report validation with required fields |
| UserAccessCommand | 🔄 Planned | Permission validation and security errors |
| ImportCommand | 🔄 Planned | Format validation and data errors |
| OperationsCommand | 🔄 Planned | Query validation and result filtering |
| ScheduleCommand | 🔄 Planned | Date validation and conflict detection |

## Implementation Notes

### Key Patterns Identified

1. **Validation Handling**: Field-level validation with detailed context
2. **Operation Tracking**: Hierarchical tracking with parent-child relationships
3. **Interactive vs. Non-interactive**: Different handling based on format
4. **Severity Levels**: Appropriate severity for different error types
5. **Context Enrichment**: Adding operational context to error details

### Common Refactoring Steps

1. Add `ErrorHandler` and `OperationTracker` fields
2. Initialize in constructor
3. Update `setFormat` and `setVerbose` methods to configure `ErrorHandler`
4. Replace direct error output with `ErrorHandler` methods
5. Replace direct operation tracking with `OperationTracker` methods
6. Add severity-appropriate error handling
7. Implement hierarchical operation tracking
8. Remove custom error output methods

### Testing Approach

For each refactored command:

1. Add unit tests for validation errors
2. Add unit tests for expected errors
3. Add unit tests for unexpected errors
4. Verify JSON output format matches standardized structure
5. Verify operation tracking with sub-operations
6. Test both interactive and non-interactive modes
# ModelMapper Testing Plan

## Overview
This document outlines the testing approach for the `ModelMapper` utility class that was created as part of the CLI module compatibility fixes. The `ModelMapper` is a crucial component that provides bidirectional conversion between CLI model classes and domain model classes, enabling the CLI module to work with the updated domain model while maintaining backward compatibility.

## Testing Goals
1. Verify that bidirectional mapping between CLI and domain models is accurate
2. Ensure all model attributes are correctly mapped
3. Test edge cases, including null values
4. Validate that default values are applied correctly when properties are missing
5. Verify that the utility class cannot be instantiated (enforcing its static nature)

## Test Cases Implemented
The following test cases have been implemented in `ModelMapperTest.java`:

1. **WorkflowState Mapping Tests**
   - `shouldMapCliWorkflowStateToDomainWorkflowState`: Tests mapping from CLI WorkflowState to domain WorkflowState
   - `shouldMapDomainWorkflowStateToCliWorkflowState`: Tests mapping from domain WorkflowState to CLI WorkflowState
   
2. **Priority Mapping Tests**
   - `shouldMapCliPriorityToDomainPriority`: Tests mapping from CLI Priority to domain Priority
   - `shouldMapDomainPriorityToCliPriority`: Tests mapping from domain Priority to CLI Priority
   
3. **WorkItemType Mapping Tests**
   - `shouldMapCliWorkItemTypeToDomainWorkItemType`: Tests mapping from CLI WorkItemType to domain WorkItemType
   - `shouldMapDomainWorkItemTypeToCliWorkItemType`: Tests mapping from domain WorkItemType to CLI WorkItemType
   
4. **WorkItem Conversion Tests**
   - `shouldConvertCliWorkItemToDomainWorkItem`: Tests conversion from CLI WorkItem to domain WorkItem
   - `shouldConvertDomainWorkItemToCliWorkItem`: Tests conversion from domain WorkItem to CLI WorkItem
   
5. **Edge Case Tests**
   - `shouldHandleNullValuesInAllMappingMethods`: Tests handling of null values in all mapping methods
   - `shouldCreateDomainWorkItemWithDefaultValuesWhenCliWorkItemHasNullProperties`: Tests default value application
   
6. **Utility Class Design Test**
   - `shouldNotInstantiateModelMapper`: Verifies that the ModelMapper cannot be instantiated

## Key Mapping Validations

### WorkflowState Mappings
- CLI `CREATED` ↔ Domain `FOUND`
- CLI `READY` ↔ Domain `TO_DO` / `TRIAGED`
- CLI `IN_PROGRESS` ↔ Domain `IN_PROGRESS`
- CLI `REVIEW` / `TESTING` ↔ Domain `IN_TEST`
- CLI `DONE` ↔ Domain `DONE` / `RELEASED`
- CLI `BLOCKED` → Domain `TO_DO` (no direct mapping back)

### Priority Mappings
Direct name-based mappings:
- `LOW` ↔ `LOW`
- `MEDIUM` ↔ `MEDIUM`
- `HIGH` ↔ `HIGH`
- `CRITICAL` ↔ `CRITICAL`

### WorkItemType Mappings
- CLI `BUG` ↔ Domain `BUG`
- CLI `TASK` / `SPIKE` ↔ Domain `CHORE`
- CLI `FEATURE` / `STORY` ↔ Domain `FEATURE`
- CLI `EPIC` ↔ Domain `GOAL`

### WorkItem Attribute Mappings
- Basic attributes (id, title, description, assignee) are mapped directly
- Type-specific attributes (type, priority, state) use the corresponding type mapping methods
- Special handling for metadata to capture project and dueDate information

## Default Values
The following default values are applied when converting CLI WorkItem to domain WorkItem with missing properties:
- `id`: New random UUID if null
- `type`: `CHORE` if null
- `priority`: `MEDIUM` if null
- `state`: `FOUND` if null

## Conclusion
The comprehensive test suite for the ModelMapper utility class ensures that all bidirectional mappings between CLI models and domain models work correctly. This is a critical component of the CLI module compatibility fix, as it allows the CLI module to continue functioning with its existing model classes while interacting with the refactored domain model that follows Clean Architecture principles.

When the build environment issues are resolved, these tests should be run as part of the regular test suite to ensure ongoing compatibility between the CLI and domain layers.
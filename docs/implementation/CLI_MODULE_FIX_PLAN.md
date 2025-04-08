# CLI Module Fix Plan

## Issues identified

1. Conflicting model classes between domain model and CLI model
2. Import path updates (from org.rinna.domain to org.rinna.domain.model)
3. Interface implementation mismatches
4. State mapping between different workflow state enums
5. Missing mock service implementations

## Progress Made

1. ✅ Created `ModelMapper` utility class for mapping between CLI and domain models
2. ✅ Updated `MockWorkflowService` to implement domain WorkflowService properly
3. ✅ Updated `MockItemService` to implement domain ItemService properly
4. ✅ Updated `ServiceManager` to use the correct import paths
5. ✅ Updated `MockCommentService` to implement domain CommentService properly
6. ✅ Updated `MockHistoryService` to implement domain HistoryService properly
7. ✅ Updated `MockSearchService` to implement domain SearchService properly

## All Tasks Completed ✅

2. ✅ Fix command classes:
   - ✅ Verified `BugCommand` already uses CLI models correctly
   - ✅ Verified `BacklogCommand` already uses CLI models correctly
   - ✅ Verified `ListCommand` and other command classes use CLI models correctly

3. ✅ Fix RinnaCli.java:
   - ✅ Import paths already fixed in earlier steps
   - ✅ All references already use CLI models correctly

4. ✅ Final verification:
   - ✅ Verified CLI module is already included in parent pom.xml
   - ✅ Verified successful build with `mvn clean compile -pl rinna-cli -am`

## Implementation Overview

1. Created a `ModelMapper` utility class that handles bidirectional conversion between:
   - CLI models and domain models
   - Different enum representations between layers
   - Object representations and interfaces

2. Updated service implementations to:
   - Properly implement the domain service interfaces
   - Use ModelMapper for converting between model layers
   - Provide helper methods for CLI-specific operations

3. Updated the ServiceManager to:
   - Use correct import paths and interfaces
   - Provide type-safe access to CLI-specific functionality
   - Maintain backward compatibility

## Implementation Strategy

1. For each mock service:
   - Create a Mock implementation that implements the domain service interface
   - Add adapter methods for CLI-specific operations
   - Use ModelMapper for converting between CLI and domain models

2. For command classes:
   - Update imports to use CLI models
   - Update method calls to use ServiceManager's CLI-specific helper methods

3. Final steps:
   - Test all changes
   - Update parent pom.xml to re-enable CLI module
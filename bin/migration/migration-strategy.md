# Package Structure Migration Strategy

This document outlines the strategy for migrating from the old deep package structure to the new flattened structure in the Rinna project.

## Completed Changes

- √ Domain entities moved from `org.rinna.domain.entity` to `org.rinna.domain.model`
- √ Service interfaces moved from `org.rinna.domain.usecase` to `org.rinna.domain.service`
- √ Service implementations moved from `org.rinna.service.impl` to `org.rinna.adapter.service`
- √ Repository implementations moved from `org.rinna.persistence` to `org.rinna.adapter.repository`
- √ Updated rinna-core module to compile and pass tests with the new structure

## Transition Strategy

The migration to the new package structure will follow a staged approach to minimize disruption:

### Phase 1: Core Module Transition (Completed)

- The rinna-core module now uses the new package structure
- All tests in the rinna-core module pass with the new structure
- The Main class has been updated to use the new initialization approach

### Phase 2: Test Code Alignment (In Progress)

- Use the `fix-test-imports.sh` script to update import statements in test files
- Resolve any compilation issues in test code

### Phase 3: API Module Transition

- Update the API module to use the new package structure
- Ensure API endpoints continue to work with the new structure
- Update API tests to use the new structure

### Phase 4: CLI Module Transition

- Update the CLI module to use the new package structure
- Ensure CLI commands continue to work with the new structure
- Update CLI tests to use the new structure

### Phase 5: Backward Compatibility

For backward compatibility, we maintain the following strategies:

1. **Type-Safe Migration**: All classes with the same name (but in different packages) implement the same interfaces
2. **Legacy Package Markers**: Older packages are marked as deprecated but maintained
3. **Automatic Import Fixing**: Scripts are provided to automatically update import statements

## Migration Scripts

The following scripts are available to help with the migration:

- `fix-test-imports.sh`: Updates import statements in test files
- `setup-src-structure.sh`: Sets up the correct directory structure for the src directory

## Handling External Code

For external code that depends on the old package structure:

1. **Option 1: Update Import Statements**
   - Use the migration scripts to update import statements in external code
   - This is the recommended approach for code that you control

2. **Option 2: Use Legacy Compatibility**
   - The legacy packages are still available for backward compatibility
   - This is for code that you cannot easily modify

3. **Option 3: Interface-Based Programming**
   - Use interfaces instead of concrete implementations
   - This allows for flexibility in the underlying implementations

## Testing Strategy

For each phase of the migration, we follow this testing strategy:

1. Ensure all unit tests pass
2. Run integration tests to verify component interactions
3. Perform manual testing of key functionality
4. Verify that the API and CLI still function correctly

## Timeline

- Phase 1 (Core Module Transition): Completed
- Phase 2 (Test Code Alignment): In progress
- Phase 3 (API Module Transition): Pending
- Phase 4 (CLI Module Transition): Pending
- Phase 5 (Backward Compatibility): Ongoing

## Dependencies

- Java 21 (for pattern matching and other features used in core code)
- Maven 3.8+ (for building and testing)
- JUnit 5 (for running tests)

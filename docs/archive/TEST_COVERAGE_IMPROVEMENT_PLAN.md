# Test Coverage Improvement Plan

Based on the test coverage analysis documented in `TEST_COVERAGE_SUMMARY.md`, this document outlines a plan to improve test coverage across the Rinna project modules.

## Current Coverage Status

- **rinna-core**: 16.66% file-level coverage (20/120 files have tests)
- **rinna-cli**: 4.60% file-level coverage (7/152 files have tests)
- Overall test distribution follows the test pyramid structure

## Improvement Goals

1. **Short-term goals (1-2 weeks)**:
   - Increase rinna-cli file-level coverage to at least 20%
   - Increase rinna-core file-level coverage to at least 30%
   - Fix and complete the ModelMapper tests

2. **Medium-term goals (1-2 months)**:
   - Increase rinna-cli file-level coverage to at least 40%
   - Increase rinna-core file-level coverage to at least 50%
   - Implement proper JaCoCo integration for line-level coverage metrics

3. **Long-term goals (3-6 months)**:
   - Achieve 70% file-level coverage across all modules
   - Achieve 80% line-level coverage for critical components
   - Establish automated coverage reporting in CI pipeline

## Priority Areas

### rinna-cli Module

The CLI module has the lowest coverage (4.60%) and should be the primary focus. Priority files to test:

1. **Command Implementations (High Priority)**
   - File: `AddCommand.java`
   - File: `ListCommand.java`
   - File: `UpdateCommand.java`
   - File: `ViewCommand.java`
   - File: `GrepCommand.java` (already has tests)

2. **Utility Classes (High Priority)**
   - File: `ModelMapper.java` (implementation completed; tests need fixing)
   - File: `StateMapper.java` (requires tests)

3. **Service Mock Implementations (Medium Priority)**
   - File: `MockItemService.java`
   - File: `MockHistoryService.java`
   - File: `MockCommentService.java`
   - File: `MockWorkflowService.java`

### rinna-core Module

For the core module, focus on the domain model and repository implementations:

1. **Domain Model (High Priority)**
   - File: `DefaultWorkItem.java`
   - File: `WorkItemRecord.java`
   - File: `DefaultProject.java`
   - File: `DefaultRelease.java`

2. **Repository Implementations (Medium Priority)**
   - File: `InMemoryItemRepository.java`
   - File: `InMemoryMetadataRepository.java`
   - File: `InMemoryQueueRepository.java`
   - File: `InMemoryReleaseRepository.java`

3. **Service Implementations (Medium Priority)**
   - File: `DefaultItemService.java`
   - File: `DefaultWorkflowService.java`
   - File: `DefaultReleaseService.java`

## Implementation Approach

### 1. Setup Proper Testing Infrastructure

- Fix JaCoCo configuration in Maven to properly generate coverage reports
- Create test helper classes for common test operations
- Set up test data generation utilities

### 2. Create Templates for Each Test Type

Develop standardized templates for different types of tests:

```java
// Unit Test Template for Service Classes
@DisplayName("ServiceName Unit Tests")
public class ServiceNameTest {
    
    @Test
    @DisplayName("Should perform expected operation correctly")
    void shouldPerformExpectedOperation() {
        // Arrange
        // Act
        // Assert
    }
    
    @Test
    @DisplayName("Should handle error scenarios gracefully")
    void shouldHandleErrorScenarios() {
        // Arrange
        // Act
        // Assert
    }
}
```

### 3. Tackle Each Priority Area Sequentially

1. **CLI Command Tests**:
   - Create unit tests for basic command functionality
   - Create component tests for command integration with services
   - Test all supported options and parameters

2. **Domain Model Tests**:
   - Test model construction and validation
   - Test state transitions and business rules
   - Test serialization/deserialization if applicable

3. **Repository Tests**:
   - Test CRUD operations
   - Test query operations
   - Test error handling and edge cases

4. **Service Tests**:
   - Test business logic implementation
   - Test integration with repositories
   - Test validation and error handling

### 4. Monitor Progress

- Run the test coverage report script weekly to track progress
- Adjust priorities based on findings
- Document patterns and anti-patterns found during testing

## Integration with Development Process

To ensure ongoing test coverage improvement:

1. **New Feature Development**:
   - Require unit tests for all new features
   - Add test coverage checks to pull request reviews

2. **Bug Fixes**:
   - Require regression tests for all bug fixes
   - Use test-driven development for bug fixes

3. **Refactoring**:
   - Ensure tests are updated during refactoring
   - Use tests to validate refactoring changes

## Success Metrics

The following metrics will be used to track progress:

1. **File-level Coverage**: Percentage of source files with corresponding test files
2. **Line-level Coverage**: Percentage of lines covered by tests (when JaCoCo is properly integrated)
3. **Test-to-Source Ratio**: Ratio of test files to source files
4. **Test Distribution**: Adherence to the test pyramid structure

## Timeline

| Milestone | Target Date | Success Criteria |
|-----------|-------------|------------------|
| Initial Setup | Week 1 | JaCoCo integration, templates created |
| CLI Command Tests | Week 2-3 | 20% coverage for rinna-cli |
| Domain Model Tests | Week 4-5 | 30% coverage for rinna-core |
| Repository Tests | Week 6-7 | 40% coverage for rinna-cli |
| Service Tests | Week 8-9 | 50% coverage for rinna-core |
| Final Review | Week 10 | Full report, identify remaining gaps |

## Conclusion

By implementing this test coverage improvement plan, we will significantly enhance the quality and reliability of the Rinna project. The focus on both the CLI and core modules ensures that critical components are well-tested, while the structured approach ensures efficient use of development resources.
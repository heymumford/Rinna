# Rinna Testing Guide

This guide covers Rinna's comprehensive testing approach across multiple languages and components.

## Testing Philosophy

Rinna follows a comprehensive testing approach that combines:

1. **Test-Driven Development (TDD)** for core domain logic
2. **Behavior-Driven Development (BDD)** for user-facing features
3. **Clean Architecture Testing** for ensuring architectural boundaries
4. **API Integration Testing** for validating service interfaces

Tests are first-class citizens in the codebase and are treated with the same care as production code.

## Testing Pyramid

Rinna's testing strategy follows the testing pyramid approach:

```
        ▲ Fewer
        │
        │    ┌───────────────┐
        │    │  Performance  │ Slowest, most complex
        │    └───────────────┘
        │    ┌───────────────┐
        │    │  Acceptance   │ End-to-end workflows
        │    └───────────────┘
        │    ┌───────────────┐
        │    │  Integration  │ Tests between modules
        │    └───────────────┘
        │    ┌───────────────┐
        │    │   Component   │ Tests within modules
        │    └───────────────┘
        │    ┌───────────────┐
        │    │     Unit      │ Fastest, most granular
        │    └───────────────┘
        │
        ▼ More
```

## Running Tests

There are two ways to run tests:

### Using the build.sh Script

The most reliable way to run tests is using the root build script:

```bash
# Run all tests
./build.sh all test

# Run tests for specific components
./build.sh java test
./build.sh python test
./build.sh go test
```

### Using the rin Command

Once the `rin` CLI is built and available, you can use it for more granular test control:

```bash
# Run all tests
rin test all

# Run tests by pyramid layer
rin test unit          # Run unit tests only
rin test component     # Run component tests only
rin test integration   # Run integration tests only
rin test acceptance    # Run acceptance tests only

# Run test combinations
rin test fast          # Run unit and component tests (quick feedback)
rin test essential     # Run unit, component, and integration tests

# Run domain-specific tests
rin test domain:workflow  # Run workflow domain tests
rin test domain:release   # Run release domain tests

# Run with options
rin test unit --parallel  # Run in parallel
rin test unit --coverage  # Generate coverage report
rin test unit --watch     # Watch mode for continuous testing
```

**Important Note**: If you're modifying the Rinna CLI itself, you should use `./build.sh` directly for testing rather than the `rin` command, as the `rin` command might not reflect your latest changes until after a successful build.

## Unit Testing

Unit tests focus on individual classes and methods in isolation:

```java
@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository repository;
    
    private ItemService service;
    
    @BeforeEach
    void setUp() {
        service = new DefaultItemService(repository);
    }
    
    @Test
    @DisplayName("should find items by status")
    void shouldFindItemsByStatus() {
        // Arrange
        List<WorkItem> items = List.of(createTestItem("Test"));
        when(repository.findByStatus(WorkflowState.FOUND)).thenReturn(items);
        
        // Act
        List<WorkItem> result = service.findByStatus(WorkflowState.FOUND);
        
        // Assert
        assertEquals(1, result.size());
        assertEquals("Test", result.get(0).getTitle());
    }
}
```

## BDD Testing

BDD tests use Cucumber to define behavior in business language:

```gherkin
Feature: Workflow State Transitions
  As a team member
  I want to transition work items through defined workflows
  So that I can track progress effectively

  Scenario: Transitioning a bug from Found to Triaged
    Given a bug work item with title "Authentication failure" in state "FOUND"
    When I transition the work item to "TRIAGED"
    Then the work item should be in state "TRIAGED"
    And a transition event should be recorded with from state "FOUND" and to state "TRIAGED"
```

Step definitions implement the behavior:

```java
public class WorkflowSteps {
    
    private TestContext context;
    private WorkflowService workflowService;
    
    @Given("a bug work item with title {string} in state {string}")
    public void aWorkItemWithTitle(String title, String state) {
        WorkItem item = createWorkItem(title, WorkItemType.BUG, 
                                       WorkflowState.valueOf(state));
        context.setWorkItem(item);
    }
    
    @When("I transition the work item to {string}")
    public void iTransitionTheWorkItem(String targetState) {
        WorkItem item = context.getWorkItem();
        WorkItem updated = workflowService.transition(item.getId(), 
                                                    WorkflowState.valueOf(targetState));
        context.setWorkItem(updated);
    }
    
    @Then("the work item should be in state {string}")
    public void theWorkItemShouldBeInState(String expectedState) {
        WorkItem item = context.getWorkItem();
        assertEquals(WorkflowState.valueOf(expectedState), item.getStatus());
    }
}
```

## API Integration Testing

API integration tests validate the external REST API:

```gherkin
Feature: API Integration
  
  @json-api
  Scenario: Accepting a valid JSON payload with authentication
    Given an API authentication token "ri-5e7a9b3f2c8d" for project "billing-system"
    When the following JSON payload is submitted with the token:
      """
      {
        "type": "FEATURE",
        "title": "Support for cryptocurrency payments",
        "description": "Add support for Bitcoin and Ethereum payments",
        "priority": "HIGH"
      }
      """
    Then a work item should be created with the specified attributes
    And the work item should be associated with project "billing-system"
```

## Code Coverage

Code coverage is tracked using JaCoCo:

```bash
# Generate coverage report with build.sh
./build.sh all test

# Or with rin CLI when available
rin build test --coverage

# Full verification including coverage
rin build verify
```

The coverage report is available at `rinna-core/target/site/jacoco/index.html` and includes:
- Line coverage (target: 90%+)
- Branch coverage (target: 80%+)
- Method coverage (target: 95%+)

## Adding New Tests

### Unit Test

```java
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.rinna.base.UnitTest;

@DisplayName("My feature tests")
class MyFeatureTest extends UnitTest {
    @Test
    @DisplayName("should work correctly")
    void shouldWorkCorrectly() {
        // Test implementation
    }
}
```

### BDD Test

1. Add a scenario to a .feature file
2. Implement step definitions
3. Run with `./build.sh all test` or `rin test bdd` (if available)

## Troubleshooting Test Failures

If you encounter test failures:

1. **Use Direct Testing**: Run tests with `./build.sh all test` to eliminate any issues with the CLI
2. **Check Logs**: Test logs are stored in the `logs/` directory
3. **Run Specific Tests**: Isolate failing tests by running specific test categories
4. **Clean Before Testing**: Try `./build.sh clean` before running tests again
5. **Check Dependencies**: Ensure all test dependencies are correctly installed

## Test Tools

- **JUnit 5**: Core testing framework
- **Mockito**: Mocking framework
- **AssertJ**: Fluent assertions
- **Cucumber**: BDD testing
- **JaCoCo**: Code coverage
- **Go test framework**: For API testing
- **pytest**: For Python tests

## Best Practices

1. **One Assert Per Test**: Focus each test on one specific behavior
2. **Descriptive Test Names**: Use explicit names that describe the behavior
3. **Arrange-Act-Assert**: Structure tests in this consistent pattern
4. **Isolation**: Tests should not depend on other tests
5. **Fast Execution**: Keep unit tests fast (<100ms per test)
6. **Use Appropriate Layer**: Put tests in the right pyramid layer
7. **Watch Mode**: Use watch mode for quick feedback cycles
8. **Fallback to build.sh**: When in doubt, use the root build script

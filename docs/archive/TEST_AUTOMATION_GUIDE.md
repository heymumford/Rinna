# Comprehensive Test Automation Guide for Rinna

This guide provides a complete reference for implementing, running, and managing automated tests across the Rinna multi-language codebase (Java, Go, Python). It builds upon our [Test Pyramid Strategy](TEST_PYRAMID.md), [Unified Test Approach](UNIFIED_TEST_APPROACH.md), and [Testing Strategy](TESTING_STRATEGY.md) to provide practical implementation guidance.

## Table of Contents

1. [Test Pyramid Overview](#test-pyramid-overview)
2. [Setting Up Test Infrastructure](#setting-up-test-infrastructure)
3. [Writing Tests](#writing-tests)
   - [Unit Tests](#unit-tests)
   - [Component Tests](#component-tests)
   - [Integration Tests](#integration-tests)
   - [Acceptance Tests](#acceptance-tests)
   - [Performance Tests](#performance-tests)
4. [Cross-Language Testing](#cross-language-testing)
5. [Running Tests](#running-tests)
6. [Testing Tools and Scripts](#testing-tools-and-scripts)
7. [CI/CD Integration](#cicd-integration)
8. [Test Coverage](#test-coverage)
9. [Best Practices](#best-practices)
10. [Troubleshooting](#troubleshooting)

## Test Pyramid Overview

The Rinna testing strategy follows a classic five-layer test pyramid:

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

For a more detailed overview of our test pyramid structure, see the [Test Pyramid Strategy](TEST_PYRAMID.md) document.

## Setting Up Test Infrastructure

### Prerequisites

Before writing tests, ensure you have:

1. JDK 21 installed
2. Go 1.21+ installed
3. Python 3.8+ installed
4. Maven configured
5. Environment set up using `bin/rin-setup-unified`

### Test Directory Structure

All tests should follow this standardized structure:

**Java:**
```
src/test/java/org/rinna/
├── unit/           # Unit tests 
├── component/      # Component tests
├── integration/    # Integration tests
├── acceptance/     # Acceptance tests
│   └── steps/      # Step definitions for BDD tests
└── performance/    # Performance tests

src/test/resources/
├── features/       # BDD feature files
└── testdata/       # Test data files
```

**Go:**
```
api/test/
├── unit/           # Unit tests
├── component/      # Component tests
├── integration/    # Integration tests
├── acceptance/     # Acceptance tests
└── performance/    # Performance tests
```

**Python:**
```
python/tests/
├── unit/           # Unit tests
├── component/      # Component tests
├── integration/    # Integration tests
├── acceptance/     # Acceptance tests
└── performance/    # Performance tests
```

### Configuration Files

Configure the test environment with these files:

- `src/test/resources/junit-platform.properties` - JUnit configuration
- `src/test/resources/cucumber.properties` - Cucumber BDD configuration
- `pyproject.toml` - Python test configuration

## Writing Tests

### Unit Tests

Unit tests validate individual components in isolation, with all dependencies mocked.

#### Java Unit Tests Example

```java
// In src/test/java/org/rinna/unit/ItemServiceTest.java
@Tag("unit")
public class ItemServiceTest {
    
    @Mock
    private ItemRepository itemRepository;
    
    @InjectMocks
    private DefaultItemService itemService;
    
    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }
    
    @Test
    void shouldCreateNewItem() {
        // Arrange
        WorkItemCreateRequest request = new WorkItemCreateRequest("Test Item", WorkItemType.BUG);
        WorkItem createdItem = new DefaultWorkItem(1L, "Test Item", WorkItemType.BUG);
        
        when(itemRepository.save(any(WorkItem.class))).thenReturn(createdItem);
        
        // Act
        WorkItem result = itemService.createWorkItem(request);
        
        // Assert
        assertEquals("Test Item", result.getTitle());
        assertEquals(WorkItemType.BUG, result.getType());
        verify(itemRepository).save(any(WorkItem.class));
    }
}
```

#### Go Unit Tests Example

```go
// In api/pkg/health/health_test.go
package health

import (
    "testing"
)

func TestHealthStatus(t *testing.T) {
    // Arrange
    health := NewHealthChecker()
    
    // Act
    status := health.Status()
    
    // Assert
    if status != "UP" {
        t.Errorf("Expected status to be UP, got %s", status)
    }
}
```

#### Python Unit Tests Example

```python
# In python/tests/unit/test_version.py
import pytest
from rinna.version import get_version

def test_version_format():
    # Arrange & Act
    version = get_version()
    
    # Assert
    assert isinstance(version, str)
    assert len(version.split('.')) == 3  # Should be in format X.Y.Z
```

### Component Tests

Component tests validate behavior of closely related components that work together, with external dependencies mocked.

#### Java Component Tests Example

```java
// In src/test/java/org/rinna/component/WorkflowServiceComponentTest.java
@Tag("component")
public class WorkflowServiceComponentTest {
    
    private ItemRepository itemRepository;
    private WorkflowService workflowService;
    
    @BeforeEach
    public void setup() {
        // Use real repository implementation but with in-memory database
        itemRepository = new InMemoryItemRepository();
        workflowService = new DefaultWorkflowService(itemRepository);
    }
    
    @Test
    void shouldTransitionWorkflowState() {
        // Arrange
        WorkItem workItem = new DefaultWorkItem(1L, "Test Item", WorkItemType.BUG);
        workItem.setState(WorkflowState.FOUND);
        itemRepository.save(workItem);
        
        // Act
        workflowService.transition(workItem.getId(), WorkflowState.TRIAGED);
        
        // Assert
        WorkItem updatedItem = itemRepository.findById(workItem.getId()).orElseThrow();
        assertEquals(WorkflowState.TRIAGED, updatedItem.getState());
    }
}
```

#### Go Component Tests Example

```go
// In api/test/component/config_component_test.go
package component

import (
    "testing"
    "rinna/api/internal/config"
)

func TestConfigLoading(t *testing.T) {
    // Arrange
    configPath := "../../configs/test-config.yaml"
    
    // Act
    cfg, err := config.Load(configPath)
    
    // Assert
    if err != nil {
        t.Fatalf("Failed to load config: %v", err)
    }
    
    if cfg.Server.Port != 8080 {
        t.Errorf("Expected port 8080, got %d", cfg.Server.Port)
    }
}
```

#### Python Component Tests Example

```python
# In python/tests/component/test_component_config.py
import pytest
from rinna.config import ConfigManager
from rinna.logging import Logger

class TestConfigComponent:
    def setup_method(self):
        self.config = ConfigManager("test_config.yaml")
        self.logger = Logger(self.config)
    
    def test_config_integration(self):
        # Test that config and logger components work together properly
        log_level = self.logger.get_level()
        assert log_level == self.config.get("logging.level", "INFO")
```

### Integration Tests

Integration tests validate interactions between separate modules, often across language boundaries, with minimal mocking.

#### Java-Go Integration Test Example

```java
// In src/test/java/org/rinna/integration/ApiIntegrationTest.java
@Tag("integration")
public class ApiIntegrationTest {
    
    private ApiClient apiClient;
    private static Process apiProcess;
    
    @BeforeAll
    static void startApi() throws IOException {
        // Start the Go API server for integration testing
        ProcessBuilder processBuilder = new ProcessBuilder("go", "run", "./api/cmd/rinnasrv/main.go");
        apiProcess = processBuilder.start();
        
        // Wait for API to start
        Thread.sleep(2000);
    }
    
    @AfterAll
    static void stopApi() {
        if (apiProcess != null) {
            apiProcess.destroy();
        }
    }
    
    @BeforeEach
    void setup() {
        apiClient = new ApiClient("http://localhost:8080");
    }
    
    @Test
    void shouldCreateWorkItemThroughApi() {
        // Arrange
        WorkItemCreateRequest request = new WorkItemCreateRequest("API Test Item", WorkItemType.BUG);
        
        // Act
        WorkItem createdItem = apiClient.createWorkItem(request);
        
        // Assert
        assertNotNull(createdItem.getId());
        assertEquals("API Test Item", createdItem.getTitle());
        assertEquals(WorkItemType.BUG, createdItem.getType());
    }
}
```

#### Go Integration Test Example

```go
// In api/test/integration/cli_api_integration_test.go
package integration

import (
    "os/exec"
    "testing"
)

func TestCliApiIntegration(t *testing.T) {
    // Arrange - Ensure API server is running
    
    // Act - Run the CLI command that interacts with the API
    cmd := exec.Command("./bin/rin", "add", "--type=BUG", "--title=Integration Test Bug")
    output, err := cmd.CombinedOutput()
    
    // Assert
    if err != nil {
        t.Fatalf("Failed to run CLI command: %v\nOutput: %s", err, output)
    }
    
    // Verify the item was created in the API
    // This could be done by querying the API directly
}
```

### Acceptance Tests

Acceptance tests validate end-to-end workflows from a user's perspective, typically written in Gherkin syntax for BDD.

#### Gherkin Feature File Example

```gherkin
# In src/test/resources/features/workflow.feature
Feature: Workflow management
  To manage software work clearly and transparently
  As a software engineering team
  We need explicit workflow enforcement

  Background:
    Given the Rinna system is initialized

  Scenario: Creating a new Bug item
    When the developer creates a new Bug with title "Login fails"
    Then the Bug should exist with status "FOUND" and priority "MEDIUM" 

  Scenario: Valid workflow transition
    Given a work item in "FOUND" state
    When I transition the work item to "TRIAGED" state
    Then the transition should succeed
    And the work item should be in "TRIAGED" state
```

#### Java Step Definitions Example

```java
// In src/test/java/org/rinna/acceptance/steps/WorkflowSteps.java
@Tag("acceptance")
public class WorkflowSteps {
    
    private WorkItem currentWorkItem;
    private boolean transitionSuccess;
    private RinnaClient client = new RinnaClient();
    
    @Given("a work item in {string} state")
    public void workItemInState(String state) {
        WorkItemCreateRequest request = new WorkItemCreateRequest("Test Bug", WorkItemType.BUG);
        currentWorkItem = client.createWorkItem(request);
        
        // Ensure item is in the correct initial state
        if (!currentWorkItem.getState().name().equals(state)) {
            // Transition to the required state
            // This may require multiple transitions depending on the workflow rules
        }
    }
    
    @When("I transition the work item to {string} state")
    public void transitionWorkItem(String targetState) {
        try {
            client.transitionWorkItem(currentWorkItem.getId(), WorkflowState.valueOf(targetState));
            transitionSuccess = true;
        } catch (InvalidTransitionException e) {
            transitionSuccess = false;
        }
    }
    
    @Then("the transition should succeed")
    public void transitionShouldSucceed() {
        assertTrue(transitionSuccess);
    }
    
    @Then("the work item should be in {string} state")
    public void workItemShouldBeInState(String expectedState) {
        // Reload the item to get its current state
        WorkItem updatedItem = client.getWorkItem(currentWorkItem.getId());
        assertEquals(WorkflowState.valueOf(expectedState), updatedItem.getState());
    }
}
```

### Performance Tests

Performance tests evaluate system behavior under various load conditions.

#### Java Performance Test Example

```java
// In src/test/java/org/rinna/performance/ApiClientPerfTest.java
@Tag("performance")
public class ApiClientPerfTest {
    
    private ApiClient apiClient;
    
    @BeforeEach
    void setup() {
        apiClient = new ApiClient("http://localhost:8080");
    }
    
    @Test
    void shouldHandleBulkItemCreation() {
        // Arrange
        int itemCount = 100;
        List<WorkItemCreateRequest> requests = new ArrayList<>();
        
        for (int i = 0; i < itemCount; i++) {
            requests.add(new WorkItemCreateRequest("Perf Test Item " + i, WorkItemType.TASK));
        }
        
        // Act
        long startTime = System.currentTimeMillis();
        
        List<WorkItem> createdItems = new ArrayList<>();
        for (WorkItemCreateRequest request : requests) {
            createdItems.add(apiClient.createWorkItem(request));
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Assert
        assertEquals(itemCount, createdItems.size());
        assertTrue(duration < 10000, "Bulk creation took too long: " + duration + "ms");
        
        // Calculate and log performance metrics
        double avgTimePerItem = (double) duration / itemCount;
        System.out.println("Average time per item: " + avgTimePerItem + "ms");
    }
}
```

#### Go Performance Test Example

```go
// In api/test/performance/api_performance_test.go
package performance

import (
    "testing"
    "time"
    "net/http"
)

func BenchmarkApiEndpoint(b *testing.B) {
    // Skip the test setup from the benchmark timing
    b.ResetTimer()
    
    // Run the benchmark
    for i := 0; i < b.N; i++ {
        resp, err := http.Get("http://localhost:8080/api/health")
        if err != nil {
            b.Fatal(err)
        }
        resp.Body.Close()
    }
}

func TestApiConcurrentRequests(t *testing.T) {
    // Test concurrent API requests
    const numRequests = 100
    startTime := time.Now()
    
    // Create a channel to collect results
    results := make(chan time.Duration, numRequests)
    
    // Launch concurrent requests
    for i := 0; i < numRequests; i++ {
        go func() {
            requestStart := time.Now()
            resp, err := http.Get("http://localhost:8080/api/workitems")
            if err != nil {
                t.Errorf("Request failed: %v", err)
                results <- 0
                return
            }
            resp.Body.Close()
            results <- time.Since(requestStart)
        }()
    }
    
    // Collect results
    var totalDuration time.Duration
    for i := 0; i < numRequests; i++ {
        duration := <-results
        totalDuration += duration
    }
    
    // Calculate metrics
    totalTime := time.Since(startTime)
    avgResponseTime := totalDuration / time.Duration(numRequests)
    
    // Assert performance requirements
    if avgResponseTime > 200*time.Millisecond {
        t.Errorf("Average response time too high: %v", avgResponseTime)
    }
    
    t.Logf("Total time: %v, Average response time: %v", totalTime, avgResponseTime)
}
```

## Cross-Language Testing

Cross-language tests verify that different language components can interact correctly.

### Java-Go Integration

Test Java components communicating with Go API server:

```java
// In src/test/java/org/rinna/integration/JavaGoIntegrationTest.java
@Tag("integration")
public class JavaGoIntegrationTest {
    
    private ApiClient apiClient;
    
    @BeforeEach
    void setup() {
        // Ensure API server is running (using pre-started server or starting a test instance)
        apiClient = new ApiClient("http://localhost:8080");
    }
    
    @Test
    void shouldCreateAndRetrieveWorkItem() {
        // Create item using Java client
        WorkItemCreateRequest request = new WorkItemCreateRequest("Cross-language Test", WorkItemType.FEATURE);
        WorkItem createdItem = apiClient.createWorkItem(request);
        
        // Verify item can be retrieved
        WorkItem retrievedItem = apiClient.getWorkItem(createdItem.getId());
        
        // Assert
        assertEquals(createdItem.getId(), retrievedItem.getId());
        assertEquals(createdItem.getTitle(), retrievedItem.getTitle());
    }
}
```

### Java-Python Integration

Test Java and Python components working together:

```java
// In src/test/java/org/rinna/integration/JavaPythonIntegrationTest.java
@Tag("integration")
public class JavaPythonIntegrationTest {
    
    @Test
    void shouldGenerateDiagramsWithPython() throws IOException {
        // Invoke Python diagram generation process
        ProcessBuilder processBuilder = new ProcessBuilder(
            "python", "-m", "bin.c4_diagrams", "--type", "context");
        Process process = processBuilder.start();
        
        // Wait for completion
        int exitCode = process.waitFor();
        
        // Assert
        assertEquals(0, exitCode);
        assertTrue(Files.exists(Paths.get("docs/diagrams/rinna_context_diagram.svg")));
    }
}
```

## Running Tests

### Using the Unified Test CLI

The `rin-test` CLI provides a consistent interface for running tests across all languages:

```bash
# Run all tests
./bin/rin-test

# Run specific test categories
./bin/rin-test unit
./bin/rin-test component
./bin/rin-test integration
./bin/rin-test acceptance
./bin/rin-test performance

# Run language-specific tests
./bin/rin-test --only=java unit
./bin/rin-test --only=go unit
./bin/rin-test --only=python unit

# Run in parallel mode
./bin/rin-test --parallel unit

# Run tests with specific tags
./bin/rin-test tag:workflow
```

### Using Maven

Run Java tests using Maven:

```bash
# Run all tests
mvn test

# Run tests with specific profile
mvn test -P unit-tests
mvn test -P component-tests
mvn test -P integration-tests
mvn test -P acceptance-tests
mvn test -P performance-tests

# Run specific test classes
mvn test -Dtest=ItemServiceTest
```

### Using Go Test

Run Go tests:

```bash
# Run all Go tests
cd api && go test ./...

# Run specific test package
cd api && go test ./pkg/health

# Run tests with verbose output
cd api && go test -v ./...

# Run benchmarks
cd api && go test -bench=. ./...
```

### Using pytest

Run Python tests:

```bash
# Run all Python tests
cd python && python -m pytest

# Run specific test module
cd python && python -m pytest tests/unit/test_version.py

# Run tests with coverage
cd python && python -m pytest --cov=rinna
```

## Testing Tools and Scripts

### Test Discovery

Use the test discovery script to find and categorize tests:

```bash
# Run basic discovery
./bin/test-discovery.sh

# Run detailed discovery
./bin/test-discovery.sh --detailed
```

### Test Pyramid Coverage

Analyze test coverage across the test pyramid:

```bash
# Generate coverage report
./bin/test-pyramid-coverage.sh

# Generate JSON report
./bin/test-pyramid-coverage.sh --json --output=report.json
```

### Code Coverage

Generate code coverage reports:

```bash
# Generate unified coverage report
./bin/polyglot-coverage.sh

# Generate HTML report
./bin/polyglot-coverage.sh -o html
```

## CI/CD Integration

### GitHub Actions Integration

The Rinna project uses GitHub Actions for CI/CD. Test configuration is defined in `.github/workflows/*.yml` files:

```yaml
name: Test Suite

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      
      - name: Set up JDK
        uses: actions/setup-java@v2
        with:
          java-version: '21'
          distribution: 'adopt'
          
      - name: Set up Go
        uses: actions/setup-go@v2
        with:
          go-version: '^1.21'
          
      - name: Set up Python
        uses: actions/setup-python@v2
        with:
          python-version: '3.8'
          
      - name: Run tests
        run: ./bin/rin-test --ci
```

### Test Execution Flow in CI

1. Fast tests run on every push:
   - Unit tests
   - Component tests
   - Smoke tests

2. Full suite runs on pull requests:
   - Unit tests
   - Component tests
   - Integration tests
   - Acceptance tests

3. Performance tests run on release branches only

## Test Coverage

### Coverage Targets

The Rinna project has these code coverage targets:

- **Overall**: 70% minimum
- **Java**: 75% minimum
- **Go**: 70% minimum
- **Python**: 65% minimum

### Measuring Coverage

Generate coverage reports with:

```bash
# Generate combined coverage report
./bin/polyglot-coverage.sh

# Generate language-specific coverage
mvn test -P jacoco             # Java coverage
cd api && go test -cover ./... # Go coverage
cd python && python -m pytest --cov=rinna # Python coverage
```

## Best Practices

### General Testing Best Practices

1. **Follow the Test Pyramid**: Write more unit tests than integration tests, and more integration tests than acceptance tests.

2. **Test Isolation**: Each test should be independent and not rely on the state from other tests.

3. **Meaningful Assertions**: Make assertions that validate business logic, not just implementation details.

4. **Clean Test Data**: Create and clean up test data properly to avoid test pollution.

5. **Use Test Doubles Appropriately**:
   - **Stubs**: For providing test data
   - **Mocks**: For verifying behavior
   - **Fakes**: For simplified implementations of complex dependencies
   - **Spies**: For observing behavior without modifying it

6. **Test Edge Cases**: Include tests for boundary conditions, error handling, and edge cases.

7. **Fast Feedback**: Keep tests fast, especially unit tests.

### Java Testing Best Practices

1. **Use JUnit 5 Features**: Utilize parameterized tests, extensions, and nested tests.

2. **Mockito Best Practices**: Use annotations for clearer mock setup, verify interactions.

3. **BDD Style**: Consider using AssertJ for fluent assertions in a BDD style.

4. **Test Naming**: Use clear naming that describes the scenario and expected outcome:
   ```java
   @Test
   void shouldReturnNotFoundWhenItemDoesNotExist() { ... }
   ```

### Go Testing Best Practices

1. **Table-Driven Tests**: Use table-driven tests for testing multiple scenarios:
   ```go
   func TestValidations(t *testing.T) {
       tests := []struct {
           name  string
           input string
           want  bool
       }{
           {"empty string", "", false},
           {"valid input", "valid", true},
           // More test cases
       }
       
       for _, tt := range tests {
           t.Run(tt.name, func(t *testing.T) {
               got := isValid(tt.input)
               if got != tt.want {
                   t.Errorf("isValid(%q) = %v, want %v", tt.input, got, tt.want)
               }
           })
       }
   }
   ```

2. **Use Subtests**: Group related tests using subtests.

3. **Error Testing**: Test error conditions explicitly.

### Python Testing Best Practices

1. **Use pytest Fixtures**: Use fixtures for setup and teardown:
   ```python
   @pytest.fixture
   def client():
       return ApiClient("http://localhost:8080")
       
   def test_api_client(client):
       # Test using the client fixture
   ```

2. **Parametrize Tests**: Use parametrization for testing multiple scenarios:
   ```python
   @pytest.mark.parametrize("input,expected", [
       ("valid", True),
       ("", False),
       # More test cases
   ])
   def test_validation(input, expected):
       assert is_valid(input) == expected
   ```

3. **Mock External Services**: Use pytest-mock to mock external services.

### BDD Best Practices

1. **Focus on Business Value**: Write scenarios that describe business behavior, not technical implementation.

2. **Use Declarative Style**: Describe what happens, not how it happens.

3. **Background for Common Setup**: Use Background for steps common to all scenarios.

4. **Reusable Step Definitions**: Create reusable step definitions that can be composed into different scenarios.

## Troubleshooting

### Common Test Issues

#### Flaky Tests

Tests that sometimes pass and sometimes fail are called "flaky tests." Fix by:

1. **Identify the Source**: Use test repetition to isolate flakiness.
2. **Eliminate Race Conditions**: Add proper synchronization.
3. **Control External Dependencies**: Mock or use fixed-state test doubles.
4. **Isolate Parallel Tests**: Ensure tests run in isolation when executed in parallel.

#### Slow Tests

1. **Profile Test Execution**: Identify slow tests using timing information.
2. **Replace External Dependencies**: Use in-memory alternatives for databases, file systems, etc.
3. **Optimize Setup/Teardown**: Reuse expensive setup where appropriate.

#### Environment-Specific Issues

1. **Use Docker Containers**: Containerize test environments for consistency.
2. **Configure CI/CD Properly**: Ensure CI environment matches development environment.
3. **Document Environment Requirements**: Specify required dependencies and configuration.

### Debugging Test Failures

When tests fail, follow these steps:

1. **Check Error Messages**: Read the full error output to understand the failure point.
2. **Review Test Logs**: Check test logs for unusual behavior.
3. **Reproduce Locally**: Try to reproduce the failure in your local environment.
4. **Debug Step by Step**: Add debug logging or use a debugger to step through failing tests.
5. **Simplify the Test**: Reduce complexity to isolate the failure cause.

### Getting Help

For assistance with Rinna testing:

1. **Review Documentation**: Check the testing documentation in the `docs/testing/` directory.
2. **Contact the Test Team**: Reach out to the testing team for support.
3. **Check CI History**: Look at CI logs for patterns of failures.

## Conclusion

This comprehensive test automation guide provides the foundation for implementing and maintaining a robust test suite across the Rinna multi-language codebase. Follow these practices to ensure high-quality, maintainable tests that provide confidence in your code changes.

Remember that testing is an integral part of the development process, not an afterthought. Write tests alongside your code to ensure that features are implemented correctly and remain functional as the codebase evolves.
# Test Templates for Rinna

This document provides ready-to-use test templates for all languages and test types in the Rinna project. These templates follow the best practices outlined in the [Test Automation Guide](TEST_AUTOMATION_GUIDE.md) and can be used as starting points for new tests.

## Table of Contents

1. [Java Test Templates](#java-test-templates)
2. [Go Test Templates](#go-test-templates)
3. [Python Test Templates](#python-test-templates)
4. [BDD Test Templates](#bdd-test-templates)
5. [Cross-Language Test Templates](#cross-language-test-templates)

## Java Test Templates

### Java Unit Test Template

```java
package org.rinna.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for [ClassName]
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
public class ClassNameTest {
    
    @Mock
    private Dependency dependency;
    
    @InjectMocks
    private ClassName classUnderTest;
    
    @BeforeEach
    void setUp() {
        // Additional setup if needed
    }
    
    @Test
    void shouldPerformExpectedAction() {
        // Arrange
        when(dependency.someMethod()).thenReturn(expectedValue);
        
        // Act
        ResultType result = classUnderTest.methodUnderTest();
        
        // Assert
        assertEquals(expectedValue, result);
        verify(dependency).someMethod();
    }
    
    @Test
    void shouldHandleErrorCondition() {
        // Arrange
        when(dependency.someMethod()).thenThrow(new RuntimeException("expected"));
        
        // Act & Assert
        assertThrows(SomeException.class, () -> {
            classUnderTest.methodUnderTest();
        });
    }
}
```

### Java Component Test Template

```java
package org.rinna.component;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Component tests for [Feature] using real implementations
 * of components within a module
 */
@Tag("component")
public class FeatureComponentTest {
    
    private Service service;
    private Repository repository;
    
    @BeforeEach
    void setUp() {
        // Use real implementations but with in-memory storage or test doubles
        repository = new InMemoryRepository();
        service = new ServiceImpl(repository);
    }
    
    @Test
    void shouldCompleteEndToEndWorkflow() {
        // Arrange
        Entity entity = new Entity("test");
        
        // Act
        service.saveEntity(entity);
        Entity retrieved = service.getEntity(entity.getId());
        
        // Assert
        assertEquals(entity.getId(), retrieved.getId());
        assertEquals(entity.getName(), retrieved.getName());
    }
}
```

### Java Integration Test Template

```java
package org.rinna.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for interactions between [System] and [ExternalSystem]
 */
@Tag("integration")
public class SystemIntegrationTest {
    
    private SystemClient client;
    
    @BeforeEach
    void setUp() {
        // Connect to real external system or test instance
        client = new SystemClient("http://test-instance:8080");
    }
    
    @Test
    void shouldCommunicateWithExternalSystem() {
        // Arrange
        Request request = new Request("test-data");
        
        // Act
        Response response = client.sendRequest(request);
        
        // Assert
        assertTrue(response.isSuccessful());
        assertEquals("expected-result", response.getData());
    }
}
```

### Java Performance Test Template

```java
package org.rinna.performance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Performance tests for [Component]
 */
@Tag("performance")
public class ComponentPerformanceTest {
    
    private Component component;
    
    @BeforeEach
    void setUp() {
        component = new Component();
    }
    
    @Test
    void shouldHandleHighLoad() {
        // Arrange
        int iterations = 1000;
        
        // Act
        long startTime = System.nanoTime();
        
        for (int i = 0; i < iterations; i++) {
            component.processRequest("test-data");
        }
        
        long endTime = System.nanoTime();
        long duration = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
        
        // Assert
        long averageTime = duration / iterations;
        assertTrue(averageTime < 5, "Average processing time exceeded threshold: " + averageTime + "ms");
    }
    
    @Test
    void shouldCompleteWithinTimeLimit() {
        // Arrange
        Request complexRequest = Request.createComplex();
        
        // Act & Assert
        assertTimeout(Duration.ofMillis(100), () -> {
            component.processRequest(complexRequest);
        });
    }
}
```

## Go Test Templates

### Go Unit Test Template

```go
package module

import (
    "testing"
)

// TestFunction tests the behavior of Function
func TestFunction(t *testing.T) {
    // Arrange
    input := "test input"
    expected := "expected output"
    
    // Act
    result := Function(input)
    
    // Assert
    if result != expected {
        t.Errorf("Function(%q) = %q, want %q", input, result, expected)
    }
}

// TestFunction_TableDriven demonstrates a table-driven test approach
func TestFunction_TableDriven(t *testing.T) {
    tests := []struct {
        name     string
        input    string
        expected string
        wantErr  bool
    }{
        {
            name:     "valid input",
            input:    "test input",
            expected: "expected output",
            wantErr:  false,
        },
        {
            name:     "empty input",
            input:    "",
            expected: "",
            wantErr:  true,
        },
        // Add more test cases here
    }
    
    for _, tt := range tests {
        t.Run(tt.name, func(t *testing.T) {
            // Act
            result, err := FunctionWithError(tt.input)
            
            // Assert error cases
            if (err != nil) != tt.wantErr {
                t.Errorf("FunctionWithError(%q) error = %v, wantErr %v", 
                    tt.input, err, tt.wantErr)
                return
            }
            
            // Assert result for non-error cases
            if !tt.wantErr && result != tt.expected {
                t.Errorf("FunctionWithError(%q) = %q, want %q", 
                    tt.input, result, tt.expected)
            }
        })
    }
}
```

### Go Component Test Template

```go
package component

import (
    "testing"
    "rinna/api/internal/config"
    "rinna/api/internal/service"
)

// TestComponent_Integration tests interactions between components
func TestComponent_Integration(t *testing.T) {
    // Arrange
    config := config.NewTestConfig()
    service := service.NewService(config)
    
    // Act
    result, err := service.Process("test input")
    
    // Assert
    if err != nil {
        t.Fatalf("Expected no error, got %v", err)
    }
    
    if result != "expected output" {
        t.Errorf("Expected 'expected output', got %q", result)
    }
}
```

### Go Integration Test Template

```go
package integration

import (
    "testing"
    "net/http"
    "net/http/httptest"
    "rinna/api/internal/handlers"
)

// TestAPIIntegration tests API handler integration
func TestAPIIntegration(t *testing.T) {
    // Arrange
    handler := handlers.NewHandler()
    server := httptest.NewServer(handler)
    defer server.Close()
    
    // Act
    resp, err := http.Get(server.URL + "/api/endpoint")
    
    // Assert
    if err != nil {
        t.Fatalf("Error making request: %v", err)
    }
    defer resp.Body.Close()
    
    if resp.StatusCode != http.StatusOK {
        t.Errorf("Expected status OK, got %v", resp.Status)
    }
    
    // Read and verify response body as needed
}
```

### Go Performance Test Template

```go
package performance

import (
    "testing"
    "time"
)

// BenchmarkFunction benchmarks the performance of Function
func BenchmarkFunction(b *testing.B) {
    // Reset timer to exclude setup time
    b.ResetTimer()
    
    // Run the benchmark
    for i := 0; i < b.N; i++ {
        Function("benchmark input")
    }
}

// TestPerformance_Threshold tests that Function completes within a time threshold
func TestPerformance_Threshold(t *testing.T) {
    // Arrange
    input := "test input"
    maxDuration := 10 * time.Millisecond
    
    // Act
    start := time.Now()
    Function(input)
    duration := time.Since(start)
    
    // Assert
    if duration > maxDuration {
        t.Errorf("Function took %v, which exceeds threshold of %v", 
            duration, maxDuration)
    }
}
```

## Python Test Templates

### Python Unit Test Template

```python
import pytest
from rinna.module import Function

class TestFunction:
    def test_normal_behavior(self):
        # Arrange
        input_value = "test input"
        expected = "expected output"
        
        # Act
        result = Function(input_value)
        
        # Assert
        assert result == expected
    
    def test_error_condition(self):
        # Arrange
        input_value = None
        
        # Act & Assert
        with pytest.raises(ValueError):
            Function(input_value)
    
    @pytest.mark.parametrize("input_value,expected", [
        ("test1", "result1"),
        ("test2", "result2"),
        ("test3", "result3"),
    ])
    def test_multiple_scenarios(self, input_value, expected):
        # Act
        result = Function(input_value)
        
        # Assert
        assert result == expected
```

### Python Component Test Template

```python
import pytest
from rinna.service import Service
from rinna.repository import Repository

class TestServiceComponent:
    @pytest.fixture
    def repository(self):
        # Use real repository with in-memory storage
        return Repository(in_memory=True)
    
    @pytest.fixture
    def service(self, repository):
        return Service(repository)
    
    def test_service_repository_integration(self, service, repository):
        # Arrange
        item = {"id": 1, "name": "Test Item"}
        
        # Act
        service.save_item(item)
        result = service.get_item(1)
        
        # Assert
        assert result["id"] == item["id"]
        assert result["name"] == item["name"]
        
        # Verify repository state directly
        assert repository.has_item(1)
```

### Python Integration Test Template

```python
import pytest
import requests
import subprocess
import time

class TestAPIIntegration:
    @pytest.fixture(scope="module")
    def api_server(self):
        # Start API server for testing
        process = subprocess.Popen(["go", "run", "./api/cmd/main.go"])
        time.sleep(2)  # Wait for server to start
        
        yield "http://localhost:8080"  # Provide URL to tests
        
        # Shutdown API server after tests
        process.terminate()
        process.wait()
    
    def test_api_endpoint(self, api_server):
        # Arrange
        url = f"{api_server}/api/items"
        data = {"name": "Test Item"}
        
        # Act
        response = requests.post(url, json=data)
        
        # Assert
        assert response.status_code == 200
        response_data = response.json()
        assert "id" in response_data
        assert response_data["name"] == data["name"]
        
        # Clean up created data
        requests.delete(f"{api_server}/api/items/{response_data['id']}")
```

### Python Performance Test Template

```python
import pytest
import time
from rinna.module import Function

class TestPerformance:
    def test_function_throughput(self):
        # Arrange
        iterations = 1000
        max_total_time = 2.0  # seconds
        
        # Act
        start_time = time.time()
        
        for i in range(iterations):
            Function(f"test{i}")
            
        total_time = time.time() - start_time
        
        # Assert
        assert total_time < max_total_time
        
        # Log performance metrics
        operations_per_second = iterations / total_time
        print(f"Performance: {operations_per_second:.2f} ops/sec")
    
    @pytest.mark.benchmark
    def test_function_benchmark(self, benchmark):
        # Use pytest-benchmark for more detailed performance analysis
        result = benchmark(lambda: Function("benchmark input"))
        
        # Assert on benchmark results
        assert benchmark.stats.stats.mean < 0.001  # mean time less than 1ms
```

## BDD Test Templates

### Gherkin Feature File Template

```gherkin
Feature: Feature Name
  In order to [achieve some business value]
  As a [stakeholder type]
  I want [to be able to perform some action]

  Background:
    Given the system is initialized
    And I am authenticated as a user

  Scenario: Basic user workflow
    When I create a new item with name "Test Item"
    Then the item should be created successfully
    And the item should have the name "Test Item"

  Scenario Outline: Multiple workflow variations
    When I create a new item with name "<name>"
    Then the item should be created successfully
    And the item should have the name "<name>"
    
    Examples:
      | name        |
      | Basic Item  |
      | Complex Item|
      | Special Item|

  Scenario: Error handling workflow
    When I attempt to create an item with invalid data
    Then the creation should fail
    And I should receive an error message
```

### Java Step Definitions Template

```java
package org.rinna.acceptance.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.cucumber.java.Before;
import io.cucumber.java.After;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Step definitions for Feature
 */
public class FeatureSteps {
    
    private SystemClient client;
    private Response response;
    private Exception lastException;
    
    @Before
    public void setUp() {
        client = new SystemClient();
    }
    
    @After
    public void tearDown() {
        // Clean up test data
        client.cleanup();
    }
    
    @Given("the system is initialized")
    public void systemIsInitialized() {
        assertTrue(client.isAvailable());
    }
    
    @Given("I am authenticated as a user")
    public void authenticatedAsUser() {
        client.authenticate("test-user", "test-password");
        assertTrue(client.isAuthenticated());
    }
    
    @When("I create a new item with name {string}")
    public void createNewItem(String name) {
        try {
            response = client.createItem(name);
            lastException = null;
        } catch (Exception e) {
            lastException = e;
        }
    }
    
    @When("I attempt to create an item with invalid data")
    public void createItemWithInvalidData() {
        try {
            response = client.createItem(null);
            lastException = null;
        } catch (Exception e) {
            lastException = e;
        }
    }
    
    @Then("the item should be created successfully")
    public void itemCreatedSuccessfully() {
        assertNull(lastException);
        assertTrue(response.isSuccessful());
    }
    
    @Then("the item should have the name {string}")
    public void itemShouldHaveName(String name) {
        assertEquals(name, response.getItem().getName());
    }
    
    @Then("the creation should fail")
    public void creationShouldFail() {
        assertNotNull(lastException);
    }
    
    @Then("I should receive an error message")
    public void shouldReceiveErrorMessage() {
        assertNotNull(lastException.getMessage());
        assertFalse(lastException.getMessage().isEmpty());
    }
}
```

## Cross-Language Test Templates

### Java-Go Integration Test Template

```java
package org.rinna.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

/**
 * Tests integration between Java CLI and Go API
 */
@Tag("integration")
public class JavaGoIntegrationTest {
    
    private ApiClient apiClient;
    private static Process apiProcess;
    
    @BeforeAll
    static void startApi() throws IOException {
        // Start the Go API server for testing
        ProcessBuilder pb = new ProcessBuilder("go", "run", "./api/cmd/rinnasrv/main.go");
        apiProcess = pb.start();
        
        // Wait for server to be ready
        Thread.sleep(2000);
    }
    
    @AfterAll
    static void stopApi() {
        if (apiProcess != null) {
            apiProcess.destroy();
        }
    }
    
    @BeforeEach
    void setUp() {
        apiClient = new ApiClient("http://localhost:8080");
    }
    
    @Test
    void shouldCreateItemViaApi() {
        // Arrange
        ItemRequest request = new ItemRequest("Test Item");
        
        // Act
        ItemResponse response = apiClient.createItem(request);
        
        // Assert
        assertNotNull(response.getId());
        assertEquals("Test Item", response.getName());
        
        // Verify item exists via API
        ItemResponse retrieved = apiClient.getItem(response.getId());
        assertEquals(response.getId(), retrieved.getId());
    }
}
```

### CLI Integration Test Template

```bash
#!/bin/bash
# Test script for CLI-API integration

# Set up test environment
API_URL="http://localhost:8080"
CLI_PATH="./bin/rin"

# Start API server (if not already running)
go run ./api/cmd/rinnasrv/main.go &
API_PID=$!

# Wait for server to start
sleep 2

# Perform the test
echo "Testing CLI add command..."
ITEM_ID=$(${CLI_PATH} add --type=TASK --title="Test Task" --output=json | jq -r '.id')

if [ -z "$ITEM_ID" ]; then
  echo "Test failed: Could not create item"
  kill $API_PID
  exit 1
fi

echo "Testing CLI view command..."
ITEM_INFO=$(${CLI_PATH} view ${ITEM_ID} --output=json)
ITEM_TITLE=$(echo "$ITEM_INFO" | jq -r '.title')

if [ "$ITEM_TITLE" != "Test Task" ]; then
  echo "Test failed: Item title doesn't match. Expected 'Test Task', got '$ITEM_TITLE'"
  kill $API_PID
  exit 1
fi

# Clean up
echo "Cleaning up test data..."
${CLI_PATH} delete ${ITEM_ID}

# Stop API server
kill $API_PID

echo "Test passed successfully!"
exit 0
```

### Polyglot System Test Template

```java
package org.rinna.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Tests integration between Java Core, Go API, and Python Tools
 */
@Tag("integration")
public class PolyglotSystemTest {
    
    private ApiClient apiClient;
    private static Process apiProcess;
    
    @BeforeAll
    static void setupSystems() throws IOException {
        // Start the Go API server
        ProcessBuilder apiPb = new ProcessBuilder("go", "run", "./api/cmd/rinnasrv/main.go");
        apiProcess = apiPb.start();
        
        // Wait for systems to be ready
        Thread.sleep(2000);
    }
    
    @AfterAll
    static void tearDownSystems() {
        if (apiProcess != null) {
            apiProcess.destroy();
        }
    }
    
    @BeforeEach
    void setUp() {
        apiClient = new ApiClient("http://localhost:8080");
    }
    
    @Test
    void endToEndWorkflow() throws IOException {
        // Step 1: Create item using Java client
        WorkItem workItem = new WorkItemCreateRequest("Polyglot Test", WorkItemType.FEATURE);
        WorkItem created = apiClient.createWorkItem(workItem);
        assertNotNull(created.getId());
        
        // Step 2: Use CLI to modify item (which calls Go API)
        ProcessBuilder cliPb = new ProcessBuilder("./bin/rin", "update", 
            "--id=" + created.getId(), "--status=IN_PROGRESS");
        Process cliProcess = cliPb.start();
        int exitCode = cliProcess.waitFor();
        assertEquals(0, exitCode);
        
        // Step 3: Generate report with Python tool
        ProcessBuilder reportPb = new ProcessBuilder("python", "-m", 
            "python.rinna.reports", "--item=" + created.getId(), "--format=json");
        Process reportProcess = reportPb.start();
        exitCode = reportProcess.waitFor();
        assertEquals(0, exitCode);
        
        // Step 4: Verify report file was created
        String reportPath = "reports/item_" + created.getId() + ".json";
        assertTrue(Files.exists(Paths.get(reportPath)));
        
        // Step 5: Read report and verify content
        String reportContent = Files.readString(Paths.get(reportPath));
        assertTrue(reportContent.contains("Polyglot Test"));
        assertTrue(reportContent.contains("IN_PROGRESS"));
    }
}
```

## Additional Resources

- [Test Automation Guide](TEST_AUTOMATION_GUIDE.md) - Complete guide to test automation
- [Test Pyramid Strategy](TEST_PYRAMID.md) - Overview of the test pyramid approach
- [Unified Test Approach](UNIFIED_TEST_APPROACH.md) - Standardized approach across languages
- [Rinna CLI Reference](../user-guide/rin-cli.md) - Documentation for the Rinna CLI tool
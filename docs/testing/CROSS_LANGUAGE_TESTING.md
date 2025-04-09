# Cross-Language Testing in Rinna

This document provides a streamlined guide to testing interactions between Rinna's Java, Go, and Python components.

## Overview

Rinna is a multi-language system with components implemented in:

- **Java**: Core domain model, business logic, and CLI
- **Go**: API server and health monitoring
- **Python**: Scripting, data processing, and automation

Cross-language testing ensures these components can effectively communicate while maintaining data consistency across language boundaries.

## Simplified Test Harness

Our simplified test approach integrates cross-language testing directly into the main testing framework. Instead of having separate scripts for cross-language testing, we've incorporated it into our unified test runner.

### Running Cross-Language Tests

```bash
# Run all integration tests (including cross-language)
bin/rin-test integration

# Specify language combinations
bin/rin-test --java --go integration

# Run with coverage
bin/rin-test --coverage integration
```

## Cross-Language Test Types

### Java-Go Integration

These tests verify that Java CLI components can communicate correctly with the Go API:

- CLI command execution that interacts with the API
- Data format consistency between Java and Go
- Authentication and authorization across languages
- Error handling and propagation

### Java-Python Integration

These tests ensure seamless integration between Java core and Python scripting:

- Data processing flows from Java to Python and back
- Event handling across language boundaries
- Configuration sharing between languages
- Script execution from Java with proper input/output handling

### Go-Python Integration

These tests validate that the Go API works correctly with Python automation:

- API client functionality from Python
- Data transformation and analysis in Python
- Webhook and event processing
- Statistics and metrics collection

## Standard Test Cases

### Work Item Synchronization

Tests the complete work item lifecycle across languages:

1. Create work item in Java
2. Verify via Go API
3. Update via Python
4. Verify changes in Java

### Authentication Flow

Verifies authentication and session management:

1. Generate authentication token in Java
2. Validate token via Go API
3. Use token in Python client
4. Verify access controls are consistent

### Configuration Sharing

Tests configuration consistency:

1. Set configuration values in Java
2. Read and validate in Go
3. Update via Python
4. Verify changes propagate correctly

## Integration Patterns

All cross-language tests follow these key integration patterns:

### RESTful API Integration

- Standard HTTP/JSON communication
- Content-Type and Accept header validation
- Status code verification
- Error response handling

### File-Based Integration

- JSON/CSV/YAML file interchange
- File format validation
- Character encoding handling
- File locking for concurrent access

### Process Integration

- Standard I/O streams
- Exit code validation
- Process lifecycle management
- Signal handling

## Writing Cross-Language Tests

When writing cross-language tests:

1. **Use the appropriate directory structure**:
   - Java tests: `src/test/java/org/rinna/integration/`
   - Go tests: `api/test/integration/`
   - Python tests: `python/tests/integration/`

2. **Follow naming conventions**:
   - Java: `*CrossLanguageTest.java`
   - Go: `*_cross_language_test.go`
   - Python: `test_cross_language_*.py`

3. **Use appropriate test tags**:
   - Java: `@Tag("integration") @Tag("cross-language")`
   - Go: `// +build integration,cross-language`
   - Python: `@pytest.mark.integration @pytest.mark.cross_language`

4. **Handle environment setup properly**:
   - Start any required services before tests
   - Use dynamic port allocation to avoid conflicts
   - Clean up resources after tests complete
   - Use appropriate timeouts for cross-process communication

## Example Test Cases

### Java Integration Test Example

```java
@Tag("integration")
@Tag("cross-language")
public class WorkItemCrossLanguageTest {
    
    @Test
    public void shouldSynchronizeWorkItemAcrossLanguages() {
        // 1. Create work item using Java
        WorkItemCreateRequest createRequest = new WorkItemCreateRequest();
        createRequest.setTitle("Cross-language test item");
        WorkItem workItem = itemService.createWorkItem(createRequest);
        
        // 2. Verify it exists via Go API client
        ApiClient client = new ApiClient();
        ApiResponse response = client.getWorkItem(workItem.getId());
        assertEquals(200, response.getStatusCode());
        
        // 3. Use Python client to update the item
        ProcessBuilder pb = new ProcessBuilder(
            "python", "-m", "python.tests.integration.update_work_item",
            workItem.getId(), "Updated Title"
        );
        Process p = pb.start();
        assertEquals(0, p.waitFor());
        
        // 4. Verify the update in Java
        WorkItem updated = itemService.getWorkItem(workItem.getId());
        assertEquals("Updated Title", updated.getTitle());
    }
}
```

### Python Integration Test Example

```python
@pytest.mark.integration
@pytest.mark.cross_language
def test_go_api_integration():
    """Test Python client integration with Go API."""
    # Create test data
    work_item = {
        "title": "Python-Go integration test",
        "type": "TASK",
        "priority": "MEDIUM"
    }
    
    # Send to API and verify
    api_client = ApiClient()
    response = api_client.create_work_item(work_item)
    
    assert response.status_code == 201
    
    # Verify item was created and can be retrieved
    work_item_id = response.json()["id"]
    get_response = api_client.get_work_item(work_item_id)
    
    assert get_response.status_code == 200
    assert get_response.json()["title"] == work_item["title"]
```

## CI/CD Integration

Cross-language tests are fully integrated into our CI/CD pipeline:

```yaml
# Integration test job in GitHub Actions
integration-tests:
  name: Integration Tests
  runs-on: ubuntu-latest
  steps:
    - uses: actions/checkout@v3
    - name: Set up environment
      uses: ./.github/actions/setup-environment
    - name: Run integration tests
      run: bin/rin-test integration
    - name: Upload test results
      uses: actions/upload-artifact@v3
      with:
        name: integration-test-results
        path: target/test-reports/
```

## Troubleshooting Cross-Language Tests

### Common Issues

1. **Port conflicts**: Use dynamic port allocation in tests
2. **Environment differences**: Ensure all required dependencies are available
3. **Process communication**: Check standard output/error streams
4. **Authentication issues**: Verify tokens and credentials are passed correctly
5. **Data format inconsistencies**: Validate data formats match between languages

### Debugging Tips

- Use `bin/rin-test -v integration` for verbose output
- Check logs in `target/logs/` directory
- Use environment variables to control test behavior
- Run specific tests in isolation when debugging issues
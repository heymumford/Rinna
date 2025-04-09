# Cross-Language Testing in Rinna

This document provides a guide to using the cross-language test harness for testing interactions between Rinna's Java, Go, and Python components.

## Overview

Rinna is a multi-language system with components implemented in:

- **Java**: Core domain model, business logic, and CLI
- **Go**: API server and health monitoring
- **Python**: Scripting, data processing, and automation

The cross-language test harness ensures these components can effectively communicate and maintain data consistency across language boundaries.

## Test Harness Architecture

The test harness consists of:

1. **Central Shell Script**: `bin/cross-language-test-harness.sh` orchestrates the entire test process
2. **Language-Specific Test Scripts**: Located in `test/cross-language/` directory
3. **Integration Test Classes**: Java, Go, and Python classes that implement specific cross-language tests
4. **Mock Services**: In-memory implementations for testing without external dependencies

### Test Categories

The test harness supports the following test categories:

| Test Mode | Description | Use Case |
|-----------|-------------|----------|
| `all` | All cross-language tests | Comprehensive testing |
| `java-go` | Java-Go interaction tests | CLI-API integration testing |
| `java-python` | Java-Python interaction tests | Data processing and CLI scripting |
| `go-python` | Go-Python interaction tests | API-automation integration testing |
| `specific` | Run a specific named test | Targeted testing during development |

## Running Cross-Language Tests

### Basic Usage

Run all cross-language tests:

```bash
./bin/cross-language-test-harness.sh
```

Run specific test modes:

```bash
./bin/cross-language-test-harness.sh --mode=java-go
./bin/cross-language-test-harness.sh --mode=java-python
./bin/cross-language-test-harness.sh --mode=go-python
```

Run a specific test by name:

```bash
./bin/cross-language-test-harness.sh --test WorkItemSync
```

### Advanced Options

```bash
# Run tests in parallel
./bin/cross-language-test-harness.sh --parallel

# Skip rebuilding components (faster)
./bin/cross-language-test-harness.sh --skip-build

# Run with verbose output
./bin/cross-language-test-harness.sh --verbose

# Run with debug logging
./bin/cross-language-test-harness.sh --debug

# Custom API port (useful for parallel CI)
./bin/cross-language-test-harness.sh --api-port=8090

# Generate report in different format
./bin/cross-language-test-harness.sh --report=junit
```

## Test Implementation Guide

### Creating a New Cross-Language Test

1. **Identify the Interaction Pattern**:
   - Determine which languages will interact (Java-Go, Java-Python, or Go-Python)
   - Define the data flow and transformation path

2. **Create a Test Script**:
   - Create a new script in `test/cross-language/` with the naming convention `<language1>_<language2>_<feature>_test.sh`
   - Implement the standard test lifecycle (setup, test steps, teardown)

3. **Implement Language-Specific Components**:
   - Add Java test cases in `src/test/java/org/rinna/integration/api/`
   - Add Go test cases in `api/test/integration/`
   - Add Python test cases in `python/tests/integration/`

### Test Script Template

```bash
#!/bin/bash
#
# example_test.sh - Cross-language test for <feature>
#
# This test verifies that <feature> works across languages.

set -eo pipefail

# Source test utilities
if [[ -f "./test/common/test_utils.sh" ]]; then
  source "./test/common/test_utils.sh"
fi

# Test variables
API_PORT="${RINNA_TEST_API_PORT:-8085}"
TEST_TEMP_DIR="${RINNA_TEST_TEMP_DIR:-./target/cross-language-tests}"

# Test setup
setup() {
  echo "Setting up test..."
  # Create directories, initialize environment
}

# Test teardown
teardown() {
  echo "Tearing down test..."
  # Clean up resources
}

# Test steps
test_step_one() {
  echo "Running test step one..."
  # Implement test logic
}

test_step_two() {
  echo "Running test step two..."
  # Implement test logic
}

# Run the test
run_test() {
  local success=true
  
  setup
  
  # Run test steps
  if ! test_step_one; then
    success=false
  elif ! test_step_two; then
    success=false
  fi
  
  teardown
  
  if $success; then
    echo "Test completed successfully"
    return 0
  else
    echo "Test failed"
    return 1
  fi
}

# Run the test if executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
  run_test
fi
```

## Existing Cross-Language Tests

| Test Name | Description | Pattern |
|-----------|-------------|---------|
| `workitem_sync_test.sh` | Tests work item synchronization between all languages | Java->Go->Python->Java |
| `java_python_integration_test.sh` | Tests Java-Python data processing integration | Java->Python->Java |
| `go_python_integration_test.sh` | Tests Go API and Python scripting integration | Go<->Python |
| `authentication_flow_test.sh` | Tests authentication and session management | Java->Go->Python |
| `notification_system_test.sh` | Tests notification creation and delivery | Java->Go->Python->Java |
| `configuration_management_test.sh` | Tests configuration sharing and updates | Java->Go->Python->Java |
| `performance_benchmark_test.sh` | Measures performance metrics across language boundaries | Java<->Go<->Python |
| `security_validation_test.sh` | Validates security controls across language boundaries | Java<->Go<->Python |

### WorkItem Synchronization Test

This test verifies that work item data can be properly synchronized between languages:

1. Creates a work item via Java CLI
2. Verifies the item exists via Go API
3. Updates the item via Python script
4. Verifies the updated item via Java CLI

### Java-Python Integration Test

This test demonstrates data processing between Java and Python:

1. Exports work items from Java CLI
2. Processes the data with a Python script
3. Analyzes the data with Python and generates metrics
4. Verifies Java can parse the processed data

### Go-Python Integration Test

This test showcases API integration with Python automation:

1. Creates test work items via Python script using Go API
2. Verifies items in Go API
3. Processes and transforms items via Python
4. Analyzes statistics with Python
5. Verifies changes in Go API

### Authentication Flow Test

This test verifies authentication tokens and session management:

1. Creates authentication token via Java CLI login
2. Validates token via Go API
3. Creates session in Python using the token
4. Tests API access using Python session
5. Tests direct login from Python
6. Tests token revocation via Java CLI logout

### Notification System Test

This test verifies the notification system across languages:

1. Creates notification via Java CLI
2. Retrieves notification via Go API
3. Listens for notifications with Python script
4. Creates notification via Python
5. Lists all notifications via Java CLI
6. Marks notification as read via Go API
7. Clears read notifications via Java CLI

### Configuration Management Test

This test verifies configuration sharing and updates:

1. Sets configuration value via Java CLI
2. Retrieves configuration via Go API
3. Updates configuration via Python
4. Lists configurations via Java CLI
5. Lists configurations via Go API
6. Lists configurations via Python
7. Deletes configuration via Java CLI

### Performance Benchmark Test

This test measures performance metrics for cross-language operations:

1. **Java-Go Performance**:
   - Measures latency, success rate, memory, and CPU usage for Java CLI to Go API operations
   - Tests view, list, and update operations with concurrent clients
   - Evaluates performance under load conditions

2. **Go-Python Performance**:
   - Measures performance of Python client operations against Go API
   - Tests create, get, update, and search operations
   - Evaluates throughput with concurrent requests

3. **Java-Python Performance**:
   - Measures file-based data exchange between Java and Python
   - Tests exporting data from Java and processing with Python
   - Tests generating data with Python and importing with Java

The test generates:
- CSV files with detailed metrics for each interaction path
- Box plots showing latency distributions
- Statistical summaries with mean, median, and percentile metrics
- Analysis and recommendations based on performance patterns

### Security Validation Test

This test validates security controls across language boundaries:

1. **Authentication Security**:
   - Tests token creation via Java CLI and validation via Go API
   - Validates token expiration handling
   - Tests session management across components

2. **Input Validation**:
   - Tests SQL injection protection in Go API endpoints
   - Tests XSS protection in Java and Go components
   - Tests command injection protection in CLI commands

3. **Authorization Controls**:
   - Verifies consistent authorization enforcement across languages
   - Tests role-based access control for admin functions
   - Validates permission checking in CLI and API

4. **Data Validation Consistency**:
   - Tests boundary values (empty, null, too long)
   - Tests invalid enum values
   - Tests special characters and unicode handling
   - Ensures consistent validation across language boundaries

The test generates a comprehensive security report with:
- Detailed test results by category
- Security risk assessment
- Specific recommendations based on findings
- Consistency analysis across language implementations

## Java Integration Test Example

```java
@Tag("integration")
@Tag("polyglot")
@TestInstance(Lifecycle.PER_CLASS)
@DisplayName("Cross-Language WorkItem Integration Tests")
public class CrossLanguageWorkItemTest extends IntegrationTest {
    
    @Test
    @DisplayName("Should create WorkItem in Java and retrieve it via Go API")
    void shouldCreateWorkItemInJavaAndRetrieveItViaGoApi() {
        // Create a work item using Java domain model
        WorkItemCreateRequest createRequest = new WorkItemCreateRequest();
        createRequest.setTitle("Cross-language test from Java");
        createRequest.setType(WorkItemType.TASK);
        createRequest.setPriority(Priority.HIGH);
        
        // Send to Go API and verify
        // ...
    }
}
```

## Python Integration Test Example

```python
class TestCrossLanguageApi(unittest.TestCase):
    """Test Python integration with Go API server."""

    def test_01_create_work_item_from_python(self):
        """Test creating a work item via Python client to Go API."""
        # Create unique test data
        test_id = str(uuid.uuid4())[:8]
        
        # Prepare work item data
        work_item = {
            "title": f"Python-Go test item {test_id}",
            "type": "FEATURE",
            "priority": "MEDIUM"
        }
        
        # Send to API and verify
        # ...
    }
```

## CI/CD Integration

The cross-language test harness is fully integrated with the CI/CD pipeline through dedicated scripts and workflow configurations.

### Running Tests in CI

The `bin/ci-cross-language-tests.sh` script is designed specifically for CI environments:

```bash
# Run all tests in CI mode with JUnit reporting
./bin/ci-cross-language-tests.sh --ci

# Run specific test groups in parallel
./bin/ci-cross-language-tests.sh --ci --parallel --groups=core,auth

# Run with debug output and fail on first error
./bin/ci-cross-language-tests.sh --ci --debug --fail-fast
```

This script provides:
- Parallel test execution with worker limits
- JUnit XML report generation
- Markdown summary report
- Detailed logs for debugging
- Configurable test groups and failure modes

### GitHub Actions Workflow

A complete GitHub Actions workflow is configured in `.github/workflows/cross-language-tests.yml` that:
- Sets up the required Java, Go, and Python environments
- Builds all components
- Runs the cross-language tests
- Publishes test reports
- Uploads logs and artifacts
- Adds a test summary to the PR

You can manually trigger this workflow from the GitHub Actions UI with custom parameters:
- Select which test groups to run
- Enable/disable parallel execution
- Configure fail-fast behavior

### Local Test Runner

For developers, a dedicated test runner script is provided for easy execution:

```bash
# Run all tests
./bin/run-cross-language-tests.sh --all

# Run specific test groups
./bin/run-cross-language-tests.sh --core --auth

# Run a specific test
./bin/run-cross-language-tests.sh workitem_sync_test.sh

# Run with detailed output
./bin/run-cross-language-tests.sh --all --verbose
```

This runner provides:
- Environment validation
- Component building
- Detailed but user-friendly output
- Test selection shortcuts

## Troubleshooting

### Common Issues

1. **Port conflicts**: Use `--api-port` to specify a different port if the default is in use.
2. **Missing dependencies**: Ensure all languages (Java, Go, Python) are installed.
3. **Component build failures**: Use `--debug` to see detailed build output.
4. **Test timeouts**: Increase timeout with `--api-timeout` parameter.

### Viewing Logs

Test logs are stored in:
- Overall harness log: `logs/cross-language-tests_<timestamp>.log`
- API server log: `target/cross-language-tests/api-server.log`
- Test-specific logs: `target/cross-language-tests/<test_name>.log`

## Contributing New Tests

When adding new cross-language tests:

1. Follow the naming convention for test scripts
2. Include clear documentation in the script header
3. Structure tests with distinct setup, test steps, and teardown phases
4. Add any new test scripts to this documentation
5. Run existing tests to verify no regressions
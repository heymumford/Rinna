# Test Pyramid Strategy

## Overview

The Rinna project follows a comprehensive testing strategy across multiple languages (Java, Go, and Python) based on the Test Pyramid model. This document outlines our approach to ensuring test coverage across all layers of the application architecture.

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

## Test Categories

### Unit Tests

- **Purpose**: Test individual classes and methods in isolation
- **Scope**: Single class or method with dependencies mocked
- **Execution Speed**: Very fast (milliseconds)
- **Count**: Highest number of tests
- **Java Example**: `ViewCommandTest.java`
- **Go Example**: `version_test.go`
- **Python Example**: `test_version.py`

### Component Tests

- **Purpose**: Test interactions between closely related components
- **Scope**: Multiple classes within a module without external dependencies
- **Execution Speed**: Fast (tens to hundreds of milliseconds)
- **Count**: Moderate number of tests
- **Java Example**: `CommandExecutionTest.java`
- **Go Example**: `config_component_test.go`
- **Python Example**: `test_component_config.py`

### Integration Tests

- **Purpose**: Test interactions between modules or across language boundaries
- **Scope**: Multiple modules with real (non-mocked) dependencies
- **Execution Speed**: Moderate (hundreds of milliseconds)
- **Count**: Fewer than component tests
- **Java Example**: `CliServiceIntegrationTest.java`
- **Go Example**: `cli_api_integration_test.go`
- **Python Example**: `test_integration_cli.py`

### Acceptance Tests

- **Purpose**: Verify end-to-end functionality from a user perspective
- **Scope**: Complete workflows from user input to expected output
- **Execution Speed**: Slow (seconds)
- **Count**: Fewer than integration tests
- **Java Example**: `WorkflowAcceptanceTest.java`
- **Go Example**: `workflow_acceptance_test.go`
- **Python Example**: `test_acceptance_workflow.py`

### Performance Tests

- **Purpose**: Verify system performance under various conditions
- **Scope**: Response times, throughput, and resource utilization
- **Execution Speed**: Varies widely based on test
- **Count**: Fewest number of tests
- **Java Example**: `CliPerformanceTest.java`
- **Go Example**: `api_performance_test.go`
- **Python Example**: `test_perf_operations.py`

## Cross-Language Test Coverage

Our polyglot architecture requires tests that span multiple languages. For example:

1. **CLI-API Integration**: Tests that verify the Java CLI can correctly communicate with the Go API server
2. **Data Validation**: Tests that ensure consistent data validation rules across all languages
3. **Cross-Service Communication**: Tests that verify services implemented in different languages can interact correctly

## Test Coverage Monitoring

We use a custom `test-pyramid-coverage.sh` tool to monitor test coverage across all languages and test categories. This tool:

1. Scans the codebase for tests in each category and language
2. Generates a visual representation of our test pyramid
3. Provides recommendations for improving test coverage
4. Can be integrated into CI/CD pipelines

To run the tool:

```bash
./bin/test-pyramid-coverage.sh
```

## Test Conventions

### Java Tests

- Use JUnit 5 (Jupiter) for all tests
- Apply `@Tag` annotations to categorize tests (e.g., `@Tag("unit")`)
- Place tests in appropriate package structure (e.g., `unit`, `component`, etc.)
- Extend base test classes for common functionality

### Go Tests

- Use the standard Go testing package
- Place tests in appropriate directories based on category
- Use naming conventions to indicate test type (e.g., `_integration_test.go`)
- Use benchmarks for performance tests

### Python Tests

- Use pytest for all tests
- Place tests in category-specific directories
- Use naming conventions for test modules (e.g., `test_integration_*.py`)
- Apply pytest markers to categorize tests when needed

## CI/CD Integration

Test pyramid coverage is monitored in our CI/CD pipelines:

1. The `test-pyramid` target in our Makefile runs the coverage tool
2. CI builds generate test pyramid reports as artifacts
3. Pull requests are automatically checked for proper test pyramid balance
4. Code coverage metrics are combined with pyramid metrics for comprehensive quality assessment

## Getting Started with Tests

When adding new functionality:

1. Start with unit tests that cover the core logic
2. Add component tests to verify interactions within modules
3. Add integration tests for cross-module or cross-language interactions
4. Add acceptance tests for complete user workflows
5. Add performance tests for performance-critical paths

To run all tests:

```bash
make test
```

To run just the test pyramid analysis:

```bash
make test-pyramid
```
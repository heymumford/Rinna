# Test Pyramid Strategy

## Overview

The Rinna project follows a streamlined testing strategy based on the Test Pyramid model, applying consistent principles across all languages (Java, Go, and Python).

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

### Component Tests

- **Purpose**: Test interactions between closely related components
- **Scope**: Multiple classes within a module without external dependencies
- **Execution Speed**: Fast (tens to hundreds of milliseconds)
- **Count**: Moderate number of tests

### Integration Tests

- **Purpose**: Test interactions between modules or across language boundaries
- **Scope**: Multiple modules with real (non-mocked) dependencies
- **Execution Speed**: Moderate (hundreds of milliseconds)
- **Count**: Fewer than component tests

### Acceptance Tests

- **Purpose**: Verify end-to-end functionality from a user perspective
- **Scope**: Complete workflows from user input to expected output
- **Execution Speed**: Slow (seconds)
- **Count**: Fewer than integration tests

### Performance Tests

- **Purpose**: Verify system performance under various conditions
- **Scope**: Response times, throughput, and resource utilization
- **Execution Speed**: Varies widely based on test
- **Count**: Fewest number of tests

## Streamlined Test Directory Structure

All tests follow a consistent directory structure:

```
src/test/java/org/rinna/
├── unit/           # Unit tests
├── component/      # Component tests
├── integration/    # Integration tests
├── acceptance/     # Acceptance tests
└── performance/    # Performance tests

api/test/
├── unit/           # Unit tests
├── component/      # Component tests
├── integration/    # Integration tests
└── performance/    # Performance tests

python/tests/
├── unit/           # Unit tests
├── component/      # Component tests
├── integration/    # Integration tests
├── acceptance/     # Acceptance tests
└── performance/    # Performance tests
```

## Unified Testing Approach

We've simplified our testing approach with a unified test script that works consistently across all languages.

### Running Tests

The `bin/rin-test` script provides a unified interface for all tests:

```bash
# Run all tests
bin/rin-test

# Run specific test categories
bin/rin-test unit
bin/rin-test component
bin/rin-test integration
bin/rin-test acceptance
bin/rin-test performance

# Run tests for specific languages
bin/rin-test --java unit
bin/rin-test --go component
bin/rin-test --python integration

# Run fast tests only (unit and component)
bin/rin-test --fast

# Generate coverage reports
bin/rin-test --coverage

# Run tests with verbose output
bin/rin-test -v

# Stop on first test failure
bin/rin-test --fail-fast
```

## Test Naming Conventions

To maintain consistency, we use standard naming conventions across all languages:

| Test Type   | Java                   | Go                       | Python                |
|-------------|------------------------|--------------------------|------------------------|
| Unit        | `*Test.java`           | `*_test.go`              | `test_*_unit.py`      |
| Component   | `*ComponentTest.java`  | `*_component_test.go`    | `test_*_component.py` |
| Integration | `*IntegrationTest.java`| `*_integration_test.go`  | `test_*_integration.py` |
| Acceptance  | `*AcceptanceTest.java` | `*_acceptance_test.go`   | `test_*_acceptance.py` |
| Performance | `*PerformanceTest.java`| `*_performance_test.go`  | `test_*_performance.py` |

## Tagging Tests

We use consistent tagging across languages:

```java
// Java (JUnit 5)
@Tag("unit")
@Tag("component")
@Tag("integration")
@Tag("acceptance")
@Tag("performance")
```

```go
// Go (build tags)
// +build unit
// +build component
// +build integration
// +build performance
```

```python
# Python (pytest)
@pytest.mark.unit
@pytest.mark.component
@pytest.mark.integration
@pytest.mark.acceptance
@pytest.mark.performance
```

## Fast Test Mode

For rapid feedback during development, use:

```bash
bin/rin-test --fast
```

This runs only the unit and component tests, which should complete in seconds rather than minutes.

## Cross-Language Testing

Cross-language tests ensure seamless integration between components written in different languages:

```bash
# Run cross-language integration tests
bin/rin-test integration

# Run cross-language tests for specific language combinations
bin/rin-test --java --go integration
```

## Code Coverage

Generate unified code coverage reports across all languages:

```bash
bin/rin-test --coverage
```

This produces:
- Line coverage metrics for each language
- Unified HTML reports
- Consolidated summary report
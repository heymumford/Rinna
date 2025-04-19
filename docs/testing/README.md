# Rinna Test Documentation

This directory contains comprehensive documentation for the Rinna testing strategy, architecture, and implementation guidelines. These documents provide a unified approach to testing across the multi-language Rinna codebase (Java, Go, Python).

## Core Test Documentation

| Document | Description |
|----------|-------------|
| [Test Pyramid Strategy](TEST_PYRAMID.md) | Overview of our five-layer test pyramid approach and philosophy |
| [Cross-Language Testing](CROSS_LANGUAGE_TESTING.md) | Guide to cross-language testing between Java, Go, and Python |
| [TDD Practical Guide](TDD_PRACTICAL_GUIDE.md) | Step-by-step guide to Test-Driven Development in a multi-language environment |
| [Karate Test Syntax](KARATE_TEST_SYNTAX.md) | Overview of Karate API testing syntax and best practices |
| [Python Container Testing](PYTHON_CONTAINER_TESTING.md) | Guide to running Python tests in containers |
| [Docker Image Caching](DOCKER_IMAGE_CACHING.md) | Strategy for optimizing container builds with caching |
| [Container Strategy](CONTAINER_STRATEGY.md) | Overall container testing strategy |

## Streamlined Test Structure

We have simplified our test organization to follow a consistent structure across all languages:

1. **Unified Directory Structure**:
   - `rinna-core/src/test/java/org/rinna/unit/` - Core Java unit tests
   - `rinna-core/src/test/java/org/rinna/component/` - Core Java component tests
   - `rinna-core/src/test/java/org/rinna/integration/` - Core Java integration tests
   - `rinna-core/src/test/java/org/rinna/acceptance/` - Core Java acceptance tests
   - `rinna-core/src/test/java/org/rinna/performance/` - Core Java performance tests
   - `rinna-cli/src/test/java/org/rinna/cli/unit/` - CLI Java unit tests
   - `rinna-cli/src/test/java/org/rinna/cli/component/` - CLI Java component tests
   - `rinna-cli/src/test/java/org/rinna/cli/integration/` - CLI Java integration tests
   - `api/test/unit/` - Go unit tests
   - `api/test/component/` - Go component tests
   - `api/test/integration/` - Go integration tests
   - `api/test/performance/` - Go performance tests
   - `python/tests/unit/` - Python unit tests
   - `python/tests/component/` - Python component tests
   - `python/tests/integration/` - Python integration tests
   - `python/tests/acceptance/` - Python acceptance tests
   - `python/tests/performance/` - Python performance tests

2. **Standardized Test Tagging**:
   - `@Tag("unit")` for unit tests
   - `@Tag("component")` for component tests
   - `@Tag("integration")` for integration tests
   - `@Tag("acceptance")` for acceptance tests
   - `@Tag("performance")` for performance tests

3. **Standardized File Naming**:
   - Unit tests: `*Test.java`, `*_test.go`, `test_*_unit.py`
   - Component tests: `*ComponentTest.java`, `*_component_test.go`, `test_*_component.py`
   - Integration tests: `*IntegrationTest.java`, `*_integration_test.go`, `test_*_integration.py`
   - Acceptance tests: `*AcceptanceTest.java`, `*_acceptance_test.go`, `test_*_acceptance.py`
   - Performance tests: `*PerformanceTest.java`, `*_performance_test.go`, `test_*_performance.py`

## Unified Test Command

We've streamlined our test execution with a single, unified command:

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

## Language-Specific Testing

### Java Testing

Java tests use JUnit 5 with the following key features:

- `@Tag` annotations for test categorization
- Parameterized tests for data-driven testing
- Extension model for test lifecycle hooks
- Assertions for verification

Example unit test:
```java
@Tag("unit")
class WorkItemTest {
    @Test
    void shouldCreateWorkItem() {
        WorkItem item = new WorkItem("Test item");
        assertEquals("Test item", item.getTitle());
        assertEquals(WorkflowState.CREATED, item.getState());
    }
}
```

### Go Testing

Go tests use the standard testing package:

```go
// +build unit

package model

import "testing"

func TestWorkItem_Create(t *testing.T) {
    item := NewWorkItem("Test item")
    if item.Title != "Test item" {
        t.Errorf("Expected title 'Test item', got '%s'", item.Title)
    }
    if item.State != StateCREATED {
        t.Errorf("Expected state 'CREATED', got '%s'", item.State)
    }
}
```

### Python Testing

Python tests use pytest with markers:

```python
import pytest
from rinna.model import WorkItem

@pytest.mark.unit
def test_work_item_creation():
    item = WorkItem("Test item")
    assert item.title == "Test item"
    assert item.state == "CREATED"
```

## Cross-Language Integration

All our tests are organized to ensure seamless integration between components written in different languages:

- Java-Go integration through REST API
- Java-Python integration through process and file-based communication
- Go-Python integration through REST API and WebSockets

See [Cross-Language Testing](CROSS_LANGUAGE_TESTING.md) for more details.

## Container Testing

We provide comprehensive container-based testing strategies that ensure consistent environments:

- Docker/Podman support for all testing levels
- Optimized image caching for faster builds
- Multi-stage builds for different use cases (dev, test, prod)
- Consistent behavior between local development and CI/CD

See [Container Strategy](CONTAINER_STRATEGY.md) and [Python Container Testing](PYTHON_CONTAINER_TESTING.md) for details.

## Code Coverage

We use a unified approach to code coverage across all languages:

```bash
# Generate coverage report for all languages
bin/rin-test --coverage

# Generate coverage for specific tests
bin/rin-test --coverage unit
```

This integrates:
- JaCoCo for Java
- Go's built-in coverage tools
- pytest-cov for Python

Coverage reports are consolidated in `target/coverage/`.
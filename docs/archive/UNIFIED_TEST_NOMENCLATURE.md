# [ARCHIVED] Unified Test Nomenclature Across Languages

> **Note**: This document is archived and its content has been consolidated into the [Unified Test Approach](../testing/UNIFIED_TEST_APPROACH.md) document.

This document defines the standardized test nomenclature across all languages in the Rinna project, ensuring consistency with our testing pyramid approach.

## The Testing Pyramid - Universal Structure

Our testing pyramid consists of five layers, each with a specific purpose and scope:

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

## Cross-Language Naming Conventions

| Layer | Purpose | Java | Go | Python | Bash |
|-------|---------|------|-----|--------|------|
| **Unit** | Test individual units/functions | `@Tag("unit")` class extends `UnitTest` | `func TestXxx(t *testing.T)` or `_test.go` files | `test_xxx.py` decorated with `@pytest.mark.unit` | `test_unit_*.sh` |
| **Component** | Test components within modules | `@Tag("component")` class extends `ComponentTest` | `func TestXxx_Component(t *testing.T)` | `test_xxx.py` decorated with `@pytest.mark.component` | `test_component_*.sh` |
| **Integration** | Test interactions between modules | `@Tag("integration")` class extends `IntegrationTest` | `func TestXxx_Integration(t *testing.T)` | `test_xxx.py` decorated with `@pytest.mark.integration` | `test_integration_*.sh` |
| **Acceptance** | Test end-to-end workflows | `@Tag("acceptance")` class extends `AcceptanceTest` or BDD `.feature` files | Files in `test/acceptance/*.go` | `test_acceptance_xxx.py` or BDD `.feature` files | `test_acceptance_*.sh` |
| **Performance** | Test system performance | `@Tag("performance")` class extends `PerformanceTest` | `func BenchmarkXxx(b *testing.B)` | `test_perf_xxx.py` with `@pytest.mark.benchmark` | `test_perf_*.sh` |

## File Naming Conventions

| Language | Unit | Component | Integration | Acceptance | Performance |
|----------|------|-----------|-------------|------------|-------------|
| Java | `*Test.java` | `*ComponentTest.java` | `*IntegrationTest.java` | `*AcceptanceTest.java` or `.feature` | `*PerfTest.java` |
| Go | `xxx_test.go` | `xxx_component_test.go` | `xxx_integration_test.go` | `acceptance/xxx_test.go` | `xxx_bench_test.go` |
| Python | `test_xxx.py` | `test_component_xxx.py` | `test_integration_xxx.py` | `test_acceptance_xxx.py` or `.feature` | `test_perf_xxx.py` |
| Bash | `test_unit_xxx.sh` | `test_component_xxx.sh` | `test_integration_xxx.sh` | `test_acceptance_xxx.sh` | `test_perf_xxx.sh` |

## Directory Structure

To better organize tests across languages, we will use a consistent directory structure:

```
language-module/
├── test/
│   ├── unit/           # Unit tests
│   ├── component/      # Component tests
│   ├── integration/    # Integration tests
│   ├── acceptance/     # Acceptance tests
│   │   └── features/   # BDD feature files
│   └── performance/    # Performance tests
```

For example:
- `api/test/unit/*.go`
- `python/tests/unit/*.py`
- `rinna-core/src/test/java/org/rinna/unit/*.java`

## Test Tags and Annotations

### Java (JUnit 5)

```java
// Unit Test
@Tag("unit")
public class MyUnitTest extends UnitTest {
    // ...
}

// Component Test
@Tag("component")
public class MyComponentTest extends ComponentTest {
    // ...
}

// Integration Test
@Tag("integration")
public class MyIntegrationTest extends IntegrationTest {
    // ...
}

// Acceptance Test
@Tag("acceptance")
public class MyAcceptanceTest extends AcceptanceTest {
    // ...
}

// Performance Test
@Tag("performance")
public class MyPerformanceTest extends PerformanceTest {
    // ...
}
```

### Go

```go
// Unit Test
func TestMyFunction(t *testing.T) {
    // ...
}

// Component Test
func TestMyFunction_Component(t *testing.T) {
    // ...
}

// Integration Test
func TestMyFunction_Integration(t *testing.T) {
    // ...
}

// Acceptance Test
func TestMyFunction_Acceptance(t *testing.T) {
    // ...
}

// Performance Test
func BenchmarkMyFunction(b *testing.B) {
    for i := 0; i < b.N; i++ {
        // ...
    }
}
```

### Python (pytest)

```python
# Unit Test
@pytest.mark.unit
def test_my_function():
    # ...

# Component Test
@pytest.mark.component
def test_component_my_class():
    # ...

# Integration Test
@pytest.mark.integration
def test_integration_system():
    # ...

# Acceptance Test
@pytest.mark.acceptance
def test_acceptance_workflow():
    # ...

# Performance Test
@pytest.mark.benchmark
def test_perf_my_function(benchmark):
    benchmark(lambda: my_function())
```

### Bash

```bash
# Unit Test
function test_unit_my_function() {
    # ...
}

# Component Test
function test_component_my_module() {
    # ...
}

# Integration Test
function test_integration_system() {
    # ...
}

# Acceptance Test
function test_acceptance_workflow() {
    # ...
}

# Performance Test
function test_perf_operation() {
    # ...
}
```

## Command-Line Interface Integration

The `rin test` command will work consistently across all languages:

```bash
# Run tests by layer across all languages
./bin/rin test unit          # Run all unit tests
./bin/rin test component     # Run all component tests
./bin/rin test integration   # Run all integration tests
./bin/rin test acceptance    # Run all acceptance tests
./bin/rin test performance   # Run all performance tests

# Run language-specific tests
./bin/rin test java:unit     # Run Java unit tests only
./bin/rin test go:unit       # Run Go unit tests only
./bin/rin test python:unit   # Run Python unit tests only
./bin/rin test bash:unit     # Run Bash unit tests only

# Run test combinations
./bin/rin test fast          # Run unit and component tests (quick feedback)
./bin/rin test essential     # Run unit, component, and integration tests (no UI)
```

## Implementation Notes

### Java

- Base classes (`UnitTest`, `ComponentTest`, etc.) provide common functionality
- JUnit 5 tags ensure proper categorization
- Maven profiles (`unit-tests`, `component-tests`, etc.) enable targeted execution

### Go

- Naming conventions differentiate test types
- Directory structure organizes tests by category
- Test functions follow a consistent naming pattern

### Python

- pytest markers categorize tests
- Directory structure organizes tests by type
- Naming conventions enhance clarity

### Bash

- Naming conventions and directories organize tests
- Functions follow a consistent naming pattern
- Helper libraries provide testing utilities

## Migration Guide

To migrate existing tests to this nomenclature:

1. Java tests:
   - Add appropriate `@Tag` annotation
   - Extend the corresponding base class
   - Move to the appropriate directory structure

2. Go tests:
   - Rename and restructure files
   - Update test function names to follow the convention
   - Move to the appropriate directory structure

3. Python tests:
   - Add pytest markers
   - Rename and restructure files
   - Move to the appropriate directory structure

4. Bash tests:
   - Rename and restructure files
   - Update function names to follow the convention
   - Move to the appropriate directory structure

## CI/CD Integration

Our CI/CD pipeline will leverage this unified nomenclature to:

- Run the appropriate tests at each stage
- Generate comprehensive reports by test category
- Enforce quality gates at each level of the pyramid

## Conclusion

This unified test nomenclature ensures consistency across all languages in the Rinna project, making it easier to understand, execute, and maintain tests regardless of implementation language.
# Unified Test Approach for Rinna

This document outlines our standardized approach to testing across the Rinna project, ensuring consistent organization, naming, and execution of tests at all layers of the test pyramid.

## Test Pyramid Structure

We follow a classic five-layer test pyramid:

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

## Standardized Test Organization

### 1. Directory Structure

All tests will follow a consistent directory structure:

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

### 2. Naming Conventions

Files:
- Unit tests: `*Test.java` 
- Component tests: `*ComponentTest.java`
- Integration tests: `*IntegrationTest.java`
- Acceptance tests: `*AcceptanceTest.java` and `*Steps.java`
- Performance tests: `*PerformanceTest.java`

### 3. Test Tagging

All tests will use consistent JUnit 5 tagging:

```java
// Direct tagging for all tests
@Tag("unit")         // Unit tests
@Tag("component")    // Component tests
@Tag("integration")  // Integration tests
@Tag("acceptance")   // Acceptance tests
@Tag("performance")  // Performance tests

// Additional functional tags as needed
@Tag("fast")         // Very fast tests
@Tag("slow")         // Slow-running tests
@Tag("bdd")          // BDD tests with Cucumber
@Tag("sanity")       // Basic sanity checks
@Tag("regression")   // Regression test suite
@Tag("smoke")        // Smoke tests that run on every git push
```

### 4. Base Classes

We will provide consistent base classes to encourage proper test organization:

```java
// org.rinna.test.base.UnitTest
@Tag("unit")
public abstract class UnitTest { /* common unit test functionality */ }

// org.rinna.test.base.ComponentTest
@Tag("component")
public abstract class ComponentTest { /* common component test functionality */ }

// etc.
```

### 5. Test Execution

Tests can be run through:

1. Maven Profiles:
```
mvn test -P unit          # Run only unit tests
mvn test -P component     # Run only component tests
mvn test -P integration   # Run only integration tests
mvn test -P acceptance    # Run only acceptance tests
mvn test -P performance   # Run only performance tests
mvn test                  # Run all tests except performance
```

2. Command-line tool:
```
./bin/rin test unit           # Run unit tests
./bin/rin test component      # Run component tests
./bin/rin test integration    # Run integration tests
./bin/rin test acceptance     # Run acceptance tests
./bin/rin test performance    # Run performance tests
./bin/rin test all            # Run all tests
./bin/rin test fast           # Run only fast tests (unit + component)
./bin/rin test smoke          # Run only smoke tests
```

## Implementation Status: ✅ COMPLETED

We have successfully implemented all aspects of the unified test approach:

1. ✅ Refactored existing tests:
   - All tests moved to appropriate directories based on test type
   - Consistent tagging applied to all test classes
   - Base classes updated for each test category

2. ✅ Updated Maven configuration:
   - Profiles defined for each test category
   - Surefire/Failsafe plugins configured to use tags
   - Test execution streamlined with proper tag filtering

3. ✅ Updated CLI tools:
   - All `rin test` commands now reflect test pyramid organization
   - Documentation added for test commands
   - Automated test discovery implemented

4. ✅ Documented best practices:
   - Guidelines created for when to use each type of test
   - Examples provided for writing effective tests at each layer
   - Training materials developed for the team

For more details on the migration process, see the [Test Migration Summary](../../TEST_MIGRATION_SUMMARY.md) document.

## Cross-Language Naming Conventions

We maintain consistent test naming across all languages in the Rinna project:

| Layer | Purpose | Java | Go | Python | Bash |
|-------|---------|------|-----|--------|------|
| **Unit** | Test individual units/functions | `@Tag("unit")` class extends `UnitTest` | `func TestXxx(t *testing.T)` or `_test.go` files | `test_xxx.py` decorated with `@pytest.mark.unit` | `test_unit_*.sh` |
| **Component** | Test components within modules | `@Tag("component")` class extends `ComponentTest` | `func TestXxx_Component(t *testing.T)` | `test_xxx.py` decorated with `@pytest.mark.component` | `test_component_*.sh` |
| **Integration** | Test interactions between modules | `@Tag("integration")` class extends `IntegrationTest` | `func TestXxx_Integration(t *testing.T)` | `test_xxx.py` decorated with `@pytest.mark.integration` | `test_integration_*.sh` |
| **Acceptance** | Test end-to-end workflows | `@Tag("acceptance")` class extends `AcceptanceTest` or BDD `.feature` files | Files in `test/acceptance/*.go` | `test_acceptance_xxx.py` or BDD `.feature` files | `test_acceptance_*.sh` |
| **Performance** | Test system performance | `@Tag("performance")` class extends `PerformanceTest` | `func BenchmarkXxx(b *testing.B)` | `test_perf_xxx.py` with `@pytest.mark.benchmark` | `test_perf_*.sh` |

### File Naming Conventions

| Language | Unit | Component | Integration | Acceptance | Performance |
|----------|------|-----------|-------------|------------|-------------|
| Java | `*Test.java` | `*ComponentTest.java` | `*IntegrationTest.java` | `*AcceptanceTest.java` or `.feature` | `*PerfTest.java` |
| Go | `xxx_test.go` | `xxx_component_test.go` | `xxx_integration_test.go` | `acceptance/xxx_test.go` | `xxx_bench_test.go` |
| Python | `test_xxx.py` | `test_component_xxx.py` | `test_integration_xxx.py` | `test_acceptance_xxx.py` or `.feature` | `test_perf_xxx.py` |
| Bash | `test_unit_xxx.sh` | `test_component_xxx.sh` | `test_integration_xxx.sh` | `test_acceptance_xxx.sh` | `test_perf_xxx.sh` |

### Cross-Language Directory Structure

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

### Language-Specific Testing

Our `rin test` command supports language-specific test execution:

```bash
# Run language-specific tests
./bin/rin test java:unit     # Run Java unit tests only
./bin/rin test go:unit       # Run Go unit tests only
./bin/rin test python:unit   # Run Python unit tests only
./bin/rin test bash:unit     # Run Bash unit tests only
```

## Smoke Test Strategy

Smoke tests are a subset of tests from all levels of the testing pyramid that:

1. Run quickly (typically under 1 second each)
2. Cover critical functionality 
3. Provide fast feedback on essential features

Smoke tests are automatically run on every git push in the CI pipeline and serve as an early warning system for critical issues. They are tagged with `@Tag("smoke")` and can be run using:

```bash
mvn test -P smoke-tests
```

Guidelines for smoke tests:
- Every major feature should have at least one smoke test
- Smoke tests should focus on "happy path" scenarios
- Smoke tests should be deterministic and not flaky
- Keep smoke tests fast by mocking external dependencies
- Balance coverage with execution time

## Benefits

- Clear organization of tests by purpose
- Consistent naming makes it easy to identify test types
- Simplified test execution with standardized commands
- Better CI pipeline integration with predictable test execution
- Easier maintenance and discovery of tests
- Fast feedback through automated smoke tests
# Rinna Testing Strategy 

This document outlines the testing strategy for the Rinna project, inspired by the combined wisdom of Uncle Bob (Robert C. Martin) and Martin Fowler. The strategy is designed to balance quick feedback, comprehensive test coverage, and appropriate test scoping.

> 📊 For a detailed explanation of our test pyramid implementation across Java, Go, and Python, see our [Test Pyramid Strategy](TEST_PYRAMID.md) document, which explains our approach to polyglot testing.

> 📖 For the philosophical foundations of our approach to testing, application delivery, and digital transformation in the age of AI, see our [Testing Philosophy](PHILOSOPHY.md) document, which explores concepts from Dr. Danny Coward, Dave Snowden's Cynefin framework, and key Agile luminaries.

> 📋 For detailed quality standards, patterns, and language-specific implementations, see our [Quality Standards](QUALITY_STANDARDS.md) document.

> 🔄 For our unified test nomenclature across all languages (Java, Go, Python, Bash), see our [Unified Test Nomenclature](UNIFIED_TEST_NOMENCLATURE.md) document.

## Testing Pyramid

Rinna follows the testing pyramid approach, with more tests at the lower levels (unit, component) and fewer tests at the higher levels (integration, acceptance, performance). This approach provides fast feedback on most code changes while ensuring comprehensive coverage of critical application workflows.

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

Our test suite is organized into five distinct categories, each with a specific scope, purpose, and execution frequency:

### 1. Unit Tests (@unit)

- **Purpose**: Test individual units of code in isolation
- **Scope**: Classes, methods, functions
- **Dependencies**: Use mocks/stubs for all dependencies
- **Characteristics**: Fast, focused, deterministic
- **Coverage Target**: 90%+ of business logic
- **Execution Time**: Under 5 seconds for critical modules
- **When to Run**: On every build, every commit
- **CI Target**: Run on every push
- **Maven Profile**: `unit-tests`

### 2. Component Tests (@component) 

- **Purpose**: Test behavior of components that work together
- **Scope**: Small clusters of classes, single module boundaries
- **Dependencies**: Use real implementations for in-module dependencies, mocks for external dependencies
- **Characteristics**: Almost as fast as unit tests, test component contracts
- **Coverage Target**: 80%+ of component interactions
- **Execution Time**: Under 30 seconds
- **When to Run**: After unit tests succeed
- **CI Target**: Run on every push
- **Maven Profile**: `component-tests`

### 3. Integration Tests (@integration)

- **Purpose**: Test integration between modules or external dependencies
- **Scope**: Module boundaries, database access, file system, etc.
- **Dependencies**: Use real implementations where practical
- **Characteristics**: Test real interactions, may require setup/teardown
- **Coverage Target**: All module interfaces and critical paths
- **Execution Time**: Under 2 minutes
- **When to Run**: Several times per day, not necessarily every commit
- **CI Target**: Run on Pull Requests
- **Maven Profile**: `integration-tests`

### 4. Acceptance Tests (@acceptance)

- **Purpose**: Verify system meets business requirements
- **Scope**: End-to-end workflows, user scenarios (via Cucumber BDD)
- **Dependencies**: Full system, possibly with test doubles for external services
- **Characteristics**: Written in ubiquitous language, readable by stakeholders
- **Coverage Target**: All user-facing features and workflows
- **Execution Time**: Under 5 minutes
- **When to Run**: Daily or on demand
- **CI Target**: Run on Pull Requests
- **Maven Profile**: `acceptance-tests`

### 5. Performance Tests (@performance)

- **Purpose**: Verify system performance meets requirements
- **Scope**: Critical paths, high-load scenarios
- **Dependencies**: Full system in production-like environment
- **Characteristics**: Test throughput, response time, resource usage
- **Coverage Target**: Critical performance paths
- **Execution Time**: Varies based on test
- **When to Run**: Before releases
- **CI Target**: Run on release branches
- **Maven Profile**: `performance-tests`

## Implementation Details

### JUnit 5 Test Categorization

Tests are categorized using JUnit 5's `@Tag` annotation:

```java
@Tag("unit")
public class SomeUnitTest { /*...*/ }

@Tag("component")
public class SomeComponentTest { /*...*/ }

@Tag("integration")
public class SomeIntegrationTest { /*...*/ }

@Tag("acceptance")
@Tag("bdd")
public class SomeAcceptanceTest { /*...*/ }

@Tag("performance")
public class SomePerformanceTest { /*...*/ }

@Tag("smoke")
public class SmokeSuiteTest { /*...*/ }
```

### Maven Profiles

Maven profiles enable selective test execution:

```
mvn test -P unit-tests
mvn test -P component-tests
mvn test -P integration-tests
mvn test -P acceptance-tests
mvn test -P performance-tests
```

### CLI Commands

The `rin-build` CLI provides convenient commands for running specific test categories:

```bash
rin-build test unit         # Run unit tests
rin-build test component    # Run component tests
rin-build test integration  # Run integration tests 
rin-build test acceptance   # Run acceptance tests
rin-build test performance  # Run performance tests
rin-build test              # Run all tests
```

### Test Naming Conventions

- Unit tests: `*Test.java`
- Component tests: `*ComponentTest.java`
- Integration tests: `*IntegrationTest.java`
- Acceptance tests: Cucumber `.feature` files + step definitions
- Performance tests: `*PerfTest.java`

### Continuous Integration

The CI pipeline is configured to run tests at the appropriate stages:

- Every Push: Unit + Component tests
- Pull Requests: Unit + Component + Integration + Acceptance tests
- Release Branches: All tests including Performance tests

## Benefits of This Approach

1. **Fast Feedback**: Unit and component tests run quickly on every build
2. **Comprehensive Coverage**: All layers of the system are tested
3. **Appropriate Scoping**: Tests are sized appropriately for their purpose
4. **Clear Documentation**: Acceptance tests serve as living documentation
5. **Performance Awareness**: Performance is tested before release
6. **Maintainability**: Tests are categorized and can be run selectively

## Code Coverage

Rinna implements a polyglot code coverage strategy that aggregates coverage data from all languages used in the project:

### Unified Coverage Reporting

- **Automatic Coverage**: Code coverage is automatically calculated during test runs
- **Polyglot Support**: Coverage for Java, Go, and Python components
- **Consolidated View**: Weighted average coverage across all languages
- **Language-Specific Thresholds**: Different coverage targets for each language

### Implementation

The unified coverage system uses:

- **Java**: JaCoCo for Java coverage (target: 75%)
- **Go**: Native Go coverage tools (target: 70%)
- **Python**: pytest-cov (target: 65%)

### Running Coverage Reports

Coverage reports are generated automatically when running tests:

```bash
# Run tests with coverage
rin-test unit          # Shows coverage for unit tests
rin-test component     # Shows coverage for component tests
rin-test               # Shows coverage for all tests

# To skip coverage reporting
rin-test --no-coverage 

# Generate standalone coverage reports
bin/polyglot-coverage.sh        # Generate text report 
bin/polyglot-coverage.sh -o html  # Generate HTML report
bin/polyglot-coverage.sh -o json  # Generate JSON report
```

### Coverage Requirements

- **Overall**: 70% minimum coverage
- **CI Mode**: 75% minimum coverage
- **HTML Reports**: Available in `target/coverage/html/`
- **Language-Specific Reports**: Available for each language

## References

- "Clean Code" by Robert C. Martin
- "Refactoring" by Martin Fowler
- "Test-Driven Development: By Example" by Kent Beck
- "Growing Object-Oriented Software, Guided by Tests" by Steve Freeman & Nat Pryce
- "xUnit Test Patterns" by Gerard Meszaros
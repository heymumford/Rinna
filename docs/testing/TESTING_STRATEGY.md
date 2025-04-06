# Rinna Testing Strategy 

This document outlines the testing strategy for the Rinna project, inspired by the combined wisdom of Uncle Bob (Robert C. Martin) and Martin Fowler. The strategy is designed to balance quick feedback, comprehensive test coverage, and appropriate test scoping.

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

## References

- "Clean Code" by Robert C. Martin
- "Refactoring" by Martin Fowler
- "Test-Driven Development: By Example" by Kent Beck
- "Growing Object-Oriented Software, Guided by Tests" by Steve Freeman & Nat Pryce
# Rinna Quality Standards and Delivery Principles

This document serves as the North Star for Rinna's quality standards across all languages and components. It defines our principles, concepts, options, key decisions, and pragmatic patterns for ensuring quality and reliability.

## Core Principles

### 1. Testing as a First-Class Citizen

We embrace testing as a fundamental, integrated aspect of software development, not as an afterthought or external process. This means:

- Tests are written concurrently with production code (often before)
- Test quality is held to the same standards as production code
- Test coverage is measured and monitored
- Test reliability is paramount - flaky tests are actively fixed, not ignored
- Testing tools are given the same priority as development tools

### 2. Polyglot Quality Approach

Our system consists of components in multiple languages (Java, Go, Python, Bash), each with their own testing idioms. We embrace language-specific best practices while maintaining consistency across our testing philosophy.

### 3. Shift-Left Testing

We detect issues as early as possible in the development lifecycle, emphasizing:

- Static analysis and linting during coding
- Unit and component tests that execute quickly
- Automated quality gates that prevent low-quality code from progressing

### 4. Living Documentation

Tests serve as executable documentation for our system:

- BDD specifications document requirements in human-readable form
- Well-named tests document expected behaviors
- Test organization reflects system architecture

### 5. Continuous Quality

Quality is not a one-time event but a continuous process:

- Automated testing on every commit
- Regular code reviews with quality focus
- Quality metrics tracked over time
- Technical debt actively monitored and addressed

## Cross-Language Testing Concepts

### Test Categories

We use consistent test categories across all languages:

| Category | Java | Go | Python | Purpose |
|----------|------|----|----|---------|
| **Unit** | `@Tag("unit")` | `TestXxx` functions | `test_xxx` | Test individual functions/methods in isolation |
| **Component** | `@Tag("component")` | Integration tests using `TestMain` | `test_component_xxx` | Test components within a bounded context |
| **Integration** | `@Tag("integration")` | `TestXxxIntegration_*` | `test_integration_xxx` | Test interactions between components |
| **Acceptance** | Cucumber `.feature` files | Cucumber/gherkin-go | Behave | Test end-to-end workflows |
| **Performance** | JMH benchmarks | Go benchmarks | pytest-benchmark | Test system performance |

### Shared Testing Tools

Tools that span multiple languages:

| Tool | Purpose | Languages |
|------|---------|-----------|
| JaCoCo | Code coverage for JVM | Java |
| SonarQube | Static analysis | Java, Go, Python |
| Go Cover | Code coverage for Go | Go |
| Coverage.py | Code coverage for Python | Python |
| ShellCheck | Static analysis for shell scripts | Bash |
| JUnit | Test execution for JVM | Java |
| go test | Test execution for Go | Go |
| pytest | Test execution for Python | Python |

### CLI Test Runner Integration

Our unified CLI test runner (`bin/rin test`) provides consistent execution patterns across all languages:

```bash
bin/rin test unit       # Run unit tests in all languages
bin/rin test go:unit    # Run only Go unit tests
bin/rin test java:unit  # Run only Java unit tests
bin/rin test py:unit    # Run only Python unit tests
```

## Quality Gates and Thresholds

We enforce quality through automated gates in our development process:

| Gate | Threshold | Enforcement Point |
|------|-----------|-------------------|
| Unit Test Coverage | ≥90% | Pull Request |
| Component Test Coverage | ≥80% | Pull Request |
| Integration Test Coverage | ≥70% | Release |
| Code Duplication | ≤5% | Pull Request |
| Code Complexity (Cyclomatic) | ≤15 | Pull Request |
| OWASP Dependency Check | Zero High/Critical | Pull Request |
| Build Status | All Pass | Commit |
| Performance Benchmarks | Within 5% of baseline | Release |

## Language-Specific Implementation

### Java Testing Standards

1. **Framework Choice**:
   - JUnit 5 (Jupiter) for test execution
   - Mockito for test doubles
   - AssertJ for fluent assertions
   - Cucumber for BDD tests

2. **Test Structure**:
   - Each test class extends appropriate base class (`UnitTest`, `ComponentTest`, etc.)
   - Tests follow AAA pattern (Arrange-Act-Assert)
   - Each test method tests one behavior
   - Descriptive test names using DisplayName annotation

3. **Test Execution**:
   - Maven profiles for test categories
   - JUnit tags for test selection
   - Parallel test execution enabled
   - Tagged builds in CI

4. **Coverage Tools**:
   - JaCoCo for code coverage
   - PIT for mutation testing on critical modules

### Go Testing Standards

1. **Framework Choice**:
   - Standard `testing` package
   - `testify` for assertions
   - `go-sqlmock` for database mocks
   - `httptest` for HTTP testing

2. **Test Structure**:
   - Table-driven tests preferred
   - BDD-style tests with defined test fixtures
   - Clean setup/teardown
   - Subtests for related test cases

3. **Test Execution**:
   - `go test` command with appropriate tags
   - Testing all packages with `./...`
   - Verbose output when needed with `-v`
   - Race detection with `-race`

4. **Coverage Tools**:
   - Go's built-in coverage tools
   - Coverage integration with SonarQube

### Python Testing Standards

1. **Framework Choice**:
   - pytest for test execution
   - pytest-mock for mocking
   - behave for BDD tests
   - pytest-benchmark for performance tests

2. **Test Structure**:
   - Fixtures for test setup
   - Parameterized tests for multiple cases
   - pytest.mark for test categorization
   - Descriptive test functions

3. **Test Execution**:
   - pytest command with appropriate markers
   - Parallel execution with pytest-xdist
   - Pytest configurations in pyproject.toml

4. **Coverage and Quality Tools**:
   - coverage.py for code coverage
   - Ruff for linting
   - mypy for static type checking
   - Bandit for security scanning

### Shell Script Testing Standards

1. **Framework Choice**:
   - shunit2 for unit testing
   - bats for behavior testing

2. **Static Analysis**:
   - ShellCheck for syntax checking
   - bash -n for syntax validation

3. **Test Structure**:
   - Test functions with setUp/tearDown
   - Mock commands with function overrides
   - Temporary directory isolation

4. **Test Execution**:
   - Direct execution via bats
   - Coverage using kcov where applicable

## CI/CD Integration

Our CI/CD pipeline enforces quality at every stage:

### Commit Stage

- Compile/syntax check
- Linting (all languages)
- Unit tests
- Component tests (fast)
- Security scanning
- Code coverage

### Pull Request Stage

- All Commit Stage checks
- Component tests (all)
- Integration tests
- Acceptance tests (subset)
- Code quality metrics
- Dependency vulnerability scanning

### Release Stage

- All Pull Request checks
- Full acceptance test suite
- Performance tests
- Cross-browser/platform tests
- Deployment verification tests

## Quality Tools and Gates

### Current Quality Gates

| Quality Gate | Tool | Enforced In | Threshold |
|--------------|------|-------------|-----------|
| Java Code Style | Checkstyle | Commit | 0 violations |
| Java Code Quality | SpotBugs | PR | 0 high priority |
| Java Code Quality | PMD | PR | 0 high priority |
| Go Linting | golangci-lint | Commit | 0 errors |
| Python Linting | Ruff | Commit | 0 errors |
| Shell Script Quality | ShellCheck | Commit | 0 errors |
| Test Coverage | JaCoCo/go-cover/Coverage.py | PR | 80% minimum |
| Dependency Security | OWASP Dependency Check | PR | 0 high/critical |
| Type Safety | mypy (Python) | Commit | 0 errors |
| API Documentation | OpenAPI validation | PR | Valid schema |

### Tools to Add

Based on our quality principles, we should consider adding these tools:

1. **Mutation Testing** - Verify test effectiveness
   - PIT/Pitest for Java
   - go-mutesting for Go
   - Mutmut for Python

2. **Property-Based Testing** - Find edge cases
   - jqwik for Java
   - gopter for Go
   - Hypothesis for Python

3. **Contract Testing** - Verify API contracts
   - Spring Cloud Contract for Java
   - Pact for cross-language contracts

4. **Load/Performance Testing** - Verify performance under load
   - JMeter
   - K6
   - Gatling

## Pragmatic Patterns

These patterns guide our approach to testing across all languages:

### 1. Test for Behavior, Not Implementation

Tests should verify what code does, not how it works internally. This makes tests more resilient to refactoring.

### 2. Test Data Builders

For complex test data setup, use builder patterns to make tests more readable and maintainable:

```java
// Java example
User testUser = UserBuilder.create()
    .withName("Test User")
    .withEmail("test@example.com")
    .withRole(UserRole.ADMIN)
    .build();
```

### 3. Given-When-Then Structure

Organize tests in a clear Given-When-Then or Arrange-Act-Assert pattern:

```go
// Go example
func TestItem_ChangeStatus(t *testing.T) {
    // Given
    item := NewItem("Test", StatusDraft)
    
    // When
    err := item.ChangeStatus(StatusPublished)
    
    // Then
    assert.NoError(t, err)
    assert.Equal(t, StatusPublished, item.Status)
}
```

### 4. Sociable Unit Tests for Core Domain

For core domain logic, use sociable unit tests that test real collaborations between closely related objects:

```java
// Testing domain objects together rather than in isolation
@Test
void shouldTransitionWorkItemBetweenStates() {
    // Use real WorkItem and Workflow objects, not mocks
    WorkItem item = new DefaultWorkItem("Test", WorkItemType.FEATURE);
    Workflow workflow = new DefaultWorkflow();
    
    // Act
    workflow.transition(item, WorkflowState.IN_DEV);
    
    // Assert
    assertEquals(WorkflowState.IN_DEV, item.getState());
}
```

### 5. Test Doubles Hierarchy

Choose the appropriate test double for the situation:

1. Use real objects when testing domain logic
2. Use spies for verification when side effects matter
3. Use stubs for providing test data
4. Use mocks for verifying interactions
5. Use fakes for complex dependencies

### 6. Optimize for Local Development

Tests should be easy to run and interpret locally, to encourage developers to run them frequently:

- Fast execution (sub-second for unit tests)
- Clear failure messages
- Minimal setup requirements
- IDE integration

### 7. Test Naming as Documentation

Test names should clearly document the behavior being tested:

```python
# Python example
def test_item_with_high_priority_appears_at_top_of_list():
    # Test implementation
```

## Monitoring and Reporting

We continuously monitor our quality metrics through:

1. **Dashboard** - A centralized quality dashboard showing trends in:
   - Test coverage
   - Code quality metrics
   - Build health
   - Test execution time

2. **Alerts** - Alerts when:
   - Coverage drops below thresholds
   - Build fails repeatedly
   - Tests become flaky
   - Security vulnerabilities are detected

3. **Reports** - Regular reports on:
   - Technical debt
   - Test effectiveness
   - Areas needing improved testing

## Conclusion

This document represents our North Star for quality across the Rinna project. By following these principles, patterns, and practices, we ensure that testing remains a first-class citizen in our development process across all languages and components.

When evaluating whether more tests are needed, refer to these guidelines:

1. Is the code critical to core business logic? → Higher coverage
2. Does the code handle error conditions? → Test edge cases
3. Is the code complex or hard to understand? → More tests for clarity
4. Does the code interact with external systems? → More integration tests
5. Does the code implement user-facing features? → Acceptance tests

By embedding quality into our build and notification process, we ensure that quality is maintained throughout the development lifecycle.
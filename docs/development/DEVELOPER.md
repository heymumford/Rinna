# Rinna Developer Guide

Welcome to the Rinna Developer Guide! This document is intended for developers who want to contribute to Rinna or extend its functionality. If you're looking for user documentation, please check the [User Guide](/docs/user-guide/README.md).

## Quick Start for Developers

```bash
# Clone repository
git clone https://github.com/heymumford/Rinna.git
cd Rinna

# Set up development environment
bin/rin-setup-unified --developer

# Build the project
bin/rin build

# Run tests
bin/rin test
```

## Development Environment

Rinna is a polyglot project that uses:
- Java 21+ (core domain logic)
- Go 1.21+ (API and services)
- Python 3.13.3 (utilities and scripts)
- Bash (CLI tools and utilities)

See the [Environment Setup](/docs/development/environment-setup.md) guide for detailed instructions.

## Architecture

Rinna follows Clean Architecture principles:

```
┌────────────────────────────────────────────────────────────┐
│                     Presentation Layer                     │
│ ┌────────────────┐  ┌────────────────┐  ┌────────────────┐ │
│ │      CLI       │  │      API       │  │      GUI       │ │
│ └────────────────┘  └────────────────┘  └────────────────┘ │
└───────────────────────────┬────────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────────┐
│                 Application Service Layer                  │
│ ┌────────────────┐  ┌────────────────┐  ┌────────────────┐ │
│ │    Services    │  │   Use Cases    │  │  Interactors   │ │
│ └────────────────┘  └────────────────┘  └────────────────┘ │
└───────────────────────────┬────────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────────┐
│                      Domain Layer                          │
│ ┌────────────────┐  ┌────────────────┐  ┌────────────────┐ │
│ │    Entities    │  │   Interfaces   │  │     Rules      │ │
│ └────────────────┘  └────────────────┘  └────────────────┘ │
└───────────────────────────┬────────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────────┐
│                 Infrastructure Layer                       │
│ ┌────────────────┐  ┌────────────────┐  ┌────────────────┐ │
│ │  Repositories  │  │   Adapters     │  │  Persistence   │ │
│ └────────────────┘  └────────────────┘  └────────────────┘ │
└────────────────────────────────────────────────────────────┘
```

See [Architecture Documentation](/docs/development/architecture.md) for details.

## Project Structure

```
rinna/
├── api/               # Go API server
├── bin/               # CLI tools and utilities
├── docs/              # Documentation
│   ├── architecture/  # Architectural documentation
│   ├── development/   # Developer guides and reference
│   ├── getting-started/ # Initial setup guides
│   ├── reference/     # Reference documentation
│   ├── testing/       # Testing guides and strategy
│   └── user-guide/    # End-user documentation
├── rinna-cli/         # Java CLI components
├── rinna-core/        # Java core domain model
├── src/               # Legacy source code (being migrated)
└── version-service/   # Version management service
```

See [Project Structure](/docs/development/codebase-organization.md) for details.

## Developer Workflow

### 1. Set Up Your Environment

Follow the [Environment Setup](docs/development/environment-setup.md) guide to configure your development environment correctly. We recommend:

- Using the automated setup for consistent environment configuration
- Setting up IDE integrations for checkstyle, spotbugs, and PMD
- Configuring Git hooks for pre-commit checks

### 2. Understanding the Code

Before making changes, familiarize yourself with the codebase:

1. Start with the [Architecture Guide](docs/development/architecture.md) to understand the overall system design
2. Review the [Clean Architecture Principles](docs/architecture/decisions/0003-adopt-clean-architecture-for-system-design.md) as this drives our code organization
3. Study the [Package Structure](docs/development/package-structure.md) to understand where to place your code
4. Examine existing components similar to what you want to implement

Our code organization follows these key principles:

- **Domain-Driven Design**: Business logic in the domain layer
- **Dependency Inversion**: High-level modules don't depend on low-level modules
- **Interface Segregation**: Clients depend only on what they use
- **Explicit Dependencies**: Dependencies are injected, not created internally
- **Layer Isolation**: Clear boundaries between architectural layers

### 3. Development Process

We follow a streamlined development process:

1. **Issue Creation**
   - Start with a clear issue description
   - Label with appropriate categories (bug, feature, enhancement)
   - Link to related issues and discussions

2. **Feature Branch**
   - Create a branch with descriptive name: `git checkout -b feature/your-feature-name`
   - Keep branches focused on a single issue or feature
   - Rebase regularly to stay in sync with main

3. **Implementation**
   - Implement changes following our [Code Standards](docs/reference/standards/code-review-guidelines.md)
   - Follow the [Testing Strategy](docs/testing/TESTING_STRATEGY.md) (write tests first when possible)
   - Keep commits small and focused with descriptive messages
   - Document new APIs and significant features

4. **Code Review**
   - Submit a pull request with a clear description
   - Reference the issue being addressed
   - Respond to reviewer feedback
   - Update your PR as needed

5. **Continuous Integration**
   - Ensure all CI checks pass
   - Fix any test failures or static analysis issues
   - Verify integration with existing features

6. **Merging**
   - Squash or rebase commits if needed
   - Only merge when approved and CI passes
   - Delete feature branch after merging

## Key Developer Tools

### Build System

Rinna uses a sophisticated build system to manage the polyglot codebase. Our build tooling is designed to be:

- **Consistent** across languages and components
- **Fast** with targeted build modes for different use cases
- **Comprehensive** with integrated testing and quality checks

#### Build Commands

```bash
# Quick iterations during development (no tests)
bin/rin build fast

# Build with tests
bin/rin build test

# Full verification with coverage
bin/rin build verify

# Build specific components
bin/rin build java       # Java components only
bin/rin build go         # Go components only
bin/rin build python     # Python components only
bin/rin build all        # All components
```

#### Advanced Build Options

```bash
# Specify build profiles
bin/rin build --profile dev       # Development profile
bin/rin build --profile prod      # Production profile

# Skip specific steps
bin/rin build --skip-tests        # Skip all tests
bin/rin build --skip-quality      # Skip quality checks
bin/rin build --skip-javadoc      # Skip JavaDoc generation

# Performance options
bin/rin build --parallel          # Use parallel builds
bin/rin build --offline           # Use offline mode (no dependency downloads)
bin/rin build --verbose           # Show detailed build output
```

#### Maven Integration

Our Java components use Maven with a parent POM structure:

```bash
# Base Maven commands (using Maven wrapper)
./mvnw clean install            # Clean and build all
./mvnw compile                  # Compile only
./mvnw test                     # Run tests only
./mvnw verify                   # Run tests and quality checks
```

#### Quality Checks

The build system incorporates various quality tools:

- **Checkstyle**: Java code style validation
- **SpotBugs**: Java static analysis
- **PMD**: Additional Java code quality checks
- **golangci-lint**: Go static analysis
- **Pylint**: Python linting and static analysis
- **Black**: Python formatting
- **isort**: Python import sorting
- **Flake8**: Python code style validation
- **MyPy**: Python type checking

See [Build System](docs/development/build-system.md) for complete documentation of our build infrastructure.

### Testing

We follow a comprehensive testing strategy based on the testing pyramid. Our testing approach is:

- **Multi-layered**: Different test types targeting different concerns
- **Cross-language**: Consistent testing across Java, Go, and Python
- **Automated**: Integrated with our CI/CD pipeline
- **Fast feedback**: Quick test runs for development iterations

#### Test Layers

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

#### Test Commands

```bash
# Run tests by pyramid layer
bin/rin test unit          # Run unit tests
bin/rin test component     # Run component tests
bin/rin test integration   # Run integration tests
bin/rin test acceptance    # Run acceptance tests
bin/rin test performance   # Run performance tests

# Run test combinations
bin/rin test fast          # Run unit and component tests
bin/rin test essential     # Run unit, component, and integration

# Run tests with options
bin/rin test --tag=security     # Run tests with security tag
bin/rin test --coverage         # Generate code coverage report
bin/rin test --watch            # Monitor files and run tests on changes
bin/rin test --fail-fast        # Stop on first failure
bin/rin test --parallel         # Run tests in parallel
```

#### Creating Tests

Each test layer has specific conventions:

##### Unit Tests

```java
// Java example unit test
@Tag("unit")
public class DefaultWorkItemTest {
    @Test
    void shouldCreateWithValidParameters() {
        // Test implementation
    }
}
```

##### Component Tests

```java
// Java example component test
@Tag("component")
public class WorkflowServiceComponentTest {
    @Test
    void shouldTransitionWorkItemToNextState() {
        // Test implementation
    }
}
```

##### BDD Tests

For acceptance tests, we use BDD-style Cucumber tests:

```gherkin
Feature: Work Item Management

  Scenario: Add a new work item
    Given I am logged in as a team member
    When I create a work item with title "Fix authentication bug"
    Then the work item should be created with status "TO_DO"
```

See [Testing Strategy](docs/testing/TESTING_STRATEGY.md) for detailed documentation of our testing approach.

### Version Management

Rinna uses a centralized version management system with XMLStarlet integration:

```bash
# Using the high-level wrapper:
bin/rin-version current   # View version information
bin/rin-version patch     # Bump patch version
bin/rin-version minor     # Bump minor version
bin/rin-version verify    # Check consistency
bin/rin-version update    # Sync all files with version.properties

# Using the version manager directly (more options available):
bin/version-manager.sh current          # View version information
bin/version-manager.sh bump patch       # Bump patch version
bin/version-manager.sh increment-build  # Increment build number
bin/version-manager.sh set 2.0.0        # Set specific version
bin/version-manager.sh verify           # Check version consistency
```

IMPORTANT: Always use these tools for version management. Never manually edit version-related files or use text manipulation tools like sed/grep on XML files.

See [Version Management](docs/development/version-management.md) for comprehensive details.

## Specialized Developer Guides

- [Java 21 Features](docs/development/java21-features.md)
- [Cross-Language Logging](docs/development/cross-language-logging.md)
- [Dependency Management](docs/development/dependency-management.md)
- [CI Workflow](docs/development/ci-workflow.md)
- [Test-Driven Development](docs/testing/TDD_FEATURES.md)

## Debugging and Troubleshooting

### Logging

Rinna uses a unified logging approach across languages:

```java
// Java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

private static final Logger logger = LoggerFactory.getLogger(YourClass.class);
logger.debug("Processing item: {}", item);
```

```go
// Go
import "github.com/heymumford/rinna/api/pkg/logger"

log := logger.GetLogger("component-name")
log.Debug("Processing request", "requestID", reqID)
```

```python
# Python
from rinna.common.logging import get_logger

logger = get_logger(__name__)
logger.debug(f"Processing data: {data}")
```

### Python Development

Rinna's Python components use Poetry for dependency management, pytest for testing, and a comprehensive set of linting tools.

#### Poetry Setup and Usage

```bash
# Ensure correct Python version (3.13.3)
pyenv local 3.13.3
python --version  # Should show Python 3.13.3

# Install dependencies
cd python
poetry env use 3.13.3  # Explicitly set Poetry to use Python 3.13.3
poetry install

# Install with optional dependencies
poetry install -E reports  # Install report generation dependencies
poetry install -E web      # Install web API dependencies
poetry install -E all      # Install all optional dependencies

# Activate virtual environment
poetry shell

# Add a new dependency
poetry add package-name

# Add a development dependency
poetry add --group dev package-name
```

#### Python Testing

```bash
# Run tests with Poetry
cd python
poetry run pytest

# Run specific test categories
poetry run pytest -m unit
poetry run pytest -m integration

# Run with coverage
poetry run pytest --cov=rinna

# Using the test script
bin/run-python-tests.sh -u              # Run unit tests
bin/run-python-tests.sh -C              # Run component tests
bin/run-python-tests.sh -i              # Run integration tests
bin/run-python-tests.sh -c              # Run tests with coverage
bin/run-python-tests.sh -l              # Run pylint
bin/run-python-tests.sh -f              # Run formatters (black and isort)
bin/run-python-tests.sh -t              # Run type checking
bin/run-python-tests.sh --all           # Run all checks
```

#### Python Code Quality

```bash
# Format code
poetry run black .
poetry run isort .

# Lint code
poetry run pylint rinna

# Type check
poetry run mypy rinna

# Run pre-commit hooks
poetry run pre-commit run --all-files
```

See [Cross-Language Logging](docs/development/cross-language-logging.md) for comprehensive logging documentation.

### Common Issues

#### 1. Build Failures

- **Issue**: `mvn clean install` fails with compilation errors
- **Solution**: Check for:
  - Java version (must be 21+)
  - Missing dependencies
  - Syntax errors in recently modified files

#### 2. Test Failures

- **Issue**: Tests pass locally but fail in CI
- **Solution**:
  - Pull latest changes and rebase
  - Check for environment-specific configurations
  - Verify test data consistency

#### 3. API Connection Issues

- **Issue**: Cannot connect to API server
- **Solution**:
  - Verify the API server is running: `bin/rin-server status`
  - Check log files in `logs/api/`
  - Ensure correct ports are being used

#### 4. Permission Errors

- **Issue**: Permission denied when running scripts
- **Solution**:
  - Ensure scripts are executable: `chmod +x bin/*`
  - Check file ownership
  - Run with appropriate permissions

### Debugging Tools

#### Remote Debugging

For Java components:
```bash
# Start with remote debugging enabled
bin/rin-server start --debug

# Connect your IDE to port 5005
```

#### Profiling

- Java: Use VisualVM or JProfiler
- Go: Use pprof for CPU and memory profiling
- API: Enable performance logging with `bin/rin-server start --perf-log`

## Reference Documentation

- [Coding Standards](docs/reference/standards/code-review-guidelines.md)
- [Documentation Requirements](docs/reference/standards/documentation-requirements.md)
- [Logging Guidelines](docs/reference/standards/logging-guidelines.md)
- [Naming Conventions](docs/reference/standards/naming-conventions.md)
- [TODO Review Process](docs/reference/standards/todo-review-process.md)
- [Version Numbering](docs/reference/standards/version-numbering.md)

## Contributing

Please see our [Contribution Guidelines](CONTRIBUTING.md) for details on how to contribute to Rinna.

## Architecture Decision Records (ADRs)

We document significant architectural decisions in our ADR directory:

- [ADR-0003: Adopt Clean Architecture](docs/architecture/decisions/0003-adopt-clean-architecture-for-system-design.md)
- [ADR-0004: Refactor Package Structure](docs/architecture/decisions/0004-refactor-package-structure-to-align-with-clean-architecture.md)
- [ADR-0005: Multi-Language Approach](docs/architecture/decisions/0005-adopt-multi-language-approach-for-system-components.md)
- [ADR-0006: Testing Pyramid Strategy](docs/architecture/decisions/0006-implement-comprehensive-testing-pyramid-strategy.md)
- [ADR-0007: Security Framework](docs/architecture/decisions/0007-establish-security-compliance-framework.md)

## Advanced Topics

This section covers advanced concepts for experienced developers who want to dive deeper into Rinna's internals.

### Polyglot Communication

Rinna implements cross-language communication in several ways:

1. **Service-based**: Go API services with Java backends
2. **File-based**: Shared configuration files (version.properties, etc.)
3. **Process-based**: Shell commands for interprocess communication
4. **Message-based**: Unified logging across languages

See the [Cross-Language Communication Guide](docs/development/cross-language-logging.md) for implementation details.

### Custom Extensions

Rinna is designed to be extended through well-defined extension points:

- **Custom Validators**: Add new validation rules for work items
- **Custom Report Templates**: Create new document templates
- **Workflow Extensions**: Add custom workflow state transitions
- **Integration Points**: Connect to external systems

To create an extension:

1. Identify the appropriate extension point (adapter interface)
2. Implement the required interface
3. Register your extension using the service registry
4. Add configuration to enable your extension

### Performance Optimization

For performance-critical applications:

- Use the `--profile prod` build flag for optimized builds
- Implement caching for frequently accessed data
- Configure appropriate memory settings for the JVM
- Use the built-in profiling tools to identify bottlenecks

### Multi-tenant Deployments

For enterprise deployments with multiple teams:

1. Set up isolated project spaces for each team
2. Configure team-specific authentication and permissions
3. Implement resource quotas to prevent resource contention
4. Use the admin console for centralized monitoring

## License

This project is licensed under the [MIT License](LICENSE).
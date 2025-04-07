# Rinna: Developer-Centric Workflow Management

<div align="center">

*A terminal-based task management tool for developers who code, not managers who report.*

[![Rinna CI](https://github.com/heymumford/Rinna/actions/workflows/rin-ci.yml/badge.svg)](https://github.com/heymumford/Rinna/actions/workflows/rin-ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Java Version](https://img.shields.io/badge/java-21-orange.svg)](https://openjdk.java.net/projects/jdk/21/)
[![Go Version](https://img.shields.io/badge/go-1.21-blue.svg)](https://golang.org/doc/go1.21)
[![Version](https://img.shields.io/badge/version-1.10.3-blue.svg)](https://github.com/heymumford/Rinna/releases)
[![GitHub Stars](https://img.shields.io/github/stars/heymumford/Rinna?style=social)](https://github.com/heymumford/Rinna/stargazers)

[📥 Download](https://github.com/heymumford/Rinna/releases) • [📚 User Guide](docs/user-guide/README.md) • [👩‍💻 Developer Guide](DEVELOPER.md) • [🚀 Getting Started](docs/getting-started/README.md) • [🤝 Contribute](CONTRIBUTING.md) • [📋 Changelog](CHANGELOG.md)

</div>

## What Is Rinna?

Rinna is a workflow management system built for software engineers. It minimizes process overhead and integrates directly into your development environment, providing clear visibility without excessive ceremony.

**Rinna isn't replacing enterprise tools – it exists to make workflow management work _for_ developers, not the other way around.**

### The Problem

Traditional workflow tools:
- Force context-switching away from coding
- Interrupt [flow state](docs/technical-specification.md#core-philosophy)
- Prioritize reporting over productivity
- Add unnecessary complexity

### The Solution

- **Terminal-first interface** integrates with git workflows and IDEs
- **Zero-friction workflow** adds only what's necessary
- **Developer-owned process** puts control in the right hands
- **Clean architecture** with Go API and Java core
- **Standardized logging** with SLF4J and clearly defined log levels

## Work Model

- **Work Items**: Goals → Features → Bugs → Chores
- **Workflow**: Found → Triaged → To Do → In Progress → In Test → Done
- **No customization needed**: We've built what works based on experience

## Example Usage

### CLI

```bash
# Create a work item
bin/rin-cli add "Fix auth bypass" --type=BUG --priority=HIGH

# List work items in development
bin/rin-cli list --status=IN_DEV

# Update a work item
bin/rin-cli update WI-123 --status=DONE --assignee=developer1
```

### API

```bash
# Create work item via API
curl -X POST "http://localhost:8080/api/v1/workitems" \
  -H "Authorization: Bearer ri-dev-token" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Implement payment gateway",
    "type": "FEATURE",
    "priority": "HIGH"
  }'
```

### Administrative Operations

```bash
# Audit log management
bin/rin admin audit list                                  # List recent audit events
bin/rin admin audit configure --retention=90              # Configure audit log retention (days)
bin/rin admin audit export --format=csv                   # Export audit logs to file

# Compliance management
bin/rin admin compliance report financial                 # Generate financial compliance report
bin/rin admin compliance validate --project=demo          # Validate project compliance
bin/rin admin compliance configure --framework=iso27001   # Set compliance framework

# System monitoring
bin/rin admin monitor dashboard                           # Display system dashboard
bin/rin admin monitor metrics --type=system               # Show system metrics
bin/rin admin monitor alerts                              # Display active alerts

# System diagnostics
bin/rin admin diagnostics run                             # Run comprehensive diagnostics
bin/rin admin diagnostics schedule --interval=daily       # Schedule daily diagnostics

# Backup and recovery
bin/rin admin backup configure --location=/backup         # Configure backup location
bin/rin admin backup start --type=full                    # Start full system backup
bin/rin admin recovery plan --from=latest                 # Create recovery plan
```

## Installation

```bash
# Clone and build
git clone https://github.com/heymumford/Rinna.git
cd Rinna
chmod +x bin/rin bin/rin-version bin/rin-build bin/run-tests.sh
bin/rin build
```

### Build System

The Rinna build system supports multiple development workflows with a mode-based architecture:

```bash
# Quick iterations during development
bin/rin build fast

# Build with tests
bin/rin build test

# Full verification with coverage
bin/rin build verify

# Test categories
bin/rin build test unit        # Run unit tests
bin/rin build test component   # Run component tests
bin/rin build test integration # Run integration tests
bin/rin build test acceptance  # Run acceptance tests
bin/rin build test performance # Run performance tests

# Prepare for release
bin/rin build prepare-release
```

Advanced options include:
```bash
# Run parallel tests with coverage
bin/rin build test --parallel --coverage

# Monitor file changes and run tests automatically
bin/rin build test --watch

# Stop tests at first failure
bin/rin build test --fail-fast
```

See [Build System](docs/development/build-system.md) for detailed documentation.

### Maven Integration

```xml
<dependency>
    <groupId>org.rinna</groupId>
    <artifactId>rinna-core</artifactId>
    <version>1.8.1</version>
</dependency>
```

### Version Management

The project uses a centralized version management approach with `version.properties` as the single source of truth for all components (Java, Go, Python):

```bash
bin/rin version current   # View version information
bin/rin version patch     # Bump patch version
bin/rin version minor     # Bump minor version
bin/rin version verify    # Check consistency
bin/rin version update    # Sync all files with version.properties
bin/rin version release   # Create a release with git tag
```

Key features of our versioning system:
- **Single Source of Truth**: All components read from `version.properties`
- **Cross-Language Support**: Java, Go, and Python use the same version
- **Consistency Verification**: Checks all components have the same version
- **Automated Updates**: Updates all components when version changes

Integration with the build system enables streamlined release workflows:
```bash
bin/rin build prepare-release  # Run tests, package, update version
```

See [Version Management](docs/development/version-management.md) for details on our centralized approach.

## Requirements

- Java 21+
- Go 1.21+ (for API server)
- Maven 3.8+
- `jq` for CLI client

## Testing as a First-Class Citizen

At Rinna, testing is not an afterthought but a core part of our development philosophy. We believe that quality and testing are essential components of modern application delivery, embedded in every stage of development.

### Testing Pyramid Architecture

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

### Why This Approach is Timeless

We believe the testing pyramid represents a foundational approach to quality that transcends technological shifts:

- **Aligned with System Complexity**: Maps to how all complex systems are composed of simpler components
- **Technology-Agnostic**: Remains valid across programming languages, architecture styles, and infrastructure models
- **Mirrors Human Cognition**: Matches how people naturally think about and decompose problems
- **Economically Sound**: The cost/benefit ratio of each test level remains consistent regardless of technology
- **Empirically Validated**: Proven effective across multiple technology eras from mainframes to AI

These principles make our testing approach not just a current best practice, but a future-proof foundation for quality engineering in any era. [Read more about our testing philosophy](docs/testing/PHILOSOPHY.md).

### Implementation Across Languages

Our testing strategy spans multiple languages (Java, Go, Python, and Bash) and provides:

- **Base Test Classes** - Standardized parent classes for all test types
- **Layered Discovery** - Intelligent categorization of tests by purpose
- **Smart Test Runner** - Optimized execution based on the testing pyramid
- **Advanced CLI** - Seamless integration with our build system
- **Test Pyramid Analysis** - Automated monitoring of test pyramid balance across languages

```bash
# Generate a test pyramid coverage report
make test-pyramid

# Or directly use the analysis tool
./bin/test-pyramid-coverage.sh
```

See our [Test Pyramid Strategy](docs/testing/TEST_PYRAMID.md) for details on our polyglot testing approach.
\n### Test-Driven Development
\nRinna provides comprehensive support for Test-Driven Development (TDD) workflows with specific features for both general TDD practices and engineering-specific scenarios:
\n
\n```bash
\n# Run TDD tests
\nbin/rin test --tag=tdd
\n
\n# Run positive TDD scenarios
\nbin/rin test --tag=tdd --tag=positive
\n
\n# Run negative TDD scenarios
\nbin/rin test --tag=tdd --tag=negative
\n
\n# Run specific engineering TDD scenarios
\nbin/rin test --tag=tdd --include="*API*"
\n```
\n
\nSee our [TDD Features](docs/testing/TDD_FEATURES.md) documentation for details on how to use Rinna for effective Test-Driven Development across your entire engineering workflow.

### Running Tests

```bash
# Run tests by pyramid layer
bin/rin test unit          # Run unit tests only
bin/rin test component     # Run component tests only 
bin/rin test integration   # Run integration tests only
bin/rin test acceptance    # Run acceptance tests only
bin/rin test performance   # Run performance tests only

# Run test combinations
bin/rin test fast          # Run unit and component tests (quick feedback)
bin/rin test essential     # Run unit, component, and integration tests (no UI)

# Configure execution
bin/rin test --coverage    # Generate code coverage report
bin/rin test --watch       # Monitor files and run tests on changes
bin/rin test --fail-fast   # Stop on first failure
bin/rin test --no-parallel # Disable parallel execution
```

- [Test-Driven Development Features](docs/testing/TDD_FEATURES.md)
See our [Testing Strategy](docs/testing/TESTING_STRATEGY.md) for implementation details and our [Testing Philosophy](docs/testing/PHILOSOPHY.md) for our philosophical approach to quality in the age of AI.

## Comparison

| Feature | Rinna | Jira | GitHub Issues | Linear |
|---------|-------|------|--------------|--------|
| Focus | Developer experience | Management reporting | Issue tracking | Project management |
| Workflow | Fixed, streamlined | Highly customizable | Basic | Customizable |
| Git integration | Native | Plugin | Native | Plugin |
| Terminal-based | Yes | No | No | No |
| CLI | Full-featured | No | Limited | No |
| API | Go-powered | Yes | Yes | Yes |
| Test pyramid | First-class citizen | Limited | Limited | Limited |
| Cross-language testing | Comprehensive | No | No | No |
| Test discovery | Intelligent | Manual | Basic | Manual |
| Test automation | Advanced | Plugin-dependent | Basic | Limited |
| Quality gate integration | Built-in | Plugin | Limited | Plugin |
| Learning curve | Low | High | Medium | Medium |

## FAQ

### Why no workflow customization?
Most customization adds complexity without value. Our workflow represents the essential states needed for effective development.

### Enterprise tool integration?
Rinna provides mapping capabilities to synchronize with tools like Jira while maintaining its clean workflow.

### Suitable for large teams?
Yes. The streamlined model works for teams of all sizes while maintaining consistency.

### Extensibility?
Rinna follows clean architecture principles with well-defined interfaces for extending functionality.

## Documentation

### For Users

- [📚 User Guide](docs/user-guide/README.md) - Complete guide for using Rinna
- [🚀 Getting Started](docs/getting-started/README.md) - Quick start guide
- [📄 Documentation Generation](docs/user-guide/documents.md) - Generate documentation
- [📋 Changelog](CHANGELOG.md) - Release history

### For Developers

- [👩‍💻 Developer Guide](DEVELOPER.md) - Complete guide for developing Rinna
- [🤝 Contribution Guidelines](CONTRIBUTING.md) - How to contribute
- [🧪 Testing Strategy](docs/testing/TESTING_STRATEGY.md) - Comprehensive testing approach
- [🏗️ Architecture](docs/development/architecture.md) - System architecture
- [🔧 Build System](docs/development/build-system.md) - Build and development workflow
- [📊 Version Management](docs/development/version-management.md) - Version control approach

For complete developer documentation, see the [Developer Guide](DEVELOPER.md).

## License and Acknowledgments

This project is licensed under the [MIT License](LICENSE) - see the 
[LICENSE](LICENSE) file for details.

### Development Tools

This project was developed with analytical assistance from:
- Claude 3.7 Sonnet LLM by Anthropic
- Claude Code executable
- Google Gemini Deep Research LLM

These AI tools were used as paid analytical services to assist in development.
All intellectual property rights remain with Eric C. Mumford (@heymumford).

<div align="center">

*Built for developers who want to get things done, not fiddle with process.*


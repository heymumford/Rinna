# Rinna Development Guide

## Architecture and Design

Rinna follows a clean architecture approach:

- **Core**: Domain logic and business rules
- **Model**: Domain entities (Goal, Feature, Bug, Chore)
- **Service**: Business services managing workflow and releases
- **DB**: Data persistence layer
- **CLI**: Command-line interface
- **Util**: Utility classes

For our overall implementation approach and design decisions, see:

- [Design Approach](design-approach.md) - Our phased implementation strategy
- [Architecture](architecture.md) - Detailed system architecture
- [Package Refactoring](package-refactoring.md) - Guide for the com.rinna to org.rinna package migration

## Development Setup

```bash
# Clone the repository
git clone https://github.com/heymumford/rinna.git

# Build the project
cd rinna
./mvnw clean install

# Make scripts executable
chmod +x bin/rin bin/rin-version
```

## Code Style Guidelines

- **Java**: Follow Oracle Java style guide
- **Naming**: CamelCase for classes, lowerCamelCase for methods/variables
- **Imports**: Group and order: java.*, javax.*, org.*, com.*
- **Formatting**: 4-space indentation, 100 char line limit
- **Error Handling**: Use explicit exceptions with meaningful messages
- **Types**: Prefer immutable objects, use interfaces for declarations
- **Documentation**: JavaDoc for all public methods and classes
- **Testing**: BDD tests for high-level features, JUnit for unit tests

## Development Workflow

Use the Rinna CLI tool for simplified build and test management:

```bash
# Clean, build, and test the project
bin/rin all

# Run tests with verbose output
bin/rin -v test

# Build with errors-only output
bin/rin -e build
```

### Common Build Operations

Our project uses Maven with a wrapper (./mvnw) for build operations. The `bin/rin` tool provides a convenient interface to these commands:

| Operation | Rin Command | Maven Command |
|-----------|-------------|---------------|
| Clean | `bin/rin clean` | `./mvnw clean` |
| Compile | `bin/rin build` | `./mvnw compile` |
| Test | `bin/rin test` | `./mvnw test` |
| Package | `bin/rin build` | `./mvnw package` |
| Run all checks | `bin/rin all` | `./mvnw verify` |
| Generate docs | - | `./mvnw site` |
| Static analysis | - | `./mvnw verify` |

## Adding Features

1. Create domain models in `org.rinna.model`
2. Implement business logic in `org.rinna.service`
3. Add persistence in `org.rinna.db`
4. Extend CLI commands in `org.rinna.cli`
5. Write tests in `src/test/java/org/rinna`
6. Add Cucumber scenarios in `src/test/resources/features`

## Testing

We use both JUnit and Cucumber for testing:

```bash
# Run all tests
bin/rin test

# Run tests with full output
bin/rin -v test
```

## Contributing

Before submitting your code:

1. Ensure all tests pass
2. Verify code follows the style guidelines
3. Run static analysis tools
   ```bash
   ./mvnw verify
   ```
4. Add appropriate documentation
5. Create a detailed pull request
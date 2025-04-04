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
git clone https://github.com/samstraumr/rinna.git

# Build the project
cd rinna
mvn clean install

# Run tests
mvn test
```

## Adding Features

1. Create domain models in `org.rinna.model`
2. Implement business logic in `org.rinna.service`
3. Add persistence in `org.rinna.db`
4. Extend CLI commands in `org.rinna.cli`
5. Write tests in `src/test/java/org/rinna`
6. Add Cucumber scenarios in `src/test/resources/features`

## Testing

```bash
# Run unit tests
mvn test

# Run Cucumber tests
cucumber features/
```
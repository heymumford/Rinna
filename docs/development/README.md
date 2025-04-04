# Rinna Development Guide

## Architecture

Rinna follows a clean architecture approach:

- **Core**: Domain logic and business rules
- **Model**: Domain entities (Goal, Feature, Bug, Chore)
- **Service**: Business services managing workflow and releases
- **DB**: Data persistence layer
- **CLI**: Command-line interface
- **Util**: Utility classes

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

1. Create domain models in `com.rinna.model`
2. Implement business logic in `com.rinna.service`
3. Add persistence in `com.rinna.db`
4. Extend CLI commands in `com.rinna.cli`
5. Write tests in `src/test/java/com/rinna`
6. Add Cucumber scenarios in `src/test/resources/features`

## Testing

```bash
# Run unit tests
mvn test

# Run Cucumber tests
cucumber features/
```
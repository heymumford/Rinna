# Rinna API Karate Tests

This directory contains Karate API tests for the Rinna REST API. Karate is a BDD-style API testing framework that combines API test automation, assertions, and test data management in a single, easy-to-use package.

## Directory Structure

```
karate/
├── karate-config.js            # Global configuration for all tests
├── KarateTests.java            # JUnit 5 runner for executing tests
├── workitems.feature           # Tests for the Work Items API
├── projects.feature            # Tests for the Projects API (to be implemented)
├── releases.feature            # Tests for the Releases API (to be implemented)
└── auth.feature                # Authentication utilities (to be implemented)
```

## Running the Tests

### Running via Maven

```bash
# Run all Karate tests
mvn test -Dtest=KarateTests

# Run only smoke tests
mvn test -Dtest=KarateTests#smokeTests

# Run with specific environment
mvn test -Dkarate.env=staging -Dtest=KarateTests

# Run with tags
mvn test -Dkarate.options="--tags @crud" -Dtest=KarateTests
```

### Running via Gradle

```bash
# Run all Karate tests
./gradlew test --tests *KarateTests

# Run with specific environment
./gradlew test -Dkarate.env=qa --tests *KarateTests
```

## Configuration

The `karate-config.js` file provides environment-specific configurations:

- `dev` - Local development environment (default)
- `qa` - QA testing environment
- `staging` - Pre-production environment
- `prod` - Production environment

## Best Practices

1. **Organize by Resource**: Each API resource should have its own feature file
2. **Reuse Scenarios**: Use call syntax to reuse scenarios (e.g., for setup)
3. **Tagged Tests**: Use tags to categorize tests for selective execution
4. **Environment Awareness**: Use karate.env for environment-specific behavior
5. **Parallel Execution**: Use parallel execution for faster feedback

## Adding New Tests

1. Create a new feature file for the API resource
2. Add the feature file to KarateTests.java
3. Run the tests to ensure they work as expected

For more information on Karate syntax and features, see [Karate Test Syntax](../../../docs/testing/KARATE_TEST_SYNTAX.md).
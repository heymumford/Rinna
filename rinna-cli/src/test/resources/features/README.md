# Feature Files for Rinna CLI BDD Tests

This directory contains feature files used for BDD-style testing of the Rinna CLI application.

## Available Features

- [grep-command.feature](grep-command.feature) - Tests for the grep command functionality, which allows searching for text patterns in work items
- [linux-style-commands.feature](linux-style-commands.feature) - Tests for Linux-style commands like ls, cat, grep, etc.

## Running the Tests

To run all BDD tests:

```bash
mvn test -Dcucumber.filter.tags="@bdd"
```

To run specific feature tests:

```bash
mvn test -Dcucumber.filter.tags="@grep"
```

## Adding New Tests

1. Create a new feature file in this directory with the `.feature` extension
2. Implement the step definitions in `org.rinna.cli.bdd` package
3. Tag your feature file with appropriate tags for filtering
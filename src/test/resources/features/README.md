# Rinna BDD Feature Files

This directory contains Cucumber feature files that define the behavior of the Rinna workflow management system using Behavior-Driven Development (BDD) methodology.

## Feature Files

- `workflow.feature` - Core workflow management functionality
- `release.feature` - Release management functionality
- `cli-integration.feature` - CLI integration with the Rinna API

## Running Tests

The features can be run using the specialized runners or the universal CucumberRunner:

```bash
# Run all BDD tests
./bin/rin test bdd

# Run specific feature tests
./bin/rin test workflow
./bin/rin test release
./bin/rin test cli

# Run tests with a specific tag
./bin/rin test tag:cli
./bin/rin test tag:negative
```

## CLI Integration Feature

The `cli-integration.feature` file defines the behavior of the command-line interface for the Rinna system. The CLI follows a Git-like syntax and communicates with a local web service that acts as a bridge to the Rinna API server.

Key commands:

```bash
# Creating work items
rin add 'Fix authentication bug'
rin add --type=BUG --priority=HIGH 'Database connection failure'
rin add --project=billing-system 'Add PayPal integration'

# Viewing work items
rin view WI-456

# Listing work items
rin list
rin list --status=FOUND

# Updating work items
rin update WI-601 --status=IN_DEV
rin update WI-602 --assignee=developer1
```

## Adding New Tests

1. Add a new scenario to an existing feature file or create a new .feature file
2. Implement step definitions in the corresponding Steps class
3. Run the tests with `./bin/run-tests.sh bdd`

## Best Practices

1. Use domain-specific language that business stakeholders understand
2. Focus on behavior from the user's perspective
3. Create reusable step definitions
4. Use tags to organize tests
5. Keep scenarios independent and self-contained
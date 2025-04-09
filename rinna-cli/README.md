# Rinna CLI Module

## ✅ STATUS: ENABLED

This module has been re-enabled in the main build after fixing package structure incompatibilities resulting from the project-wide Clean Architecture refactoring.

## Overview

The Rinna CLI module provides a command-line interface for interacting with the Rinna workflow management system. It follows Clean Architecture principles to ensure maintainability and proper separation of concerns.

## API Integration

The CLI can operate in two modes:
1. **Standalone mode** - Using in-memory repositories for local operations
2. **API mode** - Connecting to the Go API server for distributed operations

### Using the CLI with API Server

To use the CLI with the Go API server:

1. **Start the API server**:
   ```bash
   # From the project root
   ./api/bin/rinnasrv --port 8080
   ```

2. **Configure the CLI**:
   ```bash
   # Set the API endpoint in the CLI
   bin/rin config --api-url http://localhost:8080/api/v1
   ```

3. **Use the CLI normally**:
   ```bash
   bin/rin list                    # List work items via API
   bin/rin add --title "New task"  # Create via API
   ```

### Key Integration Points

- **Authentication**: The CLI and API share the same authentication system
- **Operation Tracking**: All operations are tracked with unique IDs across CLI and API
- **Health Monitoring**: The CLI can check the API's health status
- **Consistent Data**: Data models are synchronized between CLI and API

## Project Structure

The CLI module follows the standard Maven project structure with enhancements for Clean Architecture:

```
/home/emumford/NativeLinuxProjects/Rinna/rinna-cli/
├── pom.xml                       # Maven project configuration
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── org/rinna/cli/
│   │   │       ├── RinnaCli.java # Main application entry point
│   │   │       ├── adapter/      # External adapters (Clean Architecture)
│   │   │       ├── command/      # CLI commands and subcommands
│   │   │       ├── config/       # Configuration classes
│   │   │       ├── domain/       # Domain models and entities
│   │   │       ├── messaging/    # Messaging components
│   │   │       ├── model/        # Data models for CLI
│   │   │       ├── service/      # Service implementations
│   │   │       └── util/         # Utility classes
│   │   └── resources/            # Application resources
│   └── test/
│       ├── java/
│       │   └── org/rinna/cli/
│       │       ├── acceptance/   # Acceptance tests
│       │       ├── bdd/          # BDD-style tests with Cucumber
│       │       ├── command/      # Command unit tests
│       │       ├── component/    # Component tests
│       │       ├── integration/  # Integration tests
│       │       ├── performance/  # Performance tests
│       │       └── unit/         # Unit tests
│       └── resources/
│           ├── features/         # Cucumber feature files
│           ├── cucumber.properties # Cucumber configuration
│           └── junit-platform.properties # JUnit configuration
```

## Testing Architecture

The testing architecture follows the Testing Pyramid approach with multiple levels of tests:

1. **Unit Tests**: Testing individual classes and methods in isolation
   - Located in: `/home/emumford/NativeLinuxProjects/Rinna/rinna-cli/src/test/java/org/rinna/cli/unit/`
   - Naming convention: `*Test.java`

2. **Component Tests**: Testing interaction between multiple units
   - Located in: `/home/emumford/NativeLinuxProjects/Rinna/rinna-cli/src/test/java/org/rinna/cli/component/`
   - Naming convention: `*ComponentTest.java`

3. **Integration Tests**: Testing integration with external dependencies
   - Located in: `/home/emumford/NativeLinuxProjects/Rinna/rinna-cli/src/test/java/org/rinna/cli/integration/`
   - Naming convention: `*IntegrationTest.java`

4. **BDD Tests**: Behavior-driven development tests using Cucumber
   - Cucumber features: `/home/emumford/NativeLinuxProjects/Rinna/rinna-cli/src/test/resources/features/`
   - Step definitions: `/home/emumford/NativeLinuxProjects/Rinna/rinna-cli/src/test/java/org/rinna/cli/bdd/`
   - Runner class: `/home/emumford/NativeLinuxProjects/Rinna/rinna-cli/src/test/java/org/rinna/cli/bdd/CucumberRunner.java`

5. **Acceptance Tests**: User-centric end-to-end tests
   - Located in: `/home/emumford/NativeLinuxProjects/Rinna/rinna-cli/src/test/java/org/rinna/cli/acceptance/`
   - Naming convention: `*AcceptanceTest.java`

6. **Performance Tests**: Tests for performance characteristics
   - Located in: `/home/emumford/NativeLinuxProjects/Rinna/rinna-cli/src/test/java/org/rinna/cli/performance/`
   - Naming convention: `*PerformanceTest.java`

### BDD Testing Configuration

The BDD testing infrastructure uses Cucumber with the following configuration:

#### Maven Dependencies

```xml
<dependency>
  <groupId>io.cucumber</groupId>
  <artifactId>cucumber-java</artifactId>
  <version>7.15.0</version>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>io.cucumber</groupId>
  <artifactId>cucumber-junit</artifactId>
  <version>7.15.0</version>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>io.cucumber</groupId>
  <artifactId>cucumber-junit-platform-engine</artifactId>
  <version>7.15.0</version>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>io.cucumber</groupId>
  <artifactId>cucumber-spring</artifactId>
  <version>7.15.0</version>
  <scope>test</scope>
</dependency>
```

#### Configuration Files

1. JUnit Platform Properties: `/home/emumford/NativeLinuxProjects/Rinna/rinna-cli/src/test/resources/junit-platform.properties`
   ```properties
   junit.jupiter.execution.parallel.enabled=true
   junit.jupiter.execution.parallel.mode.default=same_thread
   junit.jupiter.execution.parallel.mode.classes.default=concurrent
   cucumber.publish.quiet=true
   cucumber.junit-platform.naming-strategy=long
   cucumber.plugin=pretty, html:target/cucumber-reports/cucumber.html, json:target/cucumber-reports/cucumber.json
   cucumber.glue=org.rinna.cli.bdd
   cucumber.features=src/test/resources/features
   ```

2. Cucumber Properties: `/home/emumford/NativeLinuxProjects/Rinna/rinna-cli/src/test/resources/cucumber.properties`
   ```properties
   cucumber.publish.quiet=true
   cucumber.object-factory=io.cucumber.spring.SpringFactory
   cucumber.plugin=pretty
   cucumber.filter.tags=${cucumber.filter.tags}
   cucumber.execution.order=random
   cucumber.execution.dry-run=false
   cucumber.execution.wip=false
   cucumber.ansi-colors.disabled=false
   ```

#### Feature Organization

Feature files are organized by functionality in the `/home/emumford/NativeLinuxProjects/Rinna/rinna-cli/src/test/resources/features/` directory:

- `grep-command.feature`: Tests for the grep command functionality
- `linux-style-commands.feature`: Tests for Linux-style command functionality

Each feature file follows the Gherkin syntax with Given-When-Then scenarios and uses tags to categorize tests (@bdd, @grep, @linux).

## Building and Testing

### Using the Combined Build System

For the most reliable build that ensures API-CLI compatibility, use the combined build script:

```bash
# From the project root
./build-api-cli.sh         # Build both API and CLI components
```

This script:
1. Builds the Go API server and health check utility
2. Runs the API unit tests
3. Builds the Java CLI with proper dependencies
4. Automatically skips tests that might be failing during migration

### Building the CLI Only

You can build just the CLI module using the specialized build script:

```bash
./bin/rin-build-cli        # Normal build with tests
./bin/rin-build-cli -s     # Skip tests
./bin/rin-build-cli -c     # Clean before building
./bin/rin-build-cli -v     # Verbose output
```

Or directly with Maven:

```bash
cd /home/emumford/NativeLinuxProjects/Rinna/rinna-cli
mvn clean compile
mvn clean package -DskipTests    # Build with tests skipped
```

### Running Tests

To run all tests:

```bash
cd /home/emumford/NativeLinuxProjects/Rinna/rinna-cli
mvn test
```

To run only BDD tests:

```bash
cd /home/emumford/NativeLinuxProjects/Rinna/rinna-cli
mvn test -P bdd-only
```

To run specific feature tests:

```bash
cd /home/emumford/NativeLinuxProjects/Rinna/rinna-cli
mvn test -P grep-tests    # Run only grep command tests
mvn test -P linux-tests   # Run only Linux-style command tests
```

To run tests with a custom tag:

```bash
cd /home/emumford/NativeLinuxProjects/Rinna/rinna-cli
mvn test -Dcucumber.filter.tags="@mytag"
```

### Test Reports

Test reports are generated in the following locations:

- Cucumber HTML Report: `/home/emumford/NativeLinuxProjects/Rinna/rinna-cli/target/cucumber-reports/cucumber.html`
- Cucumber JSON Report: `/home/emumford/NativeLinuxProjects/Rinna/rinna-cli/target/cucumber-reports/cucumber.json`
- JUnit Reports: `/home/emumford/NativeLinuxProjects/Rinna/rinna-cli/target/surefire-reports/`

## Test-Driven Development (TDD) Workflow

The recommended workflow for implementing features using TDD is:

1. **Write Feature File**: Create or update a Cucumber feature file with scenarios
2. **Generate Step Definitions**: Run the tests to generate step definition snippets
3. **Implement Step Definitions**: Fill in the step definitions with test code
4. **Implement Production Code**: Write the minimal code to make the tests pass
5. **Refactor**: Clean up both test and production code while keeping tests passing

## Issues and Solutions

The CLI module had several issues that have been addressed:

1. **Type incompatibilities:**
   - `org.rinna.domain.WorkflowState` vs. `org.rinna.domain.model.WorkflowState` 
   - `org.rinna.domain.WorkItem` vs. `org.rinna.domain.model.WorkItem`
   - Missing `BacklogService` implementations

2. **Constructor parameter mismatches:**
   - `DefaultItemService`, `DefaultWorkflowService` constructors have changed

3. **Missing implementations:**
   - Several service methods needed to be implemented in the adapter classes

4. **Operation tracking:**
   - Commands needed overloaded method implementations to properly handle operation tracking
   - Added consistent operation tracking with unique IDs for all commands

## Fix Checklist

- [x] Update import statements to use new package structure
- [x] Fix `WorkflowCommand` class to use the correct `WorkflowState` enum
- [x] Update `ServiceManager` to use proper constructor patterns
- [x] Fix `BacklogCommand` and `BugCommand` implementations
- [x] Update test classes to match the new package structure
- [x] Re-enable the module in the parent pom.xml
- [x] Implement proper operation tracking for all command methods
- [x] Add overloaded methods for operation tracking in `MsgCommand`
- [x] Add API integration points for CLI-API communication
- [x] Ensure consistent error handling between CLI and API interfaces

## References

For more information on the package restructuring, see:
- [Migration Status](../docs/development/MIGRATION_STATUS.md)
- [Package Structure](../docs/development/package-structure.md)
- [Testing Strategy](../docs/testing/TESTING_STRATEGY.md)
# BUILD.md

This file provides guidance when working with code in this repository.

## Build & Test Commands
- Compile: `javac -d bin src/**/*.java`
- Run: `java -cp bin:lib/* com.rinna.Main`
- Run single test: `java -cp bin:lib/* org.junit.runner.JUnitCore com.rinna.test.TestClassName`
- Run Cucumber tests: `cucumber features/`
- Lint: `checkstyle -c checkstyle.xml src/`

## Code Style Guidelines
- **Java**: Follow Oracle Java style guide
- **Naming**: CamelCase for classes, lowerCamelCase for methods/variables
- **Imports**: Group and order: java.*, javax.*, com.*, org.*
- **Formatting**: 4-space indentation, 100 char line limit
- **Error Handling**: Use explicit exceptions with meaningful messages
- **Types**: Prefer immutable objects, use interfaces for declarations
- **Documentation**: JavaDoc for all public methods and classes
- **Testing**: BDD tests for high-level features, JUnit for unit tests
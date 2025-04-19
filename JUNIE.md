# JUNIE.md

This file provides guidance to Junie when working with code in this repository.

## Key Commands
- Build all components: `./build.sh all [dev|test|prod]`
- Build specific components: `./build.sh [java|python|go|api-specs] [dev|test|prod]`
- Test Java: `cd java && mvn test -Dtest=TestClassName#testMethodName`
- Run tests by category: `cd java && mvn test -Dgroups="unit,component"`
- Run BDD tests: `cd java && mvn test -P bdd-only -Dcucumber.filter.tags="@feature-tag"`
- Run Python tests: `cd python && poetry run pytest`
- Run Go tests: `cd go && go test ./...`
- Java linting: `cd java && mvn checkstyle:check pmd:check`
- Python linting: `cd python && poetry run pylint rinna`
- Go linting: `cd go && golint ./...`
- CI verification: `./scripts/utils/run-checks.sh` (validates architecture and dependencies)
- Activate Python environment: `cd python && poetry env use python3.13 && poetry env activate`

## XML Manipulation
**IMPORTANT**: For all XML files (especially POM files), ALWAYS use `scripts/utils/xml-tools.sh`. NEVER use text tools (grep, sed).

XML Helper Commands:
```bash
# Format and validate all XML files
./scripts/utils/xml-tools/xml-cleanup.sh

# Using XML functions (after sourcing the library)
source ./scripts/utils/xml-tools.sh
xml_dependency_exists "java/pom.xml" "groupId" "artifactId"
xml_add_dependency "java/pom.xml" "org.example" "example-lib" "1.0.0" "test"
xml_format_pom "java/pom.xml"
```

## Code Style Guidelines
- **Clean Architecture**: Domain independent from infrastructure; use proper layering
- **Packages**: Follow `org.rinna.[domain|adapter|usecase]` structure
- **Naming**: PascalCase for classes, camelCase for methods/variables, UPPER_SNAKE_CASE for constants
- **Operation Tracking**: Use MetadataService for CLI commands with hierarchical operation tracking
- **Error Handling**: Use ErrorHandler utility with appropriate error categories
- **Logging**: Use SLF4J with appropriate log levels; never log sensitive information
- **Output Formatting**: Use OutputFormatter for consistent JSON/text output
- **Imports**: Group by domain (java core, external libs, project-specific)
- **Testing**: Tag tests with @Tag("unit"|"component"|"integration"|"acceptance"|"performance")
- **BDD Testing**: Follow Gherkin format with clear Given-When-Then structure
- **Java Version**: Use Java 23 features (updated project requirement)

## Project Structure
- Core domain: `java/rinna-core/src/main/java/org/rinna/domain`
- Services: `java/rinna-core/src/main/java/org/rinna/usecase`
- Implementations: `java/rinna-core/src/main/java/org/rinna/adapter`
- CLI Commands: `java/rinna-cli/src/main/java/org/rinna/cli/command`
- API Server: `go/src/` (Go implementation)
- Python Components: `python/rinna/`
- API Specifications: `api-specs/`
- Configuration: `config/`
- Build and utility scripts: `scripts/`

## Junie-Specific Guidelines
- When analyzing code, prioritize clean architecture principles
- For refactoring suggestions, focus on maintainability and testability
- When generating new code, follow the established patterns in the codebase
- Respect the polyglot nature of the project when suggesting cross-component changes
- Consider performance implications for operations on large datasets

## Polyglot Development Guidelines
- Maintain clear language boundaries between components
- Use the language-specific standard conventions for each component
- API definitions in `api-specs/` are the contract between components
- Shared configuration in `config/shared/` should be used for cross-component settings
- Each language component has its own build process and tests
- Use the unified build script for consistent builds across all components

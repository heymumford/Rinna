# JUNIE.md

This file provides guidance to Junie when working with code in this repository.

## Project Overview

Rinna is a polyglot workflow management system that integrates Java, Python, and Go components in a clean architecture. The system uses a language-specific approach where each component is isolated in its own directory with appropriate build tools and dependencies, while sharing common interfaces through API specifications.

## Key Commands

### Build Commands
- Build all components: `./build.sh all [dev|test|prod]`
- Build specific components: `./build.sh [java|python|go|api-specs] [dev|test|prod]`
- Build with parallel processing: `./build.sh all [dev|test|prod]` (automatically uses 12 parallel threads)
- Check build artifacts: `ls -la build/artifacts/` (contains build outputs and logs)
- View build summary: `cat logs/build-summary-latest.log`

### Testing Commands
- Test Java: `cd java && mvn test -Dtest=TestClassName#testMethodName`
- Run tests by category: `cd java && mvn test -Dgroups="unit,component"`
- Run BDD tests: `cd java && mvn test -P bdd-only -Dcucumber.filter.tags="@feature-tag"`
- Run Python tests: `cd python && poetry run pytest`
- Run Go tests: `cd go && go test ./...`

### Linting and Formatting
- Java linting: `cd java && mvn checkstyle:check pmd:check spotbugs:check`
- Python linting: `cd python && poetry run pylint rinna`
- Go linting: `cd go && golint ./...`
- Java code formatting: `cd java && mvn spotless:apply`
- Python formatting: `cd python && poetry run black . && poetry run isort .`
- Go formatting: `cd go && go fmt ./...`

### Development Environment
- CI verification: `./scripts/utils/run-checks.sh` (validates architecture and dependencies)
- Activate Python environment: `cd python && poetry env use python3.13 && poetry env activate`
- Setup IntelliJ IDEA: `./setup-intellij.sh` (configures IntelliJ IDEA for polyglot development)

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
- **Java Version**: Use Java 23 features with preview enabled (see java/pom.xml)
- **Operation Tracking**: Use MetadataService for CLI commands with hierarchical operation tracking
- **Error Handling**: Use ErrorHandler utility with appropriate error categories
- **Logging**: Use SLF4J with appropriate log levels; never log sensitive information
- **Output Formatting**: Use OutputFormatter for consistent JSON/text output
- **Imports**: Group by domain (java core, external libs, project-specific)
- **Testing**: Tag tests with @Tag("unit"|"component"|"integration"|"acceptance"|"performance")
- **BDD Testing**: Follow Gherkin format with clear Given-When-Then structure

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
- Build artifacts: `build/artifacts/`
- Build logs: `logs/`

## IDE Integration
The project includes IntelliJ IDEA configuration files in the `.idea` directory and run configurations in the `.run` directory. These can be set up using:

```bash
./setup-intellij.sh
```

This will configure IntelliJ IDEA with:
- Java, Python, and Go module structures
- Maven project import for Java components
- Run configurations for the build script
- Appropriate source directories and encoding settings

## Junie-Specific Guidelines
- When analyzing code, prioritize clean architecture principles
- For refactoring suggestions, focus on maintainability and testability
- When generating new code, follow the established patterns in the codebase
- Respect the polyglot nature of the project when suggesting cross-component changes
- Consider performance implications for operations on large datasets
- Suggest parallel processing opportunities where appropriate
- Focus on error handling and robust recovery in failure scenarios
- Recommend logging improvements for better observability
- Consider build system optimizations when appropriate

## Parallel Processing
The build system leverages parallel processing where appropriate:
- Java: Maven builds use `-T 12` threads
- Go: Uses `GOMAXPROCS=12` for compilation
- Python: Tests can use pytest-xdist for parallel execution
- Validation: API specifications can be validated in parallel

To monitor resource usage during builds:
```bash
# View CPU usage during build
htop  # Or Activity Monitor on macOS

# Check build logs for parallelism details
grep "parallel" logs/build-*.log
```

## Polyglot Development Guidelines
- Maintain clear language boundaries between components
- Use the language-specific standard conventions for each component
- API definitions in `api-specs/` are the contract between components
- Shared configuration in `config/shared/` should be used for cross-component settings
- Each language component has its own build process and tests
- Use the unified build script for consistent builds across all components
- Create language-appropriate error handling mechanisms
- Structure logging consistently across language boundaries
- Ensure thread safety in parallel operations across all languages
- Follow clean code principles in all languages (small functions, clear naming)

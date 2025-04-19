# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Key Commands
- Build all components: `./build.sh all [dev|test|prod]`
- Build specific components: `./build.sh [java|python|go|api-specs] [dev|test|prod]`
- Test Java: `cd java && mvn test -Dtest=TestClassName#testMethodName`
- Run tests by category: `cd java && mvn test -Dgroups="unit,component"`
- Run BDD tests: `cd java && mvn test -P bdd-only -Dcucumber.filter.tags="@feature-tag"`
- Run Python tests: `cd python && poetry run pytest`
- Run Go tests: `cd go && go test ./...`
- Java linting: `cd java && mvn checkstyle:check pmd:check spotbugs:check`
- Python linting: `cd python && poetry run pylint rinna`
- Go linting: `cd go && golint ./...`
- Java code formatting: `cd java && mvn spotless:apply`
- Python formatting: `cd python && poetry run black . && poetry run isort .`
- Go formatting: `cd go && go fmt ./...`
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
- **Java Version**: Use Java 23 features with preview enabled (see java/pom.xml)
- **Operation Tracking**: Use MetadataService for CLI commands with hierarchical operation tracking
- **Error Handling**: Use ErrorHandler utility with appropriate error categories
- **Logging**: Use SLF4J with appropriate log levels; never log sensitive information
- **Output Formatting**: Use OutputFormatter for consistent JSON/text output
- **Imports**: Group by domain in order: java, javax, org, com
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

## Java Development
- Java version: Java 23
- Build tool: Maven
- Testing: JUnit 5 with category tags
- BDD testing: Cucumber
- Linting: Checkstyle, PMD, SpotBugs
- Code formatting: Spotless

## Python Development
- Python version: 3.13.3 (via pyenv)
- Dependencies: Use Poetry for dependency management
- Testing: Use pytest for running tests, pytest-cov for coverage
- Linting: Use pylint, black, isort, flake8, and mypy
- Project config: See `python/pyproject.toml` for full configuration
- Pre-commit hooks: Install with `cd python && poetry run pre-commit install`

```bash
# Python development workflow (Python 3.13.3)
cd python
poetry env use 3.13.3      # Ensure using Python 3.13.3
poetry install             # Install dependencies
poetry run pytest          # Run tests
poetry run black .         # Format code
poetry run isort .         # Sort imports
poetry run pylint rinna    # Lint code
poetry run mypy rinna      # Type check
```

## Go Development
- Go version: 1.21+
- Dependencies: Go modules (go.mod)
- Testing: Go standard testing package
- Linting: golint, go vet
- Project structure: Standard Go project layout

```bash
# Go development workflow
cd go
go mod download           # Download dependencies
go build ./...            # Build all packages
go test ./...             # Run all tests
go vet ./...              # Run Go vet
golint ./...              # Run golint
go fmt ./...              # Format code
```

## Cross-Language Integration
- API specifications are in `api-specs/`
- Shared configuration is in `config/shared/`
- Each language component has its own directory and build process
- Components communicate through well-defined APIs

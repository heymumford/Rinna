# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Key Commands
- Build: `./bin/build.sh` or `mvn clean install`
- Build specific components: `bin/rin build [java|go|python|all]`
- Test: `./bin/rin-test [unit|component|integration|acceptance|performance]`
- Run a single test: `mvn test -Dtest=TestClassName#testMethodName`
- Run BDD tests: `mvn test -P bdd-only -Dcucumber.filter.tags="@feature-tag"`
- Run tests by language: `./bin/rin-test --java unit` (use --go or --python for other languages)
- Linting: `./bin/run-quality-checks.sh` or `mvn checkstyle:check && mvn pmd:check`
- CI verification: `./bin/run-checks.sh` (validates architecture and dependencies)
- Version management: `bin/rin-version [current|patch|minor|verify|update]`

## XML Manipulation
**IMPORTANT**: For all XML files (especially POM files), ALWAYS use `bin/xml-tools.sh`. NEVER use text tools (grep, sed).

XML Helper Commands:
```bash
# Format and validate all XML files
./bin/xml-tools/xml-cleanup.sh

# Using XML functions (after sourcing the library)
source ./bin/xml-tools.sh
xml_dependency_exists "pom.xml" "groupId" "artifactId"
xml_add_dependency "pom.xml" "org.example" "example-lib" "1.0.0" "test"
xml_format_pom "pom.xml"
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
- **Java Version**: Use Java 21 features (project requirement)

## Project Structure
- Core domain: `rinna-core/src/main/java/org/rinna/domain`
- Services: `rinna-core/src/main/java/org/rinna/usecase`
- Implementations: `rinna-core/src/main/java/org/rinna/adapter`
- CLI Commands: `rinna-cli/src/main/java/org/rinna/cli/command`
- API Server: `api/` (Go implementation)
- Python Components: `python/rinna/`
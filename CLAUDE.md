# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview
Rinna is a polyglot workflow management system integrating Java, Python, and Go components in a clean architecture.

## Key Commands
### Build & Run
- Build all: `./build.sh all [dev|test|prod]` 
- Component build: `./build.sh [java|python|go|api-specs] [dev|test|prod]`
- Java build: `cd java && mvn clean install -T 12 [-P production|test]`
- Python build: `cd python && poetry install [--extras 'all'] [--with dev]`
- Go build: `cd go && go build -p 12 ./...`
- Documentation: `./build-docs.sh`

### Testing
- Java single test: `cd java && mvn test -Dtest=TestClassName#testMethodName`
- Test categories: `cd java && mvn test -Dgroups="unit,component"`
- BDD tests: `cd java && mvn test -P bdd-only -Dcucumber.filter.tags="@feature-tag"`
- Python tests: `cd python && poetry run pytest tests/path/to/test_file.py::TestClass::test_method`
- Go tests: `cd go && go test ./... -run TestFunctionName`
- Coverage: `cd java && mvn test jacoco:report` or `cd python && poetry run pytest --cov=rinna`
- Cross-language: `./java/src/test/cross-language/workitem_sync_test.sh`

### Linting & Formatting
- Java: `mvn checkstyle:check pmd:check spotbugs:check` and `mvn spotless:apply`
- Python: `poetry run pylint rinna && poetry run mypy rinna` and `poetry run black . && poetry run isort .`
- Go: `golangci-lint run && golint ./...` and `go fmt ./...`

## Code Style Guidelines
- **Architecture**: Follow clean architecture with proper domain/adapter/usecase separation
- **Naming**: PascalCase (classes), camelCase (methods/variables), UPPER_SNAKE_CASE (constants)
- **Packages**: Use `org.rinna.[domain|adapter|usecase]` structure
- **Imports**: Group by domain (java core, external libs, project-specific)
- **Error Handling**: Use ErrorHandler utility with appropriate error categories
- **Logging**: Use MultiLanguageLogger (Java), rinna_logger.py (Python), or middleware/logger (Go); apply consistent correlation IDs across components; never log sensitive information
- **Tests**: Tag with @Tag("unit|component|integration|acceptance|performance")
- **XML Files**: Use `scripts/utils/xml-tools.sh` for manipulation, NEVER text tools (grep, sed)
- **Java Style**: 120 character line length, Javadoc for public APIs
- **Python Style**: 88 character line length, type hints required
- **Go Style**: Standard Go conventions, structured error handling

## Development Best Practices
- Follow established patterns in the codebase
- Maintain language boundaries between components
- Use API specs as contract between components
- Respect polyglot nature when suggesting cross-component changes
- Consider performance for operations on large datasets
- Follow TDD approach when implementing new features
- Use Antora for documentation (`./build-docs.sh` to generate)
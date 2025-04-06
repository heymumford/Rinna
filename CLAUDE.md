# Rinna Project Commands for Claude

## Java Environment
```bash
# Use Java 21 for Maven builds
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

# Project-specific activation (when inside project directory)
source activate-java.sh
```

## Build Commands
```bash
mvn clean install     # Clean and build the entire project
mvn compile           # Compile the source code
mvn test              # Run Java tests only
mvn verify            # Run all tests including quality checks
mvn package           # Package the application

# Cross-language testing
mvn verify -P cross-language-tests   # Run Java tests and Go/Python/CLI tests and generate C4 diagrams
```

## Code Quality

### Java
```bash
# Quality checks now run automatically with standard Maven commands
mvn compile           # Runs checkstyle during validate phase
mvn test              # Runs checkstyle during validate phase
mvn verify            # Runs all checks (checkstyle, spotbugs, PMD)

# For faster builds, skip quality checks with the 'skip-quality' profile
mvn -P skip-quality test

# To run individual quality checks manually
mvn checkstyle:check  # Run checkstyle validation only
mvn com.github.spotbugs:spotbugs-maven-plugin:check  # Run spotbugs analysis only
mvn pmd:check         # Run PMD analysis only
mvn dependency-check:check  # Run OWASP dependency check
```

### Python
```bash
# Run Python quality checks using the python-quality script
./bin/python-quality all      # Run all Python quality checks
./bin/python-quality lint     # Run only linting (Ruff)
./bin/python-quality format   # Run only formatting (Black, isort)
./bin/python-quality types    # Run only type checking (MyPy)
./bin/python-quality security # Run only security checks (Bandit)
./bin/python-quality --fix    # Run checks and fix issues where possible
```

### Shell Scripts
```bash
# Run Shell Script quality checks
./bin/shell-quality           # Run basic checks for all shell scripts
./bin/shell-quality --fix     # Auto-fix common issues when possible

# Run Python tests
python -m pytest python/tests     # Run all Python tests
python -m pytest python/tests -v  # Run tests with verbose output
python -m unittest bin/test_c4_diagrams.py  # Run C4 diagram tests

# C4 Diagram Generation
./bin/c4_diagrams.py --type all            # Generate all diagram types
./bin/c4_diagrams.py --type context        # Generate context diagram
./bin/c4_diagrams.py --type container      # Generate container diagram
./bin/c4_diagrams.py --type component      # Generate component diagram
./bin/c4_diagrams.py --type code           # Generate code diagram
./bin/c4_diagrams.py --output svg          # Generate in SVG format
./bin/c4_diagrams.py --upload              # Upload to LucidChart
```

## Project-Specific CLI Commands

Use the `rin` CLI utility located in the bin directory:

```bash
# Build commands
./bin/rin build       # Build the project
./bin/rin clean       # Clean the project
./bin/rin all         # Clean, build, and test

# Advanced test commands (testing pyramid)
./bin/rin test unit          # Run unit tests only
./bin/rin test component     # Run component tests only
./bin/rin test integration   # Run integration tests only
./bin/rin test acceptance    # Run acceptance tests only
./bin/rin test performance   # Run performance tests only
./bin/rin test bdd           # Run all BDD tests (alias for acceptance)
./bin/rin test fast          # Run unit and component tests only (quick feedback)
./bin/rin test essential     # Run unit, component, and integration tests (no UI)

# Test configuration options
./bin/rin test --coverage    # Generate code coverage report
./bin/rin test --watch       # Monitor files and run tests on changes
./bin/rin test --fail-fast   # Stop on first failure
./bin/rin test --no-parallel # Disable parallel execution
./bin/rin test --verbose     # Show detailed output
./bin/rin test --workers N   # Set maximum number of parallel workers

# Legacy domain-specific test commands
./bin/rin test workflow      # Run workflow BDD tests only 
./bin/rin test release       # Run release BDD tests only
./bin/rin test input         # Run input interface BDD tests only
./bin/rin test api           # Run API integration tests only
./bin/rin test tag:<name>       # Run tests with a specific tag (e.g., tag:client)

# Advanced test scripts
./bin/smart-test-runner.sh all       # Run all tests with testing pyramid approach
./bin/test-discovery.sh              # Discover and categorize tests
./bin/test-discovery.sh --detailed   # Show detailed test report
./bin/run-tests.sh all               # Run all tests with legacy runner
./bin/run-tests.sh unit              # Run only unit tests with legacy runner
./bin/run-tests.sh -p bdd            # Run all BDD tests in parallel mode
./bin/run-new-tests.sh               # Test only the CLI commands
./bin/rinna-tests.sh                 # Run Java, Go, and Python tests
./bin/rinna-tests.sh minimal         # Run minimal tests for CI

# Version management
./bin/rin-version current       # Show current version information
./bin/rin-version major         # Bump major version (x.0.0)
./bin/rin-version minor         # Bump minor version (0.x.0)
./bin/rin-version patch         # Bump patch version (0.0.x)
./bin/rin-version set <version> # Set to specific version (e.g., 1.2.3)
./bin/rin-version release       # Create a release from current version
./bin/rin-version tag           # Create a git tag for current version
./bin/rin-version verify        # Verify version consistency across files
```

## Project Structure
- Core domain model is in `rinna-core/src/main/java/org/rinna/domain/entity`
- Service interfaces are in `rinna-core/src/main/java/org/rinna/domain/usecase`
- Service implementations are in `rinna-core/src/main/java/org/rinna/adapter/service`
- In-memory repositories are in `rinna-core/src/main/java/org/rinna/adapter/persistence`
- API server is in `api/cmd/rinnasrv`
- API health package is in `api/pkg/health`
- Python modules are in `python/rinna/`
- Python tests are in `python/tests/`
- Python quality configurations are in `config/python/` and `pyproject.toml`

## Next Development Tasks
1. Implement QueryService for developer-focused filtering
2. Create SQLite persistence module (rinna-data-sqlite)
3. Develop CLI interface module (rinna-cli)
4. Complete the Main application entry point

## Environment Management
- Run `java-switch 21` to switch to Java 21
- Run `java-switch 17` to switch to Java 17 
- Inside project: `source activate-java.sh` to use project-specific Java
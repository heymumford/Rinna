# Test Command Reference

This quick reference guide provides common test commands for all languages and tools used in the Rinna project. Use this as a handy cheat sheet for executing tests and analyzing test results.

## Unified Test Commands

The `rin-test` command provides a unified interface for running tests across all languages.

```bash
# Basic commands
./bin/rin-test                       # Run all tests
./bin/rin-test unit                  # Run unit tests only
./bin/rin-test component             # Run component tests only 
./bin/rin-test integration           # Run integration tests only
./bin/rin-test acceptance            # Run acceptance tests only
./bin/rin-test performance           # Run performance tests only

# Test combinations
./bin/rin-test fast                  # Run fast tests (unit + component)
./bin/rin-test essential             # Run essential tests (unit + component + integration)

# Language-specific tests
./bin/rin-test --only=java unit      # Run Java unit tests only
./bin/rin-test --only=go unit        # Run Go unit tests only
./bin/rin-test --only=python unit    # Run Python unit tests only

# Tag-based filtering
./bin/rin-test tag:workflow          # Run tests tagged with "workflow"
./bin/rin-test tag:critical          # Run tests tagged with "critical"

# File-based filtering
./bin/rin-test file:WorkItemTest     # Run tests matching filename pattern

# Execution options
./bin/rin-test --parallel unit       # Run unit tests in parallel
./bin/rin-test --verbose unit        # Run with verbose output
./bin/rin-test --quiet unit          # Run with minimal output
./bin/rin-test --no-coverage unit    # Skip coverage reporting

# Test execution mode
./bin/rin-test --minimal             # Run minimal test set
./bin/rin-test --ci                  # Run in CI mode
./bin/rin-test --detailed            # Run comprehensive test suite
```

## Java Test Commands

### Maven Commands

```bash
# Basic Maven test commands
mvn test                             # Run all tests
mvn test -DskipTests                 # Skip tests during build

# Test category profiles
mvn test -P unit-tests               # Run unit tests only
mvn test -P component-tests          # Run component tests only
mvn verify -P integration-tests      # Run integration tests only
mvn verify -P acceptance-tests       # Run acceptance tests only
mvn verify -P performance-tests      # Run performance tests only

# Test selection
mvn test -Dtest=WorkItemTest         # Run specific test class
mvn test -Dtest=*Service*Test        # Run test classes matching pattern
mvn test -Dtest=WorkItemTest#shouldCreateWorkItem  # Run specific test method

# JUnit tags
mvn test -Dgroups=unit,fast          # Run tests with specific tags
mvn test -DexcludedGroups=slow       # Exclude tests with specific tags

# Test output
mvn test -Dsurefire.useFile=false    # Output to console instead of files
mvn test -DtrimStackTrace=false      # Show full stack traces

# Coverage
mvn test -P jacoco                   # Generate JaCoCo coverage report
mvn verify -P sonar                  # Run tests and upload to SonarQube
```

### Gradle Commands

```bash
# Basic Gradle test commands
./gradlew test                       # Run all tests
./gradlew test --tests "WorkItemTest"  # Run specific test class
./gradlew test --tests "*Service*"   # Run tests matching pattern

# Test filtering
./gradlew test --tests "org.rinna.unit.*"  # Run tests in package
./gradlew test -PincludeTags="unit"  # Include tests with tag
./gradlew test -PexcludeTags="slow"  # Exclude tests with tag

# Test output
./gradlew test --info                # More detailed test output
./gradlew test --debug               # Debug-level output

# Coverage
./gradlew jacocoTestReport           # Generate JaCoCo coverage report
```

## Go Test Commands

```bash
# Basic Go test commands
go test ./...                        # Run all tests in current directory and subdirectories
go test ./pkg/health                 # Run tests in specific package

# Test filtering
go test -run TestWorkItem            # Run tests matching pattern
go test -run TestWorkItem/Create     # Run subtests matching pattern

# Test output
go test -v ./...                     # Verbose output (show all tests)
go test -count=1 ./...              # Disable test caching

# Coverage
go test -cover ./...                 # Show coverage percentage
go test -coverprofile=coverage.out ./...  # Generate coverage profile
go tool cover -html=coverage.out     # View coverage report in browser

# Benchmarks
go test -bench=.                     # Run all benchmarks
go test -bench=BenchmarkCreateWorkItem  # Run specific benchmark
go test -benchmem                    # Show memory allocations in benchmarks

# Race detection
go test -race ./...                  # Run tests with race detector

# Build tags
go test -tags=integration ./...      # Run tests with specific build tag
```

## Python Test Commands

```bash
# Basic pytest commands
python -m pytest                     # Run all tests
python -m pytest tests/unit/         # Run tests in specific directory
python -m pytest tests/unit/test_version.py  # Run specific test file

# Test selection
python -m pytest -k "WorkItem"       # Run tests matching keyword
python -m pytest tests/unit/test_version.py::test_get_version  # Run specific test

# Test markers
python -m pytest -m "unit"           # Run tests with specific marker
python -m pytest -m "not slow"       # Exclude tests with marker

# Test output
python -m pytest -v                  # Verbose output
python -m pytest -q                  # Quiet output
python -m pytest --no-header         # Hide pytest header

# Show print statements
python -m pytest -s                  # Show print statements
python -m pytest --capture=no        # Disable output capturing

# Coverage
python -m pytest --cov=rinna         # Show coverage for module
python -m pytest --cov=rinna --cov-report=html  # Generate HTML coverage report

# Parallel execution
python -m pytest -xvs -n 4           # Run tests in parallel with 4 processes

# Test durations
python -m pytest --durations=10      # Show 10 slowest tests
```

## Test Analysis Commands

```bash
# Test discovery
./bin/test-discovery.sh              # Show test distribution
./bin/test-discovery.sh --detailed   # Show detailed test listing

# Test pyramid coverage
./bin/test-pyramid-coverage.sh       # Show test pyramid distribution
./bin/test-pyramid-coverage.sh --json  # Output in JSON format
./bin/test-pyramid-coverage.sh --output=report.md  # Write to file

# Code coverage
./bin/polyglot-coverage.sh           # Generate unified coverage report
./bin/polyglot-coverage.sh -o html   # Generate HTML coverage report
./bin/polyglot-coverage.sh -t 80     # Set coverage threshold to 80%

# Cross-language test compatibility
python bin/generate-compatibility-matrix.py  # Generate compatibility matrix
```

## BDD Test Commands

```bash
# Run all BDD tests
./bin/rin-test acceptance            # Run all acceptance tests
./bin/rin-test bdd                   # Alias for acceptance tests

# Domain-specific BDD tests
./bin/rin-test tag:workflow          # Run workflow BDD tests
./bin/rin-test tag:release           # Run release BDD tests

# Maven BDD commands
mvn verify -P acceptance-tests       # Run all BDD tests
mvn verify -Dcucumber.filter.tags="@workflow"  # Run tagged scenarios

# Cucumber options
mvn verify -Dcucumber.features="src/test/resources/features/workflow.feature"  # Run specific feature
mvn verify -Dcucumber.glue="org.rinna.bdd"  # Set glue path
mvn verify -Dcucumber.plugin="pretty,html:target/cucumber-reports"  # Set plugins
```

## CI/CD Test Commands

```bash
# Run tests in CI mode
./bin/rin-test --ci                  # Run tests optimized for CI

# Skip long-running tests
./bin/rin-test --ci --exclude=slow   # Skip slow tests in CI

# Generate reports for CI
./bin/rin-test --ci --junit          # Generate JUnit XML reports
./bin/polyglot-coverage.sh --ci      # Generate CI coverage report

# Fail fast
./bin/rin-test --ci --fail-fast      # Stop on first test failure
```

## Admin Test Commands

```bash
# Run all admin tests
./bin/run-admin-tests.sh             # Run all admin tests

# Run specific admin test groups
./bin/run-admin-tests.sh --config    # Run configuration tests only
./bin/run-admin-tests.sh --integration  # Run integration tests only
./bin/run-admin-tests.sh --project   # Run project management tests only

# Run specific admin test runner
./bin/run-admin-tests.sh --specific=AdminUserManagementRunner  # Run specific test runner
```

## Debugging Tests

```bash
# Java test debugging
mvn test -Dmaven.surefire.debug      # Start with debugger on port 5005

# Go test debugging
dlv test ./pkg/health                # Debug tests with Delve

# Python test debugging
python -m pytest --pdb               # Enter debugger on failure
python -m pytest -xvs                # Show output and enter debugger

# Run single test with additional output
./bin/rin-test file:WorkItemTest --verbose  # Run with verbose output
```

## Environment Setup for Tests

```bash
# Set up clean test environment
./bin/rin-setup-unified test         # Set up test environment

# Set Java test options
export JAVA_OPTS="-Xmx2g -XX:+HeapDumpOnOutOfMemoryError"  # Set Java memory options

# Set test database
export TEST_DB_URL="jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1"  # Set test database URL
```

## Related Resources

- [Test Automation Guide](TEST_AUTOMATION_GUIDE.md) - Complete guide to test automation
- [Test Templates](TEST_TEMPLATES.md) - Ready-to-use test templates
- [Test Troubleshooting Guide](TEST_TROUBLESHOOTING.md) - Solutions for common test issues
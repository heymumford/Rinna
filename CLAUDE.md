# Rinna SUSBS (Standardized Utility Shell-Based Solution) Commands for Claude

> **IMPORTANT**: For all XML manipulation (especially POM files), ALWAYS use the XMLStarlet-based tools in `bin/xml-tools.sh`. NEVER use grep, sed, or other text-based tools for XML files. The project has dedicated XML manipulation utilities for safety and consistency.

## Environment Setup
```bash
# Unified setup system 
bin/rin-setup-unified                  # Full setup with all components
bin/rin-setup-unified --minimal        # Minimal setup for core components
bin/rin-setup-unified --fast           # Fast setup mode (non-interactive)
bin/rin-setup-unified --graphical      # Graphical installer
bin/rin-setup-unified --verbose        # Show verbose debug output
bin/rin-setup-unified --skip-tests     # Skip environment testing
bin/rin-setup-unified --force          # Force reinstallation of components

# Install specific components
bin/rin-setup-unified java go python   # Install only specific components
bin/rin-setup-unified config           # Setup configuration files
bin/rin-setup-unified api              # Setup API service
bin/rin-setup-unified samples          # Create sample Java projects
bin/rin-setup-unified venv             # Setup Python virtual environment

# Non-interactive installation
bin/rin-setup-unified install java go  # Auto-install components without prompting
bin/rin-setup-unified check java go    # Check components without installing

# Additional component options
bin/rin-setup-unified api --verbose    # API setup with detailed output
bin/rin-setup-unified --all python     # Install all Python dependencies including optional ones
bin/rin-setup-unified --force venv     # Force recreate Python virtual environment

# Activate all environments at once
source activate-rinna.sh               # Created by rin-setup-unified

# Component-specific activation (created by rin-setup-unified)
source activate-java.sh                # Java environment only
source activate-go.sh                  # Go environment only
source activate-python.sh              # Python virtual environment
source activate-api.sh                 # API service environment
```

## Java Environment
```bash
# Use Java 21 for Maven builds
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

# Project-specific activation (when inside project directory)
source activate-java.sh
```

## Build Commands

### Unified Build System
```bash
# Run the complete build with all phases
./bin/build.sh

# Run specific build phase only
./bin/build.sh --phase=compile
./bin/build.sh --phase=test
./bin/build.sh --phase=package

# Quick build (skip tests and quality checks)
./bin/build.sh --quick

# Skip tests only
./bin/build.sh --skip-tests

# Skip quality checks only
./bin/build.sh --skip-quality

# Build specific components only
./bin/build.sh --components=java
./bin/build.sh --components=go,python

# Set specific Maven profile
./bin/build.sh --profile=ci

# Show verbose output
./bin/build.sh --verbose
```

### Maven Commands
```bash
mvn clean install     # Clean and build the entire project
mvn compile           # Compile the source code
mvn test              # Run Java tests only
mvn verify            # Run all tests including quality checks
mvn package           # Package the application

# Cross-language testing
mvn verify -P cross-language-tests   # Run Java tests and Go/Python/CLI tests and generate C4 diagrams

# Architecture validation
mvn validate -P validate-architecture  # Run architecture validation checks
```

### Makefile Targets
```bash
make                  # Build and test everything (default)
make build            # Build all components
make build-java       # Build Java components only
make build-go         # Build Go API server only
make test             # Run all tests
make test-java        # Run Java tests only
make test-go          # Run Go tests only
make clean            # Clean all build artifacts
make lint             # Run all linters
make quick            # Quick build (skip tests and quality checks)
```

## Code Quality

### Java
```bash
# Quality gates for different environments
./bin/run-quality-checks.sh        # Run quality checks with local thresholds
./bin/run-quality-checks.sh --ci   # Run quality checks with CI thresholds
./bin/run-quality-checks.sh --owasp-async # Run with async OWASP scan

# Quality checks are run automatically with standard Maven commands
mvn compile           # Runs checkstyle during validate phase
mvn test              # Runs checkstyle during validate phase
mvn verify            # Runs all checks (checkstyle, spotbugs, PMD)

# Maven profiles for quality gates
mvn -P local-quality verify  # Use local quality thresholds (default)
mvn -P ci verify            # Use CI quality thresholds (stricter)
mvn -P skip-quality test    # Skip all quality checks for faster builds
mvn -P jacoco test          # Generate JaCoCo coverage report only
mvn -P polyglot-coverage test  # Generate coverage for all languages

# To run individual quality checks manually
mvn checkstyle:check  # Run checkstyle validation only
mvn com.github.spotbugs:spotbugs-maven-plugin:check  # Run spotbugs analysis only
mvn pmd:check         # Run PMD analysis only
```

### Security Scanning
```bash
# CI environment only - full blocking scan
mvn verify -P ci      # Run OWASP dependency checks in CI mode

# Local CI testing - asynchronous scan
./bin/run-ci-local.sh  # Run tests + async OWASP scan in background

# Check dependency convergence
mvn org.apache.maven.plugins:maven-enforcer-plugin:3.4.1:enforce -Drules=dependencyConvergence  # Check dependency convergence 

# Manual security scan
mvn org.owasp:dependency-check-maven:check -Ddependency-check.skip=false  # Run OWASP scan directly
```

### Architecture Validation
```bash
# Run all architecture validation checks
./bin/run-checks.sh

# Run individual validation checks
./bin/checks/dependency-validator.sh
./bin/checks/test-structure-validator.sh
./bin/checks/check-clean-architecture.sh

# Run validation via Maven
mvn validate -P validate-architecture
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
./bin/generate-diagrams.sh                 # Generate all diagram types
./bin/generate-diagrams.sh --type context  # Generate context diagram only
./bin/generate-diagrams.sh --type container # Generate container diagram only
./bin/generate-diagrams.sh --type component # Generate component diagram only
./bin/generate-diagrams.sh --type code     # Generate code diagram only
./bin/generate-diagrams.sh --type clean    # Generate clean architecture diagram
./bin/generate-diagrams.sh --format svg    # Generate in SVG format (default)
./bin/generate-diagrams.sh --format png    # Generate in PNG format
./bin/generate-diagrams.sh --async         # Generate diagrams asynchronously
./bin/generate-diagrams.sh --clean         # Clean before generating

# Generate diagrams using Maven
mvn -P diagrams                           # Generate diagrams with dedicated profile
mvn package                               # Normal build includes async diagram generation

# Legacy diagram generation (direct access to Python script)
./bin/c4_diagrams.py --type all           # Generate all diagram types
./bin/c4_diagrams.py --output svg         # Generate in SVG format
./bin/c4_diagrams.py --upload             # Upload to LucidChart

# Swagger/OpenAPI Documentation
./bin/generate-swagger.sh                  # Generate API documentation in YAML format
./bin/generate-swagger.sh --validate-only  # Only validate the swagger.yaml file
./bin/generate-swagger.sh --format=json    # Generate documentation in JSON format
./bin/generate-swagger.sh --format=html    # Generate documentation in HTML format
```

## Project-Specific CLI Commands

Use the `rin` CLI utility located in the bin directory:

```bash
# Security and Authentication
./bin/rin login                    # Interactive login prompt
./bin/rin login username           # Login as specific user (prompts for password)
./bin/rin login --user=username    # Alternative syntax
./bin/rin logout                   # End current session

# User Access Management (admin only)
./bin/rin access help              # Show user access management help
./bin/rin access grant-permission --user=username --permission=perm    # Grant permission
./bin/rin access revoke-permission --user=username --permission=perm   # Revoke permission
./bin/rin access grant-admin --user=username --area=area               # Grant area-specific admin access
./bin/rin access revoke-admin --user=username --area=area              # Revoke area-specific admin access
./bin/rin access promote --user=username                               # Promote to full admin

# Server management
./bin/rin-server start             # Start the Rinna API server
./bin/rin-server start --port 8080 # Start server on custom port
./bin/rin-server start --no-auto-start # Start without auto-starting Java server
./bin/rin-server stop              # Stop the Rinna API server
./bin/rin-server restart           # Restart the server
./bin/rin-server status            # Check server status
./bin/rin-server log               # View server logs
./bin/rin-server configure         # Configure server settings

# Notification management
./bin/rin notify               # List all notifications
./bin/rin notify list          # List all notifications
./bin/rin notify unread        # Show only unread notifications
./bin/rin notify read <id>     # Mark a notification as read
./bin/rin notify markread <id> # Mark a notification as read
./bin/rin notify markall       # Mark all notifications as read
./bin/rin notify clear         # Clear old notifications
./bin/rin notify help          # Show notification help

# Statistics and metrics
./bin/rin stats                # Show summary statistics
./bin/rin stats dashboard      # Show statistics dashboard with visualizations
./bin/rin stats all            # Show all available statistics
./bin/rin stats distribution   # Show item distributions with charts
./bin/rin stats detail completion  # Show detailed completion metrics
./bin/rin stats detail workflow    # Show detailed workflow metrics
./bin/rin stats detail priority    # Show detailed priority metrics
./bin/rin stats detail assignments # Show detailed assignment metrics
./bin/rin stats --format=table     # Specify output format
./bin/rin stats --limit=5          # Limit output to top 5 items

# Build commands
./bin/rin build       # Build the project
./bin/rin clean       # Clean the project
./bin/rin all         # Clean, build, and test

# Administrative commands
./bin/rin admin audit list                                  # List audit logs
./bin/rin admin audit configure --retention=90              # Configure audit retention
./bin/rin admin audit export --format=csv                   # Export audit logs

./bin/rin admin compliance report financial                 # Generate compliance report
./bin/rin admin compliance validate --project=demo          # Validate project compliance
./bin/rin admin compliance configure --framework=iso27001   # Set compliance framework

./bin/rin admin monitor dashboard                           # Display system dashboard
./bin/rin admin monitor metrics --type=system               # Show system metrics
./bin/rin admin monitor alerts                              # Display active alerts

./bin/rin admin diagnostics run                             # Run system diagnostics
./bin/rin admin diagnostics schedule --interval=daily       # Schedule diagnostics

./bin/rin admin backup configure --location=/backup         # Configure backup location
./bin/rin admin backup start --type=full                    # Start system backup
./bin/rin admin recovery plan --from=latest                 # Create recovery plan

# Simplified Unified Test Runner
./bin/rin-test                    # Run all tests
./bin/rin-test unit               # Run only unit tests
./bin/rin-test component          # Run only component tests
./bin/rin-test integration        # Run only integration tests
./bin/rin-test acceptance         # Run only acceptance tests
./bin/rin-test performance        # Run only performance tests

# Language-Specific Test Commands
./bin/rin-test --java unit        # Run only Java unit tests
./bin/rin-test --go component     # Run only Go component tests
./bin/rin-test --python integration # Run only Python integration tests
./bin/rin-test --java --go integration # Run Java and Go integration tests

# Mode Options
./bin/rin-test --fast             # Run only fast tests (unit + component)
./bin/rin-test --ci               # Run tests optimized for CI environment

# Execution Options
./bin/rin-test --no-parallel      # Disable parallel test execution
./bin/rin-test --fail-fast        # Stop on first test failure
./bin/rin-test --coverage         # Generate code coverage report
./bin/rin-test --verbose          # Show verbose output
./bin/rin-test -v                 # Short form for verbose output

# Coverage Reports
./bin/rin-test --coverage         # Generate full coverage report
./bin/rin-test --coverage unit    # Generate coverage for unit tests only
./bin/rin-test --coverage --java  # Generate coverage for Java only

# Maven Test Profiles
mvn test -P unit-tests            # Run unit tests only
mvn test -P component-tests       # Run component tests only
mvn verify -P integration-tests   # Run integration tests only
mvn verify -P acceptance-tests    # Run acceptance tests only
mvn verify -P performance-tests   # Run performance tests only
mvn verify -P jacoco              # Generate Java code coverage with JaCoCo

# Critical path analysis
./bin/rin path               # Show critical path for the current project
./bin/rin path --blockers    # Show only blocking items in the project
./bin/rin path --item WI-123 # Show dependencies for a specific work item

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

# Code coverage commands
./bin/polyglot-coverage.sh           # Generate unified coverage report (text)
./bin/polyglot-coverage.sh -o html   # Generate HTML coverage report
./bin/polyglot-coverage.sh -o json   # Generate JSON coverage report
./bin/polyglot-coverage.sh --verbose # Show detailed coverage information
./bin/polyglot-coverage.sh -t 80     # Set minimum threshold to 80%

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
- Architecture validation scripts are in `bin/checks/`

## Test Structure
Tests are organized according to the test pyramid in standardized directories:
- Unit tests in `src/test/java/org/rinna/unit/`
- Component tests in `src/test/java/org/rinna/component/`
- Integration tests in `src/test/java/org/rinna/integration/`
- Acceptance tests in `src/test/java/org/rinna/acceptance/`
- Performance tests in `src/test/java/org/rinna/performance/`

Test documentation:
- [Unified Test Approach](docs/testing/UNIFIED_TEST_APPROACH.md)
- [Testing Strategy](docs/testing/TESTING_STRATEGY.md)
- [Admin Testing Guide](docs/testing/ADMIN_TESTING.md)
- [Test Migration Summary](TEST_MIGRATION_SUMMARY.md)

## Next Development Tasks
1. Implement QueryService for developer-focused filtering
2. Create SQLite persistence module (rinna-data-sqlite)
3. Develop CLI interface module (rinna-cli)
4. Complete the Main application entry point

## Environment Management
- Run `bin/rin-setup-unified` to install and configure all environments
- Inside project: `source activate-rinna.sh` to activate all environments
- Component-specific activations:
  - `source activate-java.sh` for Java environment
  - `source activate-go.sh` for Go environment
  - `source activate-python.sh` for Python virtual environment
  - `source activate-api.sh` for API service environment

## Utility Scripts
The Rinna project uses a collection of well-organized utility scripts:
- Common utilities in `bin/common/`
  - Shared functions in `bin/common/rinna_utils.sh`
  - Cross-language logging in `bin/common/rinna_logger.sh`
- Output formatting in `bin/formatters/`
  - Build output formatter in `bin/formatters/build_formatter.sh`
- Unified build orchestration in `bin/build.sh`
- Setup and environment configuration in `bin/rin-setup-unified`
- Configuration management in `bin/rin-config`
- Test framework in `bin/rin-test`
- Cross-language testing in `bin/run-polyglot-tests.sh`
- Version management in `bin/rin-version`
- Server management in `bin/rin-server`

## Components
The unified setup system can configure the following components:
- `java`: Java development environment (JDK 21)
- `go`: Go development environment (Go 1.21+)
- `python`: Python 3.8+ system installation
- `maven`: Apache Maven build system
- `venv`: Python virtual environment
- `config`: Configuration files and environment
- `ui`: User interface components and CLI tools
- `api`: Go API server for Rinna
- `samples`: Java sample projects demonstrating Clean Architecture

## Build Process

### Build Phases
The Rinna unified build system implements Maven-like build phases:

1. **Initialize** - Prepare the build environment and check prerequisites
2. **Validate** - Validate project code and configurations
3. **Compile** - Compile all components (Java, Go, Python)
4. **Test** - Run all tests (unit, component, integration, polyglot)
5. **Package** - Create distributable packages
6. **Verify** - Run additional checks and validations
7. **Install** - Install artifacts locally

### Cross-Language Integration
The build system ensures consistent coordination between components written in different languages:

- **Java Components**: Compiled with Maven and tested with JUnit/Cucumber
- **Go Components**: Built and tested with Go tools
- **Python Components**: Installed as packages and tested with pytest
- **Cross-Language Tests**: Validate integration between all components

The unified build script (`./bin/build.sh`) handles all components with consistent formatting and progress reporting. 

### Consistent Output Format
All build scripts use a standardized output format with consistent status indicators:

- 🔄 Task in progress
- ✅ Task completed successfully
- ❌ Task failed
- ⏭️ Task skipped
- ⚠️ Warning message

This provides clear visibility into the build process with "going to do / doing / done" updates.
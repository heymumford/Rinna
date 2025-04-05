<!-- Copyright (c) 2025 [Eric C. Mumford](https://github.com/heymumford) [@heymumford] -->

# Rinna CLI Tool

The `rin` command-line tool simplifies building, testing, and running Rinna with different verbosity levels. It uses the system Maven installation to provide consistent builds.

## Installation

The `rin` CLI tool is located in the `bin` directory of your Rinna installation. Make sure the scripts are executable:

```bash
chmod +x bin/rin bin/rin-version
```

For convenience, you can add the Rinna `bin` directory to your PATH or create a symlink to the script in a directory that's already in your PATH.

## Commands

`rin` supports the following commands:

| Command | Description |
|---------|-------------|
| **Work Item Commands** |
| `add` | Create a new work item |
| `view <id>` | View details of a work item |
| `list [filters]` | List work items with optional filters |
| `update <id> [options]` | Update a work item's properties |
| **Service Commands** |
| `server status` | Check status of Rinna services |
| `server start` | Start Rinna services |
| `server stop` | Stop Rinna services |
| `server restart` | Restart Rinna services |
| **Build Commands** |
| `build` | Build the Rinna project |
| `clean` | Clean build artifacts |
| `test`  | Run tests |
| `all`   | Run clean, build, and test (default if no command specified) |
| **Version Commands** |
| `version` | Version management (see below) |

### Version Management Commands

The `version` command provides a set of subcommands for managing versions across the project:

| Subcommand | Description |
|------------|-------------|
| `current` | Display current version information |
| `major` | Bump major version (X.0.0) |
| `minor` | Bump minor version (0.X.0) |
| `patch` | Bump patch version (0.0.X) |
| `set <version>` | Set to a specific version (e.g., 1.2.3) |
| `release` | Create a GitHub release from the current version |
| `tag` | Create a git tag for the current version |

#### Version Command Options

| Option | Description |
|--------|-------------|
| `-m, --message <msg>` | Specify a custom message for commits, tags, or releases |
| `-d, --dry-run` | Show what would happen without making changes |

## Global Options

`rin` provides several global options that can be used with any command:

| Option | Description |
|--------|-------------|
| `-v, --verbose` | Shows detailed output, useful for debugging. |
| `-c, --config <path>` | Specifies a custom configuration file path. |
| `--no-auto-start` | Prevents automatic starting of services. |
| `-h, --help` | Shows help information for a command. |
| `--version` | Shows the CLI version information. |

### Verbosity Options

`rin` provides three output modes to control how much information is displayed:

| Option | Description |
|--------|-------------|
| `-t, --terse` | **Default mode**. Shows minimal output with success/failure indicators and execution time, with full errors if something fails. |
| `-v, --verbose` | Shows all output from the underlying build tools. Useful for debugging or when you need to see every detail. |
| `-e, --errors` | Shows only errors and the steps that lead to them. Ideal for keeping output clean while still catching problems. |

## Output Examples

Here are examples of how the output appears in different modes:

### Terse Mode (Default)

The terse mode shows minimal output with success/failure indicators and execution times:

```
[Running tests...]
Running org.rinna.DemoAppTest
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
✓ Running tests completed successfully in 2s
[Test Summary]
Tests passed: 1, Tests failed: 0, Total: 1

All operations completed successfully!
```

If a test fails, it will show the failure but still keep the output minimal:

```
[Running tests...]
Running org.rinna.DemoAppTest
FAILED: testGetGreeting
Tests run: 1, Failures: 1, Errors: 0, Skipped: 0
✗ Running tests failed in 2s
[Test Summary]
Tests passed: 0, Tests failed: 1, Total: 1

Failed tests:
  • org.rinna.DemoAppTest

Completed with some failures
```

### Verbose Mode

The verbose mode shows all output from the underlying build tools, useful for debugging:

```
[Running tests...]
Running org.rinna.DemoAppTest
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.051 s
Test completed
✓ Running tests completed successfully in 0s
[Test Summary]
Tests passed: 1, Tests failed: 0, Total: 1
All operations completed successfully!
```

### Errors-Only Mode

The errors-only mode shows only errors and the steps that lead to them:

```
[Running tests...]
Error: Test failure detected in DemoAppTest
  org.opentest4j.AssertionFailedError: expected: <Hello, Rinna!> but was: <Goodbye, Rinna!>
  at org.rinna.DemoAppTest.testGetGreeting(DemoAppTest.java:24)
✗ Running tests failed in 2s
[Test Summary]
Tests passed: 0, Tests failed: 1, Total: 1

Failed tests:
  • org.rinna.DemoAppTest

Tests completed with failures
```

## Usage Examples

### Work Item Examples

```bash
# Create a new work item
rin add "Implement payment gateway" --type=FEATURE --priority=HIGH

# View a work item
rin view WI-123

# List all high priority bugs
rin list --type=BUG --priority=HIGH

# Update a work item's status
rin update WI-123 --status=IN_PROGRESS --assignee=johndoe
```

### Service Management Examples

```bash
# Check status of all services
rin server status

# Start all services
rin server start

# Start all services in verbose mode
rin -v server start

# Stop all services
rin server stop

# Restart all services
rin server restart

# Run a command without auto-starting services
rin --no-auto-start list
```

### Build Examples

```bash
# Build the project
rin build

# Run tests with verbose output
rin -v test

# Clean, build, and test the project with errors-only output
rin -e all

# Run tests in a specific module
cd rinna-core && ../bin/rin test
```

### Version Management Examples

```bash
# Check current version information
rin version current

# Bump minor version (e.g., 1.0.0 → 1.1.0)
rin version minor

# Set a specific version with custom message
rin version set 2.0.0 -m "Major release with workflow improvements"

# Create a tag for the current version
rin version tag -m "Release version 1.2.3"

# Create a GitHub release with automatic publishing to GitHub Packages
rin version release
```

## Help

To see the help text and available commands, use:

```bash
rin --help
```

## Configuration

Rinna uses a layered configuration system:

1. **Default configuration**: Built-in defaults
2. **Global configuration**: `~/.rinna/config.conf`
3. **Project configuration**: `.rinna.yaml` in the project root
4. **Command-line options**: Override other settings

### Configuration File Format

The project configuration file (`.rinna.yaml`) uses YAML format:

```yaml
# Project information
project:
  name: "Rinna"
  description: "Developer-Centric Workflow Management"
  version: "1.2.5"

# API configuration
api:
  endpoint: "http://localhost:9080/api/v1"
  
  # Backend services
  backend:
    java:
      host: "localhost"
      port: 8081
  
# Service management
service:
  auto_start: true
  startup_timeout: 30
```

For more details on service configuration, see [Service Management](service-management.md).

## Benefits

- **Automated Service Management**: Services start automatically when needed
- **Simplified Workflow**: Focus on tasks, not infrastructure management
- **Polyglot Architecture**: Seamless integration between Java and Go components
- **Consistent Interface**: Same command structure regardless of the underlying system
- **Configuration Flexibility**: Customize behavior per-project or globally
- **Execution Tracking**: Shows how long each phase takes, helping identify bottlenecks
- **Color-Coded Output**: Success, warnings, and errors are color-coded for easy identification
- **Adaptive Verbosity**: Control how much information is shown based on your needs

## Related Documentation

- [Service Management](service-management.md) - Detailed documentation on service architecture
- [Configuration Reference](configuration-reference.md) - Complete configuration options
- [Build System](../development/build-system.md) - Information about the build system
- [Version Management](../development/version-management.md) - Version control details

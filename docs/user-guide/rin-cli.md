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
| `done <id>` | Mark a work item as complete |
| `edit <id>` | Interactively edit a work item |
| `history <id>` | Show history of a work item |
| `comment <id> <text>` | Add a comment to a work item |
| **Search and Filtering Commands** |
| `grep <pattern> [options]` | Search work items for text patterns |
| `find [options]` | Find work items matching specific criteria |
| `backlog [options]` | Show items in the backlog |
| `cat <id>` | Display work item content (similar to view) |
| `ls [options]` | Show work items in directory-like format |
| **Workflow Commands** |
| `workflow [options]` | Manage and view workflow states |
| `path [options]` | Show critical path for the project |
| `import [source]` | Import work items from external source |
| `undo [operation]` | Undo previous operations |
| **Service Commands** |
| `server status` | Check status of Rinna services |
| `server start` | Start Rinna services |
| `server stop` | Stop Rinna services |
| `server restart` | Restart Rinna services |
| **Communication Commands** |
| `msg <user> <message>` | Send a message to another user |
| `notify [options]` | Manage notifications |
| **Report Commands** |
| `stats [options]` | Show project statistics |
| `report [options]` | Generate various reports |
| `schedule [options]` | Manage scheduled reports |
| **System Commands** |
| `test [options]` | Run tests for the system |
| `admin [subcommand]` | Administrative commands |
| `access [options]` | Manage user access and permissions |
| **Build Commands** |
| `build` | Build the Rinna project |
| `clean` | Clean build artifacts |
| `all`   | Run clean, build, and test (default if no command specified) |
| **Version Commands** |
| `version` | Version management (see below) |
| **Authentication Commands** |
| `login [username]` | Log in to the system |
| `logout` | Log out from the system |

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

### Admin Commands

The `admin` command provides administrative functions for managing Rinna:

| Subcommand | Description |
|------------|-------------|
| `audit [options]` | Manage audit logs and compliance |
| `backup [options]` | Backup and recovery management |
| `compliance [options]` | View and configure compliance settings |
| `diagnostics [options]` | Run system diagnostics |
| `monitor [options]` | System monitoring tools |
| `recovery [options]` | Restore from backups |
| `operations [options]` | View and manage system operations |

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

# Mark a work item as complete
rin done WI-123

# Add a comment to a work item
rin comment WI-123 "Fixed the issue with payment processing"

# View work item history
rin history WI-123
```

### Search and Filtering Examples

```bash
# Search for text in work items (like grep)
rin grep "payment gateway"

# Case-sensitive search
rin grep -s "API"

# Search with context
rin grep -c 3 "error"

# Format search results as JSON
rin grep "database" --format=json

# Find work items by criteria
rin find --type=BUG --status=IN_PROGRESS --assigned=me

# View backlog items
rin backlog

# List items in directory-like format
rin ls --sort=priority
```

### Workflow Examples

```bash
# Show workflow states
rin workflow states

# Show critical path for the project
rin path

# Show only blocking items on the critical path
rin path --blockers

# Show dependencies for a specific item
rin path --item=WI-123

# Critical path JSON output
rin path --format=json

# Import work items from JIRA
rin import jira --project=PROJ

# Undo the last operation
rin undo
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

### Communication Examples

```bash
# Send a message to a user
rin msg johndoe "Meeting at 3pm to discuss the release"

# List all notifications
rin notify list

# Show only unread notifications
rin notify unread

# Mark a notification as read
rin notify read 123

# Mark all notifications as read
rin notify markall
```

### Reporting Examples

```bash
# Show summary statistics
rin stats

# Show statistics dashboard with visualizations
rin stats dashboard

# Show all available statistics
rin stats all

# Show item distributions with charts
rin stats distribution

# Show detailed completion metrics
rin stats detail completion

# Show detailed workflow metrics
rin stats detail workflow

# Generate a full report
rin report generate

# Generate a specific report type
rin report generate --type=burndown

# Schedule a recurring report
rin schedule report --type=weekly --day=friday
```

### Admin Examples

```bash
# List audit logs
rin admin audit list

# Configure audit retention
rin admin audit configure --retention=90

# Generate compliance report
rin admin compliance report financial

# Run system diagnostics
rin admin diagnostics run

# Show system metrics
rin admin monitor metrics --type=system

# Perform a system backup
rin admin backup start --type=full

# Create a recovery plan
rin admin recovery plan --from=latest
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

### Authentication Examples

```bash
# Login with interactive prompt
rin login

# Login as specific user
rin login johndoe

# Logout of current session
rin logout
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

For help on a specific command:

```bash
rin <command> --help
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
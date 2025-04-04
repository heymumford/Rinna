# Rinna CLI Tool

The `rin` command-line tool simplifies building, testing, and running Rinna with different verbosity levels. It uses the Maven wrapper to ensure consistent builds across different environments without requiring a specific Maven installation.

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
| **Developer Workflow Commands** |
| `my-work` | Show all work items assigned to you |
| `next-task` | Show what you should work on next (prioritized) |
| `progress <id>` | Move an item to the next stage in your workflow |
| `start <id>` | Start working on an item (assigns to you and moves to In Progress) |
| `ready-for-test <id>` | Mark an item as ready for testing |
| `done <id>` | Complete an item |
| `my-history` | View your work history and statistics |
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

## Verbosity Options

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

### Developer Workflow Examples

```bash
# Show all work items assigned to you
rin my-work

# Show what you should work on next
rin next-task

# Start working on a task
rin start TASK-123

# Mark a task as ready for testing
rin ready-for-test TASK-123

# Complete a task
rin done TASK-123

# View your work history and productivity
rin my-history
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

## Benefits

- **Environment Independence**: Uses Maven wrapper to ensure consistent builds
- **Consistent Interface**: Same command structure regardless of the underlying build system
- **Execution Tracking**: Shows how long each phase takes, helping identify bottlenecks
- **Color-Coded Output**: Success, warnings, and errors are color-coded for easy identification
- **Test Summary**: Provides a clean summary of test results with pass/fail counts
- **Adaptive Verbosity**: Control how much information is shown based on your needs
- **Failed Test Details**: Clearly identifies which tests failed and provides error details

## Implementation Notes

The `rin` tool is implemented as a clean, focused bash script that uses the Maven wrapper (`./mvnw`) as its underlying build system. The implementation includes robust error handling to ensure that:

1. Build errors are properly reported
2. Test failures are clearly indicated
3. The appropriate level of detail is shown based on the selected verbosity mode
4. Color-coding helps quickly identify success and failure states

Version management is handled by a separate `rin-version` script that is called when using the `version` command. Both scripts are designed to be minimal and maintainable.
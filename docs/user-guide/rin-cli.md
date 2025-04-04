# Rinna CLI Tool

The `rin` command-line tool simplifies building, testing, and running Rinna with different verbosity levels. It's designed to make development and testing more efficient by providing a consistent interface with useful output formats.

## Installation

The `rin` CLI tool is located in the `bin` directory of your Rinna installation. Make sure the script is executable:

```bash
chmod +x bin/rin
```

For convenience, you can add the Rinna `bin` directory to your PATH or create a symlink to the script in a directory that's already in your PATH.

## Commands

`rin` supports the following commands:

| Command | Description |
|---------|-------------|
| `build` | Build the Rinna project |
| `clean` | Clean build artifacts |
| `test`  | Run tests |
| `all`   | Run clean, build, and test (default if no command specified) |

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
Running com.rinna.DemoAppTest
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
✓ Running tests completed successfully in 2s
[Test summary:]
Tests passed: 1, Tests failed: 0, Total: 1

All operations completed successfully!
```

If a test fails, it will show the failure but still keep the output minimal:

```
[Running tests...]
Running com.rinna.DemoAppTest
FAILED: testGetGreeting
Tests run: 1, Failures: 1, Errors: 0, Skipped: 0
✗ Running tests failed in 2s
[Test summary:]
Tests passed: 0, Tests failed: 1, Total: 1

Failed tests:
  • com.rinna.DemoAppTest

Completed with some failures
```

### Verbose Mode

The verbose mode shows all output from the underlying build tools, useful for debugging:

```
[Running tests...]
Running tests...
Running com.rinna.DemoAppTest
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.051 s
Test completed
✓ Running tests completed successfully in 0s
[Test summary:]
Tests passed: 1, Tests failed: 0, Total: 1
[Total execution time: 0s]
All operations completed successfully!
```

### Errors-Only Mode

The errors-only mode shows only errors and the steps that lead to them:

```
[Running tests...]
Error: Test failure detected in DemoAppTest
  org.opentest4j.AssertionFailedError: expected: <Hello, Rinna!> but was: <Goodbye, Rinna!>
  at com.rinna.DemoAppTest.testGetGreeting(DemoAppTest.java:24)
✗ Running tests failed in 2s
[Test summary:]
Tests passed: 0, Tests failed: 1, Total: 1

Failed tests:
  • com.rinna.DemoAppTest

Tests completed with failures
```

## Examples

### Build with minimal output

```bash
rin build
```

### Run tests with full output

```bash
rin -v test
```

### Clean, build, and test the project with errors-only output

```bash
rin -e all
```

### Run tests with terse output for the core module

```bash
cd rinna-core && ../bin/rin test
```

## Benefits

- **Consistent Interface**: Same command structure regardless of the underlying build system.
- **Execution Tracking**: Shows how long each phase takes, helping identify bottlenecks.
- **Color-Coded Output**: Success, warnings, and errors are color-coded for easy identification.
- **Test Summary**: Provides a clean summary of test results with pass/fail counts.
- **Adaptive Verbosity**: Control how much information is shown based on your needs.
- **Failed Test Details**: Clearly identifies which tests failed and provides error details.
- **Time Tracking**: Shows execution time for each phase and the total run time.

## Implementation Notes

The `rin` tool uses Maven as its underlying build system but encapsulates it with a more user-friendly interface. The script detects project characteristics to provide contextual help and examples.

The implementation includes robust error handling to ensure that:
1. Build errors are properly reported
2. Test failures are clearly indicated
3. The appropriate level of detail is shown based on the selected verbosity mode
4. Color-coding helps quickly identify success and failure states

## Source Code

The `rin` tool is available in multiple versions in the Rinna repository:

- `bin/rin`: Full implementation with project detection and context-aware help
- `bin/rin-simple`: Simplified implementation that's easier to understand and modify
- `bin/rin-demo`: Demonstration version for quick testing of the verbosity modes

You can view and modify the source code to suit your specific needs.
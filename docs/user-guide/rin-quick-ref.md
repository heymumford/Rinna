# Rinna CLI Quick Reference

## Commands

| Command | Description | Example |
|---------|-------------|---------|
| `build` | Build the project | `rin build` |
| `clean` | Clean artifacts | `rin clean` |
| `test` | Run tests | `rin test` |
| `all` | Clean, build, test | `rin all` |

## Output Modes

| Mode | Flag | Use Case | Example |
|------|------|----------|---------|
| Terse | `-t, --terse` | Default mode - minimal output | `rin -t test` |
| Verbose | `-v, --verbose` | Debug, see all details | `rin -v build` |
| Errors Only | `-e, --errors` | CI/CD pipelines, logs | `rin -e all` |

## Key Features

- **Color Coding**: 
  - <span style="color:green">Green</span> for success
  - <span style="color:red">Red</span> for failures
  - <span style="color:blue">Blue</span> for status/info

- **Execution Times**: 
  - Per-task timing
  - Total execution time

- **Test Summary**:
  - Tests passed/failed count
  - Failed test details
  - Stack traces for errors

## Examples

### Clean Build

```
rin clean build
```

### Verbose Testing

```
rin -v test
```

### Errors-Only for CI Pipeline

```
rin -e all
```

### Test a Specific Module

```
cd rinna-core && ../bin/rin test
```

## Exit Codes

- **0**: All operations completed successfully
- **Non-zero**: One or more operations failed
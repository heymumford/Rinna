<!-- Copyright (c) 2025 [Eric C. Mumford](https://github.com/heymumford) [@heymumford] -->

# Rinna CLI Quick Reference

## Commands

| Command | Description | Example |
|---------|-------------|---------|
| `build` | Build the project | `rin build` |
| `clean` | Clean artifacts | `rin clean` |
| `test` | Run tests | `rin test` |
| `all` | Clean, build, test | `rin all` |
| `version` | Version management | See below |

## Version Commands

| Command | Description | Example |
|---------|-------------|---------|
| `current` | Show version info | `rin version current` |
| `major` | Bump major version | `rin version major` |
| `minor` | Bump minor version | `rin version minor` |
| `patch` | Bump patch version | `rin version patch` |
| `set <ver>` | Set specific version | `rin version set 1.2.3` |
| `tag` | Create git tag | `rin version tag` |
| `release` | Create GitHub release | `rin version release` |

## Test Commands

| Command | Description | Example |
|---------|-------------|---------|
| `test unit` | Run unit tests | `rin test unit` |
| `test component` | Run component tests | `rin test component` |
| `test integration` | Run integration tests | `rin test integration` |
| `test acceptance` | Run acceptance tests | `rin test acceptance` |
| `test performance` | Run performance tests | `rin test performance` |
| `test-pyramid` | Run test pyramid analysis | `make test-pyramid` |

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

### Generate Test Pyramid Report

```
./bin/test-pyramid-coverage.sh
```

## Utility Scripts

| Script | Purpose | Example |
|--------|---------|---------|
| `bin/rin` | Main CLI tool | `bin/rin test` |
| `bin/rin-version` | Version management | `bin/rin-version minor` |
| `bin/run-checks.sh` | Architecture validation | `./bin/run-checks.sh` |
| `bin/test-pyramid-coverage.sh` | Test pyramid analysis | `./bin/test-pyramid-coverage.sh` |

## Architecture Validation

| Command | Description | Example |
|---------|-------------|---------|
| `run-checks.sh` | All validation checks | `./bin/run-checks.sh` |
| `dependency-validator.sh` | Check dependencies | `./bin/checks/dependency-validator.sh` |
| `test-structure-validator.sh` | Check test structure | `./bin/checks/test-structure-validator.sh` |
| `check-clean-architecture.sh` | Verify clean architecture | `./bin/checks/check-clean-architecture.sh` |

## Exit Codes

- **0**: All operations completed successfully
- **Non-zero**: One or more operations failed

## Package Structure

- **Base Package**: `org.rinna`
- **Core Package**: `org.rinna.core`
- **Domain Package**: `org.rinna.domain`
- **Adapter Package**: `org.rinna.adapter`
# Python Container Testing

This guide explains how to run Python tests in containers using Podman or Docker.

## Overview

The Rinna project uses a containerized approach for Python testing, which provides consistent environments across development and CI/CD pipelines. Our container setup follows these principles:

- **Test Pyramid Structure**: Tests are organized according to the standard test pyramid (unit, component, integration, acceptance, performance)
- **Security**: Containers run as non-root users with proper permissions
- **Flexibility**: Supports both Podman (preferred) and Docker
- **Performance**: Optimized for fast builds and test runs with proper caching

## Prerequisites

- Podman (preferred) or Docker installed
- Python 3.8+ for local development
- Git client

## Quick Start

Run all Python tests in a container:

```bash
./bin/run-python-container.sh all
```

Run only unit tests:

```bash
./bin/run-python-container.sh unit
```

Force rebuild of the container:

```bash
./bin/run-python-container.sh --rebuild unit
```

Start a development shell:

```bash
./bin/run-python-container.sh dev
```

## Test Pyramid Structure

Our tests follow the standard test pyramid structure:

1. **Unit Tests**: Test individual functions and classes in isolation
   ```bash
   ./bin/run-python-container.sh unit
   ```

2. **Component Tests**: Test interactions between related components
   ```bash
   ./bin/run-python-container.sh component
   ```

3. **Integration Tests**: Test integration with other system components
   ```bash
   ./bin/run-python-container.sh integration
   ```

4. **Acceptance Tests**: Test end-to-end user scenarios
   ```bash
   ./bin/run-python-container.sh acceptance
   ```

5. **Performance Tests**: Test system performance
   ```bash
   ./bin/run-python-container.sh performance
   ```

## Container Configuration

### Directory Structure

The containerized test setup follows this structure:

```
/app/
├── bin/                  # Scripts directory
│   └── test-python-pyramid.sh  # Test runner script
├── python/               # Python package
│   ├── rinna/            # Main package
│   │   └── api/          # API components
│   ├── tests/            # Test directory
│   │   ├── unit/         # Unit tests
│   │   ├── component/    # Component tests
│   │   ├── integration/  # Integration tests
│   │   ├── acceptance/   # Acceptance tests
│   │   └── performance/  # Performance tests
│   └── setup.py          # Package setup file
├── test-output/          # Test output reports
└── coverage/             # Coverage reports
```

### Environment Variables

You can configure the container behavior using these environment variables:

- `USER_ID`: User ID to use inside the container (default: current user's ID)
- `GROUP_ID`: Group ID to use inside the container (default: current user's group ID)
- `LOG_LEVEL`: Logging level (default: info)
- `TEST_LEVEL`: Test level to run (default: all)
- `TESTOPTS`: Additional options to pass to the test command

Example:
```bash
LOG_LEVEL=debug TEST_LEVEL="--verbose unit" ./bin/run-python-container.sh
```

### Multiple Language Testing

For integration testing with Java and Go components, use the project-level Podman Compose file:

```bash
# Build all services
podman-compose -f podman-compose.yml build

# Run Python tests with other services available
podman-compose -f podman-compose.yml --profile testing up python-tests
```

## CI/CD Integration

For CI/CD pipelines, the container provides a consistent testing environment. Example GitHub Actions workflow:

```yaml
name: Python Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up Podman
        uses: containers/podman-action@v1
        
      - name: Run Python tests
        run: |
          ./bin/run-python-container.sh --rebuild all
        
      - name: Upload test results
        uses: actions/upload-artifact@v3
        with:
          name: test-results
          path: |
            test-output/
            coverage/
```

## Troubleshooting

### Permission Issues

If you encounter permission issues with volumes:

```bash
# Fix ownership of output directories
sudo chown -R $(id -u):$(id -g) test-output/ coverage/
```

### Container Build Failures

If the container fails to build:

```bash
# Clean up old containers and images
podman container prune -f
podman image prune -f

# Rebuild with verbose output
./bin/run-python-container.sh --rebuild --verbose all
```

## Advanced Usage

### Custom Test Options

Pass custom options to the test command:

```bash
./bin/run-python-container.sh --options="--no-summary -v" unit
```

### Development Environment

Start a development environment with all dependencies installed:

```bash
./bin/run-python-container.sh dev
```

### Package Installation

Test package installation in a clean environment:

```bash
./bin/run-python-container.sh install
```
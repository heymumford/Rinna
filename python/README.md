# Rinna Python Package

This package contains the Python components of the Rinna polyglot workflow management system. It's designed to work alongside other language components while maintaining clean separation of concerns.

## Features

- Report generation for Work Items, Projects, and Releases
- API integration for cross-language communication
- Metrics and analytics processing
- Document generation in multiple formats (PDF, HTML)
- Lucidchart integration

## Installation

### Using Poetry (Recommended)

```bash
cd python
poetry install
```

To install with optional dependencies:

```bash
# Install with report generation capabilities
poetry install --extras "reports"

# Install with web API dependencies
poetry install --extras "web"

# Install with diagram generation
poetry install --extras "diagrams"

# Install with documentation tools
poetry install --extras "docs"

# Install all optional dependencies
poetry install --extras "all"
```

## Development

### Setting up the development environment

```bash
# Install development dependencies
poetry install

# Activate the virtual environment (Poetry 2.0.0+)
poetry env use python3.13
poetry env activate
# Then run the source command from the output
```

### Running tests

```bash
# Run all tests
pytest

# Run only unit tests
pytest -m unit

# Run with coverage
pytest --cov=rinna

# Generate coverage report
pytest --cov=rinna --cov-report=html
```

### Code quality

```bash
# Run linting with pylint
pylint rinna

# Format code with black
black .

# Sort imports with isort
isort .

# Type checking with mypy
mypy rinna
```

## Polyglot Architecture

This Python package is designed to be part of a larger polyglot application:

- `/python`: Contains this Python component (managed with Poetry)
- Other language components in separate directories at the project root
- Cross-language communication via defined APIs

## Project Structure

- `rinna/`: Main package
  - `api/`: API interface for cross-language communication
  - `reports/`: Report generation utilities
    - `renderer.py`: Base classes for report rendering
    - `service.py`: Report service with templating
    - `*_renderer.py`: Specific rendering engines
- `lucidchart_py/`: Lucidchart integration modules
- `tests/`: Test suite organized by test type
  - `unit/`: Unit tests
  - `component/`: Component tests
  - `integration/`: Integration tests
  - `acceptance/`: Acceptance tests
  - `performance/`: Performance tests

## Python Requirements

- Python 3.13.3 or higher
- Poetry 2.0.0+ for dependency management

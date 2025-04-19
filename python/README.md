# Rinna Python Package

This package contains the Python components of the Rinna workflow management system.

## Features

- Report generation for Work Items, Projects, and Releases
- API integration for cross-language communication
- Metrics and analytics processing
- Document generation in multiple formats (PDF, HTML, DOCX)

## Installation

### Using Poetry (Recommended)

```bash
cd python
poetry install
```

To install with optional dependencies:

```bash
# Install with report generation capabilities
poetry install -E reports

# Install with web API dependencies
poetry install -E web

# Install with diagram generation
poetry install -E diagrams

# Install with documentation tools
poetry install -E docs

# Install all optional dependencies
poetry install -E all
```

### Using pip

```bash
pip install -e .
```

## Development

### Setting up the development environment

```bash
# Install development dependencies
poetry install

# Install pre-commit hooks
poetry run pre-commit install
```

### Running tests

```bash
# Run all tests
poetry run pytest

# Run only unit tests
poetry run pytest -m unit

# Run with coverage
poetry run pytest --cov=rinna

# Generate coverage report
poetry run pytest --cov=rinna --cov-report=html
```

### Code quality

```bash
# Run linting with pylint
poetry run pylint rinna

# Format code with black
poetry run black .

# Sort imports with isort
poetry run isort .

# Type checking with mypy
poetry run mypy rinna
```

## Project Structure

- `rinna/api/`: API interface for cross-language communication
- `rinna/reports/`: Report generation utilities
  - `renderer.py`: Base classes for report rendering
  - `service.py`: Report service with templating
  - `*_renderer.py`: Specific rendering engines (WeasyPrint, ReportLab, etc.)
- `tests/`: Test suite organized by test type
  - `unit/`: Unit tests
  - `component/`: Component tests
  - `integration/`: Integration tests
  - `acceptance/`: Acceptance tests
  - `performance/`: Performance tests
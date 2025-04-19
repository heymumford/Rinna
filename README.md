# Rinna Project

A polyglot workflow management system.

## Project Structure

This project follows a polyglot architecture pattern:

```
/
├── java/                     # Java components
│   ├── rinna-core/           # Core Java library
│   ├── rinna-cli/            # Command-line interface
│   └── pom.xml               # Maven configuration for Java
│
├── python/                   # Python components
│   ├── rinna/                # Main Python package
│   ├── lucidchart_py/        # Secondary Python package
│   ├── tests/                # Python tests
│   ├── scripts/              # Python utility scripts
│   │   └── api/              # API-related scripts
│   └── pyproject.toml        # Poetry configuration
│
├── api/                      # API definition files (Swagger, OpenAPI)
│   └── specs/                # API specifications
│
├── config/                   # Configuration files
│   ├── java/                 # Java-specific configs
│   ├── python/               # Python-specific configs
│   └── shared/               # Shared configuration
│
├── docs/                     # Documentation
│
├── build/                    # Build output directory
│
└── scripts/                  # Build and utility scripts
    └── build.sh              # Main build script
```

## Building the Project

```bash
# Build all components
./scripts/build.sh

# Build specific components
./scripts/build.sh java
./scripts/build.sh python
```

## Development

### Java Components

```bash
cd java
mvn clean install
```

### Python Components

```bash
cd python
poetry install
poetry env use python3.13
poetry env activate
# Then run the source command from the output
```

# Rinna Project

A polyglot workflow management system designed for efficient cross-language communication and unified workflow management.

## Project Structure

This project follows a clean polyglot architecture pattern with clear language boundaries:

```
/
├── java/                     # Java components
│   ├── rinna-core/           # Core Java library
│   ├── rinna-cli/            # Command-line interface
│   ├── rinna-data/           # Data access components
│   ├── src/                  # Shared Java source files
│   └── pom.xml               # Maven configuration for Java
│
├── python/                   # Python components
│   ├── rinna/                # Main Python package
│   ├── lucidchart_py/        # Lucidchart integration module
│   ├── tests/                # Python tests
│   ├── scripts/              # Python utility scripts
│   │   ├── api/              # API-related scripts
│   │   ├── bin/              # Command-line scripts
│   │   └── utils/            # Utility functions
│   ├── pyproject.toml        # Poetry configuration
│   └── poetry.lock           # Poetry lock file
│
├── go/                       # Go components
│   ├── src/                  # Go source code
│   ├── pkg/                  # Shared Go packages
│   ├── bin/                  # Go binaries output
│   ├── go.mod                # Go module definition
│   └── go.sum                # Go module checksums
│
├── api-specs/                # API definition files
│   ├── swagger/              # Swagger specifications
│   └── openapi/              # OpenAPI specifications
│
├── config/                   # Configuration files
│   ├── java/                 # Java-specific configs
│   ├── python/               # Python-specific configs
│   ├── go/                   # Go-specific configs
│   └── shared/               # Shared configuration
│
├── docs/                     # Documentation
│
├── scripts/                  # Build and utility scripts
│   ├── build/                # Build scripts
│   ├── deploy/               # Deployment scripts
│   └── utils/                # Utility scripts
│
└── logs/                     # Build and execution logs
```

## Building the Project

The main build script supports building all components or specific language components:

```bash
# Build all components
./build.sh all [dev|test|prod]

# Build specific components
./build.sh java [dev|test|prod]
./build.sh python [dev|test|prod]
./build.sh go [dev|test|prod]
```

Build modes:
- `dev` (default): Development build
- `test`: Development build with tests
- `prod`: Production build with optimizations

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

### Go Components

```bash
cd go
go mod download
go build ./...
```

## Cross-Language Communication

The components communicate through well-defined APIs:

- REST APIs with OpenAPI/Swagger specifications
- Cross-language logging mechanism
- Shared configuration files

## Documentation

Comprehensive documentation is available in the `docs` directory, covering:

- Architecture decisions
- API specifications
- User guides
- Development guides

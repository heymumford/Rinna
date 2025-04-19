# Rinna Project

A polyglot workflow management system designed for efficient cross-language communication and unified workflow management, with a focus on clean architecture principles and high-performance execution.

## Project Overview

Rinna integrates Java, Python, and Go components in a modular architecture that maintains clear language boundaries while providing seamless cross-component communication. The system leverages each language's strengths:

- **Java**: Core domain models and business logic implementation
- **Python**: Data processing, reporting, and visualization 
- **Go**: High-performance API services and system interfaces

Each component is isolated in its own directory with appropriate build tools and dependencies, while sharing common interfaces through API specifications.

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
├── build/                    # Build output directory
│   └── artifacts/            # Build artifacts
│
├── .idea/                    # IntelliJ IDEA configuration
│
├── .run/                     # Run configurations for IntelliJ
│
└── logs/                     # Build and execution logs
```

## Building the Project

The main build script supports building all components or specific language components with parallel processing capabilities:

```bash
# Build all components
./build.sh all [dev|test|prod]

# Build specific components
./build.sh java [dev|test|prod]
./build.sh python [dev|test|prod]
./build.sh go [dev|test|prod]
./build.sh api-specs [dev|test|prod]

# Get help on build options
./build.sh help
```

### Build Modes

- `dev` (default): Development build with minimal validation
- `test`: Development build with comprehensive tests
- `prod`: Production build with optimizations and full validation

### Build Artifacts

Each build creates archives of outputs and logs in `build/artifacts/` for later analysis. A summary report is generated in `logs/build-summary-latest.log`.

## Development

### Java Components

```bash
cd java
mvn clean install

# Run Java tests
mvn test

# Format Java code
mvn spotless:apply

# Run linting
mvn checkstyle:check pmd:check spotbugs:check
```

### Python Components

```bash
cd python
poetry install
poetry env use python3.13
poetry env activate
# Then run the source command from the output

# Run Python tests
poetry run pytest

# Format Python code
poetry run black .
poetry run isort .

# Run linting
poetry run pylint rinna
```

### Go Components

```bash
cd go
go mod download
go build ./...

# Run Go tests
go test ./...

# Format Go code
go fmt ./...

# Run linting
golint ./...
go vet ./...
```

## IDE Integration

The project includes IntelliJ IDEA configuration for a unified development experience across all language components:

```bash
# Set up or update IntelliJ IDEA configuration
./setup-intellij.sh
```

This configures:
- Maven projects for Java components
- Poetry environment for Python
- Go modules integration
- Run configurations for the build script

## Parallel Processing

The build system leverages parallel processing for improved performance:

- Java: Maven builds use multi-threading
- Go: Compilation uses multiple CPU cores
- Python: Tests can run in parallel with pytest-xdist
- Build script: Components can be built in parallel

## Cross-Language Communication

The components communicate through well-defined APIs:

- REST APIs with OpenAPI/Swagger specifications
- Cross-language logging mechanism
- Shared configuration files
- Consistent error handling patterns

## Documentation

Comprehensive documentation is available in the `docs` directory, covering:

- Architecture decisions
- API specifications
- User guides
- Development guides

## Contributing

Please see [CONTRIBUTING.md](./docs/CONTRIBUTING.md) for information on how to contribute to the project.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

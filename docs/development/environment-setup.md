# Development Environment Setup

This guide provides detailed instructions for setting up a development environment for contributing to Rinna.

## Prerequisites

Rinna is a polyglot project that requires the following tools:

- **Java 21+** - Core domain logic
- **Go 1.21+** - API and services
- **Python 3.8+** - Utilities and scripts
- **Maven 3.8+** - Build system
- **Git** - Version control
- **jq** - CLI utilities
- **bash** - Shell scripts

## Automated Setup

Rinna provides a unified setup script that configures all required components:

```bash
# Set up a full development environment with all components
bin/rin-setup-unified --developer

# Set up only specific components
bin/rin-setup-unified --developer java go python
```

The `--developer` flag installs additional tools needed for development, such as linters, code formatters, and testing tools.

## Manual Setup

If you prefer to set up components manually, follow these steps:

### 1. Java Setup

```bash
# Install Java 21 (Ubuntu example)
sudo apt-get update
sudo apt-get install openjdk-21-jdk

# Verify installation
java -version
```

### 2. Go Setup

```bash
# Install Go 1.21+ (adjust version as needed)
wget https://golang.org/dl/go1.21.0.linux-amd64.tar.gz
sudo tar -C /usr/local -xzf go1.21.0.linux-amd64.tar.gz
export PATH=$PATH:/usr/local/go/bin

# Verify installation
go version
```

### 3. Python Setup

```bash
# Install Python 3.8+ (Ubuntu example)
sudo apt-get update
sudo apt-get install python3.8 python3.8-venv python3-pip

# Create and activate virtual environment
python3 -m venv .venv
source .venv/bin/activate

# Install dependencies
pip install -r requirements.txt
pip install -r requirements-dev.txt  # Development dependencies
```

### 4. Maven Setup

```bash
# Install Maven (Ubuntu example)
sudo apt-get update
sudo apt-get install maven

# Verify installation
mvn -version
```

### 5. Additional Tools

```bash
# Install jq (Ubuntu example)
sudo apt-get install jq

# Install linters and formatters
pip install black isort flake8 mypy
```

## Post-Setup Verification

After setting up your environment, verify the installation:

```bash
# Verify Java build
bin/rin build java

# Verify Go build
bin/rin build go

# Verify Python tools
bin/rin build python

# Run tests to confirm everything is working
bin/rin test fast
```

## Environment Activation

Rinna provides activation scripts to set up the environment variables needed for development:

```bash
# Activate all environments
source activate-rinna.sh

# Activate specific environments
source activate-java.sh
source activate-go.sh
source activate-python.sh
source activate-api.sh
```

## IDE Setup

### IntelliJ IDEA

1. Import the project as a Maven project
2. Configure Java 21 as the project SDK
3. Enable annotation processing
4. Install the Go plugin if working on Go code

For detailed instructions on setting up IntelliJ IDEA on a Mac Studio, see the [IntelliJ Mac Setup Guide](intellij-mac-setup.md).

### Visual Studio Code

1. Install the following extensions:
   - Java Extension Pack
   - Go
   - Python
   - Maven for Java
   - Checkstyle for Java

2. Configure settings:
   ```json
   {
     "java.configuration.updateBuildConfiguration": "automatic",
     "java.checkstyle.configuration": "${workspaceFolder}/config/checkstyle/checkstyle.xml",
     "python.linting.enabled": true,
     "python.linting.flake8Enabled": true,
     "go.lintTool": "golangci-lint"
   }
   ```

## Troubleshooting

### Common Issues

#### Java Module Issues

If you encounter module resolution issues:

```bash
# Clean and rebuild the project
mvn clean install -DskipTests
```

#### Go Dependency Issues

If Go dependencies are not resolving:

```bash
# Update Go dependencies
cd api
go mod tidy
```

#### Python Virtual Environment Issues

If the Python virtual environment isn't working:

```bash
# Recreate the virtual environment
rm -rf .venv
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

### Getting Help

If you encounter issues not covered here:

1. Check the [GitHub Issues](https://github.com/heymumford/Rinna/issues) for similar problems
2. Ask in the development channel on our Discord server
3. Create a new issue with details about your environment and the problem

## Next Steps

After setting up your environment:

1. Read the [Architecture Guide](architecture.md)
2. Explore the [Testing Strategy](../testing/TESTING_STRATEGY.md)
3. Review [Clean Architecture Principles](../architecture/decisions/0003-adopt-clean-architecture-for-system-design.md)
4. Review the [Linting and Build Procedure](../../build/README.md) for all components
5. Follow the [Contribution Guidelines](../../CONTRIBUTING.md)

# Getting Started with Rinna Development

This guide will help you set up your development environment and make your first contribution to Rinna.

## Prerequisites

Rinna is a polyglot project that requires:

- **Java 21+** - Core domain logic
- **Go 1.21+** - API and services
- **Python 3.8+** - Utilities and scripts
- **Maven 3.8+** - Build system
- **Git** - Version control
- **jq** - CLI utilities
- **bash/zsh** - Shell scripts

## Setting Up Your Environment

### Automated Setup

Rinna provides a unified setup script that configures all required components:

```bash
# Set up a full development environment with all components
bin/rin-setup-unified --developer

# Set up only specific components
bin/rin-setup-unified --developer java go python
```

The `--developer` flag installs additional tools needed for development, such as linters, code formatters, and testing tools.

### Manual Setup

If you prefer to set up components manually:

#### 1. Java Setup

```bash
# Install Java 21 (Ubuntu example)
sudo apt-get update
sudo apt-get install openjdk-21-jdk

# Verify installation
java -version
```

#### 2. Go Setup

```bash
# Install Go 1.21+ (adjust version as needed)
wget https://golang.org/dl/go1.21.0.linux-amd64.tar.gz
sudo tar -C /usr/local -xzf go1.21.0.linux-amd64.tar.gz
export PATH=$PATH:/usr/local/go/bin

# Verify installation
go version
```

#### 3. Python Setup

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

#### 4. Maven Setup

```bash
# Install Maven (Ubuntu example)
sudo apt-get update
sudo apt-get install maven

# Verify installation
mvn -version
```

## Clone the Repository

```bash
# Clone the repository
git clone https://github.com/heymumford/rinna.git
cd rinna

# Make scripts executable
chmod +x build.sh bin/*
```

## Initial Build

First, build the project using the root build script:

```bash
# Build all components
./build.sh all dev

# If you want to run tests during the build
./build.sh all test
```

This script will build all components of Rinna, including the `rin` CLI tool that you can use for subsequent development operations.

## Verify Your Setup

After building, verify the installation:

```bash
# Check if the build was successful
./build.sh all test

# Or if you've already built the project and the rin CLI is available:
bin/rin test fast
```

## Development Workflow

A typical development workflow looks like:

1. **Create a Feature Branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. **Build and Test**
   ```bash
   # For full builds or when the rin CLI is unavailable:
   ./build.sh all dev
   
   # For incremental builds when rin CLI is available:
   bin/rin build fast  # Quick build without tests
   bin/rin test unit   # Run unit tests for quick feedback
   ```

3. **Run Full Verification**
   ```bash
   # Most reliable approach:
   ./build.sh all test
   
   # Or with rin CLI:
   bin/rin build verify  # Full build with tests and quality checks
   ```

4. **Submit a Pull Request**
   - Push your branch to GitHub
   - Create a pull request with a clear description
   - Address review comments

## Troubleshooting

If you encounter issues with the `rin` CLI (which might happen if you're making changes to the CLI itself), always fall back to using the root `build.sh` script:

```bash
# Clean and rebuild everything
./build.sh clean
./build.sh all dev
```

## Next Steps

Now that you have your environment set up, explore these guides to learn more:

- [Architecture](architecture.md) - Understand the system design
- [Build System](build-system.md) - Learn the build tools and commands
- [Testing](testing.md) - Discover the testing approach and tools
- [Contributing](contributing.md) - Guidelines for making contributions

For questions or issues, check the troubleshooting section or contact the maintainers.

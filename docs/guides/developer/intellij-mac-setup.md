# IntelliJ IDEA Setup for Mac Studio

This guide provides detailed instructions for setting up IntelliJ IDEA on a Mac Studio for developing and building the Rinna polyglot project.

## Prerequisites

Before setting up IntelliJ IDEA, ensure you have the following installed:

- **IntelliJ IDEA** (Ultimate edition recommended for full polyglot support)
- **Java 21+** - Required for core development
- **Go 1.21+** - Required for API development
- **Python 3.8+** - Required for utilities and scripts
- **Maven 3.8+** - Required for Java build system
- **golangci-lint** - Required for Go linting
- **Python linting tools** - mypy, black, isort, ruff, flake8

## Installation

### Installing IntelliJ IDEA

1. Download IntelliJ IDEA Ultimate from the [JetBrains website](https://www.jetbrains.com/idea/download/#section=mac)
2. Install IntelliJ IDEA on your Mac Studio
3. Launch IntelliJ IDEA

### Installing Required Plugins

1. Open IntelliJ IDEA
2. Go to **IntelliJ IDEA → Preferences → Plugins**
3. Install the following plugins:
   - **Go** - For Go language support
   - **Python** - For Python language support
   - **CheckStyle-IDEA** - For Java code style checking
   - **Maven Helper** - For enhanced Maven support
   - **SonarLint** - For additional code quality checks (optional)

## Project Setup

### Importing the Project

1. Clone the Rinna repository:
   ```bash
   git clone https://github.com/heymumford/Rinna.git
   cd Rinna
   ```

2. Open IntelliJ IDEA
3. Select **File → Open**
4. Navigate to the Rinna directory and click **Open**
5. When prompted, select **Open as Project**
6. Wait for IntelliJ IDEA to index the project

### Configuring Java

1. Go to **IntelliJ IDEA → Preferences → Build, Execution, Deployment → Build Tools → Maven**
2. Set **JDK for Importer** to Java 21
3. Go to **IntelliJ IDEA → Preferences → Build, Execution, Deployment → Compiler → Java Compiler**
4. Set **Project bytecode version** to 21

### Configuring Go

1. Go to **IntelliJ IDEA → Preferences → Languages & Frameworks → Go**
2. Set **GOROOT** to your Go installation directory
3. Set **GOPATH** to your Go workspace
4. Enable **Go modules integration**
5. Under **Go Tools**, ensure **golangci-lint** is configured

### Configuring Python

1. Go to **IntelliJ IDEA → Preferences → Project → Python Interpreter**
2. Add a new Python interpreter or select an existing one (Python 3.8+)
3. Go to **IntelliJ IDEA → Preferences → Tools → Python Integrated Tools**
4. Set **Testing** to **pytest**
5. Configure linters:
   - Go to **IntelliJ IDEA → Preferences → Editor → Inspections → Python**
   - Enable **PEP 8 coding style violation**
   - Enable **Type checker (mypy)**
   - Enable **Ruff**

## Linting and Build Configuration

The project includes a pre-configured IntelliJ setup file that sets up all necessary linting and build configurations. To use it:

1. Make sure the `.idea` directory exists in your project root (it should be created automatically when you open the project)
2. The `rinna-intellij-setup.xml` file should be automatically loaded by IntelliJ

If you need to manually configure the linting and build settings:

### Java Linting (Checkstyle)

1. Go to **IntelliJ IDEA → Preferences → Tools → Checkstyle**
2. Add a new configuration file:
   - Description: **Rinna Checkstyle**
   - File: **config/checkstyle/checkstyle.xml**
3. Set the new configuration as active
4. Enable **Scan before checkin**

### Go Linting (golangci-lint)

1. Go to **IntelliJ IDEA → Preferences → Languages & Frameworks → Go → Linters**
2. Enable **golangci-lint**
3. Set the path to the golangci-lint executable
4. Set arguments to **run**

The project includes a comprehensive `.golangci.yml` configuration file in the `api` directory that configures all necessary linters and their settings. This file is automatically used by golangci-lint when run from the `api` directory.

### Python Linting

1. Go to **IntelliJ IDEA → Preferences → Editor → Inspections → Python**
2. Enable all relevant linters (mypy, flake8, black, isort, ruff)
3. Configure each linter according to the project's settings in `pyproject.toml`

## Build Configurations

The following build configurations are available:

### Java

- **Build Java**: Cleans, compiles, and packages the Java components
- **Test Java**: Runs Java tests

### Go

- **Build Go**: Builds the Go API server
- **Test Go**: Runs Go tests

### Python

- **Build Python**: Installs the Python package in development mode
- **Test Python**: Runs Python tests

### Combined

- **Build All**: Builds all components (Java, Go, Python)
- **Test All**: Runs all tests (Java, Go, Python)

## Troubleshooting

### Java Module Issues

If you encounter module resolution issues:

1. Go to **File → Invalidate Caches / Restart**
2. Select **Invalidate and Restart**
3. After restart, run **Maven → Reimport**

### Go Dependency Issues

If Go dependencies are not resolving:

1. Open a terminal in IntelliJ
2. Navigate to the API directory: `cd api`
3. Run: `go mod tidy`

### Python Virtual Environment Issues

If the Python virtual environment isn't working:

1. Go to **IntelliJ IDEA → Preferences → Project → Python Interpreter**
2. Click the gear icon and select **Add**
3. Create a new virtual environment or select an existing one
4. Install requirements: `pip install -r requirements.txt`

## Mac Studio Specific Optimizations

To optimize IntelliJ IDEA performance on Mac Studio:

1. Adjust memory settings in **IntelliJ IDEA → Preferences → Appearance & Behavior → System Settings → Memory Settings**
   - Set **IDE max heap size** to at least 4096 MB
   - For M1/M2 Mac Studio, consider setting it to 8192 MB

2. Enable parallel builds:
   - Go to **IntelliJ IDEA → Preferences → Build, Execution, Deployment → Build Tools → Maven → Runner**
   - Check **Delegate IDE build/run actions to Maven**
   - Set **Parallel build** to **true**

3. Optimize indexing:
   - Go to **IntelliJ IDEA → Preferences → Editor → File Types**
   - Add common build output directories to **Ignored Files and Folders**:
     - `target/`
     - `dist/`
     - `*.pyc`
     - `.venv/`

4. Use the Apple Silicon optimized version of IntelliJ IDEA for best performance on M1/M2 Mac Studio

## Additional Resources

- [IntelliJ IDEA Documentation](https://www.jetbrains.com/help/idea/installation-guide.html)
- [Maven Integration](https://www.jetbrains.com/help/idea/maven-support.html)
- [Go Development](https://www.jetbrains.com/help/idea/go-plugin.html)
- [Python Development](https://www.jetbrains.com/help/idea/python.html)

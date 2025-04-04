# Getting Started with Rinna

## Installation

### Maven Dependency

```xml
<dependency>
    <groupId>org.rinna</groupId>
    <artifactId>rinna-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

### CLI Installation

```bash
# Clone the repository
git clone https://github.com/heymumford/rinna.git

# Build the project
cd rinna
./mvnw clean install

# Make scripts executable
chmod +x bin/rin bin/rin-version
```

## Quick Start

### Development Commands

```bash
# Clean, build, and test the project
bin/rin all

# Run tests with verbose output
bin/rin -v test

# Build with errors-only output
bin/rin -e build
```

### Workflow Management

```bash
# Create a feature
rin workflow create feature "Add user authentication"

# List all items
rin workflow list

# Update item status
rin workflow update ITEM-1 --status "In Progress"
```

See the [User Guide](../user-guide/README.md) for more commands and options.
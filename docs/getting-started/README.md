# Getting Started with Rinna

## Installation

### Maven Dependency

```xml
<dependency>
    <groupId>org.samstraumr</groupId>
    <artifactId>rinna-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

### CLI Installation

```bash
# Clone the repository
git clone https://github.com/samstraumr/rinna.git

# Build the project
cd rinna
mvn clean install

# Run CLI
java -jar target/rinna-cli.jar
```

## Quick Start

```bash
# Initialize Rinna in your project
rinna init

# Create a feature
rinna create feature "Add user authentication"

# List all items
rinna list

# Update item status
rinna update ITEM-1 --status "In Progress"
```

See the [User Guide](../user-guide/README.md) for more commands and options.
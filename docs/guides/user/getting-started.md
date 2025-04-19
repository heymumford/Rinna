# Getting Started with Rinna

This quick start guide will help you set up and begin using Rinna for your project.

## Installation

### Maven Integration

To add Rinna to your Java project, update your project's `pom.xml` file:

```xml
<dependency>
    <groupId>org.rinna</groupId>
    <artifactId>rinna-core</artifactId>
    <version>1.8.0</version>
</dependency>
```

Additionally, add the Rinna CLI plugin to the build section:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.rinna</groupId>
            <artifactId>rinna-maven-plugin</artifactId>
            <version>1.8.0</version>
            <executions>
                <execution>
                    <goals>
                        <goal>initialize</goal>
                    </goals>
                </execution>
            </executions>
            <configuration>
                <adminInit>true</adminInit>
                <!-- Optional: customize initial project name -->
                <projectName>${project.name}</projectName>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### First Build

Once you've updated your POM file, run:

```bash
mvn clean package
```

This will:
1. Download Rinna dependencies
2. Build your project
3. Initialize the Rinna CLI and server
4. Set up default admin credentials (username: `admin`, password: `nimda`)

## Basic Usage

### Project Setup

```bash
# Create a new project
rin project create --name "My Project" --description "Project description"

# Rename current project
rin project rename --current --name "New Project Name"

# View project summary
rin project summary
```

### Work Item Management

```bash
# Create a work item
rin add "Implement login feature" --type FEATURE --priority HIGH

# List work items
rin list

# View work item details
rin view WI-123

# Update a work item
rin update WI-123 --status "IN_PROGRESS"
```

### Developer-Focused Commands

```bash
# Show all work items assigned to you
rin my-work

# Show what you should work on next
rin next-task

# Start working on an item
rin start WI-123

# Mark an item as ready for testing
rin ready-for-test WI-123

# Complete an item
rin done WI-123
```

## Server Management

The Rinna server will automatically start when you use CLI commands. To manually manage the server:

```bash
# Check server status
rin server status

# Start server explicitly
rin server start

# Stop server
rin server stop

# Restart server
rin server restart
```

## Container Option

For quick setup without Maven integration, use the container option:

```bash
# Pull and run Rinna using the all-in-one container
docker run -d --name rinna-all-in-one \
  -p 8080:8080 -p 8081:8081 -p 5000:5000 \
  -v rinna-data:/shared \
  heymumford/rinna:latest
```

This container includes all services and dependencies, with no need to clone the repository or install additional components.

## Next Steps

Once you have Rinna set up, explore these topics:

1. [Workflow Guide](workflow-guide.md) - Understanding Rinna's workflow system
2. [CLI Reference](cli-reference.md) - Complete command reference
3. [Work Items](work-items.md) - Working with different types of work items
4. [Administration](administration.md) - More advanced configuration

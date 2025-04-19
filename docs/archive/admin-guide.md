# Rinna Admin Guide

This guide provides instructions for administrators to set up Rinna in a new Java project, configure it, and use the CLI tools to manage projects, users, work items, and workflows.

## Table of Contents
- [Installation](#installation)
  - [Maven Integration](#maven-integration)
  - [First Build](#first-build)
- [CLI Setup](#cli-setup)
  - [Default Admin Credentials](#default-admin-credentials)
  - [Server Initialization](#server-initialization)
- [Project Management](#project-management)
  - [Creating a Project](#creating-a-project)
  - [Renaming a Project](#renaming-a-project)
  - [Project Summary](#project-summary)
- [User Management](#user-management)
  - [Adding Users](#adding-users)
  - [Modifying User Permissions](#modifying-user-permissions)
  - [User Profiles](#user-profiles)
- [Work Item Management](#work-item-management)
  - [Defining Work Item Types](#defining-work-item-types)
  - [Custom Fields](#custom-fields)
- [Workflow Configuration](#workflow-configuration)
  - [Defining States](#defining-states)
  - [Setting Up Transitions](#setting-up-transitions)
  - [Workflow Visualization](#workflow-visualization)
- [Advanced Configuration](#advanced-configuration)
  - [Security Settings](#security-settings)
  - [Integration Options](#integration-options)

## Installation

### Maven Integration

To add Rinna to your Java project, update your project's `pom.xml` file with the following dependency:

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
4. Set up default admin credentials

## CLI Setup

After the initial build, Rinna CLI (`rin`) will be available in your project. You can add it to your PATH or use it directly from the `bin` directory.

### Default Admin Credentials

During installation, Rinna automatically creates an admin user with the following credentials:

- **Username**: admin
- **Password**: nimda
- **User ID**: Your local machine username
- **Machine ID**: Your computer's hostname

These credentials allow you to log in and perform administrative tasks.

### Server Initialization

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

## Project Management

### Creating a Project

To create a new project:

```bash
rin project create --name "My New Project" --description "Description of my project"
```

You can specify additional parameters:
- `--template <template-name>` - Use a predefined project template
- `--public` - Make the project visible to all users
- `--start-date "YYYY-MM-DD"` - Set project start date

### Renaming a Project

To rename an existing project:

```bash
rin project rename --id <project-id> --name "New Project Name"
```

Or for the current project:

```bash
rin project rename --current --name "New Project Name"
```

### Project Summary

To view a summary of your project including work item types and workflows:

```bash
rin project summary
```

For more detailed information:

```bash
rin project info --verbose
```

## User Management

### Adding Users

Add new users to your Rinna instance:

```bash
rin user create --name "John Doe" --email "john@example.com" --role user
```

For admin users:

```bash
rin user create --name "Jane Smith" --email "jane@example.com" --role admin
```

### Modifying User Permissions

Toggle admin rights:

```bash
# Grant admin rights
rin user role --id <user-id> --grant admin

# Revoke admin rights
rin user role --id <user-id> --revoke admin
```

### User Profiles

Users can have multiple profiles for different projects:

```bash
# Create a new profile for an existing user
rin profile create --user <user-id> --name "Project Alpha Profile" --project "ALPHA"

# List user profiles
rin user list-profiles --id <user-id>

# Switch between profiles
rin user switch --profile <profile-id>
```

## Work Item Management

### Defining Work Item Types

Define custom work item types for your project:

```bash
# Create a basic work item type
rin type create --name EPIC --description "Large feature that spans multiple releases"

# Create with custom fields
rin type create --name STORY --description "User story" --field storyPoints:number --field acceptanceCriteria:text
```

### Custom Fields

Add custom fields to existing work item types:

```bash
# Add a field to an existing type
rin type field --type BUG --add severity:enum "Severity level" --values low,medium,high,critical

# Make a field required
rin type field --type INCIDENT --add priority:enum "Priority level" --values low,medium,high --required

# Set default values
rin type default --type TASK --field priority=MEDIUM --field estimatedHours=4
```

Field types supported:
- `text` - Multi-line text
- `string` - Single line text
- `number` - Numeric value
- `enum` - Selection from predefined values
- `date` - Date picker
- `user` - User selection
- `multi` - Multi-select from options
- `url` - URL link

## Workflow Configuration

### Defining States

Create workflow states for your project:

```bash
# Add basic workflow state
rin workflow add-state --name CODE_REVIEW --description "Code is being reviewed"

# Add state with metadata
rin workflow add-state --name UAT --description "User acceptance testing" --meta phase=verification
```

### Setting Up Transitions

Define transitions between states:

```bash
# Create simple transition
rin workflow add-transition --from CODE_REVIEW --to TESTING

# Create conditional transition
rin workflow add-transition --from TESTING --to DONE --requires-role qa --requires-field testsPassed=true
```

Mark states as start or end states:

```bash
# Set a start state
rin workflow set-start-state --name BACKLOG

# Set an end state
rin workflow set-end-state --name DONE
```

### Workflow Visualization

View your workflow structure:

```bash
# List all workflow states
rin workflow states

# List all transitions
rin workflow transitions

# Visualize workflow
rin workflow diagram
```

## Advanced Configuration

### Security Settings

Configure security settings for your Rinna instance:

```bash
# Set password complexity requirements
rin config security --password-min-length 10 --require-special-chars

# Configure session timeout
rin config security --session-timeout 60
```

### Integration Options

Set up integration with other systems:

```bash
# Configure email notifications
rin config notifications --smtp-server mail.example.com --smtp-port 587

# Enable webhooks
rin config webhooks --enable --url https://example.com/webhook
```

For additional configuration options, refer to the [Configuration Reference](./configuration-reference.md).

---

By following this guide, you'll have a fully functional Rinna setup with customized project settings, user access controls, work item types, and workflows. The system will automatically start the Rinna server as needed and provide CLI tools for all administrative operations.
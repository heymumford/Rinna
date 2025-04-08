# Rinna Admin CLI Quick Start Guide

This quick start guide provides essential commands for administrators to set up and manage a Rinna project.

## Initial Setup

After adding Rinna to your project's POM file and running `mvn clean package`:

1. The CLI tool `rin` will be available in your project
2. Default admin user is created with username `admin` and password `nimda`
3. The server will start automatically when needed

## Essential Commands

### Project Setup

```bash
# Create a new project
rin project create --name "My Project" --description "Project description"

# Rename current project
rin project rename --current --name "New Project Name"

# View project summary
rin project summary
```

### User Management

```bash
# Create a regular user
rin user create --name "John Doe" --email "john@example.com"

# Create an admin user
rin user create --name "Jane Smith" --email "jane@example.com" --role admin

# Grant admin rights
rin user role --id user123 --grant admin

# List all users
rin user list
```

### Work Item Type Configuration

```bash
# Create a basic work item type
rin type create --name EPIC --description "Large feature"

# Create work item type with fields
rin type create --name STORY --description "User story" --field storyPoints:number --field acceptanceCriteria:text

# Add field to existing type
rin type field --type BUG --add severity:enum "Severity level" --values low,medium,high,critical
```

### Workflow Configuration

```bash
# List current workflow states
rin workflow states

# Add a new workflow state
rin workflow add-state --name CODE_REVIEW --description "Code is being reviewed"

# Create a transition between states
rin workflow add-transition --from CODE_REVIEW --to TESTING

# Set start and end states
rin workflow set-start-state --name BACKLOG
rin workflow set-end-state --name DONE
```

### Server Management

```bash
# Check server status
rin server status

# Manually start/stop server
rin server start
rin server stop
```

## Example Setup Script

Here's a complete example to set up a new project with custom types and workflow:

```bash
# Create project
rin project create --name "Software Development" --description "Main development project"

# Add custom work item types
rin type create --name EPIC --description "Large feature spanning multiple releases"
rin type create --name STORY --description "User story" --field storyPoints:number --field acceptanceCriteria:text
rin type create --name TASK --description "Development task" --field estimatedHours:number
rin type create --name BUG --description "Software defect" --field severity:enum --values low,medium,high,critical

# Configure workflow states
rin workflow add-state --name BACKLOG --description "Items ready for planning"
rin workflow add-state --name PLANNING --description "Items being planned" 
rin workflow add-state --name READY --description "Ready for development"
rin workflow add-state --name IN_PROGRESS --description "Currently in development"
rin workflow add-state --name CODE_REVIEW --description "Code is being reviewed"
rin workflow add-state --name TESTING --description "Being tested"
rin workflow add-state --name DONE --description "Completed items"

# Set start and end states
rin workflow set-start-state --name BACKLOG
rin workflow set-end-state --name DONE

# Configure transitions
rin workflow add-transition --from BACKLOG --to PLANNING
rin workflow add-transition --from PLANNING --to READY
rin workflow add-transition --from READY --to IN_PROGRESS
rin workflow add-transition --from IN_PROGRESS --to CODE_REVIEW
rin workflow add-transition --from CODE_REVIEW --to TESTING
rin workflow add-transition --from TESTING --to DONE
rin workflow add-transition --from TESTING --to IN_PROGRESS

# Add team members
rin user create --name "Project Manager" --email "pm@example.com" --role admin
rin user create --name "Developer 1" --email "dev1@example.com" --meta team=backend
rin user create --name "Developer 2" --email "dev2@example.com" --meta team=frontend
rin user create --name "QA Engineer" --email "qa@example.com" --meta team=quality

# Show summary
rin project summary
```

After running these commands, you'll have a fully configured project with custom work item types, workflow states, and users ready for team collaboration.

For more detailed information, refer to the [full Admin Guide](./admin-guide.md).
# Rinna Administration Guide

This guide provides instructions for administering and configuring Rinna for your project.

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

### Default Credentials

During installation, Rinna automatically creates an admin user with the following credentials:

- **Username**: admin
- **Password**: nimda
- **User ID**: Your local machine username
- **Machine ID**: Your computer's hostname

## Project Management

### Creating a Project

```bash
# Create a new project
rin project create --name "My New Project" --description "Description of my project"

# Create with additional parameters
rin project create --name "Software Development" --description "Main development project" --template agile
```

### Renaming a Project

```bash
# Rename an existing project by ID
rin project rename --id <project-id> --name "New Project Name"

# Rename the current project
rin project rename --current --name "New Project Name"
```

### Project Information

```bash
# View project summary
rin project summary

# Get detailed project information
rin project info --verbose
```

## User Management

### Adding Users

```bash
# Create a regular user
rin user create --name "John Doe" --email "john@example.com"

# Create an admin user
rin user create --name "Jane Smith" --email "jane@example.com" --role admin
```

### Modifying User Permissions

```bash
# Grant admin rights
rin user role --id user123 --grant admin

# Revoke admin rights
rin user role --id user123 --revoke admin
```

### User Profiles

```bash
# List all users
rin user list

# List user profiles
rin user list-profiles --id <user-id>

# Create a profile for a user
rin profile create --user <user-id> --name "Project Alpha Profile" --project "ALPHA"
```

## Work Item Type Configuration

### Creating Work Item Types

```bash
# Create a basic work item type
rin type create --name EPIC --description "Large feature"

# Create work item type with fields
rin type create --name STORY --description "User story" --field storyPoints:number --field acceptanceCriteria:text
```

### Customizing Fields

```bash
# Add field to existing type
rin type field --type BUG --add severity:enum "Severity level" --values low,medium,high,critical

# Make a field required
rin type field --type INCIDENT --add priority:enum "Priority level" --values low,medium,high --required

# Set default values
rin type default --type TASK --field priority=MEDIUM --field estimatedHours=4
```

## Workflow Configuration

### Managing Workflow States

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

### Workflow Visualization

```bash
# View the workflow structure
rin workflow diagram
```

## Server Management

### Basic Server Commands

```bash
# Check server status
rin server status

# Start server
rin server start

# Stop server
rin server stop

# Restart server
rin server restart
```

### Server Configuration

```bash
# Set server port
rin config server --port 9090

# Set server host (for binding)
rin config server --host 0.0.0.0

# Configure server memory
rin config server --memory 512m

# Set database location
rin config server --database /path/to/database
```

### Environment Configuration

```bash
# Set production mode
rin config env --mode production

# Configure external database
rin config database --type postgresql --host db.example.com --port 5432 --name rinna --user dbuser --password PASSWORD

# Enable HTTPS
rin config security --enable-https --cert-path /path/to/cert.pem --key-path /path/to/key.pem
```

## Security Settings

```bash
# Set password complexity requirements
rin config security --password-min-length 10 --require-special-chars

# Configure session timeout
rin config security --session-timeout 60

# Enable strict security mode
rin config security --strict-mode

# Configure allowed hosts
rin config security --allowed-hosts 192.168.1.0/24,10.0.0.0/8
```

## Container Deployment

For containerized deployment, Rinna provides a zero-install option:

```bash
# Pull and run Rinna with Docker
docker run -d --name rinna-all-in-one \
  -p 8080:8080 -p 8081:8081 -p 5000:5000 \
  -v rinna-data:/shared \
  heymumford/rinna:latest

# Using the universal script
./bin/rinna-container.sh --zero-install start
```

### Container Management

```bash
# Check container status
docker ps --filter "name=rinna-all-in-one"

# View logs
docker logs rinna-all-in-one

# Access shell
docker exec -it rinna-all-in-one /bin/bash
```

## Backup and Recovery

```bash
# Create a backup
rin admin backup start --type=full

# List available backups
rin admin backup list

# Restore from backup
rin admin recovery restore --from latest
```

## Monitoring and Diagnostics

```bash
# Show system health
rin server health

# View active connections
rin server connections

# Monitor performance
rin server performance

# Generate diagnostic report
rin server diagnostics
```

## Integration Options

```bash
# Configure email notifications
rin config notifications --smtp-server mail.example.com --smtp-port 587

# Enable webhooks
rin config webhooks --enable --url https://example.com/webhook

# Configure OAuth provider
rin config oauth github --client-id CLIENT_ID --client-secret CLIENT_SECRET
```

## Troubleshooting

### Common Issues

- **Server won't start**: Check for port conflicts using `netstat -tuln`
- **Authentication failures**: Verify credentials in `~/.rinna/config.conf`
- **Database errors**: Check database connection settings
- **Performance issues**: Monitor server load with `rin server performance`

### Generating Diagnostics

```bash
# Generate comprehensive diagnostic report
rin admin diagnostics report --output diagnostics.zip

# Check specific components
rin admin diagnostics check --component database
```

For more detailed configuration options, see the [Configuration Reference](configuration-reference.md).

# Rinna Service Management

This document explains the service architecture of Rinna and how services are automatically managed by the CLI.

## Architecture Overview

Rinna uses a polyglot service architecture with two main components:

1. **Java Backend Service**: Core business logic and workflow management
2. **Go API Server**: HTTP interface for external communications

The CLI provides seamless service management to ensure these components work together efficiently.

## Automatic Service Management

When you use the Rinna CLI (`rin`), services are automatically managed as follows:

1. **Detection**: The CLI detects if a command requires services to be running
2. **Auto-start**: If services aren't running, they're automatically started
3. **Health Check**: The CLI waits for services to become healthy before proceeding 
4. **Command Execution**: Once services are ready, your command is executed

This means you can focus on your workflow without manually starting servers.

## Service Management Commands

For explicit control over services, use the `server` command:

```bash
# Check service status
rin server status

# Start services
rin server start

# Stop services
rin server stop

# Restart services
rin server restart
```

### Options

| Option | Description |
|--------|-------------|
| `-f, --force` | Force action (e.g., restart even if services aren't running) |
| `-d, --detach` | Start services in detached mode |
| `--no-auto-start` | Skip auto-starting services for any command |

## Configuration

Service behavior can be customized through configuration:

### Global Configuration

The global configuration is stored in `~/.rinna/config.conf`:

```hocon
service {
  # Enable/disable auto-start (true by default)
  auto_start = true
  
  # Service startup timeout in seconds
  startup_timeout = 30
  
  # Shutdown grace period in seconds
  shutdown_grace = 5
}
```

### Project Configuration

Project-specific configuration is stored in `.rinna.yaml` in your project root:

```yaml
# Service management
service:
  auto_start: true
  startup_timeout: 30
  shutdown_grace: 5
  
  # Service paths (relative to project root)
  paths:
    start_script: "bin/start-services.sh"
    java_server: "bin/start-java-server.sh"
    go_server: "api/bin/start-go-server.sh"
```

## Service Ports

Services use the following default ports:

| Service | Default Port | Configuration |
|---------|--------------|---------------|
| Java Backend | 8081 | `.rinna.yaml` - `api.backend.java.port` |
| Go API Server | 9080 | `.rinna.yaml` - `api.port` |

## Service Lifecycle

The service lifecycle is managed as follows:

1. **Startup**:
   - Environment is loaded from `.env` and `activate-java.sh`
   - Java backend is started first
   - Go API server is started next
   - Health checks confirm both services are running correctly

2. **Runtime**:
   - PIDs are tracked in `~/.rinna/rinna-server.pid`
   - Health is periodically checked by the CLI

3. **Shutdown**:
   - Services are gracefully terminated
   - PID files are cleaned up

## Debugging Services

For troubleshooting service issues:

```bash
# Check service status with verbose output
rin -v server status

# Directly run services in foreground mode
bin/start-services.sh --foreground

# View service logs
tail -f ~/.rinna/logs/rinna-services.log
```

## Advanced Topics

### Custom Service Port

To run services on custom ports:

```bash
# Start services with custom ports
rin server start --java-port=8082 --go-port=9081

# Or use environment variables
export RINNA_JAVA_PORT=8082
export RINNA_GO_PORT=9081
rin server start
```

### Running as a System Service

For long-running installations:

```bash
# Install as a system service
rin server install-service

# Remove the system service
rin server uninstall-service
```

### Development Mode

During development, you might want more control:

```bash
# Start only the Java backend
bin/start-java-server.sh

# Start only the Go API server
api/bin/start-go-server.sh

# Use the 'no-auto-start' flag to manage services separately
rin --no-auto-start add "Fix bug in API" --type=BUG
```
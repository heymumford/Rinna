# Rinna Configuration Reference

This document provides a comprehensive reference for Rinna configuration options.

## Configuration Layers

Rinna uses a layered configuration approach:

1. **Default Configuration**: Built-in defaults in the Rinna code
2. **Global User Configuration**: `~/.rinna/config.conf` 
3. **Project Configuration**: `.rinna.yaml` in the project root
4. **Environment Variables**: Runtime configuration via env vars
5. **Command Line Arguments**: Highest priority, overrides all others

## Configuration File Formats

Rinna supports two configuration file formats:

### HOCON Format (Global Config)

The global configuration file in `~/.rinna/config.conf` uses HOCON format:

```hocon
# API Configuration
api {
  endpoint = "http://localhost:9080/api/v1"
  token = "development-token"
  
  timeout {
    connection = 5000
    read = 30000
  }
  
  backend {
    java_health_url = "http://localhost:8081/health"
  }
}

# Service Management
service {
  auto_start = true
  startup_timeout = 30
  shutdown_grace = 5
}

# Debug Settings
debug = false
```

### YAML Format (Project Config)

The project-specific configuration file `.rinna.yaml` uses YAML format:

```yaml
# Project information
project:
  name: "Rinna"
  description: "Developer-Centric Workflow Management"
  version: "1.2.5"
  org: "heymumford"
  repo: "Rinna"

# API configuration
api:
  endpoint: "http://localhost:9080/api/v1"
  timeout:
    connection: 5000
    read: 30000
  
  # Backend services
  backend:
    java:
      host: "localhost"
      port: 8081
      health_url: "http://localhost:8081/health"

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

# CLI settings
cli:
  default_type: "FEATURE"
  default_priority: "MEDIUM"
  enable_colors: true

# Environment settings
environment:
  java_version: "21"
  enable_preview: true
```

## Environment Variables

Environment variables override configuration settings with the `RINNA_` prefix:

| Environment Variable | Description | Example |
|----------------------|-------------|---------|
| `RINNA_API_ENDPOINT` | API endpoint URL | `http://localhost:9080/api/v1` |
| `RINNA_API_TOKEN` | Authentication token | `dev-token-123` |
| `RINNA_JAVA_PORT` | Java backend port | `8081` |
| `RINNA_GO_PORT` | Go API server port | `9080` |
| `RINNA_AUTO_START` | Enable/disable auto-start | `true` or `false` |
| `RINNA_DEBUG` | Enable debug mode | `true` or `false` |

## Configuration Sections

### Project Section

Information about the current project.

| Option | Description | Type | Default |
|--------|-------------|------|---------|
| `project.name` | Project name | String | "default" |
| `project.description` | Project description | String | "" |
| `project.version` | Project version | String | From version.properties |
| `project.org` | Organization/user name | String | "" |
| `project.repo` | Repository name | String | "" |

### API Section

Settings for API communication.

| Option | Description | Type | Default |
|--------|-------------|------|---------|
| `api.endpoint` | API server endpoint | String | "http://localhost:9080/api/v1" |
| `api.token` | Authentication token | String | "rinna-development-token" |
| `api.timeout.connection` | Connection timeout (ms) | Integer | 5000 |
| `api.timeout.read` | Read timeout (ms) | Integer | 30000 |
| `api.backend.java.host` | Java backend host | String | "localhost" |
| `api.backend.java.port` | Java backend port | Integer | 8081 |
| `api.backend.java_health_url` | Java health check URL | String | "http://localhost:8081/health" |

### Service Section

Service management settings.

| Option | Description | Type | Default |
|--------|-------------|------|---------|
| `service.auto_start` | Enable automatic startup | Boolean | true |
| `service.startup_timeout` | Startup timeout (seconds) | Integer | 30 |
| `service.shutdown_grace` | Graceful shutdown period (seconds) | Integer | 5 |
| `service.paths.start_script` | Path to service start script | String | "bin/start-services.sh" |
| `service.paths.java_server` | Path to Java server script | String | "bin/start-java-server.sh" |
| `service.paths.go_server` | Path to Go server script | String | "api/bin/start-go-server.sh" |

### CLI Section

CLI behavior settings.

| Option | Description | Type | Default |
|--------|-------------|------|---------|
| `cli.default_type` | Default work item type | String | "FEATURE" |
| `cli.default_priority` | Default priority | String | "MEDIUM" |
| `cli.enable_colors` | Enable colored output | Boolean | true |

### Environment Section

Environment configuration.

| Option | Description | Type | Default |
|--------|-------------|------|---------|
| `environment.java_version` | Required Java version | String | "21" |
| `environment.enable_preview` | Enable Java preview features | Boolean | true |

## Command Line Options

Command line options take precedence over all other configuration:

| Option | Description | Overrides |
|--------|-------------|-----------|
| `-c, --config <path>` | Custom config file path | All config files |
| `--no-auto-start` | Disable auto-starting services | service.auto_start |
| `-v, --verbose` | Enable verbose output | debug |
| `--java-port <port>` | Java backend port | api.backend.java.port |
| `--go-port <port>` | Go API server port | api.port |

## Examples

### Minimal Configuration

A minimal `.rinna.yaml` configuration:

```yaml
project:
  name: "MyProject"

service:
  auto_start: true
```

### Complete Configuration

See the `.rinna.yaml` file in the project root for a complete example.

## Related Documentation

- [Service Management](service-management.md)
- [CLI Documentation](rin-CLI.md)
- [Architecture Overview](../development/architecture.md)
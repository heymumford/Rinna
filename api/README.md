# Rinna API Server

This directory contains the Go API server for the Rinna workflow management system.

## Overview

The API server provides a RESTful interface for clients to interact with the Rinna system. It communicates with the Java-based core business logic to manage work items, releases, and projects, while also providing a robust health checking system.

## Architecture

The API server is structured according to Go best practices:

```
api/
├── cmd/                  # Command-line applications
│   ├── rinnasrv/         # API server entry point
│   └── healthcheck/      # Standalone health check utility
├── configs/              # Configuration files
├── docs/                 # API documentation (Swagger/OpenAPI)
├── internal/             # Private application code
│   ├── client/           # Client for Java service
│   ├── config/           # Internal configuration
│   ├── handlers/         # HTTP request handlers
│   ├── middleware/       # HTTP middleware
│   ├── models/           # Data models
│   └── server/           # Server management (auto-start)
├── pkg/                  # Public libraries
│   ├── config/           # Configuration loader
│   ├── health/           # Health check API
│   └── logger/           # Structured logging
└── test/                 # Test suites
    ├── acceptance/       # Acceptance tests
    ├── component/        # Component tests
    ├── integration/      # Integration tests
    ├── performance/      # Performance tests
    └── unit/             # Unit tests
```

## Getting Started

### Prerequisites

- Go 1.21+
- Java 21+ (for the core services)

### Unified Build System

Use the unified build script to build both API and CLI components:

```bash
# From the root directory
./build-api-cli.sh
```

### Building Individually

Build just the API server:

```bash
# From the root directory
cd api
go build -o bin/rinnasrv ./cmd/rinnasrv
go build -o bin/healthcheck ./cmd/healthcheck
```

### Running

Start the API server:

```bash
# From the root directory after building
./api/bin/rinnasrv

# With health checks only (standalone mode)
./api/bin/healthcheck
```

Server flags:
```
--port     Server port (default: 8080)
--host     Server host (default: localhost)
--config   Path to configuration file
--no-autostart  Disable automatic Java server startup
```

Health check flags:
```
--port      Server port (default: 8080)
--host      Server host (default: localhost)
--log-level Logging level (TRACE, DEBUG, INFO, WARN, ERROR)
```

## CLI Integration

The API server is designed to integrate seamlessly with the Rinna CLI:

1. **Auto-start Feature**: The API server can automatically start the Java backend service
2. **Client Library**: Java client library provides transparent communication
3. **Health Monitoring**: Both components use the same health check system
4. **Common Authentication**: Authentication tokens work across both interfaces

To enable CLI-API integration:

```bash
# Configure API for CLI access
./api/bin/rinnasrv --port 8080

# Use CLI with API backend
cd rinna-cli
./bin/rin config --api-url http://localhost:8080/api/v1
./bin/rin list
```

## API Endpoints

### Work Items

- `GET /api/v1/workitems` - List work items
- `POST /api/v1/workitems` - Create a work item
- `GET /api/v1/workitems/{id}` - Get a work item
- `PUT /api/v1/workitems/{id}` - Update a work item
- `POST /api/v1/workitems/{id}/transitions` - Transition a work item

### Releases

- `GET /api/v1/releases` - List releases
- `POST /api/v1/releases` - Create a release
- `GET /api/v1/releases/{id}` - Get a release
- `PUT /api/v1/releases/{id}` - Update a release
- `GET /api/v1/releases/{id}/workitems` - Get work items for a release

### Projects

- `GET /api/v1/projects` - List projects
- `POST /api/v1/projects` - Create a project
- `GET /api/v1/projects/{id}` - Get a project
- `PUT /api/v1/projects/{id}` - Update a project
- `GET /api/v1/projects/{id}/workitems` - Get work items for a project

### Webhooks

- `POST /api/v1/webhooks/github` - GitHub webhook handler

### Health Check

- `GET /health` - Detailed health check endpoint
- `GET /health/live` - Liveness probe (for Kubernetes)
- `GET /health/ready` - Readiness probe (for Kubernetes)

## Configuration

The API server can be configured using a YAML file or environment variables:

```yaml
server:
  port: 8080
  host: localhost
  shutdownTimeout: 15
  autoStart: true  # Auto-start Java service if needed

java:
  command: java -jar rinna-core.jar
  host: localhost
  port: 8081
  connectTimeout: 5000
  requestTimeout: 30000
  endpoints:
    health: /health
    workitems: /api/workitems
    projects: /api/projects
    releases: /api/releases

logging:
  level: info
  format: json
  file: /var/log/rinna/api.log
  console: true

auth:
  tokenSecret: your-secret-key
  tokenExpiry: 1440  # 1 day in minutes
  allowedOrigins:
    - http://localhost:3000
    - https://app.example.com
```

Environment variables can be used to override the configuration:

```bash
RINNA_SERVER_PORT=9090
RINNA_JAVA_HOST=java-service
RINNA_AUTH_TOKENSECRET=production-secret-key
RINNA_LOG_LEVEL=DEBUG
RINNA_LOG_DIR=/path/to/logs
```

## Health Checking System

The health system provides:

1. **Liveness**: Simple check to verify service is running
2. **Readiness**: Checks service dependencies (Java backend) are ready
3. **Detailed Health**: Full system diagnostics with memory usage and uptime

Health information is cached with configurable TTL to minimize impact on dependencies.

## Development and Testing

Run API tests:

```bash
cd api
go test ./pkg/health  # Test health package
go test ./...         # Run all tests
```

Run combined API and CLI integration tests:

```bash
cd api/test/integration
go test ./cli_api_integration_test.go
```

The API includes a robust test pyramid:
- Unit tests for individual components
- Component tests for API handlers and middleware
- Integration tests for API-CLI and API-Java interactions
- Performance tests for throughput validation
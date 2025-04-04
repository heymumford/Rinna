# Rinna API Server

This directory contains the Go API server for the Rinna workflow management system.

## Overview

The API server provides a RESTful interface for clients to interact with the Rinna system. It communicates with the Java-based core business logic to manage work items, releases, and projects.

## Architecture

The API server is structured according to Go best practices:

```
api/
├── cmd/                  # Command-line applications
│   └── rinnasrv/         # API server entry point
├── configs/              # Configuration files
├── internal/             # Private application code
│   ├── client/           # Client for Java service
│   ├── handlers/         # HTTP request handlers
│   ├── middleware/       # HTTP middleware
│   └── models/           # Data models
└── pkg/                  # Public libraries
    └── config/           # Configuration loader
```

## Getting Started

### Prerequisites

- Go 1.21+
- Java 21+ (for the core services)

### Building

Build the API server:

```bash
# From the root directory
go build -o bin/rinnasrv ./api/cmd/rinnasrv

# Or from the api directory
cd api
go build -o ../bin/rinnasrv ./cmd/rinnasrv
```

### Running

Start the API server:

```bash
# From the root directory
./bin/rinnasrv

# Or with a custom config
RINNA_SERVER_PORT=9090 ./bin/rinnasrv
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

### Projects

- `GET /api/v1/projects` - List projects
- `POST /api/v1/projects` - Create a project
- `GET /api/v1/projects/{id}` - Get a project
- `PUT /api/v1/projects/{id}` - Update a project

### Health Check

- `GET /health` - Health check endpoint

## Configuration

The API server can be configured using a YAML file or environment variables:

```yaml
server:
  port: 8080
  host: localhost
  shutdownTimeout: 15

java:
  command: java
  host: localhost
  port: 8081
  connectTimeout: 5000
  requestTimeout: 30000

logging:
  level: info
  format: json
  file: /var/log/rinna/api.log

auth:
  tokenSecret: your-secret-key
  tokenExpiry: 1440  # 1 day in minutes
  allowedOrigins:
    - http://localhost:3000
```

Environment variables can be used to override the configuration:

```bash
RINNA_SERVER_PORT=9090
RINNA_JAVA_HOST=java-service
RINNA_AUTH_TOKENSECRET=production-secret-key
```
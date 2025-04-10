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
4. **Secure Authentication**: Enhanced token-based authentication system
5. **Common Authentication**: Authentication tokens work across both interfaces

To enable CLI-API integration:

```bash
# Configure API for CLI access
./api/bin/rinnasrv --port 8080

# Use CLI with API backend
cd rinna-cli
./bin/rin config --api-url http://localhost:8080/api/v1
./bin/rin list
```

## Security Features

### Secure Token Management

The API uses a robust token-based authentication system with the following security features:

- **Encrypted Tokens**: All tokens are encrypted using AES-GCM with a configurable encryption key
- **Token Claims**: Tokens contain metadata like project ID, expiration date, scope, and more
- **Token Rotation**: Automatic warning and rotation before tokens expire
- **Secure Storage**: Tokens are stored securely with proper file permissions
- **Format Versioning**: Token format versioning for future enhancements
- **Token Revocation**: Support for immediate token revocation

The token format follows this pattern:

```
ri-<type>-<base64(encrypted(claims))>
```

Where:
- `ri-` is the token prefix
- `<type>` indicates the token type (dev, test, prod)
- The claims section contains encrypted metadata:
  - Token ID (unique identifier)
  - Project ID (associated project)
  - Issuance and expiration timestamps
  - Format version
  - Scope (permissions)
  - User ID (if applicable)

### Token Management Demo

A token management demo application is provided for testing and demonstration purposes:

```
go run ./cmd/tokendemo/main.go help
```

Commands:
- `generate <project-id>`: Generate a new token
- `validate <token>`: Validate a token
- `revoke <token>`: Revoke a token
- `list`: List all tokens
- `clean`: Clean expired tokens

### Webhook Security

The webhook system securely verifies the authenticity of webhook requests using multiple layers of protection:

#### Signature Verification
- GitHub: HMAC-SHA256 signature verification via X-Hub-Signature-256
- GitLab: Token comparison via X-Gitlab-Token
- Bitbucket: Signature verification via X-Hub-Signature
- Custom webhooks: Configurable signature methods

#### Advanced Security Features
- **Replay Protection**: Nonce tracking prevents webhook replay attacks
- **Rate Limiting**: Configurable per-source, per-project rate limits
- **IP Whitelisting**: Allow trusted IPs to bypass certain security checks
- **Secret Rotation**: Support for rotating webhook secrets with grace periods
- **Timestamp Validation**: Rejects requests with outdated timestamps
- **Security Logging**: Comprehensive logging of all security-related events

See [Webhook Security](../docs/WEBHOOK_SECURITY.md) for detailed documentation on the webhook security features.

## API Endpoints

### Authentication

- `POST /api/v1/auth/token/generate` - Generate a new secure token
- `POST /api/v1/auth/token/validate` - Validate an existing token
- `POST /api/v1/auth/token/revoke` - Revoke a token

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

- `POST /api/v1/webhooks/github?project={projectKey}` - GitHub webhook handler
- `POST /api/v1/webhooks/gitlab?project={projectKey}` - GitLab webhook handler
- `POST /api/v1/webhooks/bitbucket?project={projectKey}` - Bitbucket webhook handler
- `POST /api/v1/webhooks/custom/{id}?project={projectKey}` - Custom webhook handler

### Health Check

- `GET /health` - Detailed health check endpoint
- `GET /health/live` - Liveness probe (for Kubernetes)
- `GET /health/ready` - Readiness probe (for Kubernetes)

## Configuration

The API server can be configured using a YAML file or environment variables:

```yaml
project:
  name: "Rinna"
  version: "1.0.0"
  environment: "development"  # Options: development, staging, production
  data_dir: "${HOME}/.rinna/data"
  temp_dir: "${HOME}/.rinna/temp"
  config_dir: "${HOME}/.rinna/config"

server:
  port: 8080
  host: localhost
  read_timeout: 30
  write_timeout: 30
  shutdownTimeout: 15
  autoStart: true  # Auto-start Java service if needed

security:
  api_token_expiration_days: 90
  webhook_token_expiration_days: 365
  token_encryption_key: "${RINNA_TOKEN_ENCRYPTION_KEY:-your-encryption-key}"
  enable_cors: true
  allowed_origins:
    - "http://localhost:3000"
    - "https://*.example.com"

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
    auth_token: /api/auth/token/validate
    token_generate: /api/auth/token/generate
    token_revoke: /api/auth/token/revoke
    webhook_secret: /api/projects/webhooks/secret

logging:
  level: info
  format: json
  file: /var/log/rinna/api.log
  console: true

auth:
  tokenSecret: "your-secret-key"
  tokenExpiry: 60  # 60 minutes
  secretExpiry: 60  # 60 minutes
  webhookSecretExpiry: 1440  # 24 hours
  tokenEncryptionKey: "${RINNA_TOKEN_ENCRYPTION_KEY:-your-encryption-key}"
  devMode: true  # Set to false in production
  allowedSources:
    - "github"
    - "gitlab"
    - "bitbucket"
    - "custom"
  allowedOrigins:
    - "http://localhost:3000"
    - "https://*.example.com"
```

Environment variables can be used to override the configuration:

```bash
RINNA_SERVER_PORT=9090
RINNA_JAVA_HOST=java-service
RINNA_AUTH_TOKENENCRYPTIONKEY=production-encryption-key
RINNA_AUTH_TOKENSECRET=production-secret-key
RINNA_AUTH_DEVMODE=false
RINNA_LOG_LEVEL=DEBUG
RINNA_LOG_DIR=/path/to/logs
RINNA_PROJECT_ENVIRONMENT=production
RINNA_SECURITY_ENABLE_CORS=true
```

## Health Checking System

The health system provides:

1. **Liveness**: Simple check to verify service is running
2. **Readiness**: Checks service dependencies (Java backend) are ready
3. **Detailed Health**: Full system diagnostics with memory usage and uptime

Health information is cached with configurable TTL to minimize impact on dependencies.

## API Documentation

The API is documented using the OpenAPI (Swagger) specification for easy understanding and client generation:

- [API Documentation (Swagger UI)](docs/swagger-ui/index.html)
- [Swagger YAML Definition](swagger.yaml)
- [Swagger JSON Definition](docs/swagger.json)
- [API Examples](docs/api-examples.md)
- [API Security Best Practices](docs/api-security-guide.md)
- [Rate Limiting Documentation](docs/rate-limiting.md)
- [Secure API Integration Examples](docs/secure-integration-examples.md)

### Swagger Documentation Management

We provide a Python-based tool for synchronizing Swagger documentation between YAML and JSON formats:

```bash
# Validate Swagger YAML
python3 bin/sync-swagger.py --validate

# Convert YAML to JSON (default)
python3 bin/sync-swagger.py

# Convert JSON to YAML
python3 bin/sync-swagger.py --direction json-to-yaml

# Bidirectional synchronization
python3 bin/sync-swagger.py --direction both

# Custom file paths
python3 bin/sync-swagger.py --yaml path/to/swagger.yaml --json path/to/swagger.json
```

#### Running the API Documentation Server

We provide a dedicated documentation server for exploring the API without needing to run the full API server:

```bash
# Start the documentation server on the default port (8080)
./bin/start-docs-server.sh

# Start on a custom port
./bin/start-docs-server.sh 9090
```

When the documentation server is running, the following endpoints are available:
- Documentation Home: http://localhost:8080/api/docs/
- Swagger UI: http://localhost:8080/api/docs/swagger-ui/
- API Examples: http://localhost:8080/api/docs/examples
- Security Guide: http://localhost:8080/api/docs/security-guide
- Swagger JSON: http://localhost:8080/api/docs/swagger.json
- Swagger YAML: http://localhost:8080/api/docs/swagger.yaml (downloadable)

The documentation server provides a lightweight way to browse the API documentation without requiring the full API server to be running. This is particularly useful during development and for sharing the API documentation with stakeholders.

### Documentation Features

- **Rich interactive documentation** with Swagger UI
- **Code examples** in multiple languages (JavaScript, Python, Go, curl)
- **Security implementation guide** with best practices
- **Custom themed interface** for better readability
- **Markdown rendering** for easy-to-maintain documentation
- **Downloadable API specifications** in both YAML and JSON formats

When updating API documentation:
1. Edit the swagger.yaml file (source of truth)
2. Run the sync tool to update the JSON version
3. Test with the Swagger UI at http://localhost:8080/api/docs

### Security Documentation

Our API security documentation provides detailed guidance for secure integration:

- **API Security Best Practices**: Guidelines for secure authentication, error handling, and more
- **Rate Limiting**: Detailed explanation of our rate limiting system and best practices
- **Webhook Security**: Comprehensive guide to securely receiving webhooks
- **OAuth Integration**: How to securely connect to third-party services
- **Integration Examples**: Code samples in multiple languages demonstrating secure integration

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

## API Testing with Karate

For comprehensive API testing, we recommend using Karate, a powerful BDD-style API testing framework. Karate allows you to quickly create readable and maintainable API tests without writing code.

See the [Karate Test Syntax Guide](../docs/testing/KARATE_TEST_SYNTAX.md) for detailed information on setting up and using Karate for API testing in the Rinna project.
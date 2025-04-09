# Rinna Container Strategy

This document outlines the comprehensive container strategy for the Rinna project, with a focus on Python components. The strategy is designed to address the needs of local developers, CI/CD pipelines, and deployment to QA and production environments.

## Container Architecture

### Multi-Stage Builds

The container strategy is based on multi-stage Docker builds that optimize for different use cases:

1. **Base Stage**
   - Common dependencies and configurations
   - Minimal OS packages to reduce image size

2. **Builder Stage**
   - Compiles and packages application code
   - Installs all required dependencies
   - Results are copied to subsequent stages

3. **Testing Stage**
   - Optimized for running tests
   - Includes testing frameworks and tools
   - Runs as non-root user for security

4. **Development Stage**
   - Based on testing stage
   - Includes additional tools for development (vim, debuggers, etc.)
   - Mounts source code for live editing

5. **Production Stage**
   - Minimal runtime dependencies
   - No build tools or development packages
   - Uses non-root user for security
   - Proper health checks and signal handling
   - Optimized for size and startup time

## Usage

### Local Development

```bash
# Run tests
./bin/run-python-container.sh unit

# Start development container
./bin/run-python-container.sh dev

# Start production-like container locally
./bin/run-python-container.sh prod
```

### CI/CD Integration

The GitHub Actions workflow in `.github/workflows/python-container-ci.yml` implements:

1. Building optimized container images
2. Testing at each level of the pyramid
3. Pushing images to GitHub Container Registry
4. Reporting test results

### Deployment

Deployment is streamlined with version-tagged images:

```bash
# Deploy to QA
kubectl set image deployment/rinna-python-service python-service=ghcr.io/yourorg/rinna/prod:v1.2.3

# Rollback
kubectl rollout undo deployment/rinna-python-service
```

## Image Caching Strategy

To optimize build time during development, the project includes an image caching system:

1. **Save and load images** from local cache
2. **Detect changes** in relevant files to determine if rebuilds are needed
3. **Share cached images** across team members using Git LFS

Commands:
```bash
# Cache management
./bin/cache-python-image.sh --type=test build   # Build and cache test image
./bin/cache-python-image.sh --type=dev build    # Build and cache dev image
./bin/cache-python-image.sh --type=prod build   # Build and cache prod image

# Auto-managed through run script
./bin/run-python-container.sh unit              # Uses cached image when possible
```

## Container Image Types

### 1. Test Images (`rinna-python-tests:latest`)

- Purpose: Running tests in the testing pyramid
- Includes: Test frameworks, code coverage tools
- Used by: Developers for testing, CI pipelines

### 2. Development Images (`rinna-python-dev:latest`)

- Purpose: Interactive development environment
- Includes: Development tools, debuggers, live reloading
- Used by: Developers for code writing and debugging

### 3. Production Images (`rinna-python:latest`)

- Purpose: Running in production/QA environments
- Includes: Minimal runtime dependencies
- Used by: Deployment pipelines, production environments

## Environment Configurations

Each image type has optimized environment configurations:

### Test Environment
- `PYTHONPATH=/app:/app/python`
- `LOG_LEVEL=info` (configurable)
- `PYTHONDONTWRITEBYTECODE=1`

### Development Environment
- Same as test plus:
- `DEV_MODE=true`
- Source code mounted for live editing

### Production Environment
- `PYTHONPATH=/app`
- `PORT=5000` (configurable)
- `API_URL=http://api-server:8080` (configurable)
- Health check configured

## Security Considerations

1. **Non-root User**: All containers run as a non-root user
2. **Minimal Base Image**: Using slim Python image to reduce attack surface
3. **Multi-stage Builds**: Production image doesn't contain build tools
4. **Dependencies**: Regular scans for vulnerabilities in dependencies
5. **Secrets Management**: No hardcoded secrets in container images

## Best Practices

### Caching Dependencies

The Dockerfile is organized to optimize layer caching:
1. Copy requirements first
2. Install dependencies
3. Copy application code

### Image Size Optimization

- Use slim base images
- Clean up package caches in the same RUN command
- Multi-stage builds to exclude build dependencies

### Tagging Strategy

- `latest`: Always points to latest main branch
- `commit-sha`: Unique tag for each build
- `vX.Y.Z`: Semantic version tags for releases
- `branch-name`: For feature branch testing

## Integration with Other Components

The container strategy is designed to work with the other components of the Rinna system:

1. **API Server (Go)**: Python container connects to Go API service
2. **Core Services (Java)**: Communication through Go API
3. **Shared Storage**: Volume mounts for shared data

## Cross-Platform Compatibility

The container strategy supports both Docker and Podman:

```bash
# Use Docker explicitly
./bin/run-python-container.sh --docker unit

# Default is Podman if available, fallback to Docker
./bin/run-python-container.sh unit
```

## Setup for New Developers

New developers can get started with:

```bash
# Clone repository
git clone https://github.com/yourorg/rinna.git

# Run tests directly
./bin/run-python-container.sh all

# Start development environment
./bin/run-python-container.sh dev
```

## Implementation Details

Key files for the container strategy:
- `python/Dockerfile`: Multi-stage development and testing image
- `python/Dockerfile.prod`: Production-optimized image
- `python/docker-compose.yml`: Container service definitions
- `bin/run-python-container.sh`: Convenient script for running containers
- `bin/cache-python-image.sh`: Image caching system
- `.github/workflows/python-container-ci.yml`: CI/CD integration
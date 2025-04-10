# Cross-Platform Container Setup for Rinna

Rinna is designed to run seamlessly on any platform with minimal setup requirements. This guide explains how to set up and use Rinna's containerized deployment on Windows, WSL, and native Linux.

## Prerequisites

The only required software is one of:
- Docker Desktop (Windows, macOS)
- Docker Engine (Linux)
- Podman (preferred on Linux)

No other software, runtime, or development environment is needed to run Rinna.

## Quick Start

### Step 1: Clone the Repository

```bash
git clone https://github.com/heymumford/Rinna.git
cd Rinna
```

### Step 2: Start the Containers

With Docker:
```bash
docker-compose up -d
```

With Podman:
```bash
podman-compose up -d
```

### Step 3: Access Rinna

CLI access:
```bash
# Connect to Rinna CLI container
docker exec -it rinna-cli-1 /bin/bash

# Run Rinna commands
rin list
```

Web access:
- API documentation: http://localhost:8080/docs
- Web interface: http://localhost:8080/ui

## Platform-Specific Setup

### Windows

1. Install Docker Desktop for Windows
2. Enable WSL 2 integration (recommended)
3. Clone the repository
4. Run Docker Compose

```powershell
# PowerShell
git clone https://github.com/heymumford/Rinna.git
cd Rinna
docker-compose up -d
```

### Windows Subsystem for Linux (WSL)

1. Install and set up WSL 2
2. Install Docker inside WSL or enable Docker Desktop WSL integration
3. Clone the repository in your WSL environment
4. Run Docker Compose or Podman Compose

```bash
# Inside WSL
git clone https://github.com/heymumford/Rinna.git
cd Rinna
docker-compose up -d  # or podman-compose up -d
```

### Native Linux

1. Install Docker Engine or Podman
2. Clone the repository
3. Run Docker Compose or Podman Compose

```bash
# Native Linux
git clone https://github.com/heymumford/Rinna.git
cd Rinna

# With Podman (preferred)
podman-compose up -d

# With Docker
docker-compose up -d
```

## Container Architecture

Rinna uses a multi-container architecture that works identically across platforms:

```
┌────────────────┐     ┌────────────────┐     ┌────────────────┐
│                │     │                │     │                │
│   API Server   │◄────┤  Java Service  │◄────┤ Python Service │
│     (Go)       │     │                │     │                │
│                │     │                │     │                │
└───────┬────────┘     └────────────────┘     └────────────────┘
        │
        │
┌───────▼────────┐     ┌────────────────┐
│                │     │                │
│   CLI Client   │     │  Documentation │
│                │     │    Server      │
│                │     │                │
└────────────────┘     └────────────────┘
```

All containers:
- Use shared volumes for persistent data
- Communicate on an internal network
- Expose only necessary ports
- Include health checks and self-healing
- Use the same configuration across platforms

## Volume Management

Rinna containers use named volumes for persistent data:

```yaml
volumes:
  go-api-data:      # API server data
  java-data:        # Java service data
  python-data:      # Python service data
  shared-storage:   # Cross-service shared data
```

These volumes work consistently across platforms with proper permission handling.

## Development Environment

For development, use the included dev container:

```bash
# Start development environment
docker-compose --profile dev up -d

# Connect to development container
docker exec -it rinna-dev-environment-1 /bin/bash
```

The development container includes all necessary dependencies for working on any part of Rinna.

## Container Profiles

Rinna uses container profiles for different use cases:

- **Default**: Core services only
- **dev**: Development environment
- **testing**: Test execution containers
- **docs**: Documentation servers

```bash
# Start with testing profile
docker-compose --profile testing up -d

# Run tests
docker exec rinna-python-tests-1 python -m pytest
```

## Troubleshooting

### Windows-Specific Issues

- **Line Ending Problems**: Set Git to use LF on checkout:
  ```
  git config --global core.autocrlf input
  ```

- **Volume Permission Issues**: Use Docker Desktop WSL integration or set appropriate permissions in volume mounts

### WSL Issues

- **Docker Socket Connection**: If using Docker Desktop from WSL, ensure the socket is properly exposed:
  ```
  echo 'export DOCKER_HOST=tcp://localhost:2375' >> ~/.bashrc
  ```

### Linux Issues

- **Podman Rootless Mode**: If using rootless Podman, you may need to adjust permissions:
  ```
  podman unshare chown -R 1000:1000 ./shared-data
  ```

## Health Checking and Self-Healing

Rinna containers include health checks to ensure services are running properly:

```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8080/health"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 5s
```

The containers are configured to restart automatically if they fail, providing self-healing capabilities.

## Zero-Install Option

For teams that want to avoid even cloning the repository, we provide a zero-install option:

```bash
# Pull and run Rinna without cloning the repository
docker run -d --name rinna-all-in-one heymumford/rinna:latest
```

This single command downloads and runs a pre-configured Rinna environment with all necessary components.

## Monitoring Container Status

```bash
# Check status of all Rinna containers
docker ps --filter "name=rinna"

# View container logs
docker logs rinna-api-server-1

# Check container health
docker inspect --format "{{.Name}} {{.State.Health.Status}}" $(docker ps -q --filter "name=rinna")
```

## Related Resources

- [Admin Server Setup Guide](admin-server-setup.md)
- [Container Strategy](../testing/CONTAINER_STRATEGY.md)
- [Docker Image Caching](../testing/DOCKER_IMAGE_CACHING.md)
- [Python Container Testing](../testing/PYTHON_CONTAINER_TESTING.md)
# Cross-Platform Container Setup for Rinna

Rinna is designed to run seamlessly on any platform with minimal setup requirements. This guide explains how to set up and use Rinna's containerized deployment on Windows, WSL, and native Linux.

## Prerequisites

The only required software is one of:
- Docker Desktop (Windows, macOS)
- Docker Engine (Linux)
- Podman (preferred on Linux)

No other software, runtime, or development environment is needed to run Rinna.

## Quick Start

### Option 1: Universal Container Script (Recommended)

We've created a universal script that works across all platforms and handles environment detection automatically:

```bash
# Clone the repository
git clone https://github.com/heymumford/Rinna.git
cd Rinna

# Make script executable
chmod +x bin/rinna-container.sh

# Start all containers
./bin/rinna-container.sh start

# Check status
./bin/rinna-container.sh status

# Access shell in development container
./bin/rinna-container.sh shell
```

### Option 2: Manual Container Setup

If you prefer to manage containers directly:

```bash
# Clone the repository
git clone https://github.com/heymumford/Rinna.git
cd Rinna

# With Docker
docker-compose up -d

# Or with Podman
podman-compose up -d
```

### Option 3: Zero-Install Mode

For the fastest setup with minimal requirements:

```bash
# No repository cloning needed, just run this command:
docker run -d --name rinna-all-in-one \
  -p 8080:8080 -p 8081:8081 -p 5000:5000 \
  -v rinna-data:/shared \
  heymumford/rinna:latest
```

See the [Zero-Install Container Guide](zero-install-container.md) for more details.

## Accessing Rinna

CLI access:
```bash
# Using the universal script
./bin/rinna-container.sh shell

# Manual access to CLI container
docker exec -it rinna-dev-environment-1 /bin/bash

# Run Rinna commands
rin list
```

Web access:
- API documentation: http://localhost:8080/docs
- Web interface: http://localhost:8080/ui
- Metrics dashboard: http://localhost:8080/metrics

## Universal Container Script Features

The `rinna-container.sh` script provides a consistent interface across all platforms:

```bash
# Start specific services only
./bin/rinna-container.sh --type=api start    # Start API server only
./bin/rinna-container.sh --type=java start   # Start Java service only 
./bin/rinna-container.sh --type=python start # Start Python service only
./bin/rinna-container.sh --type=dev start    # Start development environment

# Container management
./bin/rinna-container.sh status              # Check container status
./bin/rinna-container.sh logs                # View logs from all containers
./bin/rinna-container.sh health              # Check container health
./bin/rinna-container.sh restart             # Restart all containers
./bin/rinna-container.sh stop                # Stop all containers
./bin/rinna-container.sh clean               # Remove containers and optionally data

# Force specific container engine
./bin/rinna-container.sh --docker start      # Use Docker explicitly
./bin/rinna-container.sh --podman start      # Use Podman explicitly

# Platform-specific configuration
./bin/rinna-container.sh --windows start     # Configure for Windows Git Bash
./bin/rinna-container.sh --wsl start         # Configure for WSL
./bin/rinna-container.sh --linux start       # Configure for native Linux

# Zero-install mode
./bin/rinna-container.sh --zero-install start # Use prebuilt all-in-one image
```

## Platform-Specific Setup

### Windows

The universal container script automatically detects Windows and configures the environment appropriately:

```bash
# Git Bash or PowerShell
git clone https://github.com/heymumford/Rinna.git
cd Rinna
chmod +x bin/rinna-container.sh
./bin/rinna-container.sh start
```

For optimal Windows performance:
1. Enable WSL 2 integration in Docker Desktop
2. Allocate sufficient resources in Docker Desktop settings
3. Use volume mapping instead of bind mounts for faster filesystem access

### Windows Subsystem for Linux (WSL)

The script automatically detects WSL and configures Docker/Podman accordingly:

```bash
# Inside WSL
git clone https://github.com/heymumford/Rinna.git
cd Rinna
chmod +x bin/rinna-container.sh
./bin/rinna-container.sh start
```

The script handles WSL-specific requirements:
- Detecting Docker socket location
- Setting appropriate path mappings
- Configuring permissions for mounted volumes

### Native Linux

On Linux systems, the script prioritizes Podman in rootless mode if available:

```bash
# Native Linux
git clone https://github.com/heymumford/Rinna.git
cd Rinna
chmod +x bin/rinna-container.sh
./bin/rinna-container.sh start
```

The script automatically:
- Sets up proper volume permissions for rootless Podman
- Creates consistent mount points regardless of container engine
- Configures network settings for secure operation

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

The universal script automatically creates and configures consistent volumes:

```yaml
volumes:
  go-api-data:      # API server data
  java-data:        # Java service data
  python-data:      # Python service data
  shared-storage:   # Cross-service shared data
  test-output:      # Test results
  coverage-data:    # Code coverage reports
```

Benefits of this approach:
- Consistent path structure across platforms
- Automatic permission handling
- Persistence between container restarts
- Optimized for each platform's filesystem performance

## Container Profiles

Rinna uses container profiles for different use cases:

- **Default**: Core services only
- **dev**: Development environment
- **testing**: Test execution containers
- **docs**: Documentation servers

```bash
# Start with testing profile
./bin/rinna-container.sh --type=test start

# Run tests
./bin/rinna-container.sh logs
```

## Health Checking and Self-Healing

All Rinna containers include health checks and self-healing capabilities:

```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8080/health"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 5s
```

The universal script provides additional health monitoring:

```bash
# Check container health status
./bin/rinna-container.sh health

# Health monitoring runs in the background and automatically restarts
# unhealthy containers
```

Health monitoring features:
- Background health check process
- Automatic unhealthy container detection
- Self-healing with automatic restarts
- Health status logging and history

## Zero-Install Option

For teams that want to avoid even cloning the repository, we provide a zero-install option:

```bash
# Pull and run Rinna without cloning the repository
docker run -d --name rinna-all-in-one \
  -p 8080:8080 -p 8081:8081 -p 5000:5000 \
  -v rinna-data:/shared \
  heymumford/rinna:latest
```

The zero-install mode:
- Packages all services in a single container
- Includes all dependencies and configurations
- Provides the same functionality with simplified setup
- Supports health monitoring and self-healing

For more details, see the [Zero-Install Container Guide](zero-install-container.md).

## Troubleshooting

### Common Issues

- **Permission problems**: Fixed by the universal script's automatic permission handling
- **Volume mapping issues**: Resolved with consistent bind mount configurations
- **Network conflicts**: Avoided with dedicated container network
- **Resource constraints**: Check Docker/Podman resource allocation

### Platform-Specific Troubleshooting

#### Windows

- **Line ending issues**: The script automatically handles line ending conversion
- **Path mapping problems**: Fixed with proper path normalization
- **Performance issues**: Use WSL 2 integration and volume mapping instead of bind mounts

#### WSL

- **Docker socket connection**: The script checks and warns about socket availability
- **Path inconsistencies**: Automatically resolved with consistent path handling
- **Resource limitations**: Configure WSL 2 memory and CPU allocation in `.wslconfig`

#### Linux

- **Podman rootless mode**: The script handles rootless configuration automatically
- **SELinux constraints**: Apply appropriate labels to mounted volumes
- **User namespace mapping**: Configured for consistent user/group IDs

## Advanced Container Management

### Custom Configuration

Create a `.rinna-containers/user-config.env` file to customize defaults:

```bash
# Example user configuration
CONTAINER_ENGINE=podman
PLATFORM=linux
COMPOSE_FILE=custom-compose.yml
STORAGE_PATH=/data/rinna
ZERO_INSTALL_IMAGE=myorg/rinna:custom
HEALTH_CHECK_INTERVAL=60
```

### Build Custom Container Images

```bash
# Build all container images
./bin/rinna-container.sh build

# Build specific container types
./bin/rinna-container.sh --type=python build
```

### Container Performance Optimization

For improved performance:

1. Use volume mounts instead of bind mounts
2. Configure appropriate resource limits
3. Use health monitoring to detect and fix issues
4. Consider the zero-install option for simpler deployments

## Related Resources

- [Zero-Install Container Guide](zero-install-container.md)
- [Admin Server Setup Guide](admin-server-setup.md)
- [Container Strategy](../testing/CONTAINER_STRATEGY.md)
- [Docker Image Caching](../testing/DOCKER_IMAGE_CACHING.md)
- [Python Container Testing](../testing/PYTHON_CONTAINER_TESTING.md)
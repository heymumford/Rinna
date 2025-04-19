# Rinna Zero-Install Container Guide

This guide explains how to use Rinna's "zero-install" container option, which allows you to run the entire system with a single command, without needing to clone the repository or install any dependencies other than Docker or Podman.

## What is Zero-Install Mode?

Zero-Install mode packages the entire Rinna system—including API server, Java services, Python components, and documentation—into a single pre-built container image. This allows you to:

- Run Rinna with just one command
- Avoid setting up the development environment
- Get started quickly for evaluation or demos
- Use Rinna on any platform with minimal setup

## Prerequisites

The only requirement is Docker or Podman:

- **Windows**: Docker Desktop
- **macOS**: Docker Desktop
- **Linux**: Docker Engine or Podman (recommended)

## Quick Start

### Using Docker

```bash
# Pull and run the Rinna zero-install container
docker run -d --name rinna-all-in-one \
  -p 8080:8080 -p 8081:8081 -p 5000:5000 \
  -v rinna-data:/shared \
  heymumford/rinna:latest

# Check container status
docker ps --filter "name=rinna-all-in-one"

# View logs
docker logs rinna-all-in-one
```

### Using Podman

```bash
# Pull and run the Rinna zero-install container
podman run -d --name rinna-all-in-one \
  -p 8080:8080 -p 8081:8081 -p 5000:5000 \
  -v rinna-data:/shared \
  heymumford/rinna:latest

# Check container status
podman ps --filter "name=rinna-all-in-one"

# View logs
podman logs rinna-all-in-one
```

### Using the Universal Script

If you've cloned the repository, you can use the included `rinna-container.sh` script:

```bash
# Start Rinna in zero-install mode
./bin/rinna-container.sh --zero-install start

# Check status
./bin/rinna-container.sh --zero-install status

# Stop Rinna
./bin/rinna-container.sh --zero-install stop
```

## Accessing Rinna

Once the container is running, you can access Rinna's components:

- **Web API**: http://localhost:8080
- **API Documentation**: http://localhost:8080/docs
- **Web UI**: http://localhost:8080/ui
- **Metrics Dashboard**: http://localhost:8080/metrics

## Connect to the Container Shell

To access the CLI and other tools within the container:

```bash
# Using Docker
docker exec -it rinna-all-in-one /bin/bash

# Using Podman
podman exec -it rinna-all-in-one /bin/bash

# Using the universal script
./bin/rinna-container.sh --zero-install shell
```

Once connected, you can use the Rinna CLI:

```bash
# Inside the container
rin --help
rin list
```

## Data Persistence

The zero-install container mounts a volume for persistent data storage:

```
volumes:
  - rinna-data:/shared
```

This ensures your work items, configuration, and other data persist across container restarts.

## Container Health Monitoring

The zero-install container includes built-in health checks:

```bash
# Check container health
docker inspect --format "{{.State.Health.Status}}" rinna-all-in-one

# Using the universal script
./bin/rinna-container.sh --zero-install health
```

The container will automatically restart if it becomes unhealthy.

## Platform-Specific Notes

### Windows

On Windows, ensure Docker Desktop is installed and running. For optimal performance:

1. Allocate sufficient resources to Docker in Settings
2. Enable WSL 2 integration for better performance
3. Use volume mapping instead of bind mounts for faster I/O

### WSL (Windows Subsystem for Linux)

When running in WSL:

1. Ensure Docker Desktop's WSL integration is enabled
2. Run the container command from within your WSL distribution
3. Access the services using localhost from either Windows or WSL

### Native Linux

On Linux systems:

1. Podman (rootless mode) is recommended for enhanced security
2. Set up volume permissions if accessing local filesystem:
   ```bash
   podman unshare chown -R 1000:1000 /path/to/data
   ```

## Advanced Configuration

### Custom Ports

To use different ports:

```bash
docker run -d --name rinna-all-in-one \
  -p 9090:8080 -p 9091:8081 -p 9092:5000 \
  -v rinna-data:/shared \
  heymumford/rinna:latest
```

### Environment Variables

The container supports several environment variables:

```bash
docker run -d --name rinna-all-in-one \
  -p 8080:8080 -p 8081:8081 -p 5000:5000 \
  -v rinna-data:/shared \
  -e LOG_LEVEL=debug \
  -e DEMO_DATA=true \
  -e ADMIN_MODE=true \
  heymumford/rinna:latest
```

Available environment variables:
- `LOG_LEVEL`: Logging verbosity (debug, info, warning, error)
- `DEMO_DATA`: Whether to initialize with demo data (true/false)
- `ADMIN_MODE`: Enable admin features (true/false)
- `API_PORT`: Override API port (default: 8080)
- `JAVA_PORT`: Override Java service port (default: 8081)
- `PYTHON_PORT`: Override Python service port (default: 5000)

## Limitations

The zero-install mode has some limitations compared to the full development setup:

1. Limited customization options
2. Cannot modify the codebase without rebuilding the image
3. Performance may be slightly reduced compared to individually optimized containers
4. Some advanced development features are not available

## Troubleshooting

### Container Won't Start

Check if ports are already in use:

```bash
docker ps -a
# or
netstat -tuln | grep -E '8080|8081|5000'
```

### Service Unavailable

Check container logs:

```bash
docker logs rinna-all-in-one
```

### Slow Performance

For better performance, especially on Windows:

1. Increase Docker resource allocation (CPU, memory)
2. Use volume mapping instead of bind mounts
3. Ensure WSL 2 integration is enabled on Windows

## Related Resources

- [Cross-Platform Container Setup](cross-platform-container-setup.md)
- [Admin Server Setup](admin-server-setup.md)
- [CLI Quick Reference](rin-cli-printable-reference.md)
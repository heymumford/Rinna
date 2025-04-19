# Docker/Podman Image Caching Strategy

This document explains the Docker/Podman image caching strategy used for Python testing in the Rinna project.

## Overview

Building Docker/Podman images from scratch can be time-consuming, especially during development when you want to run tests frequently. To optimize this process, we've implemented a Docker image caching system that allows you to:

1. Save Docker/Podman images to a local cache
2. Load images from this cache rather than rebuilding
3. Automatically detect when rebuilds are necessary based on file changes
4. Store these images in the repository for team sharing

## How It Works

### Image Cache Files

Images are stored in the `/docker-cache` directory as tar files:

```
docker-cache/
├── rinna-python-tests-latest.tar       # The cached image
└── rinna-python-tests-latest.md5       # Checksum to detect changes
```

The `.tar` file contains the actual Docker/Podman image, while the `.md5` file contains a checksum hash of all the files used to build the image. This allows the system to detect when these files have changed and a rebuild is necessary.

### Cache Management Scripts

Two scripts manage the image cache:

1. **cache-python-image.sh**: Handles saving, loading, and updating cached images
2. **run-python-container.sh**: Uses cached images when running tests

### Automatic Rebuilds

The system automatically:

1. Checks if image files have changed by comparing checksums
2. Rebuilds the image only when necessary
3. Updates the cache when rebuilds occur

## Using the Cache System

### Basic Usage

In most cases, you don't need to interact with the cache system directly. The `run-python-container.sh` script will automatically manage the cache:

```bash
./bin/run-python-container.sh unit
```

### Manual Cache Management

For direct cache management, use the `cache-python-image.sh` script:

```bash
# Save current image to cache
./bin/cache-python-image.sh save

# Load image from cache
./bin/cache-python-image.sh load

# Update cache if needed
./bin/cache-python-image.sh update

# Build image and update cache
./bin/cache-python-image.sh build
```

### Force Rebuilding

To force a rebuild regardless of cache status:

```bash
./bin/run-python-container.sh --rebuild unit
```

## Git Integration

The Docker cache files are stored in the repository to allow sharing between team members. This approach has pros and cons:

### Pros:
- No need for external Docker registry
- Images available immediately after clone
- Version history for container images
- Works in offline environments

### Cons:
- Increases repository size (mitigated by Git LFS)
- May cause merge conflicts (rare due to binary nature)

### Git LFS Configuration

To efficiently handle large Docker image files, we recommend using Git Large File Storage (Git LFS):

1. Install Git LFS:
   ```bash
   git lfs install
   ```

2. Track Docker cache files:
   ```bash
   git lfs track "docker-cache/*.tar"
   ```

3. Add `.gitattributes` to the repository:
   ```bash
   git add .gitattributes
   ```

## Managing Cache Size

To prevent the repository from growing too large:

1. Periodically review and clean up old cache files
2. Consider implementing cache rotation policies
3. Use submodules for very large image files

## Troubleshooting

### Cache Corruption

If the cached image becomes corrupted:

```bash
# Remove corrupted cache
rm -f docker-cache/rinna-python-tests-latest.tar
rm -f docker-cache/rinna-python-tests-latest.md5

# Rebuild and cache
./bin/cache-python-image.sh build
```

### Permissions Issues

If you encounter permission issues with cached images:

```bash
sudo chown -R $(id -u):$(id -g) docker-cache/
```

### Load/Save Failures

If loading or saving images fails, check disk space and permissions. You may also need to manually remove all images and start fresh:

```bash
# Remove images
podman rmi -f rinna-python-tests:latest

# Rebuild
./bin/cache-python-image.sh build
```

## Best Practices

1. **When to commit cache files**: After significant changes to Docker files or dependencies
2. **When to rebuild**: When modifying Dockerfiles, dependencies, or test infrastructure
3. **Local-only caching**: For frequent minor changes, consider keeping cache files local (add to .gitignore)
4. **CI/CD integration**: Configure CI systems to use cached images for faster test runs
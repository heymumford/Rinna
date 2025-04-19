# Build Number Management in Rinna

Rinna maintains build numbers as part of its versioning strategy. Build numbers provide a sequential identifier for every build of the application.

## Overview

The build number system has the following attributes:

- Build numbers are sequential integers starting from 500
- Build numbers increment with every push to the repository
- Build numbers are stored in `version.properties` files
- The build number is independent of the semantic version

## Build Number Files

Build numbers are stored in the following files:

- Main: `/version.properties`
- Version Service: `/version-service/version.properties`

## Automatic Incrementation

The build number is automatically incremented when:

1. Using the `git pushbuild` alias (configured via `bin/install-git-hooks.sh --configure`)
2. The `post-push` Git hook is triggered

## Manual Management

The build number can also be managed manually with the following commands:

```bash
# View current build number
bin/rin version current

# Set a specific build number
bin/increment-build.sh set <number>

# Increment build number manually
bin/increment-build.sh
```

## Implementation Details

The build number incrementation is implemented through:

1. A post-push Git hook (`bin/hooks/post-push`) that automatically increments the build number
2. A dedicated script (`bin/increment-build.sh`) for manual build number management
3. A modification to the version bumping script (`bin/rin-version`) to preserve the build number

## Setup

To set up automatic build number incrementation:

```bash
# Install git hooks and configure the pushbuild alias
bin/install-git-hooks.sh --configure
```

Once configured, use `git pushbuild` instead of `git push` to automatically increment the build number after pushing to the repository.

## Best Practices

1. Always use `git pushbuild` instead of `git push` to ensure the build number is incremented
2. After a successful CI build, the build number should be used in release artifacts
3. Use `bin/rin version current` to view the current build number
4. Include the build number in logs and reports for traceability
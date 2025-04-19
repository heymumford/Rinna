# Version Numbering System

This document was developed with analytical assistance from AI tools including Claude 3.7 Sonnet, Claude Code, and Google Gemini Deep Research, which were used as paid services. All intellectual property rights remain exclusively with the copyright holder Eric C. Mumford (@heymumford). Licensed under the Mozilla Public License 2.0.

## Overview

This document defines the version numbering system used across the Rinna project. Consistent version numbering is essential for tracking changes, managing dependencies, and communicating the nature of updates to stakeholders.

## Semantic Versioning

Rinna follows [Semantic Versioning 2.0.0](https://semver.org/) (SemVer) with some project-specific extensions.

### Basic Format

```
MAJOR.MINOR.PATCH[-PRERELEASE][+BUILD]
```

Where:

- **MAJOR**: Incremented for incompatible API changes
- **MINOR**: Incremented for backward-compatible functionality additions
- **PATCH**: Incremented for backward-compatible bug fixes
- **PRERELEASE**: Optional identifier for pre-release versions (alpha, beta, rc)
- **BUILD**: Optional build metadata

### Examples

- `1.0.0`: Initial stable release
- `1.1.0`: New features added in a backward-compatible manner
- `1.1.1`: Bug fixes to existing features
- `2.0.0`: Changes that break backward compatibility
- `1.2.0-alpha.1`: Alpha release of upcoming 1.2.0 version
- `1.2.0-beta.2`: Second beta release of upcoming 1.2.0 version
- `1.5.0+20250406`: Build metadata indicating build date

## Version Lifecycle

### Development Phases

1. **Alpha** (`-alpha.N`): Early development versions, incomplete features, likely to have significant bugs
2. **Beta** (`-beta.N`): Feature complete, undergoing testing, may have known issues
3. **Release Candidate** (`-rc.N`): Potential release version, undergoing final validation
4. **Stable**: Production-ready version without prerelease identifier
5. **Maintenance**: Versions receiving only critical bug fixes after newer versions are released

### Numbering Rules

1. **Major Version Zero** (0.y.z): Initial development phase where anything may change at any time
2. **Version 1.0.0**: Defines the first stable public API
3. **Patch Releases** (x.y.Z): Only for backward-compatible bug fixes
4. **Minor Releases** (x.Y.z): For backward-compatible new features and non-essential changes
5. **Major Releases** (X.y.z): For changes that break backward compatibility

## Module-Specific Versioning

### Core Components

All core components share the same version number to ensure compatibility:

- rinna-core
- rinna-cli
- rinna-api

### Independent Components

Some components may have their own version numbers if they can evolve independently:

- rinna-utils: `U.V.W` (may differ from core version)
- rinna-plugins: Each plugin may have its own version

## Release Cadence

- **Patch Releases**: As needed for critical bug fixes
- **Minor Releases**: Scheduled every 4-6 weeks
- **Major Releases**: Scheduled every 6-12 months

## Version Management

### Version Files

Version information is maintained in:

- `version.properties`: Primary source of truth for version numbers
- `pom.xml`: Maven project version (uses XMLStarlet for precise updates)
- Go version files: Version constants in .go files
- Other configuration files: Updated automatically by version tools

### Version Management Tools

Rinna provides two tools for version management:

#### High-Level Wrapper (bin/rin-version)

The user-friendly `bin/rin-version` utility provides a simplified interface:

```bash
# Display current version
bin/rin-version current

# Increment versions
bin/rin-version major           # 1.0.0 -> 2.0.0
bin/rin-version minor           # 1.3.0 -> 1.4.0 
bin/rin-version patch           # 1.3.1 -> 1.3.2

# Set specific version
bin/rin-version set 2.1.0

# Set prerelease version
bin/rin-version prerelease beta.1

# Clear prerelease status
bin/rin-version release

# Create git tag for current version
bin/rin-version tag

# Verify and update
bin/rin-version verify          # Check version consistency
bin/rin-version update          # Update all files from version.properties
```

#### Direct Version Manager (bin/version-manager.sh)

For more advanced control, use the core version manager directly:

```bash
# Display current version
bin/version-manager.sh current

# Bump versions
bin/version-manager.sh bump major           # 1.0.0 -> 2.0.0
bin/version-manager.sh bump minor           # 1.3.0 -> 1.4.0
bin/version-manager.sh bump patch           # 1.3.1 -> 1.3.2
bin/version-manager.sh bump patch --no-commit # Without creating a git commit

# Set specific version
bin/version-manager.sh set 2.1.0
bin/version-manager.sh set 2.1.0 --no-commit

# Build number management
bin/version-manager.sh increment-build      # Increment build number
bin/version-manager.sh set-build 100        # Set specific build number

# Verification
bin/version-manager.sh verify               # Check version consistency
```

IMPORTANT: Always use these version management tools. Never manually edit version.properties, POM files, or other version-related files directly. The tools ensure consistency and use XMLStarlet for safe XML manipulation.

## API Version Management

### API Versioning

The REST API uses versioning in the URL path:

```
/api/v1/resources
/api/v2/resources
```

Rules:
- Minor and patch changes use the same API version
- Major changes increment the API version
- Multiple API versions may be supported simultaneously during transition periods

### Database Schema Versioning

Database schemas use a separate version number:

```
DB_SCHEMA_VERSION=5
```

Database migrations are named with sequential numbers and descriptions:

```
V1__Initial_schema.sql
V2__Add_user_preferences.sql
V3__Extend_metadata_fields.sql
```

## Backward Compatibility Policy

- **Major Versions**: May contain breaking changes, which must be documented
- **Minor Versions**: Must maintain backward compatibility
- **Patch Versions**: Must maintain backward compatibility
- **Deprecation**: Features scheduled for removal must be deprecated in a minor release before removal in a major release
- **Migration Path**: All breaking changes must include documentation on how to migrate

## Changelog Management

Each version should be documented in `CHANGELOG.md` using the following categories:

- **Added**: New features
- **Changed**: Changes in existing functionality
- **Deprecated**: Features that will be removed in upcoming releases
- **Removed**: Features removed in this release
- **Fixed**: Bug fixes
- **Security**: Vulnerabilities fixed

## Continuous Integration

- CI builds use the version number with build metadata: `1.2.3+ci.{build-number}`
- Release builds drop the build metadata for clean version numbers

## Conclusion

Adhering to this version numbering system ensures clear communication about changes, facilitates dependency management, and provides a consistent approach across all project components.
# Version Manager for Rinna

This document describes the version management tools available in the Rinna project.

## Overview

Rinna uses a centralized version management system with XMLStarlet integration for precise handling of XML files. The `version.properties` file serves as the single source of truth for version information across the project.

## Available Tools

### Core Version Manager

`bin/version-manager.sh` is the centralized tool for managing versions:

```bash
# Display current version
bin/version-manager.sh current

# Verify version consistency
bin/version-manager.sh verify

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
```

### Backwards Compatibility Wrappers

Several scripts provide backwards compatibility with older workflows:

- `bin/update-versions.sh`: Updates all version references 
- `bin/increment-build.sh`: Increments the build number
- `bin/check-versions.sh`: Verifies version consistency

### User-Friendly Wrapper

`bin/rin-version` is a user-friendly wrapper around the version manager:

```bash
bin/rin-version current   # Display current version
bin/rin-version patch     # Bump patch version
bin/rin-version minor     # Bump minor version
bin/rin-version verify    # Check version consistency
bin/rin-version update    # Update all files from version.properties
```

## XML Tools Integration

All XML manipulation is performed using XMLStarlet through the `bin/xml-tools.sh` library:

```bash
source bin/xml-tools.sh

# Get version from a POM file
xml_get_version "path/to/pom.xml"

# Set version in a POM file
xml_set_version "path/to/pom.xml" "1.2.3"
```

## Files Updated by Version Manager

The version manager updates the following files:

1. `version.properties`: The primary source of truth
2. `version-service/version.properties`: A copy of the main properties file
3. Maven POM files: Using XMLStarlet for safe XML manipulation
   - Main `pom.xml`
   - Module POM files (parent reference)
4. Go version files: 
   - `api/internal/version/version.go`
   - `api/pkg/health/version.go`
   - `version-service/core/version.go`
5. Test scripts containing version references

## Checking Version Consistency

To verify that all files have consistent versions:

```bash
bin/version-manager.sh verify
```

This will check:
- All POM files for correct versions
- All Go version files
- The version-service properties file
- Other configuration files and scripts

## Logs and Backups

The version manager automatically creates:

- Logs: `backup/version-*/version-manager.log` files contain detailed operation logs
- Backups: `backup/version-*/` directories contain backups of all modified files

## Best Practices

1. Always use the version manager tools, never edit files manually
2. Run `bin/version-manager.sh verify` after version changes
3. Use XMLStarlet (via xml-tools.sh) for any POM file manipulations
4. Commit version changes in separate commits from code changes

## Related Documentation

- [Version Management](../docs/development/version-management.md)
- [Version Numbering](../docs/reference/standards/version-numbering.md)
- [XML Tools README](README-XML-TOOLS.md)
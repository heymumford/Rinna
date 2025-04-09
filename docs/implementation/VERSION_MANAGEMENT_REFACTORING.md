# Version Management System Refactoring

This document summarizes the refactoring of Rinna's version management system to use a centralized approach with XMLStarlet integration.

## Motivation

The previous version management system had several issues:

1. Inconsistent version handling across different file types
2. Unsafe manipulation of XML files using text-based tools like sed and grep
3. No centralized verification system to ensure consistency
4. No automated backup of modified files
5. Limited error handling and recovery mechanisms
6. Insufficient documentation on version management best practices

## Solution

We implemented a comprehensive refactoring of the version management system, focusing on these key improvements:

1. **Single Source of Truth**: Centralized all version management in `version-manager.sh`
2. **XMLStarlet Integration**: Used XMLStarlet for safe and precise XML file manipulation
3. **Verification System**: Created a comprehensive version consistency checker
4. **Backup System**: Implemented automatic backups before any modifications
5. **Detailed Logging**: Added comprehensive logging for troubleshooting
6. **Backward Compatibility**: Created wrapper scripts for legacy commands
7. **Comprehensive Documentation**: Updated all documentation to reflect the new approach

## Implementation Details

### New Components

1. **bin/version-manager.sh**: The central version management script that:
   - Provides a unified interface for all version operations
   - Uses XMLStarlet for XML file manipulation
   - Creates backups and logs for all operations
   - Verifies version consistency across all files

2. **bin/xml-tools.sh**: A library for XML manipulation that:
   - Provides functions for querying and updating XML content
   - Ensures safe handling of XML files
   - Prevents corruption of XML structure

3. **bin/README-VERSION-MANAGER.md**: New documentation for the version manager

### Updated Components

1. **update-versions.sh**: Updated to use version-manager.sh
2. **increment-build.sh**: Updated to use version-manager.sh
3. **check-versions.sh**: Updated to use version-manager.sh

### Documentation Updates

1. **docs/development/version-management.md**: Comprehensive update
2. **docs/reference/standards/version-numbering.md**: Added version management tools
3. **DEVELOPER.md**: Updated version management section
4. **README.md**: Updated version badges

## Key Features

### Centralized Version Management

All version-related operations are now handled through a single script:

```bash
# Show current version
bin/version-manager.sh current

# Verify consistency
bin/version-manager.sh verify

# Bump versions
bin/version-manager.sh bump major
bin/version-manager.sh bump minor
bin/version-manager.sh bump patch

# Set specific version
bin/version-manager.sh set 1.2.3

# Build number management
bin/version-manager.sh increment-build
bin/version-manager.sh set-build 100
```

### XMLStarlet Integration

All XML file manipulations now use XMLStarlet for safe handling:

```bash
# Get version from a POM file
xml_get_version "pom.xml"

# Set version in a POM file
xml_set_version "pom.xml" "1.2.3"

# Update parent reference in module POM
xmlstarlet ed -N "pom=http://maven.apache.org/POM/4.0.0" \
    -u "/pom:project/pom:parent/pom:version" -v "1.2.3" \
    "module-pom.xml"
```

### Backup and Logging

Every operation creates backups and detailed logs:

- Backups: `backup/version-*/` directories
- Logs: `backup/version-*/version-manager.log` files

### Verification System

The verification system checks consistency across:

1. Parent POM files
2. Module POM parent references
3. Go version files
4. Properties files
5. Test script references

## Best Practices

The refactoring established these best practices:

1. Always use the version-manager.sh for version-related operations
2. Never manually edit version.properties or POM files
3. Always use XMLStarlet for XML manipulation
4. Run `version-manager.sh verify` after version changes
5. Commit version changes in separate commits from code changes

## Results

The refactored version management system provides:

1. Consistent version information across all files
2. Safe handling of XML files using XMLStarlet
3. Comprehensive verification to detect inconsistencies
4. Clear documentation and examples for developers
5. Backward compatibility with existing scripts
6. Automated backup and recovery mechanisms

## Future Enhancements

Future work could include:

1. Adding custom pre-commit hooks for version verification
2. Creating GitHub Actions for automated version bumping
3. Enhancing release note generation from version changes
4. Adding component-specific versioning for microservices
5. Implementing pre-release and build metadata support
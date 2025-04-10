# XML Tools for Rinna Project

This directory contains XML-based tools for precisely manipulating and validating XML files (particularly Maven POM files) in the Rinna project.

## Overview

The XML tools use XMLStarlet for precise XML manipulation and validation, which provides several advantages over text-based tools like `sed` or `grep`:

- **Well-formed XML manipulation**: Changes respect the XML structure and won't break well-formedness
- **Namespace awareness**: Properly handles XML namespaces in Maven POM files
- **XPath-based selection**: Precise targeting of XML elements via XPath expressions
- **Consistent formatting**: Maintains proper indentation and formatting

## Tools

### xml-tools.sh

A utility library that provides functions for working with XML files:

- `xml_dependency_exists` - Check if a dependency exists in a POM file
- `xml_find_missing_test_scope` - Find dependencies without test scope
- `xml_add_test_scope` - Add test scope to a dependency
- `xml_remove_dependency` - Remove a dependency
- `xml_add_dependency` - Add a new dependency
- `xml_get_version` - Get the project version
- `xml_set_version` - Update the project version
- `xml_format_pom` - Format a POM file with proper indentation

### dependency-validator-xml.sh

Checks for circular dependencies and proper test scope in dependencies:

- Ensures modules only depend on modules lower in the dependency hierarchy
- Ensures test libraries are properly marked with `<scope>test</scope>`
- More precise than text-based validation

### fix-pom-dependencies-xml.sh

Fixes common issues in POM files:

- Removes duplicate dependencies
- Ensures test dependencies have proper test scope
- Follows clean XML editing practices

### update-version-xml.sh

Updates version numbers across the project:

- Updates versions in POM files (both project and parent versions)
- Updates version information in version.properties
- Creates backups before modifications
- Commits changes to git

### xml-cleanup.sh

Formats and validates all XML files in the repository:

- Formats XML files with proper indentation
- Validates XML syntax correctness
- Fixes common POM file issues (incorrect tags, escaped comments)
- Checks for dependency consistency across modules

### pom-n-tag-fixer.sh

Specialized script to fix the POM tag issue:

- Specifically targets the n vs. name tag issue in POM files
- More reliable solution compared to the generic cleanup
- Runs automatically before xml-cleanup.sh in the scheduled process

### xml-cleanup-scheduler.sh

Schedules automated XML cleanup to run periodically:

- Runs automatically after every build via integration with build.sh
- Executes full XML cleanup every 10 builds by default
- Tracks build count in .rinna-build-tracking/xml-cleanup-counter
- Can be run manually with --force flag to trigger immediate cleanup

## Usage

1. Source the XML tools library in your scripts:
   ```bash
   source "${PROJECT_ROOT}/bin/xml-tools.sh"
   ```

2. Use the utility scripts directly:
   ```bash
   bin/checks/dependency-validator-xml.sh
   bin/migration/fix-pom-dependencies-xml.sh
   bin/update-version-xml.sh 1.5.2
   ```

## Examples

### Check for a dependency
```bash
if xml_dependency_exists "pom.xml" "org.junit.jupiter" "junit-jupiter-api"; then
    echo "JUnit Jupiter API dependency exists"
fi
```

### Add a dependency with test scope
```bash
xml_add_dependency "pom.xml" "org.mockito" "mockito-core" "5.17.0" "test"
```

### Update project version
```bash
xml_set_version "pom.xml" "1.5.2"
```

### Run XML cleanup manually
```bash
# Run full cleanup (format, validate, check dependencies)
bin/xml-tools/xml-cleanup.sh

# Only validate XML files
bin/xml-tools/xml-cleanup.sh --validate-only

# Only check dependencies
bin/xml-tools/xml-cleanup.sh --deps-only

# Show detailed output
bin/xml-tools/xml-cleanup.sh --verbose
```

### Force immediate XML cleanup
```bash
# Force cleanup regardless of build counter
bin/xml-tools/xml-cleanup-scheduler.sh --force
```

## Advantages Over Text-Based Tools

1. **Safety**: Won't break XML structure, unlike `sed` which can create malformed XML
2. **Precision**: Uses XPath for exact targeting instead of pattern matching 
3. **Consistency**: Maintains proper XML structure and indentation
4. **Maintainability**: More readable and understandable code

## Requirements

- XMLStarlet (`apt-get install xmlstarlet` on Debian/Ubuntu)
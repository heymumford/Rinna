# XML Tools Standard for Rinna Project

## ⚠️ CRITICAL DEVELOPMENT STANDARD

**For all XML manipulation (especially POM files), ALWAYS use the XMLStarlet-based tools in `bin/xml-tools.sh`. NEVER use grep, sed, or other text-based tools for XML files.**

## Rationale

Text-manipulation tools like `grep`, `sed`, and `awk` are inappropriate for XML files because:

1. **XML is structured data** - not plain text
2. **XML has namespaces** - particularly important for Maven POM files
3. **Malformed XML breaks builds** - text tools easily create invalid XML

## Approved Tools

The following XMLStarlet-based tools are the ONLY approved methods for XML manipulation:

- `bin/xml-tools.sh` - Core utility library with XML manipulation functions
- `bin/update-version-xml.sh` - XML-aware version updater 
- `bin/checks/dependency-validator-xml.sh` - Dependency validation
- `bin/migration/fix-pom-dependencies-xml.sh` - POM dependency fixer

## Usage Examples

```bash
# Source the utilities in scripts
source "${PROJECT_ROOT}/bin/xml-tools.sh"

# Check for a dependency
if xml_dependency_exists "pom.xml" "org.junit.jupiter" "junit-jupiter-api"; then
    echo "JUnit Jupiter API dependency exists"
fi

# Add a dependency with test scope
xml_add_dependency "pom.xml" "org.mockito" "mockito-core" "5.17.0" "test"

# Update project version
xml_set_version "pom.xml" "1.5.2"

# Run the version updater
bin/update-version-xml.sh 1.5.3
```

## Enforcement

Violations of this standard will result in:
- Build/CI failures
- Pull request rejection
- Security warnings (as XML manipulation via text tools is a potential security risk)

For questions or assistance with XML manipulation, consult the README-XML-TOOLS.md document or contact the build system maintainer.
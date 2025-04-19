#!/bin/bash

# Exit immediately if a command exits with a non-zero status
set -e

echo "Starting project reorganization..."

# Create necessary directories
mkdir -p java/rinna-core java/rinna-cli java/rinna-data java/src
mkdir -p python/scripts/utils python/scripts/bin
mkdir -p go/src go/pkg
mkdir -p api-specs/swagger api-specs/openapi
mkdir -p config/shared config/java config/python config/go
mkdir -p scripts/build scripts/deploy scripts/utils

# Move all Java code to /java
echo "Moving Java code..."
[ -d "rinna-core" ] && mv rinna-core/* java/rinna-core/
[ -d "rinna-cli" ] && mv rinna-cli/* java/rinna-cli/
[ -d "rinna-data-sqlite" ] && mv rinna-data-sqlite/* java/rinna-data/
[ -d "src/main/java" ] && mv src/main/java/* java/src/
[ -d "java/rinna-core/target" ] && rm -rf java/rinna-core/target
[ -d "java/rinna-cli/target" ] && rm -rf java/rinna-cli/target

# Move main pom.xml to java directory
[ -f "pom.xml" ] && mv pom.xml java/
# Remove version backup files
find java -name "*.versionsBackup" -type f -delete

# Move Python scripts to python directory
echo "Moving Python code..."
[ -d "bin" ] && find bin -name "*.py" -type f -exec mv {} python/scripts/bin/ \;
[ -d "api/bin" ] && find api/bin -name "*.py" -type f -exec mv {} python/scripts/ \;
# Remove Python egg info directory
[ -d "rinna.egg-info" ] && rm -rf rinna.egg-info

# Move Go code to /go
echo "Moving Go code..."
[ -d "api/cmd" ] && mv api/cmd/* go/src/
[ -d "api/internal" ] && mv api/internal/* go/src/
[ -d "api/pkg" ] && mv api/pkg/* go/pkg/
[ -f "api/go.mod" ] && mv api/go.mod go/
[ -f "api/go.sum" ] && mv api/go.sum go/

# Move API specifications to /api-specs
echo "Moving API specifications..."
[ -d "api/specs" ] && mv api/specs/* api-specs/

# Consolidate configuration
echo "Consolidating configuration..."
[ -d "config/shared" ] && find config -maxdepth 1 -type f -exec mv {} config/shared/ \;
[ -d "config/java" ] && find config -name "*.xml" -o -name "*.properties" | grep -v "shared" | xargs -I{} mv {} config/java/
[ -d "config/python" ] && find config -name "*.py" -o -name "*.yaml" -o -name "*.yml" | grep -v "shared" | xargs -I{} mv {} config/python/
[ -f "version.properties" ] && mv version.properties config/shared/

# Consolidate scripts
echo "Consolidating scripts..."
[ -d "scripts" ] && find scripts -name "*.sh" -type f -exec mv {} scripts/utils/ \;
[ -d "bin" ] && find bin -name "*.sh" -type f -exec mv {} scripts/utils/ \;

# Move shell scripts
echo "Moving shell scripts..."
[ -f "build.sh" ] && mv build.sh scripts/build/
[ -f "movestuff.sh" ] && mv movestuff.sh scripts/utils/
[ -f "reorg.sh" ] && mv reorg.sh scripts/utils/

# Fix execution permissions
find scripts -name "*.sh" -type f -exec chmod +x {} \;

# Remove empty directories
echo "Cleaning up empty directories..."
find . -type d -empty -delete

echo "Project reorganization complete!"
echo "=================================================="
echo "New structure:"
echo "- /java        - All Java code"
echo "- /python      - All Python code"
echo "- /go          - All Go code"
echo "- /api-specs   - API specifications"
echo "- /config      - Configuration files"
echo "- /scripts     - Utility scripts"
echo "- /docs        - Documentation"
echo "=================================================="
echo "NOTE: You should review the changes and make sure everything is in order."
echo "You may need to update import paths and references in your code."


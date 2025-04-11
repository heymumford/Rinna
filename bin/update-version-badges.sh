#!/bin/bash
#
# Script to update version and build badges in README.md
# This should be integrated with the version management system
#

set -e

SCRIPT_DIR="$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
VERSION_FILE="$PROJECT_ROOT/version.properties"
README_FILE="$PROJECT_ROOT/README.md"

# Check if required files exist
if [ ! -f "$VERSION_FILE" ]; then
    echo "Error: $VERSION_FILE not found"
    exit 1
fi

if [ ! -f "$README_FILE" ]; then
    echo "Error: $README_FILE not found"
    exit 1
fi

# Read version information from version.properties
VERSION=$(grep "^version=" "$VERSION_FILE" | cut -d= -f2)
BUILD_NUMBER=$(grep "^buildNumber=" "$VERSION_FILE" | cut -d= -f2)

echo "Updating README badges with version $VERSION and build $BUILD_NUMBER"

# Update version badge
sed -i "s/\(version-\)[0-9]\+\.[0-9]\+\.[0-9]\+/\1$VERSION/g" "$README_FILE"

# Update build badge
sed -i "s/\(build-\)[0-9]\+/\1$BUILD_NUMBER/g" "$README_FILE" 

echo "README badges updated successfully!"
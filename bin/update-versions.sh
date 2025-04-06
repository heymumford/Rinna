#!/bin/bash
# Script to update version information across all languages in the Rinna project
# This script updates all version references based on the version.properties file
# Called by rin-version but can also be used directly

set -e

# Get the project root directory
PROJECT_ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd )"
cd "$PROJECT_ROOT"

# Color definitions
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Get version information from version.properties
VERSION=$(grep "^version=" version.properties | cut -d'=' -f2)
VERSION_MAJOR=$(grep "^version.major=" version.properties | cut -d'=' -f2)
VERSION_MINOR=$(grep "^version.minor=" version.properties | cut -d'=' -f2)
VERSION_PATCH=$(grep "^version.patch=" version.properties | cut -d'=' -f2)
BUILD_TIMESTAMP=$(date -u '+%Y-%m-%dT%H:%M:%SZ')

# Check if version is valid
if [[ ! $VERSION =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo -e "${RED}ERROR: Invalid version format in version.properties: $VERSION${NC}"
    echo "Version must be in the format x.y.z"
    exit 1
fi

echo -e "${BLUE}=== Rinna Version Update Tool ===${NC}"
echo "Updating all version references to: $VERSION"
echo "Build timestamp: $BUILD_TIMESTAMP"
echo ""

# Update Java POM files
update_pom() {
    local file=$1
    local xpath=$2
    
    echo -e "${BLUE}Updating $file...${NC}"
    
    # Use xmlstarlet if available, otherwise fall back to sed
    if command -v xmlstarlet &> /dev/null; then
        xmlstarlet ed -L -u "$xpath" -v "$VERSION" "$file"
    else
        # Use sed as a fallback
        if [[ "$xpath" == "/project/version" ]]; then
            sed -i "s/<version>[0-9]\+\.[0-9]\+\.[0-9]\+<\/version>/<version>$VERSION<\/version>/" "$file"
        elif [[ "$xpath" == "/project/parent/version" ]]; then
            sed -i "0,/<version>[0-9]\+\.[0-9]\+\.[0-9]\+<\/version>/s//<version>$VERSION<\/version>/" "$file"
        fi
    fi
    
    echo -e "${GREEN}✓ Updated $file${NC}"
}

# Update Go version files
update_go_version() {
    local file=$1
    
    echo -e "${BLUE}Updating $file...${NC}"
    
    # Update version string
    sed -i "s/Version[[:space:]]*=[[:space:]]*\"[0-9]\+\.[0-9]\+\.[0-9]\+\"/Version   = \"$VERSION\"/" "$file"
    
    # Update build timestamp
    sed -i "s/BuildTime[[:space:]]*=[[:space:]]*\"[0-9]\+-[0-9]\+-[0-9]\+T[0-9]\+:[0-9]\+:[0-9]\+Z\"/BuildTime = \"$BUILD_TIMESTAMP\"/" "$file"
    
    echo -e "${GREEN}✓ Updated $file${NC}"
}

# Update YAML config files
update_yaml() {
    local file=$1
    local key=$2
    
    echo -e "${BLUE}Updating $file...${NC}"
    
    # Check if the key exists
    if grep -q "$key" "$file"; then
        # Update existing key
        sed -i "s/$key[[:space:]]*\"[0-9]\+\.[0-9]\+\.[0-9]\+\"/$key \"$VERSION\"/" "$file"
    else
        # Add new section
        sed -i "1s/^/# Automatically added by update-versions.sh\nproject:\n  name: \"Rinna\"\n  version: \"$VERSION\"\n  environment: \"development\"\n\n/" "$file"
    fi
    
    echo -e "${GREEN}✓ Updated $file${NC}"
}

# Update documentation files
update_docs() {
    local file=$1
    local pattern=$2
    local replacement=$3
    
    echo -e "${BLUE}Updating $file...${NC}"
    
    if grep -q "$pattern" "$file"; then
        sed -i "s/$pattern/$replacement/g" "$file"
        echo -e "${GREEN}✓ Updated $file${NC}"
    else
        echo -e "${YELLOW}WARNING: Pattern not found in $file${NC}"
    fi
}

echo "Updating Java files..."
update_pom "pom.xml" "/project/version"
update_pom "rinna-core/pom.xml" "/project/parent/version"

echo "Updating Go files..."
update_go_version "api/internal/version/version.go"
update_go_version "api/pkg/health/version.go"

echo "Updating YAML configuration files..."
update_yaml "api/configs/config.yaml" "version:"

echo "Updating documentation files..."
update_docs "docs/development/configuration.md" "current project version is \*\*[0-9]\+\.[0-9]\+\.[0-9]\+\*\*" "current project version is **$VERSION**"
update_docs "docs/development/configuration.md" "| \`project.version\` | Project version | \"[0-9]\+\.[0-9]\+\.[0-9]\+\" |" "| \`project.version\` | Project version | \"$VERSION\" |"

echo ""
echo -e "${GREEN}Version updates complete!${NC}"
echo "Running version check to verify consistency..."
echo ""

# Run the version check to verify everything is consistent
./bin/check-versions.sh
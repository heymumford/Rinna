#!/bin/bash
# Enhanced version updater for Rinna cross-language architecture
# Updates all version references based on version.properties across Java, Go, and Python
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

# Version properties file
VERSION_PROPS="$PROJECT_ROOT/version.properties"

# Get version information from version.properties
VERSION=$(grep "^version=" $VERSION_PROPS | cut -d'=' -f2)
VERSION_MAJOR=$(grep "^version.major=" $VERSION_PROPS | cut -d'=' -f2)
VERSION_MINOR=$(grep "^version.minor=" $VERSION_PROPS | cut -d'=' -f2)
VERSION_PATCH=$(grep "^version.patch=" $VERSION_PROPS | cut -d'=' -f2)
BUILD_TIMESTAMP=$(date -u '+%Y-%m-%dT%H:%M:%SZ')

# Check if version is valid
if [[ ! $VERSION =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo -e "${RED}ERROR: Invalid version format in version.properties: $VERSION${NC}"
    echo "Version must be in the format x.y.z"
    exit 1
fi

echo -e "${BLUE}=== Rinna Version Update Tool ===${NC}"
echo "Updating all version references across Java, Go, and Python..."
echo "Version from version.properties: ${GREEN}$VERSION${NC}"
echo "Build timestamp: $BUILD_TIMESTAMP"
echo ""

# Function to update all POM files in the project
function update_all_pom_files() {
    echo -e "${BLUE}Updating Java/Maven POM files...${NC}"
    
    # Find all pom.xml files in the project
    pom_files=$(find "$PROJECT_ROOT" -name "pom.xml" -type f)
    
    for pom in $pom_files; do
        # Check if it's a Rinna project (contains org.rinna groupId)
        if grep -q "<groupId>org.rinna</groupId>" "$pom"; then
            echo "  Updating $pom"
            
            # Update version in groupId section
            if grep -q "<groupId>org.rinna</groupId>" "$pom" && grep -q "<version>" "$pom"; then
                # Use xmlstarlet if available, otherwise fall back to sed
                if command -v xmlstarlet &> /dev/null; then
                    xmlstarlet ed -L -u "//project[groupId='org.rinna']/version" -v "$VERSION" "$pom"
                else
                    sed -i -E "s|(<groupId>org\.rinna</groupId>[\s\S]*?<version>)[0-9]+\.[0-9]+\.[0-9]+(</version>)|\1$VERSION\2|g" "$pom"
                fi
            fi
            
            # Update parent version if it references org.rinna
            if grep -q "<parent>" "$pom" && grep -q "<groupId>org.rinna</groupId>" "$pom"; then
                if command -v xmlstarlet &> /dev/null; then
                    xmlstarlet ed -L -u "//parent[groupId='org.rinna']/version" -v "$VERSION" "$pom"
                else
                    sed -i -E "s|(<parent>[\s\S]*?<groupId>org\.rinna</groupId>[\s\S]*?<version>)[0-9]+\.[0-9]+\.[0-9]+(</version>[\s\S]*?</parent>)|\1$VERSION\2|g" "$pom"
                fi
            fi
        fi
    done
    
    echo -e "${GREEN}✓ Java/Maven POM files updated${NC}"
}

# Function to update Go version files
function update_go_files() {
    echo -e "${BLUE}Updating Go version files...${NC}"
    
    # Common Go version files
    go_files=(
        "$PROJECT_ROOT/api/internal/version/version.go"
        "$PROJECT_ROOT/api/pkg/health/version.go"
    )
    
    # Find any additional version.go files
    while IFS= read -r file; do
        go_files+=("$file")
    done < <(find "$PROJECT_ROOT/api" -name "version.go" -type f | grep -v -E "/(internal/version|pkg/health)/")
    
    for file in "${go_files[@]}"; do
        if [ -f "$file" ]; then
            echo "  Updating $file"
            
            # Update version string
            sed -i -E "s|(Version[[:space:]]*=[[:space:]]*\")[0-9]+\.[0-9]+\.[0-9]+(\")|\1$VERSION\2|g" "$file"
            
            # Update build timestamp
            sed -i -E "s|(BuildTime[[:space:]]*=[[:space:]]*\")[^\"]+(\")|\1$BUILD_TIMESTAMP\2|g" "$file"
        fi
    done
    
    # Update YAML config files
    yaml_files=(
        "$PROJECT_ROOT/api/configs/config.yaml"
    )
    
    for file in "${yaml_files[@]}"; do
        if [ -f "$file" ]; then
            echo "  Updating $file"
            
            # Check if the version key exists
            if grep -q "version:" "$file"; then
                sed -i -E "s|(version:[[:space:]]*\"*)[0-9]+\.[0-9]+\.[0-9]+(\"*)|\1$VERSION\2|g" "$file"
            else
                # Add new project section if it doesn't exist
                project_section="# Automatically added by update-versions.sh
project:
  name: \"Rinna\"
  version: \"$VERSION\"
  environment: \"development\"
"
                # Add the section at the beginning of the file
                sed -i "1s|^|$project_section\n\n|" "$file"
            fi
        fi
    done
    
    echo -e "${GREEN}✓ Go files updated${NC}"
}

# Function to update Python version files
function update_python_files() {
    echo -e "${BLUE}Updating Python version files...${NC}"
    
    # Update pyproject.toml
    pyproject="$PROJECT_ROOT/pyproject.toml"
    if [ -f "$pyproject" ]; then
        echo "  Updating $pyproject"
        
        if grep -q "version" "$pyproject"; then
            sed -i -E "s|(version[[:space:]]*=[[:space:]]*\")[0-9]+\.[0-9]+\.[0-9]+(\"[[:space:]]*)|\1$VERSION\2|g" "$pyproject"
        else
            # If no version field found, check if there's a [tool.poetry] section
            if grep -q "\[tool.poetry\]" "$pyproject"; then
                sed -i "/\[tool.poetry\]/a version = \"$VERSION\"" "$pyproject"
            else
                echo -e "${YELLOW}WARNING: No version field or [tool.poetry] section found in pyproject.toml${NC}"
            fi
        fi
    else
        echo -e "${YELLOW}WARNING: pyproject.toml not found${NC}"
    fi
    
    # Update Python version files
    py_version_files=(
        "$PROJECT_ROOT/python/rinna/__init__.py"
        "$PROJECT_ROOT/bin/rinna_config.py"
    )
    
    for file in "${py_version_files[@]}"; do
        if [ -f "$file" ]; then
            echo "  Updating $file"
            
            # Check for __version__ pattern
            if grep -q "__version__" "$file"; then
                sed -i -E "s|(__version__[[:space:]]*=[[:space:]]*[\"'])[0-9]+\.[0-9]+\.[0-9]+([\"'][[:space:]]*)|\1$VERSION\2|g" "$file"
            fi
            
            # Check for VERSION pattern
            if grep -q "VERSION" "$file"; then
                sed -i -E "s|(VERSION[[:space:]]*=[[:space:]]*[\"'])[0-9]+\.[0-9]+\.[0-9]+([\"'][[:space:]]*)|\1$VERSION\2|g" "$file"
            fi
        fi
    done
    
    # Update virtual env version file
    venv_dir="$PROJECT_ROOT/.venv"
    venv_version="$venv_dir/version"
    
    if [ -d "$venv_dir" ]; then
        echo "  Updating $venv_version"
        echo "$VERSION" > "$venv_version"
    fi
    
    echo -e "${GREEN}✓ Python files updated${NC}"
}

# Function to update documentation files
function update_documentation() {
    echo -e "${BLUE}Updating documentation files...${NC}"
    
    # Update README.md
    readme="$PROJECT_ROOT/README.md"
    if [ -f "$readme" ]; then
        echo "  Updating $readme"
        
        # Check for version badge
        if grep -q "badge/version" "$readme"; then
            sed -i -E "s|(badge/version-)[0-9]+\.[0-9]+\.[0-9]+|\1$VERSION|g" "$readme"
        fi
        
        # Check for other version patterns
        sed -i -E "s|(Current version: \*\*)[0-9]+\.[0-9]+\.[0-9]+(\*\*)|\1$VERSION\2|g" "$readme"
    fi
    
    # Update common documentation files
    doc_files=(
        "$PROJECT_ROOT/docs/development/configuration.md"
        "$PROJECT_ROOT/docs/user-guide/README.md"
    )
    
    for file in "${doc_files[@]}"; do
        if [ -f "$file" ]; then
            echo "  Updating $file"
            
            # Replace common version patterns
            sed -i -E "s|(current project version is \*\*)[0-9]+\.[0-9]+\.[0-9]+(\*\*)|\1$VERSION\2|g" "$file"
            sed -i -E "s|(\`project\.version\` \| Project version \| \")[0-9]+\.[0-9]+\.[0-9]+(\")|\1$VERSION\2|g" "$file"
            sed -i -E "s|([Vv]ersion[[:space:]]*)[0-9]+\.[0-9]+\.[0-9]+([[:space:]])|\1$VERSION\2|g" "$file"
        fi
    done
    
    echo -e "${GREEN}✓ Documentation updated${NC}"
}

# Execute all update functions
update_all_pom_files
update_go_files
update_python_files
update_documentation

echo ""
echo -e "${GREEN}Version updates complete!${NC}"
echo "Running version check to verify consistency..."
echo ""

# Run the version check to verify consistency
"$PROJECT_ROOT/bin/check-versions.sh"
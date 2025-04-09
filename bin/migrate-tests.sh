#!/bin/bash
#
# migrate-tests.sh - Script to migrate tests to the standardized test structure
#

set -e

BASEDIR=$(dirname "$(readlink -f "$0")")/..
cd "$BASEDIR"

# Define colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo -e "${BLUE}Starting test migration to standardized structure...${NC}"

# Create directories if they don't exist
mkdir -p src/test/java/org/rinna/unit
mkdir -p src/test/java/org/rinna/component
mkdir -p src/test/java/org/rinna/integration
mkdir -p src/test/java/org/rinna/acceptance/steps
mkdir -p src/test/java/org/rinna/performance

# Function to identify test type from filename and content
identify_test_type() {
    local file=$1
    local filename=$(basename "$file")
    local content=$(cat "$file")
    
    # Check for explicit tags
    if grep -q "@Tag(\"unit\")" "$file"; then
        echo "unit"
    elif grep -q "@Tag(\"component\")" "$file"; then
        echo "component"
    elif grep -q "@Tag(\"integration\")" "$file"; then
        echo "integration" 
    elif grep -q "@Tag(\"acceptance\")" "$file"; then
        echo "acceptance"
    elif grep -q "@Tag(\"performance\")" "$file"; then
        echo "performance"
    # Check for filename patterns
    elif [[ "$filename" =~ (Unit|Unit.*)Test\.java$ ]]; then
        echo "unit"
    elif [[ "$filename" =~ Component.*Test\.java$ ]]; then
        echo "component"
    elif [[ "$filename" =~ Integration.*Test\.java$ ]]; then
        echo "integration"
    elif [[ "$filename" =~ Acceptance.*Test\.java$ ]]; then
        echo "acceptance"
    elif [[ "$filename" =~ (Performance|Perf.*)Test\.java$ ]]; then
        echo "performance"
    # Check for content patterns
    elif grep -q "extends UnitTest" "$file"; then
        echo "unit"
    elif grep -q "extends ComponentTest" "$file"; then
        echo "component"
    elif grep -q "extends IntegrationTest" "$file"; then
        echo "integration"
    elif grep -q "extends AcceptanceTest" "$file"; then
        echo "acceptance"
    elif grep -q "extends PerformanceTest" "$file"; then
        echo "performance"
    # Check for Cucumber/BDD tests
    elif grep -q "@RunWith(Cucumber.class)" "$file" || grep -q "import io.cucumber" "$file"; then
        echo "acceptance"
    # Check for specific test characteristics
    elif grep -q "mock" "$file" && ! grep -q "integration" "$file"; then
        echo "unit"
    else
        # Default to unit test if we can't determine
        echo "unit"
    fi
}

# Function to update a test file to conform to standardized structure
update_test_file() {
    local source_file=$1
    local test_type=$2
    local base_name=$(basename "$source_file")
    local package_name=$(grep -o "package .*;" "$source_file" | sed 's/package //g' | sed 's/;//g' | tr -d '[:space:]')
    local simple_name=$(echo "$base_name" | sed 's/\.java$//')
    
    # Determine new package name
    local new_package_name="org.rinna.$test_type"
    if [[ "$package_name" =~ org\.rinna\..*$ ]]; then
        # Extract the subpackage (if any)
        local subpackage=$(echo "$package_name" | sed -E 's/org\.rinna\.([^.]*\.)?//')
        if [[ ! -z "$subpackage" ]]; then
            new_package_name="org.rinna.$test_type.$subpackage"
        fi
    fi
    
    # Determine new file name based on test type
    local new_name="$simple_name"
    case "$test_type" in
        component)
            if [[ ! "$new_name" =~ ComponentTest$ ]]; then
                new_name="${simple_name}ComponentTest"
            fi
            ;;
        integration)
            if [[ ! "$new_name" =~ IntegrationTest$ ]]; then
                new_name="${simple_name}IntegrationTest"
            fi
            ;;
        acceptance)
            if [[ ! "$new_name" =~ (AcceptanceTest|Runner|Steps)$ ]]; then
                new_name="${simple_name}AcceptanceTest"
            fi
            ;;
        performance)
            if [[ ! "$new_name" =~ PerformanceTest$ ]]; then
                new_name="${simple_name}PerformanceTest"
            fi
            ;;
    esac
    
    # Create new package directory if it doesn't exist
    local package_dir=$(echo "$new_package_name" | tr '.' '/')
    mkdir -p "src/test/java/$package_dir"
    
    # Determine new file path
    local new_file="src/test/java/$package_dir/$new_name.java"
    
    # If the file is already in the correct structure, skip it
    if [[ "$source_file" == "$new_file" ]]; then
        echo -e "${YELLOW}File already in correct structure, skipping: $source_file${NC}"
        return
    fi
    
    echo -e "${GREEN}Migrating:${NC} $source_file -> $new_file"
    
    # Update package name and add tag if needed
    local file_content=$(cat "$source_file")
    local new_content
    
    # Update package
    new_content=$(echo "$file_content" | sed -E "s/package $package_name;/package $new_package_name;/")
    
    # Check if it already has a tag annotation
    if ! grep -q "@Tag" "$source_file"; then
        # Add tag based on test type - using sed instead of perl
        new_content=$(echo "$new_content" | sed -E "s/public class $simple_name([^{]*)\{/@Tag(\"$test_type\")\npublic class $new_name\1{/")
    fi
    
    # Update class name if it was changed
    if [[ "$simple_name" != "$new_name" ]]; then
        new_content=$(echo "$new_content" | sed -E "s/class $simple_name([^{]*)\{/class $new_name\1{/")
    fi
    
    # Remove "extends BaseTest" if present
    case "$test_type" in
        unit)
            new_content=$(echo "$new_content" | sed -E "s/extends UnitTest//g")
            ;;
        component)
            new_content=$(echo "$new_content" | sed -E "s/extends ComponentTest//g")
            ;;
        integration)
            new_content=$(echo "$new_content" | sed -E "s/extends IntegrationTest//g")
            ;;
        acceptance)
            new_content=$(echo "$new_content" | sed -E "s/extends AcceptanceTest//g")
            ;;
        performance)
            new_content=$(echo "$new_content" | sed -E "s/extends PerformanceTest//g")
            ;;
    esac
    
    # Fix imports if needed - remove base class imports
    new_content=$(echo "$new_content" | sed -E "s/import .*\.(Unit|Component|Integration|Acceptance|Performance)Test;//g")
    
    # Add Tag import if needed
    if ! grep -q "import org.junit.jupiter.api.Tag;" "$source_file" && grep -q "@Tag" <<< "$new_content"; then
        # Using sed instead of perl
        new_content=$(echo "$new_content" | sed -E 's/import org.junit.jupiter.api.([^;]*);/import org.junit.jupiter.api.\1;\nimport org.junit.jupiter.api.Tag;/')
    fi
    
    # Write the updated content to the new file
    mkdir -p "$(dirname "$new_file")"
    echo "$new_content" > "$new_file"
    echo -e "${GREEN}Successfully migrated:${NC} $source_file -> $new_file"
}

# Process all test files
echo -e "${BLUE}Scanning for test files...${NC}"

# Find all test files (excluding generated, target directories)
test_files=$(find ./src ./rinna-core/src ./rinna-cli/src -name "*Test.java" -o -name "*TestCase.java" -o -name "*Runner.java" -o -name "*Steps.java" | grep -v "/target/" | grep -v "generated")

# Migrate each test
for file in $test_files; do
    # Skip files that are already in the new structure
    if [[ "$file" =~ src/test/java/org/rinna/(unit|component|integration|acceptance|performance)/ ]]; then
        echo -e "${YELLOW}Skipping already migrated file: $file${NC}"
        continue
    fi
    
    test_type=$(identify_test_type "$file")
    echo -e "${BLUE}Identified as ${CYAN}$test_type${BLUE} test:${NC} $file"
    
    update_test_file "$file" "$test_type"
done

echo -e "${GREEN}Test migration completed.${NC}"
echo -e "${BLUE}You may need to manually review and adjust some files.${NC}"
echo -e "${PURPLE}Next steps:${NC}"
echo -e "1. Compile the tests to make sure they are valid"
echo -e "2. Run the tests with the appropriate command:"
echo -e "   ${CYAN}./bin/rin-test unit${NC}"
echo -e "   ${CYAN}./bin/rin-test component${NC}"
echo -e "   ${CYAN}./bin/rin-test integration${NC}"
echo -e "   ${CYAN}./bin/rin-test acceptance${NC}"
echo -e "   ${CYAN}./bin/rin-test performance${NC}"
echo -e "3. Update CI/CD pipelines to use the standardized test commands"
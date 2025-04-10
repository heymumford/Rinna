#!/bin/bash
#
# XML files cleanup and validation script for Rinna
#
# This script formats and validates all XML files in the repository,
# with special handling for POM files to ensure proper alignment and
# cross-dependencies.
#
# Usage: ./bin/xml-tools/xml-cleanup.sh [--validate-only] [--format-only]
#

# Set script to exit on error
set -e

# Source common utilities if available
if [ -f "bin/common/rinna_logger.sh" ]; then
    source bin/common/rinna_logger.sh
else
    # Simple logging functions if common utilities are not available
    log_info() { echo -e "\033[0;34m[INFO]\033[0m $1"; }
    log_success() { echo -e "\033[0;32m[SUCCESS]\033[0m $1"; }
    log_warning() { echo -e "\033[0;33m[WARNING]\033[0m $1"; }
    log_error() { echo -e "\033[0;31m[ERROR]\033[0m $1"; }
fi

# Default behavior
VALIDATE=true
FORMAT=true
CHECK_DEPS=true
VERBOSE=false

# Parse command line arguments
for arg in "$@"; do
    case $arg in
        --validate-only)
            FORMAT=false
            CHECK_DEPS=false
            shift
            ;;
        --format-only)
            VALIDATE=false
            CHECK_DEPS=false
            shift
            ;;
        --deps-only)
            VALIDATE=false
            FORMAT=false
            shift
            ;;
        --verbose)
            VERBOSE=true
            shift
            ;;
        --help)
            echo "Usage: $0 [--validate-only] [--format-only] [--deps-only] [--verbose]"
            echo ""
            echo "Options:"
            echo "  --validate-only  Only validate XML files, don't format or check dependencies"
            echo "  --format-only    Only format XML files, don't validate or check dependencies"
            echo "  --deps-only      Only check dependencies in POM files"
            echo "  --verbose        Show more detailed output"
            echo "  --help           Show this help message"
            exit 0
            ;;
    esac
done

# Check if xmlstarlet is installed
if ! command -v xmlstarlet &> /dev/null; then
    log_error "xmlstarlet is not installed. Please install it first."
    exit 1
fi

# Define a function to format an XML file
format_xml_file() {
    local file=$1
    if [ "$VERBOSE" = true ]; then
        log_info "Formatting $file"
    fi
    
    # Create a temporary file
    local temp_file=$(mktemp)
    
    # Format the file
    if xmlstarlet fo -s 2 -o "$temp_file" "$file" 2>/dev/null; then
        # Check if the file has changed
        if ! cmp -s "$temp_file" "$file"; then
            mv "$temp_file" "$file"
            log_info "Formatted $file"
            return 0
        else
            rm "$temp_file"
            if [ "$VERBOSE" = true ]; then
                log_info "No changes needed for $file"
            fi
            return 0
        fi
    else
        rm "$temp_file"
        log_error "Failed to format $file - possible syntax error"
        validate_xml_file "$file"
        return 1
    fi
}

# Define a function to validate an XML file
validate_xml_file() {
    local file=$1
    if [ "$VERBOSE" = true ]; then
        log_info "Validating $file"
    fi
    
    local validation_result
    validation_result=$(xmlstarlet val "$file" 2>&1)
    
    if echo "$validation_result" | grep -q "validates"; then
        if [ "$VERBOSE" = true ]; then
            log_success "$file validates"
        fi
        return 0
    else
        log_error "$file does not validate:"
        echo "$validation_result" | sed 's/^/    /'
        return 1
    fi
}

# Define a function to fix common POM issues
fix_pom_issues() {
    local file=$1
    if [ "$VERBOSE" = true ]; then
        log_info "Fixing common issues in $file"
    fi
    
    # Replace <n> tag with <name> if needed
    local temp_file=$(mktemp)
    sed 's/<n>/<name>/g; s/<\/n>/<\/name>/g' "$file" > "$temp_file"
    
    # Check if the file has changed
    if ! cmp -s "$temp_file" "$file"; then
        mv "$temp_file" "$file"
        log_info "Fixed <n> tag issue in $file"
    else
        rm "$temp_file"
    fi
    
    # Fix XML comments with backslashes
    temp_file=$(mktemp)
    sed 's/<\\!--/<!--/g; s/--\\>/-->/g' "$file" > "$temp_file"
    
    # Check if the file has changed
    if ! cmp -s "$temp_file" "$file"; then
        mv "$temp_file" "$file"
        log_info "Fixed XML comment issue in $file"
    else
        rm "$temp_file"
    fi
}

# Define a function to check POM dependencies for consistency
check_pom_dependencies() {
    local file=$1
    if [ "$VERBOSE" = true ]; then
        log_info "Checking dependencies in $file"
    fi
    
    # Extract the parent version
    local parent_version=$(xmlstarlet sel -t -v "/project/parent/version" "$file" 2>/dev/null)
    
    # Extract all dependency versions that don't use properties
    local deps_with_versions=$(xmlstarlet sel -t -m "/project/dependencies/dependency[version and not(contains(version, '$'))]" -v "concat(groupId, ':', artifactId, ':', version)" -n "$file" 2>/dev/null)
    
    # Check for inconsistencies with parent version
    if [ -n "$parent_version" ] && [ -n "$deps_with_versions" ]; then
        echo "$deps_with_versions" | while read dep; do
            if [[ "$dep" == org.rinna:* ]]; then
                IFS=':' read -r group artifact version <<< "$dep"
                if [ "$version" != "$parent_version" ]; then
                    log_warning "Dependency version mismatch in $file: $group:$artifact uses version $version, parent is $parent_version"
                    log_info "Consider using \${project.version} for internal dependencies"
                fi
            fi
        done
    fi
}

# Find all XML files
xml_files=$(find . -name "*.xml" -type f -not -path "./target/*" -not -path "*/target/*" -not -path "*/.git/*")
pom_files=$(find . -name "pom.xml" -type f -not -path "./target/*" -not -path "*/target/*" -not -path "*/.git/*")

# Count files
xml_count=$(echo "$xml_files" | wc -l)
pom_count=$(echo "$pom_files" | wc -l)

log_info "Found $xml_count XML files ($pom_count POM files)"

# Fix common issues in POM files first
if [ "$FORMAT" = true ]; then
    for pom_file in $pom_files; do
        fix_pom_issues "$pom_file"
    done
fi

# Format XML files if requested
if [ "$FORMAT" = true ]; then
    formatted_count=0
    failed_format_count=0
    
    for xml_file in $xml_files; do
        if format_xml_file "$xml_file"; then
            ((formatted_count++))
        else
            ((failed_format_count++))
        fi
    done
    
    if [ "$failed_format_count" -eq 0 ]; then
        log_success "Successfully formatted $formatted_count XML files"
    else
        log_warning "Formatted $formatted_count XML files, $failed_format_count files had formatting errors"
    fi
fi

# Validate XML files if requested
if [ "$VALIDATE" = true ]; then
    valid_count=0
    invalid_count=0
    
    for xml_file in $xml_files; do
        if validate_xml_file "$xml_file"; then
            ((valid_count++))
        else
            ((invalid_count++))
        fi
    done
    
    if [ "$invalid_count" -eq 0 ]; then
        log_success "All $valid_count XML files are valid"
    else
        log_warning "$valid_count XML files are valid, $invalid_count files are invalid"
    fi
fi

# Check POM dependencies if requested
if [ "$CHECK_DEPS" = true ]; then
    for pom_file in $pom_files; do
        check_pom_dependencies "$pom_file"
    done
    log_info "Dependency check completed for $pom_count POM files"
fi

# Summary
log_success "XML cleanup and validation completed"
exit 0
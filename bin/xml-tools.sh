#!/bin/bash

# XML Tools for Maven POM files
# This script provides utility functions for working with Maven POM files using XMLStarlet

# Check if XMLStarlet is installed
if ! command -v xmlstarlet &> /dev/null; then
    echo "Error: XMLStarlet is not installed. Please install it first."
    exit 1
fi

# Function to check if a dependency exists in a POM file
# Usage: xml_dependency_exists "/path/to/pom.xml" "groupId" "artifactId"
xml_dependency_exists() {
    local pom_file="$1"
    local group_id="$2"
    local artifact_id="$3"
    
    if [ ! -f "${pom_file}" ]; then
        echo "Error: POM file does not exist: ${pom_file}"
        return 1
    fi
    
    # Count dependencies matching the criteria
    local count=$(xmlstarlet sel -N "pom=http://maven.apache.org/POM/4.0.0" -t \
        -v "count(//pom:dependency[pom:groupId='${group_id}' and pom:artifactId='${artifact_id}'])" \
        "${pom_file}" 2>/dev/null)
    
    # Return success if count > 0
    [[ "$count" -gt 0 ]]
}

# Function to find dependencies without test scope
# Usage: xml_find_missing_test_scope "/path/to/pom.xml" "junit|mockito|assertj"
xml_find_missing_test_scope() {
    local pom_file="$1"
    local pattern="$2"
    
    if [ ! -f "${pom_file}" ]; then
        echo "Error: POM file does not exist: ${pom_file}"
        return 1
    fi
    
    # Find dependencies matching pattern without test scope
    xmlstarlet sel -N "pom=http://maven.apache.org/POM/4.0.0" -t \
        -m "//pom:dependency[contains(pom:artifactId, '${pattern}') and not(pom:scope='test')]" \
        -v "concat('        <artifactId>', pom:artifactId, '</artifactId>')" -n \
        "${pom_file}" 2>/dev/null
}

# Function to add test scope to a dependency
# Usage: xml_add_test_scope "/path/to/pom.xml" "groupId" "artifactId"
xml_add_test_scope() {
    local pom_file="$1"
    local group_id="$2"
    local artifact_id="$3"
    local temp_file="${pom_file}.tmp"
    
    if [ ! -f "${pom_file}" ]; then
        echo "Error: POM file does not exist: ${pom_file}"
        return 1
    fi
    
    # Add test scope to matching dependencies that don't have a scope
    xmlstarlet ed -N "pom=http://maven.apache.org/POM/4.0.0" \
        --subnode "//pom:dependency[pom:groupId='${group_id}' and pom:artifactId='${artifact_id}' and not(pom:scope)]" \
        -t elem -n "scope" -v "test" \
        "${pom_file}" > "${temp_file}"
    
    # Check if the edit was successful
    if [ $? -eq 0 ]; then
        mv "${temp_file}" "${pom_file}"
        return 0
    else
        rm -f "${temp_file}"
        return 1
    fi
}

# Function to remove a dependency
# Usage: xml_remove_dependency "/path/to/pom.xml" "groupId" "artifactId"
xml_remove_dependency() {
    local pom_file="$1"
    local group_id="$2"
    local artifact_id="$3"
    local temp_file="${pom_file}.tmp"
    
    if [ ! -f "${pom_file}" ]; then
        echo "Error: POM file does not exist: ${pom_file}"
        return 1
    fi
    
    # Remove matching dependencies
    xmlstarlet ed -N "pom=http://maven.apache.org/POM/4.0.0" \
        -d "//pom:dependency[pom:groupId='${group_id}' and pom:artifactId='${artifact_id}']" \
        "${pom_file}" > "${temp_file}"
    
    # Check if the edit was successful
    if [ $? -eq 0 ]; then
        mv "${temp_file}" "${pom_file}"
        return 0
    else
        rm -f "${temp_file}"
        return 1
    fi
}

# Function to add a new dependency
# Usage: xml_add_dependency "/path/to/pom.xml" "groupId" "artifactId" "version" ["scope"]
xml_add_dependency() {
    local pom_file="$1"
    local group_id="$2"
    local artifact_id="$3"
    local version="$4"
    local scope="$5"
    local temp_file="${pom_file}.tmp"
    
    if [ ! -f "${pom_file}" ]; then
        echo "Error: POM file does not exist: ${pom_file}"
        return 1
    fi
    
    # Create dependency XML
    local dependency_xml="<dependency>\n            <groupId>${group_id}</groupId>\n            <artifactId>${artifact_id}</artifactId>\n            <version>${version}</version>"
    
    # Add scope if provided
    if [ ! -z "${scope}" ]; then
        dependency_xml="${dependency_xml}\n            <scope>${scope}</scope>"
    fi
    
    dependency_xml="${dependency_xml}\n        </dependency>"
    
    # Add the dependency to the dependencies section
    xmlstarlet ed -N "pom=http://maven.apache.org/POM/4.0.0" \
        --subnode "//pom:dependencies[1]" -t text -n "" -v "${dependency_xml}" \
        "${pom_file}" > "${temp_file}"
    
    # Check if the edit was successful
    if [ $? -eq 0 ]; then
        mv "${temp_file}" "${pom_file}"
        return 0
    else
        rm -f "${temp_file}"
        return 1
    fi
}

# Function to get the project version
# Usage: xml_get_version "/path/to/pom.xml"
xml_get_version() {
    local pom_file="$1"
    
    if [ ! -f "${pom_file}" ]; then
        echo "Error: POM file does not exist: ${pom_file}"
        return 1
    fi
    
    xmlstarlet sel -N "pom=http://maven.apache.org/POM/4.0.0" -t \
        -v "/pom:project/pom:version" \
        "${pom_file}" 2>/dev/null
}

# Function to update the project version
# Usage: xml_set_version "/path/to/pom.xml" "1.2.3"
xml_set_version() {
    local pom_file="$1"
    local version="$2"
    local temp_file="${pom_file}.tmp"
    
    if [ ! -f "${pom_file}" ]; then
        echo "Error: POM file does not exist: ${pom_file}"
        return 1
    fi
    
    # Update version
    xmlstarlet ed -N "pom=http://maven.apache.org/POM/4.0.0" \
        -u "/pom:project/pom:version" -v "${version}" \
        "${pom_file}" > "${temp_file}"
    
    # Check if the edit was successful
    if [ $? -eq 0 ]; then
        mv "${temp_file}" "${pom_file}"
        return 0
    else
        rm -f "${temp_file}"
        return 1
    fi
}

# Function to format a POM file (indentation, etc.)
# Usage: xml_format_pom "/path/to/pom.xml"
xml_format_pom() {
    local pom_file="$1"
    local temp_file="${pom_file}.tmp"
    
    if [ ! -f "${pom_file}" ]; then
        echo "Error: POM file does not exist: ${pom_file}"
        return 1
    fi
    
    # Format the XML with proper indentation
    xmlstarlet fo -s 4 "${pom_file}" > "${temp_file}"
    
    # Check if the edit was successful
    if [ $? -eq 0 ]; then
        mv "${temp_file}" "${pom_file}"
        return 0
    else
        rm -f "${temp_file}"
        return 1
    fi
}

# If the script is run directly, display usage information
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    echo "XML Tools for Maven POM files"
    echo "----------------------------"
    echo "This script provides utility functions for working with Maven POM files."
    echo "It is intended to be sourced by other scripts, not run directly."
    echo ""
    echo "Example usage from another script:"
    echo "  source $(basename \"${BASH_SOURCE[0]}\")"
    echo "  xml_get_version \"/path/to/pom.xml\""
fi
#!/bin/bash

# This script removes duplicate dependencies and fixes surefire plugin configuration

RINNA_DIR="/home/emumford/NativeLinuxProjects/Rinna"

# Create backup directory
BACKUP_DIR="${RINNA_DIR}/backup/pom-backup-$(date +%Y%m%d%H%M%S)"
mkdir -p "${BACKUP_DIR}"

# Backup all pom.xml files
find "${RINNA_DIR}" -name "pom.xml" -exec cp {} "${BACKUP_DIR}/$(basename $(dirname {}))_pom.xml" \;
echo "Backed up all pom.xml files to ${BACKUP_DIR}"

# Function to fix a single pom.xml file
fix_pom_file() {
    local pom_file="$1"
    echo "Fixing $pom_file..."
    
    # Create temporary file
    local temp_file=$(mktemp)
    
    # Clean up the surefire plugin dependencies 
    # 1. Remove <scope>test</scope> from plugin dependencies
    # 2. Remove duplicate dependencies
    cat "$pom_file" | awk '
        BEGIN { in_surefire = 0; in_dependencies = 0; seen_api = 0; seen_engine = 0; suite_api = ""; suite_engine = ""; print_line = 1; }
        
        /<plugin>.*maven-surefire-plugin/ { in_surefire = 1; }
        in_surefire && /<\/plugin>/ { in_surefire = 0; }
        
        in_surefire && /<dependencies>/ { in_dependencies = 1; }
        in_surefire && in_dependencies && /<\/dependencies>/ { 
            # Print saved dependencies without scope
            if (suite_api != "") {
                print suite_api; 
                gsub(/<scope>test<\/scope>/, "", suite_api);
                print suite_api;
            }
            if (suite_engine != "") {
                print suite_engine;
                gsub(/<scope>test<\/scope>/, "", suite_engine);
                print suite_engine;
            }
            in_dependencies = 0;
            print_line = 1;
        }
        
        # Capture and potentially filter junit-platform-suite-api dependencies
        in_surefire && in_dependencies && /<artifactId>junit-platform-suite-api<\/artifactId>/ {
            if (seen_api == 0) {
                seen_api = 1;
                # Capture this dependency block
                start_line = NR - 2;  # Assuming standard format
                end_line = NR + 3;    # Assuming standard format
                suite_api = "";
                for (i = start_line; i <= end_line; i++) {
                    suite_api = suite_api stored_lines[i] "\n";
                }
                gsub(/<scope>test<\/scope>/, "", suite_api);
            }
            print_line = 0;  # Skip printing this dependency block
        }
        
        # Skip printing if in the range of a skipped block
        in_surefire && in_dependencies && !print_line && NR >= start_line && NR <= end_line {
            next;
        }
        
        # Capture and potentially filter junit-platform-suite-engine dependencies
        in_surefire && in_dependencies && /<artifactId>junit-platform-suite-engine<\/artifactId>/ {
            if (seen_engine == 0) {
                seen_engine = 1;
                # Capture this dependency block
                start_line = NR - 2;  # Assuming standard format
                end_line = NR + 3;    # Assuming standard format
                suite_engine = "";
                for (i = start_line; i <= end_line; i++) {
                    suite_engine = suite_engine stored_lines[i] "\n";
                }
                gsub(/<scope>test<\/scope>/, "", suite_engine);
            }
            print_line = 0;  # Skip printing this dependency block
        }
        
        # Store each line for potential use in dependency blocks
        { stored_lines[NR] = $0; }
        
        # Print line if not in a skipped block
        print_line { print; }
        
        # Reset flag at end of skipped block
        !print_line && NR == end_line { print_line = 1; }
    ' > "$temp_file"
    
    # Replace original file
    mv "$temp_file" "$pom_file"
    
    # More reliable method: sed changes for fixing scope and duplicates
    sed -i 's/<artifactId>junit-platform-suite-api<\/artifactId>.*<scope>test<\/scope>/<artifactId>junit-platform-suite-api<\/artifactId>/g' "$pom_file"
    sed -i 's/<artifactId>junit-platform-suite-engine<\/artifactId>.*<scope>test<\/scope>/<artifactId>junit-platform-suite-engine<\/artifactId>/g' "$pom_file"
    
    # Remove duplicate dependencies blocks
    local temp_file2=$(mktemp)
    cat "$pom_file" | awk '
        BEGIN { skip = 0; }
        /<artifactId>junit-platform-suite-api<\/artifactId>/ { 
            count_api++;
            if (count_api > 1) { skip = 5; }  # Skip this dependency block
        }
        /<artifactId>junit-platform-suite-engine<\/artifactId>/ { 
            count_engine++;
            if (count_engine > 1) { skip = 5; }  # Skip this dependency block
        }
        { if (skip > 0) { skip--; } else { print; } }
    ' > "$temp_file2"
    
    mv "$temp_file2" "$pom_file"
}

# Fix all pom.xml files
find "${RINNA_DIR}" -name "pom.xml" -exec bash -c "fix_pom_file {}" \;

# Add missing dependencies to each pom.xml if needed
for POM_FILE in $(find "${RINNA_DIR}" -name "pom.xml"); do
    # Add junit platform suite dependencies if not present
    if ! grep -q '<artifactId>junit-platform-suite-api</artifactId>' "$POM_FILE"; then
        sed -i '/<dependencies>/a \
        <!-- JUnit platform suite for tests -->\
        <dependency>\
            <groupId>org.junit.platform</groupId>\
            <artifactId>junit-platform-suite-api</artifactId>\
            <version>1.10.2</version>\
            <scope>test</scope>\
        </dependency>\
        <dependency>\
            <groupId>org.junit.platform</groupId>\
            <artifactId>junit-platform-suite-engine</artifactId>\
            <version>1.10.2</version>\
            <scope>test</scope>\
        </dependency>' "$POM_FILE"
    fi
    
    # Make sure rinna-core is a dependency for the src module
    if [[ "$POM_FILE" == *"/src/pom.xml" ]] && ! grep -q '<artifactId>rinna-core</artifactId>' "$POM_FILE"; then
        sed -i '/<dependencies>/a \
        <!-- Rinna Core module -->\
        <dependency>\
            <groupId>org.rinna</groupId>\
            <artifactId>rinna-core</artifactId>\
            <version>${project.version}</version>\
        </dependency>' "$POM_FILE"
    fi
done

echo "All pom.xml files have been fixed."
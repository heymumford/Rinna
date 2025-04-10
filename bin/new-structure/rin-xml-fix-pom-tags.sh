#\!/bin/bash
#
# Script to fix the n tags in XML files
# This is a more targeted script specifically for the n tag issue

# Define logging functions directly to avoid sourcing issues
log_info() { echo -e "\033[0;34m[INFO]\033[0m $1"; }
log_success() { echo -e "\033[0;32m[SUCCESS]\033[0m $1"; }
log_warning() { echo -e "\033[0;33m[WARNING]\033[0m $1"; }
log_error() { echo -e "\033[0;31m[ERROR]\033[0m $1"; }

# Find all POM files
pom_files=$(find . -name "pom.xml" -type f -not -path "./target/*" -not -path "*/target/*" -not -path "*/.git/*")

# Count files
pom_count=$(echo "$pom_files" | wc -l)

log_info "Found $pom_count POM files to check for n tag issues"

# Process each POM file
fixed_count=0
for pom_file in $pom_files; do
    # Create a temporary file
    temp_file=$(mktemp)
    
    # Replace n tags with name tags
    sed 's/<n>/<name>/g; s/<\/n>/<\/name>/g' "$pom_file" > "$temp_file"
    
    # Check if the file has changed using diff instead of cmp
    if diff -q "$temp_file" "$pom_file" > /dev/null; then
        # Files are the same, no change needed
        rm "$temp_file"
    else
        # Files are different, apply the fix
        mv "$temp_file" "$pom_file"
        log_info "Fixed n tag issue in $pom_file"
        fixed_count=$((fixed_count + 1))
    fi
done

if [ "$fixed_count" -eq 0 ]; then
    log_success "No POM files needed n tag fixes"
else
    log_success "Fixed n tag issues in $fixed_count POM files"
fi

exit 0

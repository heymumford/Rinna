#\!/bin/bash
# Script to fix imports in the CLI module

echo "Fixing CLI module imports..."

# Regular expression pattern for imports that need to be changed
# org.rinna.domain.* needs to be changed to org.rinna.domain.model.*
# except for usecase and service packages which remain as they are

# Find all Java files in the CLI module
find rinna-cli/src/main/java/org/rinna/cli -name "*.java" -type f | while read file; do
  echo "Processing: $file"
  
  # Check if the file contains imports from org.rinna.domain (excluding usecase and service)
  if grep -q "import org.rinna.domain.[^.]*;" "$file"; then
    # Temporary file for processing
    tempfile=$(mktemp)
    
    # Replace imports from domain to domain.model
    sed 's/import org.rinna.domain.\([^.]*\);/import org.rinna.domain.model.\1;/g' "$file" > "$tempfile"
    
    # Back up the original file
    cp "$file" "${file}.bak"
    
    # Replace the original with the modified content
    mv "$tempfile" "$file"
    
    echo "  Fixed imports in $file"
  else
    echo "  No domain imports to fix in $file"
  fi
done

echo "Import fixing complete"

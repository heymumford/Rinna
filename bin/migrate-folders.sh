#!/bin/bash
# Script to migrate the Rinna folder structure from deep nesting to a flatter structure
# Reduces max folder depth from 13 to 9 levels

set -e  # Exit on error

# Check if run from project root
if [ ! -d "rinna-core" ] || [ ! -d "bin" ]; then
  echo "Error: This script must be run from the Rinna project root directory."
  exit 1
fi

echo "=== Rinna Folder Structure Migration ==="
echo "This script will migrate the project to a flatter structure with maximum depth of 9 levels."
echo "Current structure: rinna-core/src/main/java/org/rinna/domain/entity/..."
echo "Target structure: src/main/java/org/rinna/domain/..."
echo ""
echo "WARNING: This is a major change! Make sure you have committed all your changes."
read -p "Continue? (y/n): " -n 1 -r
echo ""
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
  echo "Migration aborted by user."
  exit 0
fi

# Create new directory structure
echo "Creating new directory structure..."
mkdir -p src/main/java/org/rinna/domain
mkdir -p src/main/java/org/rinna/repository
mkdir -p src/main/java/org/rinna/usecase
mkdir -p src/main/java/org/rinna/service
mkdir -p src/main/java/org/rinna/persistence
mkdir -p src/main/java/org/rinna/api
mkdir -p src/main/java/org/rinna/config
mkdir -p src/test/java/org/rinna/domain
mkdir -p src/test/java/org/rinna/service
mkdir -p src/test/java/org/rinna/usecase
mkdir -p src/test/resources

# Create backup of original structure
echo "Creating backup of original structure..."
mkdir -p backup
cp -r rinna-core backup/

# Migration functions
migrate_package() {
  local source_dir=$1
  local target_dir=$2
  local source_package=$3
  local target_package=$4

  echo "Migrating from $source_package to $target_package..."
  
  # Find all Java files in the source directory
  find "$source_dir" -name "*.java" -type f | while read -r file; do
    # Determine the target file path
    local rel_path="${file#$source_dir/}"
    local target_file="$target_dir/${rel_path}"
    local target_dir_path=$(dirname "$target_file")
    
    # Create target directory if it doesn't exist
    mkdir -p "$target_dir_path"
    
    # Update package declaration and copy the file
    sed "s/package $source_package/package $target_package/" "$file" > "$target_file"
    
    echo "  Migrated: $file -> $target_file"
  done
}

update_imports() {
  local dir=$1
  local old_import=$2
  local new_import=$3
  
  echo "Updating imports from $old_import to $new_import..."
  
  # Find all Java files and update import statements
  find "$dir" -name "*.java" -type f -exec sed -i "s/import $old_import/import $new_import/g" {} \;
}

# Migrate each package
echo "Starting migration..."

# Migrate domain entities
migrate_package \
  "rinna-core/src/main/java/org/rinna/domain/entity" \
  "src/main/java/org/rinna/domain" \
  "org.rinna.domain.entity" \
  "org.rinna.domain"

# Migrate repository interfaces
migrate_package \
  "rinna-core/src/main/java/org/rinna/domain/repository" \
  "src/main/java/org/rinna/repository" \
  "org.rinna.domain.repository" \
  "org.rinna.repository"

# Migrate service interfaces
migrate_package \
  "rinna-core/src/main/java/org/rinna/domain/usecase" \
  "src/main/java/org/rinna/usecase" \
  "org.rinna.domain.usecase" \
  "org.rinna.usecase"

# Migrate repository implementations
migrate_package \
  "rinna-core/src/main/java/org/rinna/adapter/persistence" \
  "src/main/java/org/rinna/persistence" \
  "org.rinna.adapter.persistence" \
  "org.rinna.persistence"

# Migrate service implementations
migrate_package \
  "rinna-core/src/main/java/org/rinna/adapter/service" \
  "src/main/java/org/rinna/service" \
  "org.rinna.adapter.service" \
  "org.rinna.service"

# Migrate tests
migrate_package \
  "rinna-core/src/test/java/org/rinna/domain/usecase" \
  "src/test/java/org/rinna/usecase" \
  "org.rinna.domain.usecase" \
  "org.rinna.usecase"

migrate_package \
  "rinna-core/src/test/java/org/rinna/service/impl" \
  "src/test/java/org/rinna/service" \
  "org.rinna.service.impl" \
  "org.rinna.service"

# Copy test resources
echo "Copying test resources..."
cp -r rinna-core/src/test/resources/* src/test/resources/

# Update import statements in all Java files
echo "Updating import statements..."
update_imports "src/main/java" "org.rinna.domain.entity" "org.rinna.domain"
update_imports "src/main/java" "org.rinna.domain.repository" "org.rinna.repository"
update_imports "src/main/java" "org.rinna.domain.usecase" "org.rinna.usecase"
update_imports "src/main/java" "org.rinna.adapter.persistence" "org.rinna.persistence"
update_imports "src/main/java" "org.rinna.adapter.service" "org.rinna.service"
update_imports "src/test/java" "org.rinna.domain.entity" "org.rinna.domain"
update_imports "src/test/java" "org.rinna.domain.repository" "org.rinna.repository"
update_imports "src/test/java" "org.rinna.domain.usecase" "org.rinna.usecase"
update_imports "src/test/java" "org.rinna.adapter.persistence" "org.rinna.persistence"
update_imports "src/test/java" "org.rinna.adapter.service" "org.rinna.service"

# Update pom.xml
echo "Updating pom.xml files..."
# Create new root pom.xml based on the existing one but without the module
sed '/    <modules>/,/<\/modules>/d' rinna-core/pom.xml > src/pom.xml

# Make the script executable
chmod +x bin/migrate-folders.sh

echo ""
echo "Migration complete! Please review the changes and run tests to verify functionality."
echo "Original files have been backed up to the 'backup' directory."
echo ""
echo "Next steps:"
echo "1. Review and test the migrated code"
echo "2. Update the main pom.xml file"
echo "3. Run the build to ensure everything works"
echo "4. Remove the backup directory when satisfied"
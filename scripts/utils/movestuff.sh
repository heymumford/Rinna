#!/bin/bash

# Exit on any error
set -e

echo "Beginning second phase of project reorganization..."
echo "Creating backup of current project state..."
mkdir -p ../rinna-cleanup-$(date +%Y%m%d)
cp -R . ../rinna-cleanup-$(date +%Y%m%d)/

# Create any additional needed directories
echo "Creating additional directories..."
mkdir -p java/rinna-data
mkdir -p python/samples
mkdir -p config/containers
mkdir -p java/src/test/resources
mkdir -p python/tests/resources

# Move remaining Java content
echo "Moving Java content..."
if [ -d src ]; then
  cp -R src/* java/src/ 2>/dev/null || true
fi

if [ -d rinna-data-sqlite ]; then
  cp -R rinna-data-sqlite/* java/rinna-data/ 2>/dev/null || true
fi

if [ -d test ]; then
  cp -R test/* java/src/test/ 2>/dev/null || true
fi

if [ -d test-bin ]; then
  cp -R test-bin/* java/src/test/resources/ 2>/dev/null || true
fi

# Move test data appropriately
echo "Organizing test data..."
if [ -d test-data ]; then
  # Java test data
  find test-data -name "*.java" -o -name "*.xml" -o -name "*.properties" | while read file; do
    target_dir="java/src/test/resources/$(dirname "$file" | sed 's|^test-data/||')"
    mkdir -p "$target_dir"
    cp "$file" "$target_dir/"
  done
  
  # Python test data
  find test-data -name "*.py" -o -name "*.yaml" -o -name "*.json" | while read file; do
    target_dir="python/tests/resources/$(dirname "$file" | sed 's|^test-data/||')"
    mkdir -p "$target_dir"
    cp "$file" "$target_dir/"
  done
fi

# Move utilities
echo "Organizing utilities..."
if [ -d utils ]; then
  # Java utilities
  find utils -name "*.java" | while read file; do
    target_dir="java/src/main/java/utils/$(dirname "$file" | sed 's|^utils/||')"
    mkdir -p "$target_dir"
    cp "$file" "$target_dir/"
  done
  
  # Python utilities
  find utils -name "*.py" | while read file; do
    target_dir="python/scripts/utils/$(dirname "$file" | sed 's|^utils/||')"
    mkdir -p "$target_dir"
    cp "$file" "$target_dir/"
  done
  
  # Shell utilities
  find utils -name "*.sh" | while read file; do
    target_dir="scripts/utils/$(dirname "$file" | sed 's|^utils/||')"
    mkdir -p "$target_dir"
    cp "$file" "$target_dir/"
    chmod +x "$target_dir/$(basename "$file")"
  done
fi

# Move bin scripts if not already moved
echo "Moving bin scripts..."
if [ -d bin ]; then
  # Python scripts
  find bin -name "*.py" | while read file; do
    target_dir="python/scripts/bin/$(dirname "$file" | sed 's|^bin/||')"
    mkdir -p "$target_dir"
    cp "$file" "$target_dir/"
  done
  
  # Shell scripts
  find bin -name "*.sh" | while read file; do
    target_dir="scripts/bin/$(dirname "$file" | sed 's|^bin/||')"
    mkdir -p "$target_dir"
    cp "$file" "$target_dir/"
    chmod +x "$target_dir/$(basename "$file")"
  done
fi

# Move samples
echo "Organizing samples..."
if [ -d samples ]; then
  # Java samples
  find samples -name "*.java" | while read file; do
    target_dir="java/samples/$(dirname "$file" | sed 's|^samples/||')"
    mkdir -p "$target_dir"
    cp "$file" "$target_dir/"
  done
  
  # Python samples
  find samples -name "*.py" | while read file; do
    target_dir="python/samples/$(dirname "$file" | sed 's|^samples/||')"
    mkdir -p "$target_dir"
    cp "$file" "$target_dir/"
  done
fi

# Move remaining files
echo "Moving miscellaneous files..."
if [ -f podman-compose.yml ]; then
  cp podman-compose.yml config/containers/
fi

if [ -f version.properties ]; then
  cp version.properties config/shared/
fi

# Create .gitignore in each directory
echo "Creating .gitignore files..."
cat > java/.gitignore << EOF
# Java build artifacts
target/
*.class
*.jar
.classpath
.project
.settings/
*.iml
EOF

cat > python/.gitignore << EOF
# Python artifacts
__pycache__/
*.py[cod]
*$py.class
*.so
.Python
.env
.venv
env/
venv/
.coverage
htmlcov/
*.egg-info/
EOF

# Verify content was moved successfully
echo "Verifying content was moved successfully..."
echo "Please review the files in the new structure to ensure everything was copied properly."
echo "Once verified, you can delete the old directories:"

cat << EOF
Directories that can likely be deleted (after verifying content was moved):
- bin/
- rinna-cli/
- rinna-core/
- rinna-data-sqlite/
- samples/
- src/
- target/
- test/
- test-bin/
- test-data/
- utils/

Files that can likely be deleted:
- pom.xml.versionsBackup
- podman-compose.yml (if moved to config/containers/)
- version.properties (if moved to config/shared/)
EOF

echo "Cleanup script complete. Please verify the changes before deleting any files."

#!/bin/bash
# Rinna project cleanup script
# This script helps maintain a clean codebase by removing build artifacts and temporary files

set -e  # Exit on error

echo "=== Rinna Codebase Cleanup Tool ==="
echo ""

# Define cleanup operations
clean_java() {
  echo "Cleaning Java build artifacts..."
  mvn clean
  echo "✓ Done"
}

clean_python() {
  echo "Cleaning Python cache and compiled files..."
  find . -name "*.pyc" -delete
  find . -name "__pycache__" -type d -exec rm -rf {} + 2>/dev/null || true
  find . -name "*.egg-info" -type d -exec rm -rf {} + 2>/dev/null || true
  echo "✓ Done"
}

clean_go() {
  echo "Cleaning Go build artifacts..."
  find ./api -name "*.test" -delete 2>/dev/null || true
  echo "✓ Done"
}

clean_temp() {
  echo "Cleaning temporary files..."
  find . -name "*.tmp" -o -name "*.bak" -o -name "*~" -delete 2>/dev/null || true
  find . -name ".test-tmp" -type d -exec rm -rf {} + 2>/dev/null || true
  find . -name "*.log" -not -path "*/.git/*" -delete 2>/dev/null || true
  echo "✓ Done"
}

# Process arguments
if [[ $# -eq 0 ]]; then
  echo "Performing complete cleanup..."
  clean_java
  clean_python
  clean_go
  clean_temp
  echo ""
  echo "Cleanup complete! ✨"
else
  for arg in "$@"; do
    case $arg in
      java)
        clean_java
        ;;
      python)
        clean_python
        ;;
      go)
        clean_go
        ;;
      temp)
        clean_temp
        ;;
      *)
        echo "Unknown option: $arg"
        echo "Available options: java, python, go, temp"
        exit 1
        ;;
    esac
  done
  echo ""
  echo "Selected cleanup complete! ✨"
fi

exit 0
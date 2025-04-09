#!/bin/bash

# enforce-package-structure.sh
# Validates and enforces the new package structure in the Rinna project

set -e  # Exit on error

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${PROJECT_ROOT}"

# Add ANSI color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🔍 Enforcing package structure in Rinna project...${NC}"

# Define package mappings from old to new
declare -A PACKAGE_MAPPINGS=(
  ["org.rinna.domain.entity"]="org.rinna.domain.model"
  ["org.rinna.domain.usecase"]="org.rinna.domain.service"
  ["org.rinna.service.impl"]="org.rinna.adapter.service"
  ["org.rinna.persistence"]="org.rinna.adapter.repository"
  ["org.rinna.model"]="org.rinna.domain.model"
)

# Function to check for forbidden packages
check_forbidden_packages() {
  local module_path="$1"
  local module_name="$2"
  
  if [ ! -d "$module_path" ]; then
    echo -e "  ${YELLOW}Module $module_name not found. Skipping.${NC}"
    return 0
  fi
  
  local src_dir="${module_path}/src/main/java"
  if [ ! -d "$src_dir" ]; then
    echo -e "  ${YELLOW}No source directory found for $module_name. Skipping.${NC}"
    return 0
  fi
  
  echo -e "${BLUE}Checking module: $module_name${NC}"
  
  local violations=0
  
  # Check for forbidden packages in import statements
  for old_package in "${!PACKAGE_MAPPINGS[@]}"; do
    local new_package="${PACKAGE_MAPPINGS[$old_package]}"
    echo -e "  Checking for old package ${YELLOW}$old_package${NC} (should use ${GREEN}$new_package${NC})..."
    
    # Find all Java files with imports using the old package
    local java_files_with_old_package=$(grep -r --include="*.java" "^import $old_package" "$src_dir" || true)
    
    if [ ! -z "$java_files_with_old_package" ]; then
      echo -e "  ${RED}❌ Found files using old package $old_package:${NC}"
      echo "$java_files_with_old_package" | while read -r line; do
        echo -e "    ${RED}$line${NC}"
        violations=$((violations + 1))
      done
    else
      echo -e "  ${GREEN}✅ No references to old package $old_package found${NC}"
    fi
  done
  
  # Check for temporary compatibility packages
  local temp_packages=("org.rinna.usecase._temp" "org.rinna.cli.adapter._temp")
  
  for temp_package in "${temp_packages[@]}"; do
    echo -e "  Checking for temporary package ${YELLOW}$temp_package${NC}..."
    
    # Find all Java files with imports using the temporary package
    local java_files_with_temp_package=$(grep -r --include="*.java" "^import $temp_package" "$src_dir" 2>/dev/null || true)
    local package_directories=$(find "$src_dir" -path "*/${temp_package//.//}*" -type d 2>/dev/null || true)
    
    if [ ! -z "$java_files_with_temp_package" ] || [ ! -z "$package_directories" ]; then
      echo -e "  ${RED}❌ Found references to temporary package $temp_package:${NC}"
      
      if [ ! -z "$java_files_with_temp_package" ]; then
        echo "$java_files_with_temp_package" | while read -r line; do
          echo -e "    ${RED}$line${NC}"
          violations=$((violations + 1))
        done
      fi
      
      if [ ! -z "$package_directories" ]; then
        echo "$package_directories" | while read -r dir; do
          echo -e "    ${RED}Directory exists: $dir${NC}"
          violations=$((violations + 1))
        done
      fi
    else
      echo -e "  ${GREEN}✅ No references to temporary package $temp_package found${NC}"
    fi
  done
  
  # Report results
  if [ $violations -eq 0 ]; then
    echo -e "${GREEN}✅ No package structure violations found in $module_name${NC}"
    return 0
  else
    echo -e "${RED}❌ Found $violations package structure violations in $module_name${NC}"
    return 1
  fi
}

# Check for non-compliant domain or usecase packages
check_forbidden_declarations() {
  local module_path="$1"
  local module_name="$2"
  
  if [ ! -d "$module_path" ]; then
    return 0
  fi
  
  local src_dir="${module_path}/src/main/java"
  if [ ! -d "$src_dir" ]; then
    return 0
  fi
  
  echo -e "${BLUE}Checking package declarations in module: $module_name${NC}"
  
  local violations=0
  
  # Check for forbidden package declarations
  for old_package in "${!PACKAGE_MAPPINGS[@]}"; do
    local new_package="${PACKAGE_MAPPINGS[$old_package]}"
    echo -e "  Checking for declarations in old package ${YELLOW}$old_package${NC}..."
    
    # Find all Java files with package declarations using the old package
    local java_files_with_old_package=$(grep -r --include="*.java" "^package $old_package" "$src_dir" || true)
    
    if [ ! -z "$java_files_with_old_package" ]; then
      echo -e "  ${RED}❌ Found files declared in old package $old_package:${NC}"
      echo "$java_files_with_old_package" | while read -r line; do
        echo -e "    ${RED}$line${NC}"
        violations=$((violations + 1))
      done
    else
      echo -e "  ${GREEN}✅ No files declared in old package $old_package${NC}"
    fi
  done
  
  # Report results
  if [ $violations -eq 0 ]; then
    echo -e "${GREEN}✅ No non-compliant package declarations found in $module_name${NC}"
    return 0
  else
    echo -e "${RED}❌ Found $violations non-compliant package declarations in $module_name${NC}"
    return 1
  fi
}

# Check each module
exit_code=0

for module_dir in "rinna-core" "rinna-cli" "rinna-data-sqlite"; do
  module_path="${PROJECT_ROOT}/$module_dir"
  check_forbidden_packages "$module_path" "$module_dir" || exit_code=1
  check_forbidden_declarations "$module_path" "$module_dir" || exit_code=1
done

if [ $exit_code -eq 0 ]; then
  echo -e "${GREEN}✅ Package structure validation completed successfully!${NC}"
else
  echo -e "${RED}❌ Package structure validation found violations!${NC}"
  echo -e "${YELLOW}Please update imports to use the new package structure:${NC}"
  
  for old_package in "${!PACKAGE_MAPPINGS[@]}"; do
    local new_package="${PACKAGE_MAPPINGS[$old_package]}"
    echo -e "  ${RED}$old_package${NC} -> ${GREEN}$new_package${NC}"
  done
fi

exit $exit_code
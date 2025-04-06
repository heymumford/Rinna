# Comprehensive Package Structure Migration Plan

## Root Issues Identified

After multiple attempts, we've identified the following key issues:

1. **Duplicate Classes**: The migration process is creating duplicate classes across different packages
2. **Package Conflicts**: Files in org.rinna.service are being moved to org.rinna.adapter.service, but the original files aren't deleted
3. **Incomplete Import Fixes**: Complex import patterns aren't being fully captured by sed replacements
4. **Competing Module Structure**: rinna-core and src directory both contain similar code structure

## Complete Solution Approach

Rather than attempting incremental fixes, we'll take a more methodical approach with a clean slate:

1. **Complete Cleanup**: Remove all migrated files to start fresh
2. **Direct Module Copy**: Copy the entire rinna-core module at once instead of file-by-file
3. **Targeted Package Transformation**: Use targeted package structure changes with regex
4. **Single-Pass Import Updates**: Update all imports in a single comprehensive pass
5. **Build Verification**: Test builds between each major step

## Implementation Steps

### Step 1: Clean up any partial migration

```bash
# Remove any partially-migrated model/service directories
rm -rf /home/emumford/NativeLinuxProjects/Rinna/src/main/java/org/rinna/domain/model
rm -rf /home/emumford/NativeLinuxProjects/Rinna/src/main/java/org/rinna/domain/service
rm -rf /home/emumford/NativeLinuxProjects/Rinna/src/main/java/org/rinna/adapter/repository
rm -rf /home/emumford/NativeLinuxProjects/Rinna/src/main/java/org/rinna/repository
```

### Step 2: Prepare new package structure in rinna-core

```bash
# Create the new package structure
mkdir -p /home/emumford/NativeLinuxProjects/Rinna/rinna-core/src/main/java/org/rinna/domain/model
mkdir -p /home/emumford/NativeLinuxProjects/Rinna/rinna-core/src/main/java/org/rinna/domain/service
mkdir -p /home/emumford/NativeLinuxProjects/Rinna/rinna-core/src/main/java/org/rinna/adapter/repository

# Move domain entity files to domain.model
mv /home/emumford/NativeLinuxProjects/Rinna/rinna-core/src/main/java/org/rinna/domain/entity/* \
   /home/emumford/NativeLinuxProjects/Rinna/rinna-core/src/main/java/org/rinna/domain/model/

# Move domain usecase files to domain.service
mv /home/emumford/NativeLinuxProjects/Rinna/rinna-core/src/main/java/org/rinna/domain/usecase/* \
   /home/emumford/NativeLinuxProjects/Rinna/rinna-core/src/main/java/org/rinna/domain/service/

# Move adapter persistence files to adapter.repository
mv /home/emumford/NativeLinuxProjects/Rinna/rinna-core/src/main/java/org/rinna/adapter/persistence/* \
   /home/emumford/NativeLinuxProjects/Rinna/rinna-core/src/main/java/org/rinna/adapter/repository/

# Remove old directories
rmdir /home/emumford/NativeLinuxProjects/Rinna/rinna-core/src/main/java/org/rinna/domain/entity
rmdir /home/emumford/NativeLinuxProjects/Rinna/rinna-core/src/main/java/org/rinna/domain/usecase
rmdir /home/emumford/NativeLinuxProjects/Rinna/rinna-core/src/main/java/org/rinna/adapter/persistence
```

### Step 3: Update package declarations in all files

```bash
# Update package declarations in model files
find /home/emumford/NativeLinuxProjects/Rinna/rinna-core/src/main/java/org/rinna/domain/model -name "*.java" -exec \
  sed -i 's/package org.rinna.domain.entity;/package org.rinna.domain.model;/g' {} \;

# Update package declarations in service files
find /home/emumford/NativeLinuxProjects/Rinna/rinna-core/src/main/java/org/rinna/domain/service -name "*.java" -exec \
  sed -i 's/package org.rinna.domain.usecase;/package org.rinna.domain.service;/g' {} \;

# Update package declarations in repository files
find /home/emumford/NativeLinuxProjects/Rinna/rinna-core/src/main/java/org/rinna/adapter/repository -name "*.java" -exec \
  sed -i 's/package org.rinna.adapter.persistence;/package org.rinna.adapter.repository;/g' {} \;
```

### Step 4: Update imports in all files

```bash
# Update all imports in the codebase
find /home/emumford/NativeLinuxProjects/Rinna/rinna-core/src -name "*.java" -exec \
  sed -i 's/import org.rinna.domain.entity\./import org.rinna.domain.model./g; 
          s/import org.rinna.domain.usecase\./import org.rinna.domain.service./g;
          s/import org.rinna.adapter.persistence\./import org.rinna.adapter.repository./g' {} \;
```

### Step 5: Fix direct entity and service imports

```bash
# Create a script to update direct imports
cat > /home/emumford/NativeLinuxProjects/Rinna/bin/migration/fix-direct-imports.sh << EOF
#!/bin/bash

classes=("WorkItem" "WorkItemType" "WorkQueue" "WorkflowState" "Priority" "Project" "Release" "WorkItemCreateRequest" "WorkItemMetadata" "DefaultWorkItem" "DefaultProject" "DefaultRelease" "DefaultWorkQueue" "DocumentConfig")

for class in "\${classes[@]}"; do
  find /home/emumford/NativeLinuxProjects/Rinna/rinna-core/src -name "*.java" -exec \
    sed -i "s/import org.rinna.domain.\${class};/import org.rinna.domain.model.\${class};/g" {} \;
done

services=("ItemService" "WorkflowService" "ReleaseService" "QueueService" "DocumentService" "InvalidTransitionException")

for service in "\${services[@]}"; do
  find /home/emumford/NativeLinuxProjects/Rinna/rinna-core/src -name "*.java" -exec \
    sed -i "s/import org.rinna.domain.\${service};/import org.rinna.domain.service.\${service};/g;
            s/import org.rinna.usecase.\${service};/import org.rinna.domain.service.\${service};/g" {} \;
done
EOF

chmod +x /home/emumford/NativeLinuxProjects/Rinna/bin/migration/fix-direct-imports.sh
/home/emumford/NativeLinuxProjects/Rinna/bin/migration/fix-direct-imports.sh
```

### Step 6: Verify rinna-core builds successfully

```bash
cd /home/emumford/NativeLinuxProjects/Rinna/rinna-core && mvn compile
```

### Step 7: Copy to src directory

```bash
# Create the target directory structure
mkdir -p /home/emumford/NativeLinuxProjects/Rinna/src/main/java/org/rinna

# Copy the entire structure
cp -r /home/emumford/NativeLinuxProjects/Rinna/rinna-core/src/main/java/org/rinna/* \
      /home/emumford/NativeLinuxProjects/Rinna/src/main/java/org/rinna/

# Copy resources
mkdir -p /home/emumford/NativeLinuxProjects/Rinna/src/main/resources
cp -r /home/emumford/NativeLinuxProjects/Rinna/rinna-core/src/main/resources/* \
      /home/emumford/NativeLinuxProjects/Rinna/src/main/resources/
```

### Step 8: Update tests

```bash
# Create test directories
mkdir -p /home/emumford/NativeLinuxProjects/Rinna/src/test/java/org/rinna

# Copy test files
cp -r /home/emumford/NativeLinuxProjects/Rinna/rinna-core/src/test/java/org/rinna/* \
      /home/emumford/NativeLinuxProjects/Rinna/src/test/java/org/rinna/

# Copy test resources
mkdir -p /home/emumford/NativeLinuxProjects/Rinna/src/test/resources
cp -r /home/emumford/NativeLinuxProjects/Rinna/rinna-core/src/test/resources/* \
      /home/emumford/NativeLinuxProjects/Rinna/src/test/resources/
```

### Step 9: Verify full build

```bash
cd /home/emumford/NativeLinuxProjects/Rinna && mvn clean compile
```

After these steps, the codebase will have a proper package structure with consistent naming, and the maximum folder depth will be reduced while maintaining clean architecture principles.
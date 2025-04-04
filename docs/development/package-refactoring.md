# Package Refactoring Guide

This document describes the process of refactoring the Rinna package structure from `com.rinna` to `org.rinna` and changing the Maven group ID from `org.samstraumr` to `org.rinna`.

## Overview

The Rinna project originally used the `com.rinna` package namespace and `org.samstraumr` Maven group ID. To better align with open-source standards and the project's governance model, we migrated to the `org.rinna` namespace for both Java packages and Maven artifacts.

## Automated Refactoring Tool

The refactoring process is automated using the `bin/refactor-package.sh` script, which handles the following tasks:

1. Copying Java files to their new package locations
2. Updating package declarations and import statements
3. Updating references in POM files
4. Updating references in feature files
5. Updating references in shell scripts

### Usage

```bash
# Make sure the script is executable
chmod +x bin/refactor-package.sh

# Run the refactoring script
./bin/refactor-package.sh
```

## Manual Steps After Refactoring

After running the automated refactoring, some manual steps may be necessary:

1. **Add serialVersionUID to Exception Classes**: For proper serialization compatibility
   ```java
   private static final long serialVersionUID = 1L;
   ```

2. **Update Reflection Code**: Any code using reflection to reference class names needs to be updated manually.

3. **Update Maven Group ID**: Change the Maven group ID in all POM files
   ```xml
   <!-- From -->
   <groupId>org.samstraumr</groupId>
   
   <!-- To -->
   <groupId>org.rinna</groupId>
   ```

4. **Update Documentation**: Ensure all documentation references the new package structure.

5. **Verify Build and Tests**: Run the build and tests in verbose mode to catch any issues:
   ```bash
   bin/rin -v all
   ```

## Verification Checklist

- [ ] All Java files have been moved to the new package structure
- [ ] All package declarations have been updated from `com.rinna` to `org.rinna`
- [ ] All import statements have been updated
- [ ] All references in POM files have been updated
- [ ] Maven group ID has been changed from `org.samstraumr` to `org.rinna`
- [ ] All references in feature files have been updated
- [ ] All references in shell scripts have been updated
- [ ] All serialVersionUID fields have been added to Exception classes
- [ ] All tests pass in verbose mode

## Troubleshooting

### Common Issues

1. **Maven Build Failures**: Check for missed references to the old package name in the Maven configuration.

2. **Test Failures**: Look for hardcoded references to the old package name in test files.

3. **ClassNotFoundException**: This usually indicates a missed reference to the old package structure.

### Solutions

1. Use the `grep` command to find remaining references to the old package:
   ```bash
   grep -r "com\.rinna" --include="*.java" .
   grep -r "com\.rinna" --include="*.xml" .
   ```

2. Check Java reflection code that might reference class names as strings:
   ```bash
   grep -r "\"com\.rinna" --include="*.java" .
   ```

## Notes for Future Package Changes

When making future package structure changes:

1. Create a comprehensive plan first
2. Back up all code before refactoring
3. Use automation to reduce errors
4. Test thoroughly after refactoring
5. Update all documentation to reflect changes

## Learn More

For more information on Java package conventions and best practices, see:
- [Oracle Java Package Naming Conventions](https://docs.oracle.com/javase/tutorial/java/package/namingpkgs.html)
- [Maven Module Naming Conventions](https://maven.apache.org/guides/mini/guide-naming-conventions.html)
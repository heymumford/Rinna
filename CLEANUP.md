# Repository Cleanup Notes

Date: 2025-04-06

## Files Backed Up

The following redundant or temporary files have been backed up to `backup/src-20250406105932/`:

1. CLAUDE.local.md - Temporary file containing IP documentation notes
2. Duplicate source code in src/ directory

## Project Structure Analysis

During the cleanup process, we identified that the project appears to be in the middle of a migration:

1. According to FOLDERS.md, the project is migrating from a deeply nested structure:
   ```
   /home/emumford/NativeLinuxProjects/Rinna/rinna-core/src/main/java/org/rinna/domain/entity/DefaultWorkItem.java
   ```

   To a flatter structure:
   ```
   /home/emumford/NativeLinuxProjects/Rinna/src/main/java/org/rinna/domain/DefaultWorkItem.java
   ```

2. The migration involves:
   - Eliminating the `rinna-core` module nesting
   - Flattening package structures (e.g., `org.rinna.domain.entity` to `org.rinna.domain`)
   - Using more descriptive file names

3. The migration is not yet complete:
   - The main pom.xml still references `rinna-core` and `rinna-cli` modules, not `src`
   - There are duplicate files in both the old and new locations

## Action Taken

Instead of removing files that might be needed for the migration, we've:
- Backed up the duplicate `src` directory to preserve all files
- Backed up temporary files
- Left the project structure intact so the migration can continue
- Created documentation in the backup directory explaining the backup process

## Recommendations

1. Complete the migration according to the FOLDERS.md plan
2. After the migration is complete, remove any backup directories
3. Ensure the build process is updated to use the new structure
4. Remove this CLEANUP.md file once the migration is finished and cleanup is no longer needed
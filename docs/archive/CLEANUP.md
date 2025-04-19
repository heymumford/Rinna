# Rinna Root Directory Cleanup

This document outlines the cleanup process performed on the Rinna project root directory to improve the organization and structure.

## Objectives

1. Make README.md the only markdown file in the root directory
2. Move scripts from the root directory to the appropriate locations
3. Remove unnecessary temporary files and archives
4. Organize configuration files in the proper directories
5. Ensure project structure follows clean code principles

## Directory Structure Changes

### Documentation

- Moved markdown files from root to `/docs/project-docs/`
  - CHANGELOG.md → docs/project-docs/CHANGELOG.md (with symlink)
  - CLAUDE.md → docs/project-docs/CLAUDE.md (with symlink)

### Scripts

- Moved shell scripts from root to `/utils/`
  - activate-python.sh → utils/activate-python.sh (with symlink)
  - activate-api.sh → utils/activate-api.sh (with symlink)
  - activate-rinna.sh → utils/activate-rinna.sh (with symlink)
  - activate-system-python.sh → utils/activate-system-python.sh (with symlink)
  - build.sh → utils/build.sh (with symlink)

### Configuration Files

- Organized configuration files into appropriate directories
  - pyproject.toml → config/python/pyproject.toml (with symlink)
  - requirements.txt → config/python/requirements.txt (with symlink)
  - requirements-core.txt → config/python/requirements-core.txt (with symlink)
  - podman-compose.yml → config/docker/podman-compose.yml (with symlink)
  - pom-test-config.xml → config/maven/pom-test-config.xml (with symlink)
  - test-profiles.xml → config/maven/test-profiles.xml (with symlink)
  - version.properties → config/version/version.properties (with symlink)
  - .env → config/.env (with symlink)
  - .rinna.yaml → config/.rinna.yaml (with symlink)
  - .pre-commit-config.yaml → config/hooks/.pre-commit-config.yaml (with symlink)

### Build and Test Files

- Organized build and test files into the logs directory
  - .coverage → logs/coverage/.coverage
  - .coveragerc → logs/coverage/.coveragerc
  - coverage/ → logs/coverage/
  - dependency-check.log → logs/dependency-check/
  - test-output/ → logs/test-output/
  - test-bin/ → logs/test-bin/

### Temporary Files

- Consolidated temporary directories
  - tmp/ → logs/temp/
  - temp-cleanup/ → logs/temp/
  - docker-cache/ → logs/cache/

### Utility Directories

- Consolidated utility directories
  - util/ → utils/

## Symbolic Links

Symbolic links were created for backward compatibility to ensure existing tools and scripts continue to work correctly:

```
.env -> config/.env
.pre-commit-config.yaml -> config/hooks/.pre-commit-config.yaml
.rinna.yaml -> config/.rinna.yaml
CHANGELOG.md -> docs/project-docs/CHANGELOG.md
CLAUDE.md -> docs/project-docs/CLAUDE.md
activate-api.sh -> utils/activate-api.sh
activate-python.sh -> utils/activate-python.sh
activate-rinna.sh -> utils/activate-rinna.sh
activate-system-python.sh -> utils/activate-system-python.sh
build.sh -> utils/build.sh
podman-compose.yml -> config/docker/podman-compose.yml
pom-test-config.xml -> config/maven/pom-test-config.xml
pyproject.toml -> config/python/pyproject.toml
requirements-core.txt -> config/python/requirements-core.txt
requirements.txt -> config/python/requirements.txt
test-profiles.xml -> config/maven/test-profiles.xml
version.properties -> config/version/version.properties
```

## Cleanup Scripts

Two scripts were created to handle the cleanup process:

1. `/utils/cleanup-root.sh` - Creates the necessary directories, copies files to their new locations, and creates symbolic links
2. `/utils/remove-originals.sh` - Removes original files that have been successfully moved

## README.md Update

The project README.md was updated to reflect the new directory structure, with detailed documentation of all directories and their purposes.

## Remaining Files in Root

After cleanup, the following files remain in the root directory:

- `.gitattributes` - Git attributes file
- `.gitignore` - Git ignore file
- `LICENSE` - Project license file
- `Makefile` - Project build makefile
- `README.md` - Project README
- `pom.xml` - Main Maven project file

## Benefits

This cleanup provides several benefits:

1. **Improved Organization**: Files are now organized by their purpose and type
2. **Reduced Clutter**: Root directory is now clean and focused
3. **Backward Compatibility**: Symbolic links ensure existing tools continue to work
4. **Enhanced Documentation**: README.md now accurately describes the project structure
5. **Standardized Locations**: Configuration, logs, and scripts now have standard locations
6. **Better Development Experience**: Developers can more easily find files in their expected locations

## Conclusion

The root directory cleanup has significantly improved the organization and structure of the Rinna project, aligning it with clean code principles and making it more maintainable for future development.
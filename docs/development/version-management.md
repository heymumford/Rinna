# Version Management in Rinna

This document explains Rinna's approach to version management, including semantic versioning principles, workflow, and practical examples.

## Semantic Versioning

Rinna follows [Semantic Versioning 2.0.0](https://semver.org/) (`MAJOR.MINOR.PATCH`):

- **MAJOR**: Incompatible API changes
- **MINOR**: New functionality that's backward-compatible 
- **PATCH**: Backward-compatible bug fixes

Additionally, we use Git tags prefixed with "v" (e.g., `v1.2.0`) to mark specific releases.

## Version Source of Truth

The canonical source of truth for Rinna's version is the root `pom.xml` file. The version management script maintains consistency across:

1. Root `pom.xml` file (project version)
2. Module `pom.xml` files (both parent references and their project versions)
3. README.md version badge
4. Git tags
5. GitHub releases (when applicable)

## Version Management Tool

Rinna includes a version management tool at `bin/rin-version` that handles all version-related operations. This tool maintains version consistency, creates git tags, and prepares releases.

### Available Commands

```bash
# Display current version and check consistency
bin/rin-version current

# Verify version consistency across all files
bin/rin-version verify

# Bump major version (e.g., 1.2.3 -> 2.0.0)
bin/rin-version major

# Bump minor version (e.g., 1.2.3 -> 1.3.0)
bin/rin-version minor

# Bump patch version (e.g., 1.2.3 -> 1.2.4)
bin/rin-version patch

# Set specific version
bin/rin-version set 1.5.0

# Create git tag for current version
bin/rin-version tag

# Create formal release (tag + GitHub release)
bin/rin-version release
```

### Options

```bash
# Add a custom message to version commits/tags
bin/rin-version patch -m "Fixed critical login bug"

# Preview changes without applying them
bin/rin-version minor --dry-run
```

## Version Management Workflow

### Development Cycle

1. Development work occurs on feature branches or main branch
2. After significant changes, bump the version:
   ```bash
   bin/rin-version patch  # For bug fixes
   # or
   bin/rin-version minor  # For new features
   ```
3. The script automatically:
   - Updates all version references
   - Commits the changes
   - Creates a git tag

### Release Process

1. Complete and test all changes for release
2. Bump version appropriately:
   ```bash
   bin/rin-version minor  # For typical feature releases
   ```
3. Create a formal release (optional):
   ```bash
   bin/rin-version release -m "Spring 2025 release with project management features"
   ```

### Maven Releases

For Maven deployments to GitHub packages or other repositories, include the appropriate credentials and run:

```bash
bin/rin-version release
```

## Version Strategy and Examples

### When to Bump Major Version

Bump the major version when making incompatible API changes:

```bash
bin/rin-version major -m "Overhauled workflow API with breaking changes"
```

Examples:
- Changing method signatures
- Removing public APIs
- Restructuring core domain model
- Changing database schema incompatibly

### When to Bump Minor Version

Bump the minor version when adding features in a backward-compatible manner:

```bash
bin/rin-version minor -m "Added GitHub webhook integration"
```

Examples:
- Adding new API endpoints
- Implementing new features
- Adding optional parameters to existing methods
- Expanding functionality without breaking changes

### When to Bump Patch Version

Bump the patch version for backward-compatible bug fixes:

```bash
bin/rin-version patch -m "Fixed issue with parent POM versioning"
```

Examples:
- Bug fixes
- Performance improvements
- Small refactorings
- Documentation updates
- Non-functional changes

## Pre-release and Build Metadata

For pre-release versions, use the format `MAJOR.MINOR.PATCH-PRERELEASE`:

```bash
bin/rin-version set 1.2.0-alpha.1
bin/rin-version set 1.2.0-beta.2
bin/rin-version set 1.2.0-rc.1
```

## Handling Version Inconsistencies

If version inconsistencies are detected, run the verification and resolve them:

```bash
# Check for inconsistencies
bin/rin-version verify

# Fix by setting the correct version
bin/rin-version set 1.2.0
```

## Continuous Integration and Versioning

Our CI pipeline checks version consistency as part of the build process. If versions are inconsistent, the build will fail.

To avoid these issues:
1. Always use the `bin/rin-version` tool to manage versions
2. Verify consistency before pushing: `bin/rin-version verify`
3. Resolve any inconsistencies immediately

## Practical Examples

### Example 1: Bug Fix Release

```bash
# Fix a bug in the code
git checkout -b bugfix/login-error
# Make changes...
git add .
git commit -m "Fix login error handling"
git push

# Create PR and merge to main
git checkout main
git pull

# Bump patch version
bin/rin-version patch -m "Fix login error handling"
git push --tags
```

### Example 2: Feature Release

```bash
# Implement new feature
git checkout -b feature/github-integration
# Make changes...
git add .
git commit -m "Implement GitHub webhook integration"
git push

# Create PR and merge to main
git checkout main
git pull

# Bump minor version
bin/rin-version minor -m "Add GitHub webhook integration"
git push --tags

# Create GitHub release
bin/rin-version release -m "Release v1.3.0 with GitHub integration features"
```

### Example 3: Major Release with Breaking Changes

```bash
# Implement breaking changes
git checkout -b feature/api-v2
# Make changes...
git add .
git commit -m "Overhaul API to v2"
git push

# Create PR and merge to main
git checkout main
git pull

# Bump major version
bin/rin-version major -m "Release API v2 with breaking changes"
git push --tags

# Create GitHub release with detailed notes
bin/rin-version release -m "Release v2.0.0 - Major API overhaul"
```

## FAQ

**Q: Why not use Maven Release Plugin?**  
A: Our `bin/rin-version` tool is tailored to Rinna's polyglot architecture (Java + Go) and handles more than just Maven artifacts.

**Q: How are version numbers shown to users?**  
A: Version information is exposed via:
- API version endpoints (`/api/v1/version`)
- CLI version flag (`rin --version`)
- Build artifacts (Maven, Go binaries)
- GitHub releases

**Q: Can I use pre-release versions in production?**  
A: Pre-release versions (e.g., `-alpha`, `-beta`) should never be used in production environments.

**Q: Do we maintain multiple release branches?**  
A: For major versions, we maintain separate branches (e.g., `1.x` and `2.x`). Bug fixes may be backported as needed.
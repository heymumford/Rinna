# Rinna Version Service

A clean, modular version management service for the Rinna project, designed following Clean Architecture principles.

## Features

- **Single Source of Truth:** Central version definition that all components read from
- **Cross-Language Support:** Java, Go, Python, and Bash components all share the same version
- **Clean Architecture:** Clear separation of core logic and adapters
- **Consistency Verification:** Ensures all components have matching versions
- **Extensible:** Easy to add support for new file types or languages

## Architecture

```
version-service/
  core/                # Core business logic
    version.go         # Version entity and rules
    registry.go        # Interface definitions
    properties_registry.go # Implementation
  adapters/            # Language-specific adapters
    bash/              # Bash specific handlers (README)
    java/              # Java specific handlers (Maven)
    go/                # Go specific handlers
    python/            # Python specific handlers
  cli/                 # Command-line interface
    version_cli.go     # CLI tool
```

## Usage

The version service can be used either programmatically or via the CLI tool:

```sh
# Show current version
version-cli --current

# Bump major version
version-cli --major

# Bump minor version
version-cli --minor

# Bump patch version
version-cli --patch

# Set specific version
version-cli --set 2.0.0

# Verify version consistency
version-cli --verify

# Update all files to match central version
version-cli --update

# Create a release
version-cli --release

# Create a git tag
version-cli --tag
```

You can also provide options:

```sh
# Custom commit message
version-cli --major --message "Major version release with new features"

# Force GitHub release creation
version-cli --patch --github
```

## Integration Points

The version service integrates with the following components:

1. **Java/Maven:** Updates all POM files with org.rinna groupId
2. **Go:** Updates version.go files in api/pkg/health and api/internal/version
3. **Python:** Updates rinna_config.py, pyproject.toml, and virtual environment versions
4. **Documentation:** Updates version references in README.md

## Adding New Language Support

To add support for a new language or file type:

1. Create a new adapter in the relevant adapters directory
2. Implement the `FileHandler` interface:
   - `ReadVersion()`: Read version from language-specific files
   - `WriteVersion()`: Update language-specific files with a new version
   - `VerifyVersion()`: Check if language-specific files have the correct version
3. Register the new handler in the CLI
# Migrating to the New Version Service

This guide outlines the process of migrating from the current version management scripts to the new Clean Architecture version service.

## Overview

The new version service follows Uncle Bob's Clean Architecture principles, providing a more maintainable, extensible, and language-agnostic approach to version management. It centralizes versioning logic while providing clear boundaries between core business rules and technology-specific adapters.

## Key Benefits

1. **True Single Source of Truth**: All components read from the same central version.properties file
2. **Clear Separation of Concerns**: Core version logic is separated from language-specific adapters
3. **Extensibility**: Easy to add support for new languages or file types
4. **Consistency Verification**: Built-in support for verifying version consistency across all components
5. **Testability**: Core logic can be tested independently of file system operations
6. **Reduced Duplication**: No duplication of version reading/writing logic across languages

## Migration Steps

### Step 1: Testing the New System

Before fully migrating, you can test the new system by using the `rin-version-next` command:

```bash
# Show current version
./bin/rin-version-next current

# Verify version consistency
./bin/rin-version-next verify
```

This uses the new Clean Architecture implementation but doesn't replace the existing scripts yet.

### Step 2: Switching Over

Once you're ready to switch to the new system:

1. Rename the existing scripts:
   ```bash
   mv ./bin/rin-version ./bin/rin-version-legacy
   mv ./bin/rin-version-next ./bin/rin-version
   ```

2. Update any scripts or CI workflows that directly call `rin-version`.

### Step 3: Using the New Service Programmatically

For developers who need to access version information programmatically, the new system provides clean language-specific APIs:

#### Go

```go
import "github.com/heymumford/Rinna/version-service/core"

// Create a registry instance
registry, err := core.NewPropertiesRegistry("/path/to/version.properties", "/path/to/project/root")
if err != nil {
    // Handle error
}

// Get the current version
version, err := registry.GetVersion()
if err != nil {
    // Handle error
}

fmt.Printf("Current version: %s\n", version.FullVersion)
```

#### Python

The Python API has been updated to read directly from version.properties, so no changes are needed in Python code.

#### Java

Maven projects will continue to work with the version specified in the POM file, which is updated by the version service.

## Command Reference

The new version service maintains the same command structure as the old system:

```bash
# Show current version
./bin/rin-version current

# Bump major/minor/patch version
./bin/rin-version major
./bin/rin-version minor
./bin/rin-version patch

# Set to specific version
./bin/rin-version set 2.0.0

# Verify version consistency
./bin/rin-version verify

# Update all files
./bin/rin-version update

# Create a release
./bin/rin-version release

# Create a tag
./bin/rin-version tag
```

Options such as `-m/--message` and `-g/--github` are also maintained.

## Architecture Overview

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

For more details, see the [Version Service README](/version-service/README.md).

## Troubleshooting

If you encounter issues with the new service:

1. Run `./bin/rin-version verify` to check for version inconsistencies
2. Make sure Go is installed, as it's needed to build the version CLI tool
3. Check that the version.properties file exists and is valid
4. Ensure all required adapters are registered in the CLI tool

For persistent issues, you can temporarily revert to the legacy system while the problems are addressed:

```bash
mv ./bin/rin-version ./bin/rin-version-new
mv ./bin/rin-version-legacy ./bin/rin-version
```

## Extending the Version Service

To add support for a new language or file type:

1. Create a new adapter in the relevant adapters directory
2. Implement the `FileHandler` interface
3. Register the new handler in the CLI

See the [Version Service README](/version-service/README.md) for more details.
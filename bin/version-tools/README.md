# Rinna Version Tools

This directory contains scripts and tools for managing version information across the Rinna project.

## Tools

- **version-sync.sh**: Synchronizes versions across all project components (Maven, Go, Python)
- **version-validator.sh**: Validates version consistency across project components
- **rin-version**: User-friendly CLI wrapper for version management operations

## Usage

The tools can be used directly or through the wrapper scripts in the parent `bin` directory:

### Check Versions

```bash
# Check version consistency
bin/check-versions.sh

# Show detailed report
bin/check-versions.sh --report

# Run in CI mode (exit code reflects validation result)
bin/check-versions.sh --ci
```

### Synchronize Versions

```bash
# Synchronize all files to match version.properties
bin/sync-versions.sh sync

# Show what would be synchronized without making changes
bin/sync-versions.sh sync --dry-run

# Just verify version consistency
bin/sync-versions.sh verify
```

### Version Management

```bash
# Show current version information
bin/rin-version current

# Bump major version (X.0.0)
bin/rin-version major

# Bump minor version (0.X.0)
bin/rin-version minor

# Bump patch version (0.0.X)
bin/rin-version patch

# Set specific version
bin/rin-version set 1.2.3

# Create release and git tag
bin/rin-version release -m "Release version 1.2.3"
```

## Integration in Build Process

The version validation can be integrated into the build process by adding the following to Maven profiles:

```xml
<profile>
  <id>validate-versions</id>
  <build>
    <plugins>
      <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>exec-maven-plugin</artifactId>
        <version>3.2.0</version>
        <executions>
          <execution>
            <id>validate-version-consistency</id>
            <phase>validate</phase>
            <goals>
              <goal>exec</goal>
            </goals>
            <configuration>
              <executable>${project.basedir}/bin/check-versions.sh</executable>
              <arguments>
                <argument>--ci</argument>
              </arguments>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</profile>
```

## Version Management Approach

Rinna follows semantic versioning (MAJOR.MINOR.PATCH) with the following rules:

1. **MAJOR** version: Incompatible API changes, major architecture changes
2. **MINOR** version: Backwards-compatible feature additions/changes
3. **PATCH** version: Backwards-compatible bug fixes and minor improvements

The `version.properties` file serves as the single source of truth for version information.
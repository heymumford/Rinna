# Version Guide for Users

This document explains the versioning system used in Rinna and what it means for users.

## Version Format

Rinna follows [Semantic Versioning](https://semver.org/):

```
MAJOR.MINOR.PATCH
```

- **MAJOR** - Incompatible API changes
- **MINOR** - New features (backward compatible)
- **PATCH** - Bug fixes (backward compatible)

## What Version Should I Use?

- **Latest stable release** (recommended): Use the latest version with an even MINOR number (e.g., 1.10.x)
- **Latest development release**: Use the latest version with an odd MINOR number (e.g., 1.11.x)
- **Enterprise deployments**: Use the latest LTS release (designated with LTS tag)

## Checking Your Version

```bash
# Check the current version
bin/rin version

# Check version details
bin/rin version --verbose
```

## Upgrading Rinna

To upgrade to the latest version:

```bash
# Pull the latest version
git pull

# Rebuild
bin/rin build
```

For major version upgrades, please consult the [Migration Guide](migration-guide.md) for specific instructions.

## Release Schedule

- **Patch releases**: Weekly (bug fixes)
- **Minor releases**: Monthly (new features)
- **Major releases**: Yearly (breaking changes)

## Release Notes

For detailed information about each release, please see the [Changelog](../../CHANGELOG.md).

## Support Policy

- **Latest stable release**: Full support
- **Previous minor release**: Security fixes only
- **Older versions**: No official support

## Reporting Version-specific Issues

When reporting issues, always include your Rinna version:

```bash
bin/rin version
```

This helps us identify and fix version-specific problems more efficiently.
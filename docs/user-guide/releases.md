# Release Management

## Semantic Versioning

Rinna enforces semantic versioning (`major.minor.patch`) with these rules:

- **Major**: Breaking changes
- **Minor**: New features, non-breaking
- **Patch**: Bug fixes, limited to 999 per minor release

## Creating Releases

```bash
# Create a new release
rinna release create 1.0.0

# Increment patch version
rinna release increment --patch

# Increment minor version
rinna release increment --minor

# Increment major version
rinna release increment --major
```

## Managing Release Content

```bash
# Add items to a release
rinna release add 1.0.0 ITEM-1 ITEM-2

# Remove items from a release
rinna release remove 1.0.0 ITEM-1

# List items in a release
rinna release items 1.0.0
```
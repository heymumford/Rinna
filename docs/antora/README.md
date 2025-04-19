# Rinna Documentation

This directory contains the Antora documentation sources for the main Rinna documentation component.

## Structure

```
antora/
├── antora.yml            # Component descriptor
├── modules/
    └── ROOT/             # Default module
        ├── nav.adoc      # Navigation file
        ├── pages/        # Documentation pages
        ├── partials/     # Reusable content fragments
        ├── examples/     # Example code snippets
        ├── images/       # Images and diagrams
        └── attachments/  # Downloadable files
```

## Building the Documentation

The documentation is built as part of the unified Antora documentation site. See the [Documentation Guide](../guides/developer/documentation.md) for instructions on building and viewing the documentation.

## Writing Guidelines

1. Use AsciiDoc format with `.adoc` extension
2. Include proper page headers with title and description
3. Follow the established hierarchical structure
4. Use cross-references to link related content
5. Include diagrams and examples where appropriate
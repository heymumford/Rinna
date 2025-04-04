# Rinna User Guide

## Tools

- [CLI Tool (rin)](rin-cli.md) - Command-line utility for building, testing, and running Rinna
- [Quick Reference](rin-quick-ref.md) - Concise reference for common CLI commands

## Work Item Management

### Item Types

- **Goal**: High-level objectives
- **Feature**: Incremental functionality
- **Bug**: Software issues requiring correction
- **Chore**: Non-functional maintenance tasks

### Workflow Stages

```
Found → Triaged → To Do → In Progress → In Test → Done
```

### Common Commands

```bash
# Create items
rin workflow create goal "Improve system reliability"
rin workflow create feature "Add user authentication"
rin workflow create bug "Login fails on Safari"
rin workflow create chore "Update dependencies"

# List items (with filtering)
rin workflow list
rin workflow list --type bug --status "In Progress"
rin workflow list --assignee jdoe

# Update items
rin workflow update ITEM-1 --status "In Progress"
rin workflow update ITEM-1 --assignee jdoe
rin workflow update ITEM-1 --priority high

# Show item details
rin workflow show ITEM-1
```

## Release Management

```bash
# Create a release
rin release create 1.0.0

# Add items to release
rin release add 1.0.0 ITEM-1 ITEM-2

# List releases
rin release list

# Show release details
rin release show 1.0.0
```

## Lota (Development Cycle) Management

```bash
# Create a Lota
rin lota create "Sprint 1" --start 2023-06-01 --end 2023-06-14

# Add items to Lota
rin lota add "Sprint 1" ITEM-1 ITEM-2

# Show Lota status
rin lota show "Sprint 1"
```

## Building and Testing

Use the Rinna CLI tool for simplified build and test operations:

```bash
# Clean and build the project
rin clean build

# Run tests with verbose output
rin -v test

# Full workflow with errors-only output
rin -e all
```

For complete CLI documentation, see [rin-cli.md](rin-cli.md) or run `rin --help`.
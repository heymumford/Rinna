<!-- Copyright (c) 2025 [Eric C. Mumford](https://github.com/heymumford) [@heymumford], Gemini Deep Research, Claude 3.7. -->

# Rinna User Guide

## Developer-Centric Workflow Management

Rinna focuses on providing a streamlined, developer-centric approach to workflow management with clear, explicit processes.

## Tools

- [CLI Tool (rin)](rin-cli.md) - Command-line utility for managing work and automating developer workflows
- [Quick Reference](rin-quick-ref.md) - Concise reference for common developer CLI commands

## Work Item Management

### Item Types

- **Goal**: High-level objectives that guide development efforts
- **Feature**: Incremental functionality that delivers user value
- **Bug**: Software issues requiring correction
- **Chore**: Non-functional maintenance tasks to keep the system healthy

### Developer Workflow Stages

```
Found → Triaged → To Do → In Progress → In Test → Done
```

### Developer-Focused Commands

```bash
# Show all work items assigned to you
rin my-work

# Show what you should work on next (highest priority items)
rin next-task

# Start working on an item (assigns to you and moves to In Progress)
rin start ITEM-1

# Mark an item as ready for testing
rin ready-for-test ITEM-1

# Complete an item
rin done ITEM-1

# View your work history and productivity metrics
rin my-history
```

### Standard Work Item Commands

```bash
# Create items
rin create goal "Improve system reliability"
rin create feature "Add user authentication"
rin create bug "Login fails on Safari"
rin create chore "Update dependencies"

# List items (with filtering)
rin list
rin list --type bug --status "In Progress"
rin list --assignee jdoe

# Update items
rin update ITEM-1 --status "In Progress"
rin update ITEM-1 --assignee jdoe
rin update ITEM-1 --priority high

# Show item details
rin show ITEM-1
```

## Lota (Development Cycle) Management

```bash
# Create a Lota
rin lota create "Sprint 1" --start 2023-06-01 --end 2023-06-14

# Add items to Lota
rin lota add "Sprint 1" ITEM-1 ITEM-2

# Show Lota status with developer progress
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

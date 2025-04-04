# Rinna User Guide

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
rinna create goal "Improve system reliability"
rinna create feature "Add user authentication"
rinna create bug "Login fails on Safari"
rinna create chore "Update dependencies"

# List items (with filtering)
rinna list
rinna list --type bug --status "In Progress"
rinna list --assignee jdoe

# Update items
rinna update ITEM-1 --status "In Progress"
rinna update ITEM-1 --assignee jdoe
rinna update ITEM-1 --priority high

# Show item details
rinna show ITEM-1
```

## Release Management

```bash
# Create a release
rinna release create 1.0.0

# Add items to release
rinna release add 1.0.0 ITEM-1 ITEM-2

# List releases
rinna release list

# Show release details
rinna release show 1.0.0
```

## Lota (Development Cycle) Management

```bash
# Create a Lota
rinna lota create "Sprint 1" --start 2023-06-01 --end 2023-06-14

# Add items to Lota
rinna lota add "Sprint 1" ITEM-1 ITEM-2

# Show Lota status
rinna lota show "Sprint 1"
```
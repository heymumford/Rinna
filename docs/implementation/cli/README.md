# CLI Documentation

This directory contains documentation related to the CLI module of the Rinna project.

## Parent Documentation
- [Implementation Documentation](../README.md)
- [Documentation Home](../../README.md)

## Contents

- [CLI Module Enabling Summary](./CLI_MODULE_ENABLING_SUMMARY.md) - Summary of enabling the CLI module
- [CLI Operation Analytics Implementation](../CLI_OPERATION_ANALYTICS_IMPLEMENTATION.md) - Implementation of the operation analytics dashboard

## CLI Design Principles

1. **Command Organization**
   - Commands are organized into logical groups
   - Common operations have short, memorable names
   - Advanced operations are organized into subcommands

2. **Implementation Standards**
   - CLI commands should follow the adapter pattern when interacting with the core domain
   - All CLI output should be consistent in format and style
   - Commands should support both interactive and non-interactive modes
   - Error handling should be comprehensive with clear error messages

3. **Testing Standards**
   - CLI commands should have unit tests for business logic
   - Component tests should verify command execution
   - Integration tests should verify interaction with the core domain
   - BDD tests should verify user scenarios

## Command Structure

The CLI uses a hierarchical command structure:

```
rin <command> [options] [arguments]
```

Top-level commands include:
- `add` - Add new work items
- `list` - List work items
- `view` - View details of a work item
- `update` - Update work item properties
- `workflow` - Manage workflow state transitions
- `admin` - Administrative commands
- `server` - Server management
- `report` - Reporting functions
- `stats` - Statistics and metrics
- `dashboard` - Operation analytics dashboard for CLI usage visualization
- `operations` - Manage operation metadata and tracking

See the [CLI Reference](../../guides/user/cli-reference.md) for detailed usage information.
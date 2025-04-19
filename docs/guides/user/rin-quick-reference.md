# Rinna CLI Quick Reference

This document provides a quick reference for the most commonly used Rinna CLI commands.

## Installation & Setup

```bash
# Install Rinna
./install.sh

# Build all components
./build.sh all

# Start the Rinna server
./rinna-server start
```

## Essential Commands

| Command | Description | Example |
|---------|-------------|---------|
| `rin add` | Create a new work item | `rin add "Fix login bug" --type=BUG` |
| `rin list` | List work items | `rin list` |
| `rin view` | View work item details | `rin view WI-123` |
| `rin update` | Update a work item | `rin update WI-123 --status=IN_PROGRESS` |
| `rin done` | Mark a work item as complete | `rin done WI-123` |

## Workflow Commands

```bash
# Create a new feature and start working on it
rin add "New login page" --type=FEATURE
rin update WI-123 --status=IN_PROGRESS

# Mark a work item as ready for testing
rin update WI-123 --status=IN_TEST

# Mark a work item as done
rin done WI-123
```

## Getting Help

```bash
# Get general help
rin --help

# Get help for a specific command
rin add --help
```

## Next Steps

For a comprehensive reference of all available commands, see the [CLI Reference](../../implementation/cli/README.md).

For detailed workflow information, see the [Workflow Guide](workflow-guide.md).
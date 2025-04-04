# Workflow Management

## Workflow Stages

Rinna manages work through the following explicit stages:

1. **Found**: Items initially identified
2. **Triaged**: Assessed and prioritized
3. **To Do**: Ready to be worked on
4. **In Progress**: Currently being worked on
5. **In Test**: Under verification
6. **Done**: Completed

## Stage Transitions

Items must progress through stages in sequential order. Explicit transitions include:

- Found → Triaged
- Triaged → To Do
- To Do → In Progress
- In Progress → In Test
- In Test → Done

## Managing Transitions

```bash
# Move an item to the next stage
rin workflow progress ITEM-1

# Set a specific status (must be valid transition)
rin workflow update ITEM-1 --status "In Progress"

# View transition history
rin workflow history ITEM-1
```

## Using the CLI Tool

The Rinna CLI tool (`rin`) provides different output modes for building and testing:

```bash
# Run tests with minimal output
rin test

# Build with detailed output
rin -v build

# Run the entire workflow showing only errors
rin -e all
```

For more information on using the CLI tool, see [rin-cli.md](rin-cli.md) or run `rin --help`.
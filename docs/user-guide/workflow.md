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
rinna progress ITEM-1

# Set a specific status (must be valid transition)
rinna update ITEM-1 --status "In Progress"

# View transition history
rinna history ITEM-1
```
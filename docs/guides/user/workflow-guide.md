# Rinna Workflow Guide

This document explains Rinna's workflow philosophy and how to effectively work with its fixed workflow states.

## Workflow Philosophy

Rinna deliberately uses a fixed workflow with predefined states, unlike most project management tools that emphasize customization. Here's why:

### 1. Decisions Are Expensive

Every customization decision consumes time and energy:
- Initial decisions about workflow states
- Ongoing debates about whether to change them
- Learning curve for new team members
- Maintenance of documentation about your custom process

Rinna saves you from this entire category of decisions by providing a proven workflow that just works.

### 2. The Native Developer Workflow

The workflow in Rinna represents the natural progression of software development work:

```
Found → Triaged → To Do → In Progress → In Test → Done → Released
```

Each state has a specific purpose:

- **FOUND**: Work originates somewhere (feature conceived, bug discovered, change requested)
- **TRIAGED**: Work is assessed, requirements defined, and acceptance criteria established
- **TO DO**: Work is approved and ready for development
- **IN PROGRESS**: Work is actively being developed
- **IN TEST**: Work is being verified through testing or review
- **DONE**: Work is completed and ready for deployment
- **RELEASED**: Work is deployed to production

### 3. The Only Loop Is Intentional

The only permitted loop in Rinna's workflow is from IN_TEST back to IN_PROGRESS. This represents the natural feedback cycle of development work.

There is no "BLOCKED" state because blocking is not a workflow state - it's an annotation on a work item that can be in any state.

## Workflow State Diagram

```
┌────────┐     ┌─────────┐     ┌─────────┐     ┌─────────────┐     ┌────────┐     ┌──────┐     ┌──────────┐
│ FOUND  │────>│ TRIAGED │────>│  TO DO  │────>│ IN PROGRESS │────>│IN TEST │────>│ DONE │────>│ RELEASED │
└────────┘     └─────────┘     └─────────┘     └─────────────┘     └────────┘     └──────┘     └──────────┘
                                                      ▲               │
                                                      └───────────────┘
```

## Working with Workflow States

### State Transitions

Items progress through stages in sequential order:

- Found → Triaged (Prioritization phase)
- Triaged → To Do (Planning phase)
- To Do → In Progress (Development phase)
- In Progress → In Test (Verification phase)
- In Test → Done (Completion phase)
- Done → Released (Deployment phase)

The only loop is between In Progress and In Test, supporting test-driven development and quality verification.

### Special Transitions

- **Skipping States**: Some items may skip states (e.g., Triaged → Done for items that won't be implemented)
- **Returning to Backlog**: Items can move from In Progress back to To Do if they need to be deferred

### Using CLI Commands for Transitions

```bash
# View available workflow states
rin workflow states

# View possible transitions for an item
rin workflow transitions WI-123

# Transition an item to a new state
rin workflow transition WI-123 --to-state IN_PROGRESS

# Common transition shortcuts
rin start WI-123           # Moves to IN_PROGRESS
rin ready-for-test WI-123  # Moves to IN_TEST
rin done WI-123            # Moves to DONE
```

## Enterprise Integration

If your organization requires integration with enterprise tools like Jira, Azure DevOps, or similar systems, Rinna provides mapping capabilities:

| Rinna State | Jira Example | Azure DevOps Example | GitLab Example |
|-------------|--------------|----------------------|----------------|
| Found | Backlog | New | Open |
| Triaged | Selected for Development | Approved | Triage |
| To Do | To Do | Committed | Todo |
| In Progress | In Progress | Active | Doing |
| In Test | In Review | Testing | Testing |
| Done | Done | Closed | Closed |

Configure enterprise tool mapping:

```bash
# Configure enterprise tool mapping
rin external map-workflow --system jira \
  --mapping "IN_PROGRESS:In Development,IN_TEST:In Review"
```

## Handling Special Cases

### Emergency Fixes

For emergency fixes that need to bypass normal workflow:

```bash
# Create and manage an emergency fix
rin create bug "Fix broken login in production" --priority CRITICAL --emergency
rin hotfix start WI-123
```

### Blocked Items

Rather than a separate "blocked" state, use these approaches:

```bash
# Mark as blocked with a reason
rin block WI-123 --reason "Waiting for third-party API update"

# View all blocked items
rin list --blocked
```

### Custom Validation Rules

While workflow states are fixed, validation rules can be customized:

```bash
# Create custom validation rule
rin rule create --name "require-tests" \
  --condition "type=BUG AND transition=IN_PROGRESS->IN_TEST" \
  --validator "metadata.testCoverage >= 80"
```

## Best Practices

1. **Respect State Meaning**: Use states for their intended purpose
2. **Keep Items Moving**: Avoid letting items sit in intermediate states
3. **Use Blocking Correctly**: Mark items as blocked rather than creating a "blocked" state
4. **Regular Triage**: Periodically review and triage incoming items
5. **Clear Done Criteria**: Establish clear criteria for when items are considered "Done"

For more advanced workflow scenarios, see [Advanced Workflows](advanced-workflows.md).

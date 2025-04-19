# Work Item Dependencies and Relationships

This document explains how work items in Rinna can relate to each other through dependencies and relationships, and how these connections affect workflow.

## Overview

Rinna supports two primary ways to connect work items:

1. **Dependencies**: Represent order-based relationships where one work item must be completed before another can proceed. These are crucial for critical path calculation and project scheduling.

2. **Relationships**: Represent semantic relationships between work items, such as parent-child hierarchies, duplicates, or related items. These help with organization and navigation.

## Dependencies

Dependencies represent workflow constraints between work items, indicating which items must be completed before others can start or progress.

### Dependency Types

Rinna supports the following dependency types:

- **BLOCKS**: The dependency item prevents progress on the dependent item until it's completed
- **RELATES_TO**: A soft dependency indicating that the dependent item is better completed after the dependency item

### Working with Dependencies

#### Adding Dependencies

Use the Critical Path command with the `--add-dependency` option:

```bash
rin path --add-dependency WI-123 --blocks WI-456
```

This indicates that `WI-123` blocks `WI-456`.

#### Viewing Dependencies

To view the dependencies for a specific work item:

```bash
rin path --item WI-456
```

This will show both direct and indirect dependencies for the specified work item.

#### Removing Dependencies

To remove a dependency:

```bash
rin path --remove-dependency WI-123 --from WI-456
```

### Dependency Rules

1. **No Cycles**: Dependencies cannot form cycles. For example, if A depends on B, and B depends on C, then C cannot depend on A.
2. **Transitivity**: Dependencies are transitive. If A depends on B, and B depends on C, then A implicitly depends on C.
3. **Status Constraints**: Some dependencies enforce status constraints. For example, if item A blocks item B, then item B cannot transition to DONE until item A is DONE.

## Relationships

Relationships represent semantic connections between work items that help with organization, navigation, and understanding.

### Relationship Types

- **PARENT_CHILD**: Hierarchical organization where a parent item contains child items
- **DEPENDENCY**: Same as dependencies described above, but represented as relationships
- **BLOCKING**: A special case of dependency that blocks progress
- **RELATED**: General relationship indicating connected work
- **DUPLICATE**: Indicates that one item is a duplicate of another

### Working with Relationships

#### Creating Relationships

To create a parent-child relationship:

```bash
rin link --parent WI-123 --child WI-456
```

#### Viewing Relationships

To view all relationships for a work item:

```bash
rin view --relationships WI-123
```

#### Removing Relationships

To remove a relationship:

```bash
rin unlink --parent WI-123 --child WI-456
```

## Critical Path Analysis

The dependencies between work items form the basis for critical path analysis, which helps identify the sequence of work items that determines the minimum time needed to complete a project.

### Using Critical Path Analysis

To view the project's critical path:

```bash
rin path
```

To identify blockers on the critical path:

```bash
rin path --blockers
```

To see how a specific work item fits into the critical path:

```bash
rin path --item WI-123
```

For detailed information including estimated completion dates:

```bash
rin path --verbose
```

### Critical Path Concepts

- **Critical Path**: The sequence of dependent tasks determining the minimum project completion time
- **Blockers**: Work items on the critical path that are currently impeded
- **Bottlenecks**: Work items with high dependency counts
- **Estimated Completion Date**: Projected completion date based on critical path analysis
- **Parallel Paths**: Sequences that can be worked on simultaneously

## Parent-Child Relationships

Parent-child relationships are a common way to organize work items hierarchically. They represent a containment relationship where a parent work item encompasses multiple child items.

### Creating Parent-Child Hierarchies

To make a work item a child of another:

```bash
rin link --parent WI-123 --child WI-456
```

### Viewing Children and Parents

To view a work item's children:

```bash
rin ls --parent WI-123
```

To view a work item's parent:

```bash
rin ls --child WI-456
```

### Rules for Parent-Child Relationships

1. **No Cycles**: A work item cannot be its own ancestor
2. **Single Parent**: A work item can have only one parent
3. **Status Propagation**: A parent cannot be marked as DONE until all its children are DONE
4. **Deletion Rules**: Deleting a parent does not delete its children, but orphans them

## Duplicate Relationships

Duplicate relationships indicate that two work items represent the same work.

### Marking Duplicates

To mark a work item as a duplicate of another:

```bash
rin link --duplicate WI-123 --original WI-456
```

This marks `WI-123` as a duplicate of `WI-456`.

### Rules for Duplicates

1. Once marked as a duplicate, a work item's state transitions are limited
2. Resolving the original work item typically resolves all its duplicates
3. Duplicates are excluded from metrics and critical path analysis

## Related Work Items

The RELATED relationship type indicates work items that are connected but do not have a direct dependency or parent-child relationship.

### Creating Related Connections

```bash
rin link --related-to WI-123 --item WI-456
```

## Impact on Workflow

Relationships and dependencies have significant impacts on workflow operations:

### State Transitions

- Dependencies can enforce preconditions for state transitions
- A work item with blocking dependencies cannot transition to DONE until the blockers are resolved
- A parent work item cannot be marked DONE until all its children are DONE

### Filtering and Organization

- Parent-child relationships provide hierarchical organization in listings
- Dependencies allow filtering for "blocked" or "blocking" items

### Metrics and Reporting

- Critical path analysis uses dependencies to calculate project timelines
- Relationship data feeds into bottleneck analysis and project health metrics

## Best Practices

### When to Use Dependencies vs. Relationships

- Use **dependencies** when one work item must be completed before another can proceed
- Use **parent-child relationships** for organizing related work into logical groups
- Use **related relationships** for associating items without enforcing workflow constraints

### Dependency Management Tips

1. **Be Selective**: Only create dependencies when there is a true blocking condition
2. **Keep it Direct**: Avoid creating redundant dependencies covered by transitivity
3. **Review Regularly**: Periodically review dependencies to remove any that are no longer relevant

### Organization Tips

1. **Use Consistent Hierarchies**: Develop a consistent approach to parent-child relationships
2. **Limit Hierarchy Depth**: Try to keep parent-child hierarchies to 2-3 levels at most
3. **Link Related Items**: Use RELATED relationships to create a network of associated work items

## Advanced Topics

### Impact Analysis

You can assess the impact of delays using:

```bash
rin path --impact WI-123 --delay 5
```

This shows the ripple effect of a 5-day delay on the specified work item.

### Parallel Work Identification

To identify work that can be done in parallel:

```bash
rin path --parallel
```

This helps with optimizing resource allocation.

### Relationship Visualization

While the CLI provides text-based representation, you can generate a visual graph of relationships:

```bash
rin path --graph --output graph.png
```

## API Integration

For programmatic access to relationship data, the API provides these endpoints:

- `GET /api/v1/work-items/{id}/dependencies`
- `GET /api/v1/work-items/{id}/relationships`
- `GET /api/v1/projects/{id}/critical-path`

## Reference

### Relationship Types

| Type | Description | Workflow Impact |
|------|-------------|----------------|
| PARENT_CHILD | Hierarchical organization | Parent can't be DONE until children are DONE |
| DEPENDENCY | Sequential requirement | Dependent can't progress until dependency is met |
| BLOCKING | Strong dependency | Blocked item cannot advance until blocker is resolved |
| RELATED | General association | No direct workflow impact |
| DUPLICATE | Item duplication | Original resolution typically resolves duplicate |

### CLI Commands

| Command | Description |
|---------|-------------|
| `rin path` | View critical path and dependency information |
| `rin path --item WI-123` | View dependencies for a specific work item |
| `rin path --blockers` | Identify blockers on the critical path |
| `rin link` | Create relationships between work items |
| `rin unlink` | Remove relationships between work items |
| `rin ls --parent WI-123` | List children of a parent work item |
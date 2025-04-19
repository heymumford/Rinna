# Work Items in Rinna

Rinna uses a unified work item model to represent different types of work in your software development process. This document explains how to work with different types of work items.

## Work Item Types

Rinna supports several standard work item types to represent different kinds of work:

- **FEATURE**: New capabilities or enhancements for users
- **BUG**: Problems requiring fixes
- **TASK**: Discrete units of work for implementation
- **CHORE**: Maintenance or cleanup activities
- **EPIC**: Large initiatives containing multiple items
- **STORY**: User-centered descriptions of needs

## Creating Work Items

### Basic Creation

```bash
# Create a feature
rin create feature "Implement user authentication"

# Create a bug
rin create bug "Login fails on Safari"

# Create a task
rin create task "Update dependencies"

# Create a chore
rin create chore "Clean up unused CSS"
```

### With Additional Details

```bash
# Create with priority and assignment
rin create feature "Payment integration" --priority HIGH --assignee john.doe

# Create with description
rin create bug "Server timeout" --description "Server times out when processing large files"

# Create with target release
rin create feature "Analytics dashboard" --release "v2.0"
```

## Managing Work Items

### Viewing and Listing

```bash
# View details of a work item
rin view WI-123

# List all work items
rin list

# List with filtering
rin list --type bug --status "In Progress"

# List by assignee
rin list --assignee jane.doe
```

### Updating Work Items

```bash
# Update status
rin update WI-123 --status "In Progress"

# Update assignee
rin update WI-123 --assignee john.doe

# Update priority
rin update WI-123 --priority HIGH

# Update multiple fields
rin update WI-123 --status "In Progress" --priority HIGH --assignee john.doe
```

### Comments and History

```bash
# Add a comment
rin comment WI-123 "Fixed by updating the API endpoint URL"

# View history
rin history WI-123
```

## Work Item Classification

In addition to basic types, Rinna provides rich classification to maintain clarity:

### 1. Origin Category

- `PROD`: Product-focused (features, stories)
- `ARCH`: Architecture-focused (design decisions)
- `DEV`: Development-focused (implementation tasks) 
- `TEST`: Test-focused (test cases, verification)
- `OPS`: Operations-focused (deployment, monitoring)
- `DOC`: Documentation-focused
- `CROSS`: Cross-functional work

### 2. Complexity Domains (CYNEFIN)

- `CLEAR`: Obvious cause-effect relationships, best practices apply
- `COMPLICATED`: Requires expertise to analyze, good practices apply
- `COMPLEX`: Emergent solutions, experimental approach needed
- `CHAOTIC`: High uncertainty, novel approaches required

### Classification Usage

```bash
# Create with origin category
rin create feature "Database refactoring" --category ARCH

# Create with complexity domain
rin create feature "ML prediction model" --cynefin COMPLEX

# Update classification
rin update WI-123 --category DEV --cynefin COMPLICATED
```

## Work Item Relationships

Work items can have various relationships to represent dependencies and hierarchies:

### Parent-Child Relationships

```bash
# Create parent-child relationship
rin link --parent WI-123 --child WI-456

# View children of a parent
rin ls --parent WI-123

# View parent of a child
rin ls --child WI-456
```

### Dependencies

```bash
# Create dependency (WI-123 blocks WI-456)
rin link WI-123 BLOCKS WI-456

# Create soft dependency
rin link WI-123 DEPENDS_ON WI-456

# View dependencies
rin dependencies WI-456
```

### Other Relationships

```bash
# Mark as duplicate
rin link WI-123 DUPLICATES WI-456

# Create related relationship
rin link WI-123 RELATED_TO WI-456
```

## Custom Fields and Metadata

Rinna allows adding custom fields and metadata to work items:

```bash
# Add custom field
rin metadata WI-123 set "storyPoints" "5"

# Add multiple fields
rin metadata WI-123 set "storyPoints" "5" "designReview" "true"

# Get metadata value
rin metadata WI-123 get "storyPoints"

# List all metadata
rin metadata WI-123 list
```

## Developer-Focused Views

```bash
# Show all work items assigned to you
rin my-work

# Show what you should work on next
rin next-task

# Show recent items you've worked on
rin my-history
```

## Bulk Operations

```bash
# Update multiple items
rin bulk-update --query "type=BUG" --set-status IN_PROGRESS

# Assign multiple items
rin bulk-update --query "status=TO_DO" --set-assignee jane.doe

# Find and update
rin grep "payment" | rin bulk-update --set-priority HIGH
```

## Best Practices

1. **Use Appropriate Types**: Choose the most suitable work item type for the work
2. **Keep Descriptions Clear**: Write concise, actionable descriptions
3. **Maintain Relationships**: Create explicit dependencies and hierarchies
4. **Use Custom Fields Consistently**: Define and use custom fields in a consistent manner
5. **Regular Cleanup**: Periodically review and clean up stale or duplicate work items

For more information on dependencies and relationships, see [Dependencies](dependencies.md).

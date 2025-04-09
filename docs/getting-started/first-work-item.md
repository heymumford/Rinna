# Creating Your First Work Item

This guide walks new users through creating and managing their first work item in Rinna.

## Prerequisites

- Rinna CLI installed
- A Rinna project set up

## 1. Creating a Work Item

Let's create a simple feature work item:

```bash
bin/rin-cli create feature "Add user login page" --priority MEDIUM --description "Create a login page with username and password fields"
```

The system will respond with a confirmation message and the ID of your new work item (e.g., `WI-123`).

## 2. Viewing Your Work Item

To see the details of your newly created work item:

```bash
bin/rin-cli show WI-123  # Replace WI-123 with your actual work item ID
```

You'll see the work item's details:

```
─────────────────────────────────────────────────────────────────
ID:          WI-123
Title:       Add user login page
Type:        FEATURE
Status:      FOUND
Priority:    MEDIUM
Assignee:    Unassigned
Created:     2025-04-08 14:32:21
Updated:     2025-04-08 14:32:21
─────────────────────────────────────────────────────────────────
Description:
Create a login page with username and password fields
─────────────────────────────────────────────────────────────────
```

## 3. Assigning the Work Item to Yourself

To claim the work item:

```bash
bin/rin-cli update WI-123 --assignee "your-username"
```

## 4. Moving Through the Workflow States

Rinna uses a streamlined workflow with six core states:

1. **FOUND**: Initial state for newly created work items
2. **TRIAGED**: Work items that have been reviewed and prioritized
3. **TO_DO**: Work items ready to be worked on
4. **IN_PROGRESS**: Work items currently being worked on
5. **IN_TEST**: Work items that are being tested or reviewed
6. **DONE**: Completed work items

### Moving to Triaged

```bash
bin/rin-cli update WI-123 --status TRIAGED
```

### Moving to To Do

```bash
bin/rin-cli update WI-123 --status TO_DO
```

### Starting Work (Moving to In Progress)

There's a shortcut command for starting work:

```bash
bin/rin-cli start WI-123
```

This will:
1. Assign the work item to you (if not already assigned)
2. Move it to the IN_PROGRESS state

### Moving to In Test

When you're ready for testing or code review:

```bash
bin/rin-cli ready-for-test WI-123
```

### Completing the Work Item

When the work is complete and tested:

```bash
bin/rin-cli done WI-123
```

## 5. Adding Comments

You can add comments to provide updates or additional information:

```bash
bin/rin-cli comment WI-123 "Added basic HTML structure for the login form"
```

## 6. Viewing Work Item History

To see the history of changes to your work item:

```bash
bin/rin-cli history WI-123
```

This shows a chronological list of all changes, including status transitions, assignments, and comments.

## 7. Listing Work Items

To see all work items assigned to you:

```bash
bin/rin-cli my-work
```

To see all work items in the project:

```bash
bin/rin-cli list
```

To filter work items by different criteria:

```bash
bin/rin-cli list --type FEATURE --status IN_PROGRESS
bin/rin-cli list --assignee "your-username" --priority HIGH
```

## 8. Adding Work Item Relationships

If your work item has dependencies or is related to other work items:

```bash
# Make WI-123 depend on WI-456
bin/rin-cli link WI-123 DEPENDS_ON WI-456

# Mark WI-123 as a subtask of WI-789
bin/rin-cli link WI-123 SUBTASK_OF WI-789

# Mark WI-123 as related to WI-321
bin/rin-cli link WI-123 RELATED_TO WI-321
```

## 9. Work Item Metadata

To add custom metadata to your work item:

```bash
bin/rin-cli metadata WI-123 add storyPoints 5
bin/rin-cli metadata WI-123 add component "frontend"
```

## Next Steps

Now that you've learned the basics of working with Rinna work items, you might want to explore:

- [Managing Work Item Dependencies](../user-guide/work-item-relationships.md)
- [CLI Quick Reference](../user-guide/rin-quick-reference.md) for more commands
- [Workflow Philosophy](../user-guide/workflow-philosophy.md) to understand Rinna's approach

---

**Tip**: Use `bin/rin-cli --help` or `bin/rin-cli <command> --help` for detailed command information.
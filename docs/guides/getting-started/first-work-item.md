# Creating Your First Work Item

This guide walks you through creating and managing your first work item in Rinna.

## Prerequisites

Before starting, ensure you have:
- Installed Rinna following the [Getting Started Guide](README.md)
- Started the Rinna server with `./rinna-server start`

## Creating a Work Item

To create your first work item, use the `rin add` command:

```bash
# Create a feature work item
rin add "Implement login page" --type=FEATURE --priority=HIGH

# Create a bug work item
rin add "Fix login button alignment" --type=BUG
```

The system will respond with a confirmation and the ID of your new work item:

```
Work item WI-123 created successfully.
```

## Viewing Work Items

To view your work item details:

```bash
# View by ID
rin view WI-123
```

This will display all details about your work item, including:
- ID and title
- Status and priority
- Creation and update timestamps
- Assigned user (if any)
- Description and comments

## Updating Work Items

You can update your work item using the `rin update` command:

```bash
# Update status
rin update WI-123 --status=IN_PROGRESS

# Assign to yourself
rin update WI-123 --assign=me

# Update priority
rin update WI-123 --priority=CRITICAL
```

## Workflow Transitions

As you work on your item, update its status to reflect its progress:

```bash
# Start working
rin update WI-123 --status=IN_PROGRESS

# Ready for testing
rin update WI-123 --status=IN_TEST

# Mark as complete
rin done WI-123
```

## Adding Comments

You can add comments to document progress or issues:

```bash
rin comment WI-123 "Completed the first implementation, ready for review"
```

## Next Steps

Now that you've created your first work item, you can:
- Learn about [Workflow States](../user/workflow-guide.md)
- Explore [Advanced CLI Features](../user/cli-reference.md)
- Set up a [Custom Workflow](../user/advanced-workflows.md)
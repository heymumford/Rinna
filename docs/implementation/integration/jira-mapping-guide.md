# Jira Integration and Mapping Guide

This guide provides detailed instructions for integrating Rinna with Jira, focusing on field mappings, workflow states, and synchronization strategies.

## Overview

Jira is one of the most widely used issue tracking and project management tools. Rinna's Jira integration allows for seamless two-way synchronization of work items, ensuring that teams can work in either system while maintaining consistent data.

## Prerequisites

Before setting up the Jira integration, ensure you have:

1. A Jira Cloud or Server instance (version 7.0 or higher)
2. Administrator access to your Jira instance
3. A Jira API token (created at `https://id.atlassian.com/manage-profile/security/api-tokens`)
4. Permission to create webhooks in Jira (for real-time synchronization)

## Configuration

### Basic Setup

To configure the Jira integration:

```bash
rin admin integration configure jira \
  --url="https://your-domain.atlassian.net" \
  --user="your-email@example.com" \
  --token="your-api-token" \
  --project="PROJECT" \
  --sync-mode="two-way"
```

Available sync modes:
- `two-way`: Changes in either system are synchronized
- `rinna-to-jira`: Only changes in Rinna are pushed to Jira
- `jira-to-rinna`: Only changes in Jira are pulled into Rinna
- `manual`: Synchronization only happens when explicitly triggered

### Verification

Verify your configuration:

```bash
rin admin integration test-connection jira
```

## Field Mapping

### Default Field Mapping

The default field mapping between Rinna and Jira:

| Rinna Field    | Jira Field                 | Notes                         |
|----------------|-----------------------------|-------------------------------|
| id             | key                         | Rinna work item ID to Jira key |
| title          | summary                     |                               |
| description    | description                 |                               |
| type           | issuetype                   |                               |
| priority       | priority                    | Value transformation required |
| status         | status                      | Value transformation required |
| assignee       | assignee                    | Username mapping may be needed |
| createdAt      | created                     | Read-only in Jira             |
| updatedAt      | updated                     | Read-only in Jira             |

### Custom Field Mapping

To map custom fields:

```bash
# Create a mapping file
cat > jira-custom-fields.yaml << EOL
mappings:
  - rinna_field: "story_points"
    jira_field: "customfield_10002"
  - rinna_field: "epic_link"
    jira_field: "customfield_10006"
  - rinna_field: "sprint"
    jira_field: "customfield_10007"
  - rinna_field: "business_value"
    jira_field: "customfield_10008"
EOL

# Apply the mapping
rin admin integration custom-fields apply jira --file="jira-custom-fields.yaml"
```

### Finding Jira Custom Field IDs

To find the IDs of custom fields in Jira:

1. Use the Rinna Jira field discovery tool:
   ```bash
   rin admin integration jira discover-fields --project="PROJECT"
   ```

2. Or manually check in Jira:
   a. Create a test issue
   b. Open the issue in your browser
   c. Open browser developer tools and inspect the custom field elements
   d. Look for `customfield_XXXXX` in the DOM

### Field Value Transformations

Define transformations for field values:

```bash
# Create a transformation file
cat > jira-transformations.yaml << EOL
transformations:
  - field: "priority"
    mappings:
      - from: "highest"        # Rinna value
        to: "Highest"          # Jira value
      - from: "high"
        to: "High"
      - from: "medium"
        to: "Medium"
      - from: "low"
        to: "Low"
      - from: "lowest"
        to: "Lowest"
  - field: "type"
    mappings:
      - from: "bug"
        to: "Bug"
      - from: "task"
        to: "Task"
      - from: "story"
        to: "Story"
      - from: "epic"
        to: "Epic"
EOL

# Apply the transformations
rin admin integration transformations apply jira --file="jira-transformations.yaml"
```

## Workflow State Mapping

Map Rinna workflow states to Jira statuses:

```bash
# Create a workflow mapping file
cat > jira-workflow.yaml << EOL
workflow:
  - rinna_state: "TO_DO"
    jira_status: "To Do"
  - rinna_state: "IN_PROGRESS"
    jira_status: "In Progress"
  - rinna_state: "IN_REVIEW"
    jira_status: "In Review"
  - rinna_state: "DONE"
    jira_status: "Done"
  - rinna_state: "BLOCKED"
    jira_status: "Blocked"
EOL

# Apply the workflow mapping
rin admin integration workflow-map apply jira --file="jira-workflow.yaml"
```

### Project-Specific Workflow Mapping

For project-specific workflow mappings:

```bash
rin admin integration workflow-map apply jira \
  --file="jira-workflow.yaml" \
  --project="PROJECT"
```

## User Mapping

Map Rinna users to Jira users:

```bash
# Create a user mapping file
cat > jira-users.yaml << EOL
users:
  - rinna_user: "john.doe"
    jira_user: "john.doe@example.com"
  - rinna_user: "jane.smith"
    jira_user: "jane.smith@example.com"
EOL

# Apply the user mapping
rin admin integration user-map apply jira --file="jira-users.yaml"
```

## Synchronization

### Manual Synchronization

Manually synchronize work items:

```bash
# Sync a specific work item to Jira
rin admin integration sync jira --item="WI-123"

# Sync all items in a project
rin admin integration sync jira --project="PROJECT"

# Sync items with a specific status
rin admin integration sync jira --status="IN_PROGRESS"

# Import an item from Jira
rin admin integration import jira --jira-key="PROJ-123"

# Import all items from a Jira project
rin admin integration import jira --jira-project="PROJ" --max-items=100
```

### Scheduled Synchronization

Set up a scheduled synchronization:

```bash
# Sync every 15 minutes
rin admin integration schedule sync jira --interval=15m

# Sync every hour with specific options
rin admin integration schedule sync jira \
  --interval=1h \
  --project="PROJECT" \
  --status="IN_PROGRESS,BLOCKED"
```

### Real-Time Synchronization with Webhooks

Set up Jira webhooks for real-time synchronization:

```bash
# Generate a webhook secret
webhook_secret=$(rin admin integration generate-secret)

# Configure the webhook endpoint
rin admin integration webhook create jira \
  --url="https://your-server.com/api/webhooks/jira" \
  --secret="$webhook_secret" \
  --events="jira:issue_created,jira:issue_updated,jira:issue_deleted"

# Start the webhook listener
rin admin integration webhook listen --port=8080
```

Then configure the webhook in Jira:
1. Go to Jira Settings > System > WebHooks
2. Click "Create WebHook"
3. Enter a name like "Rinna Integration"
4. Enter the URL: `https://your-server.com/api/webhooks/jira`
5. Under "Events", select:
   - Issue: created, updated, deleted
   - Comment: created, updated, deleted
6. Click "Create"

## Advanced Features

### JQL Query Import

Import work items from Jira using JQL (Jira Query Language):

```bash
# Import based on a JQL query
rin admin integration import jira \
  --jql="project = PROJ AND status = 'In Progress' AND assignee = currentUser()" \
  --max-items=50
```

### Attachment Synchronization

Configure attachment synchronization:

```bash
# Enable attachment synchronization
rin admin integration configure jira \
  --sync-attachments=true \
  --attachment-dir="/path/to/attachments" \
  --max-attachment-size=10MB
```

### Comment Synchronization

Configure comment synchronization:

```bash
# Enable comment synchronization
rin admin integration configure jira \
  --sync-comments=true \
  --sync-comment-authors=true
```

## Troubleshooting

### Common Issues

#### Authentication Failures

If you encounter authentication issues:

1. Verify your API token has not expired
2. Test the connection:
   ```bash
   rin admin integration test-connection jira --verbose
   ```
3. Check for permission issues:
   ```bash
   rin admin integration verify-permissions jira
   ```

#### Field Mapping Issues

If fields are not mapping correctly:

1. Test your mapping configuration:
   ```bash
   rin admin integration test-mapping jira --field="priority" --value="high"
   ```
2. Debug the mapping process:
   ```bash
   rin admin integration debug-mapping jira --item="WI-123" --verbose
   ```

#### Workflow State Transition Issues

If workflow state transitions fail:

1. Check if the transition is valid in Jira:
   ```bash
   rin admin integration test-transition jira \
     --item="WI-123" \
     --target-status="IN_REVIEW"
   ```
2. View available transitions:
   ```bash
   rin admin integration show-transitions jira --jira-key="PROJ-123"
   ```

### Logs and Diagnostics

Access integration logs:

```bash
# View recent integration logs
rin admin integration logs jira --lines=50

# View logs with debug level
rin admin integration logs jira --level=debug

# Export logs for support
rin admin integration logs jira --export-file="jira-integration-logs.txt"
```

Run diagnostics:

```bash
# Run a comprehensive diagnostic check
rin admin diagnostics integration jira --verbose

# Export diagnostic information
rin admin diagnostics export --file="jira-diagnostic-report.json"
```

## Best Practices

1. **Start Small**: Begin by mapping essential fields and a small number of work items
2. **Test Thoroughly**: Test the integration with sample work items before full deployment
3. **Document Mappings**: Document all field and workflow mappings for team reference
4. **User Training**: Train users on how the integration affects their workflow
5. **Monitor Synchronization**: Regularly review synchronization logs for issues
6. **Back Up Data**: Back up your Rinna data before making major changes to the integration
7. **Use Webhooks**: For active projects, use webhooks for real-time synchronization

## Conclusion

This guide provides a comprehensive overview of integrating Rinna with Jira. By following these guidelines, you can create a seamless integration between the two systems, allowing teams to work in their preferred environment while maintaining data consistency.

For additional support or advanced integration scenarios, refer to the [Enterprise System Integration Guide](./enterprise-system-integration.md) or contact Rinna support.
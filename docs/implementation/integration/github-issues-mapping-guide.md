# GitHub Issues Integration and Mapping Guide

This guide provides detailed instructions for integrating Rinna with GitHub Issues, focusing on field mappings, workflow states, and synchronization strategies.

## Overview

GitHub Issues is a popular issue tracking system integrated with the GitHub platform. Rinna's GitHub Issues integration allows for seamless synchronization of work items, enabling teams to maintain consistent data between systems while leveraging GitHub's collaboration features.

## Prerequisites

Before setting up the GitHub Issues integration, ensure you have:

1. A GitHub repository with Issues enabled
2. Administrator access to the GitHub repository
3. A GitHub Personal Access Token (PAT) with the following scopes:
   - `repo` - Full control of private repositories
   - `admin:repo_hook` - Full control of repository hooks (for webhooks)
4. Knowledge of GitHub's label structure (to represent workflow states)

## Configuration

### Basic Setup

To configure the GitHub Issues integration:

```bash
rin admin integration configure github \
  --repo="owner/repository" \
  --token="your-github-token" \
  --sync-mode="two-way"
```

Available sync modes:
- `two-way`: Changes in either system are synchronized
- `rinna-to-github`: Only changes in Rinna are pushed to GitHub
- `github-to-rinna`: Only changes in GitHub are pulled into Rinna
- `manual`: Synchronization only happens when explicitly triggered

For multiple repositories:

```bash
rin admin integration configure github \
  --repos="owner/repo1,owner/repo2,owner/repo3" \
  --token="your-github-token" \
  --sync-mode="two-way"
```

### Verification

Verify your configuration:

```bash
rin admin integration test-connection github
```

## Field Mapping

### Default Field Mapping

The default field mapping between Rinna and GitHub Issues:

| Rinna Field    | GitHub Field          | Notes                           |
|----------------|------------------------|----------------------------------|
| id             | number                 | Rinna work item ID to GitHub number |
| title          | title                  |                                |
| description    | body                   |                                |
| type           | labels                 | Mapped to type:* labels         |
| priority       | labels                 | Mapped to priority:* labels     |
| status         | state + labels         | Open/closed + status:* labels   |
| assignee       | assignees              |                                |
| createdAt      | created_at             | Read-only in GitHub            |
| updatedAt      | updated_at             | Read-only in GitHub            |
| labels         | labels                 | Excluding special labels       |

### Custom Field Mapping

To map custom fields to GitHub labels or other properties:

```bash
# Create a mapping file
cat > github-custom-fields.yaml << EOL
mappings:
  - rinna_field: "epic"
    github_field: "labels"
    prefix: "epic:"
  - rinna_field: "complexity"
    github_field: "labels"
    prefix: "complexity:"
  - rinna_field: "estimated_hours"
    github_field: "body"
    pattern: "<!-- estimated_hours: {value} -->"
  - rinna_field: "milestone"
    github_field: "milestone"
EOL

# Apply the mapping
rin admin integration custom-fields apply github --file="github-custom-fields.yaml"
```

### Label Mapping Strategy

Since GitHub Issues primarily uses labels for categorization, configure label mappings:

```bash
# Create a label mapping file
cat > github-labels.yaml << EOL
labels:
  - rinna_field: "type"
    values:
      - rinna_value: "bug"
        github_label: "type:bug"
      - rinna_value: "feature"
        github_label: "type:feature"
      - rinna_value: "task"
        github_label: "type:task"
      - rinna_value: "docs"
        github_label: "type:documentation"
  - rinna_field: "priority"
    values:
      - rinna_value: "highest"
        github_label: "priority:critical"
      - rinna_value: "high"
        github_label: "priority:high"
      - rinna_value: "medium"
        github_label: "priority:medium"
      - rinna_value: "low"
        github_label: "priority:low"
      - rinna_value: "lowest"
        github_label: "priority:trivial"
EOL

# Apply the label mapping
rin admin integration label-map apply github --file="github-labels.yaml"
```

### Markdown Field Transformations

Since GitHub Issues uses Markdown for issue bodies, configure Markdown transformations:

```bash
# Enable Markdown transformations
rin admin integration configure github \
  --enable-markdown-transform=true \
  --preserve-formatting=true
```

## Workflow State Mapping

Map Rinna workflow states to GitHub Issues states (open/closed) and labels:

```bash
# Create a workflow mapping file
cat > github-workflow.yaml << EOL
workflow:
  - rinna_state: "TO_DO"
    github_state: "open"
    github_label: "status:todo"
  - rinna_state: "IN_PROGRESS"
    github_state: "open"
    github_label: "status:in-progress"
  - rinna_state: "IN_REVIEW"
    github_state: "open"
    github_label: "status:in-review"
  - rinna_state: "DONE"
    github_state: "closed"
    github_label: "status:done"
  - rinna_state: "BLOCKED"
    github_state: "open"
    github_label: "status:blocked"
EOL

# Apply the workflow mapping
rin admin integration workflow-map apply github --file="github-workflow.yaml"
```

### Auto-create Labels

To automatically create all required labels in GitHub:

```bash
rin admin integration github create-labels --repo="owner/repository"
```

This creates all necessary labels for status, type, and priority with appropriate colors.

## User Mapping

Map Rinna users to GitHub users:

```bash
# Create a user mapping file
cat > github-users.yaml << EOL
users:
  - rinna_user: "john.doe"
    github_user: "johndoe"
  - rinna_user: "jane.smith"
    github_user: "janesmith"
EOL

# Apply the user mapping
rin admin integration user-map apply github --file="github-users.yaml"
```

## Synchronization

### Manual Synchronization

Manually synchronize work items:

```bash
# Sync a specific work item to GitHub
rin admin integration sync github --item="WI-123"

# Sync all items in a project
rin admin integration sync github --project="PROJECT"

# Sync items with a specific status
rin admin integration sync github --status="IN_PROGRESS"

# Import an issue from GitHub
rin admin integration import github --repo="owner/repository" --issue="123"

# Import all issues from a GitHub repository
rin admin integration import github --repo="owner/repository" --max-items=100
```

### Scheduled Synchronization

Set up a scheduled synchronization:

```bash
# Sync every 15 minutes
rin admin integration schedule sync github --interval=15m

# Sync every hour with specific options
rin admin integration schedule sync github \
  --interval=1h \
  --project="PROJECT" \
  --status="IN_PROGRESS,BLOCKED"
```

### Real-Time Synchronization with Webhooks

Set up GitHub webhooks for real-time synchronization:

```bash
# Generate a webhook secret
webhook_secret=$(rin admin integration generate-secret)

# Configure the webhook endpoint
rin admin integration webhook create github \
  --url="https://your-server.com/api/webhooks/github" \
  --secret="$webhook_secret" \
  --events="issues,issue_comment"

# Start the webhook listener
rin admin integration webhook listen --port=8080
```

Then configure the webhook in GitHub:
1. Go to your repository > Settings > Webhooks
2. Click "Add webhook"
3. Enter the Payload URL: `https://your-server.com/api/webhooks/github`
4. Content type: `application/json`
5. Secret: Enter the generated webhook secret
6. Under "Which events would you like to trigger this webhook?":
   - Select "Let me select individual events"
   - Check "Issues" and "Issue comments"
7. Ensure "Active" is checked
8. Click "Add webhook"

## Advanced Features

### Comment Synchronization

Configure comment synchronization:

```bash
# Enable comment synchronization
rin admin integration configure github \
  --sync-comments=true \
  --comment-format="From Rinna: {comment}" \
  --sync-comment-authors=true
```

### Milestone Mapping

Map Rinna versions to GitHub milestones:

```bash
# Create a milestone mapping file
cat > github-milestones.yaml << EOL
milestones:
  - rinna_version: "1.0"
    github_milestone: "v1.0"
  - rinna_version: "1.1"
    github_milestone: "v1.1"
EOL

# Apply the milestone mapping
rin admin integration milestone-map apply github --file="github-milestones.yaml"
```

### Auto-Create Milestones

To automatically create milestones in GitHub:

```bash
rin admin integration github create-milestones \
  --repo="owner/repository" \
  --from-versions=true
```

### GitHub Project Board Integration

Integrate with GitHub Project boards:

```bash
# Configure Project board integration
rin admin integration configure github-project \
  --repo="owner/repository" \
  --project="Project Board Name" \
  --map-columns=true
```

Map Rinna workflow states to project board columns:

```bash
# Create a project column mapping file
cat > github-project-columns.yaml << EOL
columns:
  - rinna_state: "TO_DO"
    github_column: "To do"
  - rinna_state: "IN_PROGRESS"
    github_column: "In progress"
  - rinna_state: "IN_REVIEW"
    github_column: "Review in progress"
  - rinna_state: "DONE"
    github_column: "Done"
EOL

# Apply the project column mapping
rin admin integration project-column-map apply github --file="github-project-columns.yaml"
```

## Troubleshooting

### Common Issues

#### Authentication Failures

If you encounter authentication issues:

1. Verify your Personal Access Token has not expired
2. Ensure the PAT has the required scopes
3. Test the connection:
   ```bash
   rin admin integration test-connection github --verbose
   ```
4. Check for rate limiting issues:
   ```bash
   rin admin integration github rate-limit-status
   ```

#### Label Mapping Issues

If labels are not mapping correctly:

1. Verify labels exist in the GitHub repository:
   ```bash
   rin admin integration github list-labels --repo="owner/repository"
   ```
2. Test your mapping configuration:
   ```bash
   rin admin integration test-mapping github --field="priority" --value="high"
   ```
3. Create missing labels:
   ```bash
   rin admin integration github create-label \
     --repo="owner/repository" \
     --name="priority:high" \
     --color="#ff9900" \
     --description="High priority items"
   ```

#### Webhook Issues

If webhooks are not working:

1. Check webhook delivery status in GitHub:
   - Go to repository Settings > Webhooks > Click on the webhook
   - Review recent deliveries and their responses
2. Verify the webhook server is accessible:
   ```bash
   rin admin integration webhook test-endpoint
   ```
3. Check webhook logs:
   ```bash
   rin admin integration webhook logs --lines=50 --level=debug
   ```

### Logs and Diagnostics

Access integration logs:

```bash
# View recent integration logs
rin admin integration logs github --lines=50

# View logs with debug level
rin admin integration logs github --level=debug

# Export logs for support
rin admin integration logs github --export-file="github-integration-logs.txt"
```

Run diagnostics:

```bash
# Run a comprehensive diagnostic check
rin admin diagnostics integration github --verbose

# Export diagnostic information
rin admin diagnostics export --file="github-diagnostic-report.json"
```

## Best Practices

1. **Consistent Labeling**: Establish a consistent labeling scheme in GitHub that maps to Rinna work item fields
2. **State Representation**: Use status labels consistently to represent workflow states
3. **Start Small**: Begin by mapping essential fields and a small number of work items
4. **Test Thoroughly**: Test the integration with sample work items before full deployment
5. **Use Webhooks**: For active projects, use webhooks for real-time synchronization
6. **Document Mappings**: Document all label and milestone mappings for team reference
7. **Regular Maintenance**: Periodically check and update label mappings as projects evolve
8. **Clear Communication**: Clearly communicate to team members how the integration works and impacts their workflow

## Example Integration Script

Here's a complete example script to set up a GitHub Issues integration:

```bash
#!/bin/bash
# Script to set up a complete GitHub Issues integration

# Set variables
REPO="owner/repository"
TOKEN="your-github-token"
WEBHOOK_URL="https://your-server.com/api/webhooks/github"

# Configure the integration
rin admin integration configure github \
  --repo="$REPO" \
  --token="$TOKEN" \
  --sync-mode="two-way" \
  --enable-markdown-transform=true

# Create and apply label mappings
cat > github-labels.yaml << EOL
labels:
  - rinna_field: "type"
    values:
      - rinna_value: "bug"
        github_label: "type:bug"
      - rinna_value: "feature"
        github_label: "type:feature"
      - rinna_value: "task"
        github_label: "type:task"
  - rinna_field: "priority"
    values:
      - rinna_value: "high"
        github_label: "priority:high"
      - rinna_value: "medium"
        github_label: "priority:medium"
      - rinna_value: "low"
        github_label: "priority:low"
EOL

rin admin integration label-map apply github --file="github-labels.yaml"

# Create and apply workflow mapping
cat > github-workflow.yaml << EOL
workflow:
  - rinna_state: "TO_DO"
    github_state: "open"
    github_label: "status:todo"
  - rinna_state: "IN_PROGRESS"
    github_state: "open"
    github_label: "status:in-progress"
  - rinna_state: "IN_REVIEW"
    github_state: "open"
    github_label: "status:in-review"
  - rinna_state: "DONE"
    github_state: "closed"
    github_label: "status:done"
EOL

rin admin integration workflow-map apply github --file="github-workflow.yaml"

# Create necessary labels in GitHub
rin admin integration github create-labels --repo="$REPO"

# Set up webhook
webhook_secret=$(rin admin integration generate-secret)
rin admin integration webhook create github \
  --url="$WEBHOOK_URL" \
  --secret="$webhook_secret" \
  --events="issues,issue_comment"

echo "GitHub Issues integration set up successfully"
echo "Webhook Secret: $webhook_secret"
echo "Instructions:"
echo "1. Go to $REPO/settings/hooks"
echo "2. Add webhook with URL: $WEBHOOK_URL"
echo "3. Set secret to: $webhook_secret"
echo "4. Select events: Issues, Issue comments"
echo "5. Ensure 'Active' is checked"
```

## Conclusion

This guide provides a comprehensive overview of integrating Rinna with GitHub Issues. By following these guidelines, you can create a seamless integration between the two systems, leveraging GitHub's collaboration features while maintaining consistent workflow data in Rinna.

For additional support or advanced integration scenarios, refer to the [Enterprise System Integration Guide](./enterprise-system-integration.md) or contact Rinna support.
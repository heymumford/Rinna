# Azure DevOps Integration and Mapping Guide

This guide provides detailed instructions for integrating Rinna with Azure DevOps, focusing on field mappings, workflow states, and synchronization strategies.

## Overview

Azure DevOps is a comprehensive suite of development tools for teams to plan work, collaborate on code, and build and deploy applications. Rinna's Azure DevOps integration allows for seamless two-way synchronization of work items, ensuring that teams can work in either system while maintaining consistent data.

## Prerequisites

Before setting up the Azure DevOps integration, ensure you have:

1. An Azure DevOps organization and project
2. Administrator access to your Azure DevOps organization
3. A Personal Access Token (PAT) with appropriate permissions:
   - Work Items: Read, Write, & Manage
   - Build: Read & Execute
   - Code: Read
4. Permission to create service hooks in Azure DevOps (for real-time synchronization)

## Configuration

### Basic Setup

To configure the Azure DevOps integration:

```bash
rin admin integration configure azure-devops \
  --url="https://dev.azure.com/organization" \
  --user="your-email@example.com" \
  --token="your-personal-access-token" \
  --project="PROJECT" \
  --team="TEAM" \
  --sync-mode="two-way"
```

Available sync modes:
- `two-way`: Changes in either system are synchronized
- `rinna-to-azure`: Only changes in Rinna are pushed to Azure DevOps
- `azure-to-rinna`: Only changes in Azure DevOps are pulled into Rinna
- `manual`: Synchronization only happens when explicitly triggered

### Verification

Verify your configuration:

```bash
rin admin integration test-connection azure-devops
```

## Field Mapping

### Default Field Mapping

The default field mapping between Rinna and Azure DevOps:

| Rinna Field    | Azure DevOps Field          | Notes                         |
|----------------|------------------------------|-------------------------------|
| id             | System.Id                    | Rinna work item ID to Azure ID |
| title          | System.Title                 |                               |
| description    | System.Description           |                               |
| type           | System.WorkItemType          |                               |
| priority       | Microsoft.VSTS.Common.Priority | Value transformation required |
| status         | System.State                 | Value transformation required |
| assignee       | System.AssignedTo            | User mapping may be needed    |
| createdAt      | System.CreatedDate           | Read-only in Azure DevOps     |
| updatedAt      | System.ChangedDate           | Read-only in Azure DevOps     |

### Custom Field Mapping

To map custom fields:

```bash
# Create a mapping file
cat > azure-custom-fields.yaml << EOL
mappings:
  - rinna_field: "story_points"
    azure_field: "Microsoft.VSTS.Scheduling.StoryPoints"
  - rinna_field: "business_value"
    azure_field: "Microsoft.VSTS.Common.BusinessValue"
  - rinna_field: "risk"
    azure_field: "Microsoft.VSTS.Common.Risk"
  - rinna_field: "acceptance_criteria"
    azure_field: "Microsoft.VSTS.Common.AcceptanceCriteria"
EOL

# Apply the mapping
rin admin integration custom-fields apply azure-devops --file="azure-custom-fields.yaml"
```

### Finding Azure DevOps Field References

To find the field references in Azure DevOps:

1. Use the Rinna Azure DevOps field discovery tool:
   ```bash
   rin admin integration azure-devops discover-fields --project="PROJECT"
   ```

2. Or using the Azure DevOps REST API:
   ```bash
   rin admin integration azure-devops list-fields
   ```

3. Or manually:
   - Go to Project Settings > Work > Process
   - Select your process
   - View the work item types and their fields

### Field Value Transformations

Define transformations for field values:

```bash
# Create a transformation file
cat > azure-transformations.yaml << EOL
transformations:
  - field: "priority"
    mappings:
      - from: "highest"        # Rinna value
        to: "1"                # Azure DevOps value
      - from: "high"
        to: "2"
      - from: "medium"
        to: "3"
      - from: "low"
        to: "4"
  - field: "type"
    mappings:
      - from: "bug"
        to: "Bug"
      - from: "task"
        to: "Task"
      - from: "story"
        to: "User Story"
      - from: "epic"
        to: "Epic"
      - from: "feature"
        to: "Feature"
EOL

# Apply the transformations
rin admin integration transformations apply azure-devops --file="azure-transformations.yaml"
```

## Workflow State Mapping

Map Rinna workflow states to Azure DevOps states:

```bash
# Create a workflow mapping file
cat > azure-workflow.yaml << EOL
workflow:
  - rinna_state: "TO_DO"
    azure_state: "New"
  - rinna_state: "IN_PROGRESS"
    azure_state: "Active"
  - rinna_state: "IN_REVIEW"
    azure_state: "Resolved"
  - rinna_state: "DONE"
    azure_state: "Closed"
  - rinna_state: "BLOCKED"
    azure_state: "Blocked"
EOL

# Apply the workflow mapping
rin admin integration workflow-map apply azure-devops --file="azure-workflow.yaml"
```

### Work Item Type-Specific Workflow Mapping

For workflow mappings specific to work item types:

```bash
# Create a type-specific mapping file
cat > azure-bug-workflow.yaml << EOL
workflow:
  work_item_type: "Bug"
  mappings:
    - rinna_state: "TO_DO"
      azure_state: "New"
    - rinna_state: "IN_PROGRESS"
      azure_state: "Active"
    - rinna_state: "IN_REVIEW"
      azure_state: "Resolved"
    - rinna_state: "DONE"
      azure_state: "Closed"
EOL

# Apply the type-specific workflow mapping
rin admin integration workflow-map apply azure-devops \
  --file="azure-bug-workflow.yaml" \
  --type="Bug"
```

## User Mapping

Map Rinna users to Azure DevOps users:

```bash
# Create a user mapping file
cat > azure-users.yaml << EOL
users:
  - rinna_user: "john.doe"
    azure_user: "john.doe@example.com"
  - rinna_user: "jane.smith"
    azure_user: "jane.smith@example.com"
EOL

# Apply the user mapping
rin admin integration user-map apply azure-devops --file="azure-users.yaml"
```

## Synchronization

### Manual Synchronization

Manually synchronize work items:

```bash
# Sync a specific work item to Azure DevOps
rin admin integration sync azure-devops --item="WI-123"

# Sync all items in a project
rin admin integration sync azure-devops --project="PROJECT"

# Sync items with a specific status
rin admin integration sync azure-devops --status="IN_PROGRESS"

# Import an item from Azure DevOps
rin admin integration import azure-devops --azure-id="12345"

# Import all items from an Azure DevOps project
rin admin integration import azure-devops --azure-project="PROJECT" --max-items=100
```

### Scheduled Synchronization

Set up a scheduled synchronization:

```bash
# Sync every 15 minutes
rin admin integration schedule sync azure-devops --interval=15m

# Sync every hour with specific options
rin admin integration schedule sync azure-devops \
  --interval=1h \
  --project="PROJECT" \
  --status="IN_PROGRESS,BLOCKED"
```

### Real-Time Synchronization with Service Hooks

Set up Azure DevOps service hooks for real-time synchronization:

```bash
# Generate a webhook secret
webhook_secret=$(rin admin integration generate-secret)

# Configure the webhook endpoint
rin admin integration webhook create azure-devops \
  --url="https://your-server.com/api/webhooks/azure-devops" \
  --secret="$webhook_secret" \
  --events="workitem.created,workitem.updated,workitem.deleted"

# Start the webhook listener
rin admin integration webhook listen --port=8080
```

Then configure the service hook in Azure DevOps:
1. Go to Project Settings > Service hooks
2. Click "Create subscription"
3. Select "Web Hooks" as the service
4. For the trigger, select "Work item created", "Work item updated", or "Work item deleted"
5. Set filters as needed (e.g., specific work item types)
6. For the action, enter the URL: `https://your-server.com/api/webhooks/azure-devops`
7. Add the secret as a header: `X-Hub-Signature: sha1=<your-secret>`
8. Complete the subscription

Repeat for each event type you want to synchronize.

## Advanced Features

### WIQL Query Import

Import work items from Azure DevOps using WIQL (Work Item Query Language):

```bash
# Import based on a WIQL query
rin admin integration import azure-devops \
  --wiql="SELECT [System.Id] FROM WorkItems WHERE [System.TeamProject] = 'PROJECT' AND [System.State] = 'Active' AND [System.AssignedTo] = @me" \
  --max-items=50
```

### Link Work Items

Link Rinna work items to Azure DevOps work items:

```bash
# Create parent-child relationship
rin admin integration azure-devops link-items \
  --rinna-item="WI-123" \
  --azure-id="12345" \
  --link-type="parent"

# Create a dependency link
rin admin integration azure-devops link-items \
  --rinna-item="WI-123" \
  --azure-id="12346" \
  --link-type="dependency"
```

### Attachment Synchronization

Configure attachment synchronization:

```bash
# Enable attachment synchronization
rin admin integration configure azure-devops \
  --sync-attachments=true \
  --attachment-dir="/path/to/attachments" \
  --max-attachment-size=10MB
```

### Area and Iteration Path Mapping

Map Rinna projects and versions to Azure DevOps area and iteration paths:

```bash
# Create an area path mapping file
cat > azure-area-paths.yaml << EOL
area_paths:
  - rinna_project: "ProjectA"
    azure_area_path: "PROJECT\\Area\\TeamA"
  - rinna_project: "ProjectB"
    azure_area_path: "PROJECT\\Area\\TeamB"
EOL

# Apply the area path mapping
rin admin integration area-path-map apply azure-devops --file="azure-area-paths.yaml"

# Create an iteration path mapping file
cat > azure-iteration-paths.yaml << EOL
iteration_paths:
  - rinna_version: "1.0"
    azure_iteration_path: "PROJECT\\Iteration\\Sprint1"
  - rinna_version: "1.1"
    azure_iteration_path: "PROJECT\\Iteration\\Sprint2"
EOL

# Apply the iteration path mapping
rin admin integration iteration-path-map apply azure-devops --file="azure-iteration-paths.yaml"
```

## Troubleshooting

### Common Issues

#### Authentication Failures

If you encounter authentication issues:

1. Verify your Personal Access Token has not expired
2. Ensure the PAT has the required permissions
3. Test the connection:
   ```bash
   rin admin integration test-connection azure-devops --verbose
   ```
4. Check for permission issues:
   ```bash
   rin admin integration verify-permissions azure-devops
   ```

#### Field Mapping Issues

If fields are not mapping correctly:

1. Test your mapping configuration:
   ```bash
   rin admin integration test-mapping azure-devops --field="priority" --value="high"
   ```
2. Debug the mapping process:
   ```bash
   rin admin integration debug-mapping azure-devops --item="WI-123" --verbose
   ```

#### Workflow State Transition Issues

If workflow state transitions fail:

1. Check if the transition is valid in Azure DevOps:
   ```bash
   rin admin integration test-transition azure-devops \
     --item="WI-123" \
     --target-status="IN_REVIEW"
   ```
2. View available transitions:
   ```bash
   rin admin integration show-transitions azure-devops --azure-id="12345"
   ```

### Logs and Diagnostics

Access integration logs:

```bash
# View recent integration logs
rin admin integration logs azure-devops --lines=50

# View logs with debug level
rin admin integration logs azure-devops --level=debug

# Export logs for support
rin admin integration logs azure-devops --export-file="azure-devops-integration-logs.txt"
```

Run diagnostics:

```bash
# Run a comprehensive diagnostic check
rin admin diagnostics integration azure-devops --verbose

# Export diagnostic information
rin admin diagnostics export --file="azure-devops-diagnostic-report.json"
```

## Best Practices

1. **Start Small**: Begin by mapping essential fields and a small number of work items
2. **Test Thoroughly**: Test the integration with sample work items before full deployment
3. **Document Mappings**: Document all field, workflow, and area/iteration path mappings for team reference
4. **User Training**: Train users on how the integration affects their workflow
5. **Monitor Synchronization**: Regularly review synchronization logs for issues
6. **Back Up Data**: Back up your Rinna data before making major changes to the integration
7. **Use Service Hooks**: For active projects, use service hooks for real-time synchronization
8. **Establish Governance**: Define clear rules for who can create/modify mappings

## Conclusion

This guide provides a comprehensive overview of integrating Rinna with Azure DevOps. By following these guidelines, you can create a seamless integration between the two systems, allowing teams to work in their preferred environment while maintaining data consistency.

For additional support or advanced integration scenarios, refer to the [Enterprise System Integration Guide](./enterprise-system-integration.md) or contact Rinna support.
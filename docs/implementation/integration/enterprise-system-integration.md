# Rinna Enterprise System Integration Guide

This guide provides comprehensive instructions for integrating Rinna with enterprise systems such as issue trackers, version control systems, CI/CD pipelines, and other workflow management tools.

## Overview

Rinna is designed as a Standardized Utility Shell-Based Solution (SUSBS) with built-in capabilities for enterprise integration. This document explains how to connect Rinna to various enterprise systems to create a seamless workflow ecosystem.

## Table of Contents

1. [Architecture](#architecture)
2. [Integration Patterns](#integration-patterns)
3. [Issue Tracker Integration](#issue-tracker-integration)
   - [Jira Integration](#jira-integration)
   - [Azure DevOps Integration](#azure-devops-integration)
   - [GitHub Issues Integration](#github-issues-integration)
4. [Version Control Integration](#version-control-integration)
   - [Git Integration](#git-integration)
   - [Branch and Commit Linking](#branch-and-commit-linking)
5. [CI/CD Pipeline Integration](#cicd-pipeline-integration)
6. [Authentication and Security](#authentication-and-security)
7. [Synchronization Strategies](#synchronization-strategies)
8. [Mapping Work Item Fields](#mapping-work-item-fields)
9. [Automation and Webhooks](#automation-and-webhooks)
10. [Troubleshooting](#troubleshooting)

## Architecture

Rinna uses an adapter-based architecture for integration with external systems:

```
┌─────────────┐     ┌───────────────┐     ┌────────────────┐
│             │     │               │     │                │
│   Rinna     │◄───►│  Integration  │◄───►│   External     │
│   Core      │     │  Adapters     │     │   Systems      │
│             │     │               │     │                │
└─────────────┘     └───────────────┘     └────────────────┘
```

The key components of the integration architecture:

- **Rinna Core**: The central workflow engine with clean domain model
- **Integration Adapters**: Pluggable adapters that translate between systems
- **External Systems**: Third-party tools and platforms (Jira, Git, etc.)
- **Synchronization Service**: Maintains consistency between systems
- **Mapping Configuration**: Defines how fields map between systems

## Integration Patterns

Rinna supports several integration patterns:

1. **Two-Way Sync**: Bidirectional synchronization of work items and their state
2. **One-Way Push**: Rinna pushes updates to external systems without return updates
3. **One-Way Pull**: Rinna pulls updates from external systems without sending updates
4. **Event-Based**: Systems communicate via events, using webhooks or message queues
5. **Query-Based**: Periodic polling for changes in either system

Choose the appropriate pattern based on your workflow requirements and the capabilities of the external system.

## Issue Tracker Integration

### Jira Integration

#### Configuration

To configure Jira integration:

1. Create a Jira API token at `https://id.atlassian.com/manage-profile/security/api-tokens`
2. In Rinna, set up the connection:

```bash
rin admin integration configure jira \
  --url="https://your-domain.atlassian.net" \
  --user="your-email@example.com" \
  --token="your-api-token" \
  --project="PROJECT"
```

#### Field Mapping

Define how Rinna work item fields map to Jira fields:

```bash
# Create a mapping configuration file
cat > jira-mapping.yaml << EOL
mappings:
  - rinna_field: "title"
    jira_field: "summary"
  - rinna_field: "description"
    jira_field: "description"
  - rinna_field: "priority"
    jira_field: "priority"
    transformations:
      - from: "high"
        to: "High"
      - from: "medium"
        to: "Medium"
      - from: "low"
        to: "Low"
  - rinna_field: "assignee"
    jira_field: "assignee"
  - rinna_field: "status"
    jira_field: "status"
    transformations:
      - from: "IN_PROGRESS"
        to: "In Progress"
      - from: "DONE"
        to: "Done"
      - from: "TO_DO"
        to: "To Do"
EOL

# Apply the mapping
rin admin integration apply-mapping jira --file="jira-mapping.yaml"
```

#### Synchronization

Start the synchronization service:

```bash
rin admin integration sync jira --interval=5m
```

#### API Usage

```java
// Java API example for Jira integration
JiraIntegrationAdapter jiraAdapter = new JiraIntegrationAdapter(config);

// Sync a work item to Jira
WorkItem workItem = itemService.getItem("WI-123");
String jiraKey = jiraAdapter.syncToJira(workItem);

// Get a work item from Jira
WorkItem importedItem = jiraAdapter.importFromJira("PROJ-456");
```

### Azure DevOps Integration

#### Configuration

To configure Azure DevOps integration:

1. Create a Personal Access Token in Azure DevOps
2. In Rinna, set up the connection:

```bash
rin admin integration configure azure-devops \
  --url="https://dev.azure.com/organization" \
  --user="your-email@example.com" \
  --token="your-personal-access-token" \
  --project="PROJECT" \
  --team="TEAM"
```

#### Field Mapping

Create an Azure DevOps mapping file:

```bash
# Create a mapping configuration file
cat > azure-mapping.yaml << EOL
mappings:
  - rinna_field: "title"
    azure_field: "System.Title"
  - rinna_field: "description"
    azure_field: "System.Description"
  - rinna_field: "priority"
    azure_field: "Microsoft.VSTS.Common.Priority"
    transformations:
      - from: "high"
        to: "1"
      - from: "medium"
        to: "2"
      - from: "low"
        to: "3"
  - rinna_field: "status"
    azure_field: "System.State"
    transformations:
      - from: "TO_DO"
        to: "New"
      - from: "IN_PROGRESS"
        to: "Active"
      - from: "DONE"
        to: "Closed"
EOL

# Apply the mapping
rin admin integration apply-mapping azure-devops --file="azure-mapping.yaml"
```

#### Workflow Mapping

Map Azure DevOps workflow states to Rinna workflow states:

```bash
rin admin integration workflow-map azure-devops \
  --azure-state="New" --rinna-state="TO_DO" \
  --azure-state="Active" --rinna-state="IN_PROGRESS" \
  --azure-state="Resolved" --rinna-state="IN_REVIEW" \
  --azure-state="Closed" --rinna-state="DONE"
```

#### Example Script

```bash
#!/bin/bash
# Script to sync all work items from Azure DevOps to Rinna

# Set up environment
export AZURE_TOKEN="your-token"
export RINNA_PROJECT="project-name"

# Get all work items from Azure DevOps and import to Rinna
azure_work_items=$(rin admin integration query azure-devops --wiql="SELECT [System.Id] FROM workitems WHERE [System.TeamProject] = '${RINNA_PROJECT}'" --format=json)

echo "$azure_work_items" | jq -r '.[] | .id' | while read -r id; do
  echo "Importing work item $id from Azure DevOps..."
  rin admin integration import azure-devops --id="$id" --project="${RINNA_PROJECT}"
done

echo "Import complete"
```

### GitHub Issues Integration

#### Configuration

To configure GitHub Issues integration:

1. Create a GitHub Personal Access Token with appropriate permissions
2. In Rinna, set up the connection:

```bash
rin admin integration configure github \
  --repo="owner/repository" \
  --token="your-github-token"
```

#### Field Mapping

Create a GitHub mapping file:

```bash
# Create a mapping configuration file
cat > github-mapping.yaml << EOL
mappings:
  - rinna_field: "title"
    github_field: "title"
  - rinna_field: "description"
    github_field: "body"
  - rinna_field: "labels"
    github_field: "labels"
  - rinna_field: "assignee"
    github_field: "assignee"
  - rinna_field: "status"
    github_field: "state"
    transformations:
      - from: "TO_DO"
        to: "open"
      - from: "IN_PROGRESS"
        to: "open"
      - from: "DONE"
        to: "closed"
EOL

# Apply the mapping
rin admin integration apply-mapping github --file="github-mapping.yaml"
```

#### Using Labels for Status

Since GitHub Issues only has open/closed states, you can use labels to represent workflow states:

```bash
rin admin integration configure github-labels \
  --label="status:todo" --rinna-state="TO_DO" \
  --label="status:in-progress" --rinna-state="IN_PROGRESS" \
  --label="status:in-review" --rinna-state="IN_REVIEW" \
  --label="status:done" --rinna-state="DONE"
```

## Version Control Integration

### Git Integration

#### Configuration

To configure Git integration:

```bash
rin admin integration configure git \
  --repo-path="/path/to/repository" \
  --branch-pattern="feature/{id}-*" \
  --commit-pattern="#{id}"
```

#### Branch and Commit Linking

Link work items to Git branches and commits:

```bash
# Link a work item to a branch
rin admin integration git link-branch --item="WI-123" --branch="feature/WI-123-add-new-feature"

# Link a work item to a commit
rin admin integration git link-commit --item="WI-123" --commit="a1b2c3d4e5f6"
```

#### Example Script for Auto-Linking

```bash
#!/bin/bash
# Script to automatically link commits to work items based on commit message patterns

# Get the most recent commit
commit_hash=$(git rev-parse HEAD)
commit_msg=$(git log -1 --pretty=%B)

# Extract work item IDs from commit message using pattern WI-\d+
work_item_ids=$(echo "$commit_msg" | grep -oE "WI-[0-9]+" | sort -u)

if [ -n "$work_item_ids" ]; then
  for id in $work_item_ids; do
    echo "Linking commit $commit_hash to work item $id"
    rin admin integration git link-commit --item="$id" --commit="$commit_hash"
  done
fi
```

## CI/CD Pipeline Integration

### Configuration

To configure CI/CD integration:

```bash
rin admin integration configure ci \
  --provider="jenkins" \
  --url="https://jenkins.example.com" \
  --user="jenkins-user" \
  --token="jenkins-api-token"
```

### Example: Jenkins Pipeline

```groovy
pipeline {
    agent any
    
    stages {
        stage('Update Work Item Status') {
            steps {
                script {
                    // Extract work item ID from branch name
                    def branchName = env.BRANCH_NAME
                    def workItemId = sh(script: "echo $branchName | grep -oE 'WI-[0-9]+'", returnStdout: true).trim()
                    
                    if (workItemId) {
                        // Update work item status
                        sh "rin admin integration update-status --item=$workItemId --status=IN_REVIEW --comment='Build #${env.BUILD_NUMBER} completed successfully'"
                    }
                }
            }
        }
    }
    
    post {
        success {
            script {
                def workItemId = sh(script: "echo $BRANCH_NAME | grep -oE 'WI-[0-9]+'", returnStdout: true).trim()
                if (workItemId) {
                    sh "rin admin integration add-build-result --item=$workItemId --build=${env.BUILD_NUMBER} --status=SUCCESS --url=${env.BUILD_URL}"
                }
            }
        }
        failure {
            script {
                def workItemId = sh(script: "echo $BRANCH_NAME | grep -oE 'WI-[0-9]+'", returnStdout: true).trim()
                if (workItemId) {
                    sh "rin admin integration add-build-result --item=$workItemId --build=${env.BUILD_NUMBER} --status=FAILURE --url=${env.BUILD_URL}"
                }
            }
        }
    }
}
```

## Authentication and Security

### Credential Storage

Rinna securely stores integration credentials using environment variables or a secure credential store:

```bash
# Store credentials in the secure credential store
rin admin integration credentials add jira \
  --user="your-email@example.com" \
  --token="your-api-token" \
  --encrypt

# Use credentials from environment variables
export RINNA_JIRA_USER="your-email@example.com"
export RINNA_JIRA_TOKEN="your-api-token"
```

### Permission Models

Configure permissions for integration operations:

```bash
# Set permissions for integration operations
rin admin access grant-permission --user="username" --permission="integration.sync.jira"
rin admin access grant-permission --user="username" --permission="integration.import"
rin admin access grant-permission --user="username" --permission="integration.export"
```

## Synchronization Strategies

### Real-Time Sync

Configure real-time synchronization using webhooks:

```bash
# Configure webhook endpoint for Jira
rin admin integration webhook setup jira \
  --url="https://your-server.com/webhook/jira" \
  --events="issue_created,issue_updated"

# Start webhook listener
rin admin integration webhook listen --port=8080
```

### Scheduled Sync

Configure scheduled synchronization:

```bash
# Set up a scheduled sync job for every 15 minutes
rin admin integration schedule sync jira --interval=15m
rin admin integration schedule sync azure-devops --interval=30m
```

### Conflict Resolution

Configure conflict resolution strategies:

```bash
# Configure conflict resolution
rin admin integration configure conflicts \
  --strategy="rinna-wins" \  # Options: rinna-wins, external-wins, manual
  --notification="true" \
  --report-path="/path/to/conflict-reports"
```

## Mapping Work Item Fields

### Custom Field Mapping

Define custom field mappings:

```bash
# Define a custom field mapping
cat > custom-fields.yaml << EOL
mappings:
  - rinna_field: "customer"
    external_field: "customfield_10001"
    system: "jira"
  - rinna_field: "story_points"
    external_field: "customfield_10002"
    system: "jira"
  - rinna_field: "acceptance_criteria"
    external_field: "Microsoft.VSTS.Common.AcceptanceCriteria"
    system: "azure-devops"
EOL

# Apply custom field mappings
rin admin integration custom-fields apply --file="custom-fields.yaml"
```

### Field Transformations

Define transformations for field values:

```bash
# Define field value transformations
cat > field-transformations.yaml << EOL
transformations:
  - field: "priority"
    mappings:
      - from: "1"
        to: "highest"
        system: "jira"
      - from: "2"
        to: "high"
        system: "jira"
      - from: "3"
        to: "medium"
        system: "jira"
  - field: "status"
    mappings:
      - from: "Active"
        to: "IN_PROGRESS"
        system: "azure-devops"
      - from: "Closed"
        to: "DONE"
        system: "azure-devops"
EOL

# Apply field transformations
rin admin integration transformations apply --file="field-transformations.yaml"
```

## Automation and Webhooks

### Webhook Configuration

Configure webhooks to automate integration workflows:

```bash
# Configure a webhook endpoint for GitHub
rin admin integration webhook create github \
  --url="https://your-server.com/api/webhooks/github" \
  --secret="webhook-secret" \
  --events="issues,pull_request"

# Configure a webhook endpoint for Jira
rin admin integration webhook create jira \
  --url="https://your-server.com/api/webhooks/jira" \
  --events="jira:issue_created,jira:issue_updated"
```

### Event Handling

Configure event handlers for integration events:

```bash
# Define an event handler for GitHub issue events
cat > github-event-handler.yaml << EOL
handler:
  event: "issues"
  actions:
    - when: "opened"
      do: "create_work_item"
      map_to: "bug"
    - when: "closed"
      do: "update_status"
      status: "DONE"
    - when: "reopened"
      do: "update_status"
      status: "TO_DO"
EOL

# Apply event handler
rin admin integration event-handler apply --file="github-event-handler.yaml"
```

## Troubleshooting

### Common Issues

#### Synchronization Failures

If synchronization fails:

1. Check connectivity to the external system:
   ```bash
   rin admin integration test-connection jira
   ```

2. Verify credentials:
   ```bash
   rin admin integration verify-credentials jira
   ```

3. Check synchronization logs:
   ```bash
   rin admin integration logs --system=jira --level=debug
   ```

#### Field Mapping Issues

If field values aren't mapping correctly:

1. Test the field mapping:
   ```bash
   rin admin integration test-mapping jira --field="priority" --value="high"
   ```

2. View the full field mapping configuration:
   ```bash
   rin admin integration show-mapping jira
   ```

### Diagnostic Tools

Rinna provides several diagnostic tools for integration troubleshooting:

```bash
# Run a connectivity test
rin admin diagnostics test-connectivity --system=jira

# Run a full integration diagnostic
rin admin diagnostics integration --system=jira --verbose

# Export diagnostic information
rin admin diagnostics export --file="/path/to/diagnostic-report.json"
```

### Support Information

For additional support with enterprise integration:

- Refer to the specific integration documentation in the docs/integration directory
- Join the Rinna community forum at https://community.rinna.io
- Contact enterprise support at support@rinna.io

## Conclusion

This guide provides a comprehensive overview of Rinna's enterprise integration capabilities. By following these guidelines, you can seamlessly connect Rinna to your existing enterprise systems, creating a unified workflow ecosystem that enhances productivity and visibility across your organization.

For more detailed information on specific integrations, refer to the dedicated integration guides in the docs/integration directory.
# Enterprise Integration

Rinna provides comprehensive integration capabilities for connecting with external systems, including issue trackers, version control systems, document management platforms, and CI/CD pipelines.

## Integration Capabilities

Rinna offers integration with:

### Issue Tracking Systems
- **Jira**: Bidirectional sync of work items, comments, attachments, and workflow states
- **GitHub Issues**: Integration with GitHub's issue tracking and project management
- **Azure DevOps**: Work item synchronization with Azure DevOps boards

### Version Control
- **Git**: Commit hooks, branch naming conventions, and work item linking
- **GitHub/GitLab**: Integration with pull/merge requests and code reviews

### Document Systems
- **Confluence**: Automatic documentation generation and syncing
- **SharePoint**: Document storage and team collaboration

### CI/CD Systems
- **Jenkins**: Build pipeline integration and status tracking
- **GitHub Actions**: Workflow automation and testing integration
- **Azure DevOps Pipelines**: Deployment pipeline integration

## RESTful API

### Authentication

Include API token in Authorization header:
```
Authorization: Bearer ri-dev-f72a159e4bdc
```

### Content Type
```
Content-Type: application/json
```

### Endpoints

#### Work Items
```
GET    /api/v1/workitems                # List work items
POST   /api/v1/workitems                # Create work item
GET    /api/v1/workitems/{id}           # Get work item
PUT    /api/v1/workitems/{id}           # Update work item
POST   /api/v1/workitems/{id}/transitions # Transition work item
```

#### Projects
```
GET    /api/v1/projects                 # List projects
POST   /api/v1/projects                 # Create project
GET    /api/v1/projects/{key}           # Get project
PUT    /api/v1/projects/{key}           # Update project
GET    /api/v1/projects/{key}/workitems # Get project work items
```

#### Releases
```
GET    /api/v1/releases                 # List releases
POST   /api/v1/releases                 # Create release
GET    /api/v1/releases/{id}            # Get release
PUT    /api/v1/releases/{id}            # Update release
GET    /api/v1/releases/{id}/workitems  # Get release work items
POST   /api/v1/releases/{id}/workitems  # Add work item to release
DELETE /api/v1/releases/{id}/workitems/{workItemId} # Remove from release
```

### Example Usage

#### Creating a Work Item
```bash
curl -X POST "https://your-rinna-instance/api/v1/workitems" \
  -H "Authorization: Bearer ri-dev-f72a159e4bdc" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Implement payment gateway",
    "description": "Add support for PayPal and Stripe",
    "type": "FEATURE",
    "priority": "HIGH",
    "projectId": "billing-system",
    "metadata": {
      "source": "product_roadmap",
      "estimated_points": "8"
    }
  }'
```

#### Transitioning a Work Item
```bash
curl -X POST "https://your-rinna-instance/api/v1/workitems/wi-123/transitions" \
  -H "Authorization: Bearer ri-dev-f72a159e4bdc" \
  -H "Content-Type: application/json" \
  -d '{
    "toState": "IN_DEV",
    "comment": "Starting implementation"
  }'
```

## Webhook Integration

### GitHub Integration

Webhook URL:
```
https://your-rinna-instance/api/v1/webhooks/github?project=your-project-key
```

Configuration:
1. Generate webhook secret in Rinna project settings
2. Add webhook in GitHub repository settings
3. Select events: pull requests, issues, workflows, push events

### Custom Webhook

```bash
curl -X POST "https://your-rinna-instance/api/v1/webhooks/custom?project=your-project-key" \
  -H "Authorization: Bearer ri-dev-f72a159e4bdc" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "New feature request",
    "description": "Add PDF export",
    "type": "FEATURE",
    "priority": "MEDIUM",
    "metadata": {
      "source": "customer_portal",
      "customer_id": "cust-123"
    }
  }'
```

## Client Libraries

### Java Client

```java
// Initialize client
RinnaClient client = RinnaClient.builder()
    .apiUrl("https://your-rinna-instance/api")
    .apiToken("ri-dev-f72a159e4bdc")
    .build();

// Create work item
WorkItemCreateRequest request = new WorkItemCreateRequest.Builder()
    .title("Implement feature X")
    .type(WorkItemType.FEATURE)
    .priority(Priority.HIGH)
    .description("Add support for feature X")
    .build();

WorkItem workItem = client.workItems().create(request);

// Transition work item
client.workItems().transition(workItem.getId(), WorkflowState.IN_DEV, "Starting implementation");
```

### Go Client

```go
// Initialize client
client := rinna.NewClient("https://your-rinna-instance/api", "ri-dev-f72a159e4bdc")

// Create work item
workItem, err := client.CreateWorkItem(rinna.WorkItemCreateRequest{
    Title:       "Implement feature X",
    Type:        rinna.WorkItemTypeFeature,
    Priority:    rinna.PriorityHigh,
    Description: "Add support for feature X",
})

// Transition work item
err = client.TransitionWorkItem(workItem.ID, rinna.WorkflowStateInDev, "Starting implementation")
```

## OAuth Integration

Rinna supports OAuth 2.0 integration for connecting to third-party services:

### OAuth Configuration

```yaml
oauth:
  # GitHub configuration
  github:
    enabled: true
    client_id: "your-github-client-id"
    client_secret: "your-github-client-secret"
    redirect_url: "http://your-app/api/v1/oauth/callback"
    scopes:
      - "repo"
      - "user:email"
  
  # Jira configuration
  jira:
    enabled: true
    client_id: "your-jira-client-id"
    client_secret: "your-jira-client-secret"
    redirect_url: "http://your-app/api/v1/oauth/callback"
    server_url: "https://your-instance.atlassian.net"
    scopes:
      - "read:jira-work"
      - "read:jira-user"
```

### OAuth Endpoints

- `GET /api/v1/oauth/providers` - List available OAuth providers
- `GET /api/v1/oauth/authorize/{provider}` - Initiate OAuth flow for a provider
- `GET /api/v1/oauth/callback` - Handle OAuth callback
- `GET /api/v1/oauth/tokens` - List OAuth tokens
- `DELETE /api/v1/oauth/tokens/{provider}` - Revoke an OAuth token

## External Tool Mapping

### Workflow State Mapping

Map Rinna workflow states to external systems:

```bash
# Configure Jira workflow mapping
rin external map-workflow --system jira \
  --mapping "IN_PROGRESS:In Development,IN_TEST:In Review"

# Configure Azure DevOps mapping
rin external map-workflow --system azure \
  --mapping "IN_PROGRESS:Active,IN_TEST:Testing,DONE:Closed"
```

### Field Mapping

Map work item fields to external systems:

```bash
# Configure field mapping for Jira
rin external map-fields --system jira \
  --mapping "priority:Priority,metadata.storyPoints:Story Points"
```

## CI/CD Integration

### Jenkins Integration

```bash
# Configure Jenkins integration
rin config ci jenkins \
  --url "https://jenkins.example.com" \
  --username "jenkins-user" \
  --token "jenkins-token"

# Link build job to work item
rin ci link-job WI-123 "build-project" \
  --on-transition TO_DONE
```

### GitHub Actions Integration

```bash
# Generate GitHub Actions workflow file
rin ci generate-workflow github \
  --project PROJECT-123 \
  --output .github/workflows/rinna-integration.yml
```

## Migration Tools

### Importing from External Systems

```bash
# Import from Jira
rin migrate import --source jira \
  --project "JIRA-PROJECT" \
  --mapping-file jira-mapping.json

# Import from GitHub Issues
rin migrate import --source github \
  --repo "owner/repo" \
  --mapping-file github-mapping.json
```

### Export Capabilities

```bash
# Export to CSV
rin export --format csv --output export.csv

# Export to JSON
rin export --format json --output export.json
```

## Security Considerations

- Protect API tokens and OAuth secrets
- Use HTTPS for all API communications
- Apply IP restrictions where appropriate
- Implement webhook signature validation
- Regularly audit integration access

## Best Practices

1. **Start Small**: Begin with one-way synchronization before implementing bidirectional sync
2. **Map Workflows Carefully**: Ensure workflow state mappings make sense for both systems
3. **Handle Conflicts**: Implement clear conflict resolution strategies
4. **Test Thoroughly**: Verify integration behavior in a test environment first
5. **Monitor Activity**: Set up logging and monitoring for integration activity

For detailed information on connecting to specific systems, see the individual integration guides in the documentation.

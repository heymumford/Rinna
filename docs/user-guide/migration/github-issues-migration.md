# Migrating from GitHub Issues to Rinna

This guide provides detailed steps for migrating issues and project boards from GitHub to Rinna.

## Prerequisites

Before starting the migration:

- Rinna server is set up and running
- Access to both GitHub repository and Rinna
- [Rinna CLI](../rin-cli.md) installed and configured
- GitHub Personal Access Token with `repo` scope
- User mapping between GitHub and Rinna users (optional)
- Backup of GitHub data (recommended)

## Migration Planning

### 1. Assess Migration Scope

Determine what to migrate from GitHub:

- Issues (open and/or closed)
- Labels
- Milestones
- Projects/Project Boards
- Pull Requests (as references)
- Issue comments
- Assignees and reporters
- Issue relationships

### 2. Data Mapping

| GitHub Concept | Rinna Equivalent | Notes |
|----------------|------------------|-------|
| Repository | Project | Direct mapping |
| Issue | Work Item | See mapping details below |
| Label | Tag + Type | Certain labels map to types, others to tags |
| Milestone | Release | Direct mapping |
| Project | View | GitHub Projects become Rinna Views |
| Project Column | Workflow State | Maps to corresponding states |
| Assignee | Assignee | Direct mapping |
| Comment | Comment | Direct mapping |
| Linked Issue | Link | Reference relationship |
| Pull Request | External Link | Stored as external reference |

## Standard Mapping Tables

### Issue Type Mapping (Based on Labels)

| GitHub Label | Rinna Work Item Type |
|--------------|----------------------|
| bug | BUG |
| enhancement | FEATURE |
| feature | FEATURE |
| documentation | DOCUMENTATION |
| question | TASK |
| help wanted | TASK |
| good first issue | TASK |
| *No type label* | TASK |

### Status Mapping (Based on State and Project Columns)

| GitHub State/Column | Rinna Workflow State |
|---------------------|----------------------|
| Open + Backlog | FOUND |
| Open + To do | TRIAGED |
| Open + In progress | IN_PROGRESS |
| Open + Under review | IN_TEST |
| Open + Ready | TRIAGED |
| Closed + Done | DONE |

### Priority Mapping (Based on Labels)

| GitHub Label | Rinna Priority |
|--------------|---------------|
| priority:critical | CRITICAL |
| priority:high | HIGH |
| priority:medium | MEDIUM |
| priority:low | LOW |
| *No priority label* | MEDIUM |

## Migration Steps

### 1. Export Data from GitHub

```bash
# Export all issues from a GitHub repository
rin-migrate export github \
  --repo "owner/repository" \
  --token "your-github-token" \
  --output github-export.json

# Export with additional options
rin-migrate export github \
  --repo "owner/repository" \
  --token "your-github-token" \
  --include-comments \
  --include-closed \
  --include-projects \
  --created-after "2023-01-01" \
  --output github-export.json
```

### 2. Prepare Custom Mapping (Optional)

Create a custom mapping file `github-mapping.json` to customize how GitHub data is mapped to Rinna:

```json
{
  "labelTypeMapping": {
    "bug": "BUG",
    "feature": "FEATURE",
    "enhancement": "FEATURE",
    "documentation": "DOCUMENTATION",
    "question": "TASK",
    "tech-debt": "TECHNICAL_DEBT",
    "security": "SECURITY",
    "performance": "PERFORMANCE"
  },
  "labelPriorityMapping": {
    "priority:critical": "CRITICAL",
    "priority:high": "HIGH",
    "priority:medium": "MEDIUM",
    "priority:low": "LOW",
    "P0": "CRITICAL",
    "P1": "HIGH",
    "P2": "MEDIUM",
    "P3": "LOW"
  },
  "projectColumnMapping": {
    "Backlog": "FOUND",
    "To do": "TRIAGED",
    "In progress": "IN_PROGRESS",
    "Under review": "IN_TEST",
    "Ready for review": "IN_TEST",
    "Done": "DONE",
    "Released": "RELEASED"
  },
  "labelExclude": [
    "duplicate",
    "wontfix",
    "invalid"
  ]
}
```

### 3. Create Target Project in Rinna

```bash
# Create new project in Rinna
rin admin project create --key PROJECT_KEY --name "Project Name" --description "Migrated from GitHub Issues"
```

### 4. Import Data into Rinna

```bash
# Import from GitHub export with default mappings
rin-migrate import --source github-export.json --project PROJECT_KEY

# Import with custom mappings
rin-migrate import --source github-export.json --project PROJECT_KEY --mapping github-mapping.json

# Dry-run to preview changes without importing
rin-migrate import --source github-export.json --project PROJECT_KEY --dry-run
```

### 5. Verify Data Integrity

```bash
# Verify migration results
rin-migrate verify --project PROJECT_KEY

# Generate verification report
rin-migrate verify --project PROJECT_KEY --report verification-report.md
```

### 6. Set Up Synchronization (Optional)

For hybrid migration strategy, set up ongoing synchronization:

```bash
# Configure bidirectional sync
rin admin integration add github \
  --repo "owner/repository" \
  --token "your-github-token" \
  --rinna-project PROJECT_KEY \
  --sync-interval 15m \
  --bidirectional

# Test synchronization
rin admin integration test github --project PROJECT_KEY
```

## Advanced Options

### Project Board Migration

GitHub Projects and Project Boards can be migrated to Rinna Views:

```bash
# Export GitHub Project Boards
rin-migrate export github-projects \
  --repo "owner/repository" \
  --token "your-github-token" \
  --output projects.json

# Import as Rinna Views
rin-migrate import projects.json --as-view --project PROJECT_KEY
```

### Label Management

GitHub uses labels extensively. Here's how to handle them:

1. Export labels:
   ```bash
   rin-migrate export github-labels \
     --repo "owner/repository" \
     --token "your-github-token" \
     --output labels.json
   ```

2. Import labels as tags:
   ```bash
   rin admin tags import --file labels.json --project PROJECT_KEY
   ```

### Pull Request References

To include references to Pull Requests:

```bash
# Export with PR references
rin-migrate export github \
  --repo "owner/repository" \
  --token "your-github-token" \
  --include-pr-references \
  --output github-export.json
```

The PRs will be stored as external links in the work item metadata.

### User Mapping

GitHub users can be mapped to Rinna users:

1. Create a user mapping file `user-mapping.json`:
   ```json
   {
     "github-user1": "rinna-user1",
     "github-user2": "rinna-user2",
     "octocat": "jsmith"
   }
   ```

2. Apply during import:
   ```bash
   rin-migrate import --source github-export.json \
     --project PROJECT_KEY \
     --user-mapping user-mapping.json
   ```

## Troubleshooting

### Common Issues

| Issue | Solution |
|-------|----------|
| API rate limiting | Use `--throttle` flag to slow down requests |
| Issue type detection | Use custom label-to-type mapping file |
| Missing milestone data | Ensure `--include-milestones` flag is used |
| Markdown formatting issues | Use `--preserve-markdown` flag |
| Label conflicts | Use `--label-prefix` to add prefix to imported labels |

### Error Codes

| Error Code | Description | Resolution |
|------------|-------------|------------|
| GITHUB-001 | API authentication failure | Check GitHub token and permissions |
| GITHUB-002 | Repository not found | Verify repository path (owner/repo) |
| GITHUB-003 | Export format error | Use supported export format |
| GITHUB-004 | User mapping error | Check user mapping file format |
| GITHUB-005 | Label mapping error | Check label mapping configuration |

### Logs and Diagnostics

Migration logs are stored in:
- `~/.rinna/logs/migration.log`
- `~/.rinna/logs/github-export.log`

Enable debug mode for verbose logging:
```bash
rin-migrate import --source github-export.json --project PROJECT_KEY --debug
```

## GitHub-Specific Considerations

### Issue References

GitHub issues often reference each other with `#123` syntax. During migration:

1. These references are converted to Rinna work item links
2. References to pull requests become external links
3. Text references are updated to the new work item IDs

### Pull Request Integration

For repositories still using GitHub for code:

1. Enable the GitHub integration after migration
2. Configure PR webhooks to update Rinna work items
3. Use the Rinna GitHub Action for status updates

```bash
# Set up PR integration
rin admin integration config github --pull-request-updates
```

### GitHub Actions Integration

To integrate with GitHub Actions:

1. Add the Rinna GitHub Action to your workflow:
   ```yaml
   # .github/workflows/rinna-integration.yml
   name: Rinna Integration
   on:
     issues:
       types: [opened, edited, closed, reopened]
     pull_request:
       types: [opened, closed, merged]
   
   jobs:
     sync-with-rinna:
       runs-on: ubuntu-latest
       steps:
         - uses: rinna/github-sync-action@v1
           with:
             rinna-url: 'https://your-rinna-instance.com'
             rinna-token: ${{ secrets.RINNA_TOKEN }}
             rinna-project: 'PROJECT_KEY'
   ```

## Post-Migration Steps

1. Verify issue counts match between GitHub and Rinna
2. Test workflows and transitions in Rinna
3. Configure webhook for ongoing synchronization if needed
4. Update team processes to use Rinna
5. Set up GitHub integration for code-related workflows

## Resources

- [GitHub REST API Documentation](https://docs.github.com/en/rest)
- [Rinna GitHub Integration Reference](../reference/github-integration.md)
- [Workflow State Mapping Guide](../workflow.md)
- [Common Migration Patterns](./migration-patterns.md)
# Migrating from Jira to Rinna

This guide provides detailed steps for migrating projects and work items from Jira to Rinna.

## Prerequisites

Before starting the migration:

- Rinna server is set up and running
- Admin access to both Jira and Rinna
- [Rinna CLI](../rin-cli.md) installed and configured
- User mapping between Jira and Rinna users
- Backup of Jira data (recommended)

## Migration Planning

### 1. Choose Your Migration Scope

Determine which of the following to migrate:

- Project structure and configuration
- Active (open) issues only
- All issues including closed/done
- Comments and attachments
- Custom fields and metadata
- Issue links and relationships
- Sprint and board configurations

### 2. Data Mapping Strategy

Decide how to map Jira concepts to Rinna:

| Jira Concept | Rinna Equivalent | Notes |
|--------------|------------------|-------|
| Project | Project | Direct mapping |
| Issue Type | Work Item Type | See mapping table below |
| Status | Workflow State | See mapping table below |
| Priority | Priority | Direct mapping available |
| Component | Metadata | Stored as WorkItem metadata |
| Version | Release | Direct mapping |
| Epic | Epic Work Item | Direct mapping |
| Sprint | Tag + Metadata | Tagged and stored as metadata |
| Story Points | Metadata | Stored as storyPoints metadata |
| Sub-tasks | Child Work Items | Direct mapping |
| Issue Links | Work Item Links | Types may need remapping |
| Attachments | Attachments | Direct mapping |
| Comments | Comments | Direct mapping |
| Custom Fields | Metadata | Stored as custom metadata |

## Standard Mapping Tables

### Issue Type Mapping

| Jira Issue Type | Rinna Work Item Type |
|-----------------|----------------------|
| Epic | EPIC |
| Story | STORY |
| Bug | BUG |
| Task | TASK |
| Sub-task | SUBTASK |
| Technical Task | TASK |
| Improvement | FEATURE |
| New Feature | FEATURE |

### Status Mapping

| Jira Status Category | Jira Example Statuses | Rinna Workflow State |
|----------------------|----------------------|----------------------|
| To Do | Backlog, Open, To Do | FOUND |
| To Do | Ready for Development | TRIAGED |
| In Progress | In Progress, Implementing | IN_PROGRESS |
| In Progress | In Review, Code Review | IN_TEST |
| Done | Done, Closed, Resolved | DONE |
| Done | Released, Shipped | RELEASED |

### Priority Mapping

| Jira Priority | Rinna Priority |
|---------------|---------------|
| Highest | CRITICAL |
| High | HIGH |
| Medium | MEDIUM |
| Low | LOW |
| Lowest | TRIVIAL |

## Migration Steps

### 1. Export Data from Jira

#### Option A: Using Jira API (Recommended for active instances)

```bash
# Export all issues from a Jira project
rin-migrate export jira --url https://your-jira.atlassian.net \
  --username your-username \
  --token your-api-token \
  --project PROJECT_KEY \
  --output jira-export.json

# Export with additional options
rin-migrate export jira --url https://your-jira.atlassian.net \
  --username your-username \
  --token your-api-token \
  --project PROJECT_KEY \
  --include-attachments \
  --include-comments \
  --status "all" \
  --created-after "2023-01-01" \
  --output jira-export.json
```

#### Option B: Using CSV Export (Simpler but limited)

1. In Jira, go to **Issues**
2. Search for all issues you want to export
3. Select **Export** > **CSV (All fields)**
4. Save the CSV file

### 2. Prepare Custom Mapping (Optional)

Create a custom mapping file `jira-mapping.json` if the default mappings don't fit your needs:

```json
{
  "typeMapping": {
    "Story": "STORY",
    "Bug": "BUG",
    "Task": "TASK",
    "Epic": "EPIC",
    "Support Request": "SUPPORT",
    "Technical Debt": "TASK"
  },
  "statusMapping": {
    "Backlog": "FOUND",
    "Selected for Development": "TRIAGED",
    "In Progress": "IN_PROGRESS",
    "In Review": "IN_TEST",
    "Done": "DONE",
    "Released": "RELEASED"
  },
  "priorityMapping": {
    "Highest": "CRITICAL",
    "High": "HIGH",
    "Medium": "MEDIUM",
    "Low": "LOW",
    "Lowest": "TRIVIAL"
  },
  "fieldMapping": {
    "summary": "title",
    "description": "description",
    "customfield_10010": "metadata.storyPoints",
    "customfield_10092": "metadata.businessValue",
    "components": "metadata.components",
    "fixVersions": "metadata.fixVersions"
  }
}
```

### 3. Create Target Project in Rinna

```bash
# Create new project in Rinna
rin admin project create --key PROJECT_KEY --name "Project Name" --description "Migrated from Jira"
```

### 4. Import Data into Rinna

```bash
# Import from Jira export with default mappings
rin-migrate import --source jira-export.json --project PROJECT_KEY

# Import with custom mappings
rin-migrate import --source jira-export.json --project PROJECT_KEY --mapping jira-mapping.json

# Dry-run to preview changes without importing
rin-migrate import --source jira-export.json --project PROJECT_KEY --dry-run
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
rin admin integration add jira \
  --url https://your-jira.atlassian.net \
  --username your-username \
  --token your-api-token \
  --project PROJECT_KEY \
  --jira-project JIRA_KEY \
  --sync-interval 15m \
  --bidirectional

# Test synchronization
rin admin integration test jira --project PROJECT_KEY
```

## Advanced Options

### Custom Field Handling

Jira custom fields can be complex. Here's how to handle them:

1. Identify custom fields in your Jira instance:
   ```bash
   rin-migrate jira fields --url https://your-jira.atlassian.net
   ```

2. Create matching metadata definitions in Rinna:
   ```bash
   rin admin metadata create --name storyPoints --type NUMBER
   rin admin metadata create --name businessValue --type NUMBER
   rin admin metadata create --name epicLink --type STRING
   ```

3. Update your mapping file with custom field mappings.

### Agile Board Migration

To migrate Jira board configurations:

```bash
# Export board configuration
rin-migrate export jira-board --url https://your-jira.atlassian.net \
  --board-id 123 \
  --output board-config.json

# Import board as Rinna view
rin-migrate import board-config.json --as-view --project PROJECT_KEY
```

### Handling Epic Links

Jira Epics and linked issues require special handling:

```bash
# Import with epic link preservation
rin-migrate import --source jira-export.json \
  --project PROJECT_KEY \
  --preserve-epic-links
```

### User Migration and Mapping

Create a user mapping file `user-mapping.json`:

```json
{
  "jsmith": "john.smith",
  "mjones": "maria.jones",
  "adoe": "alex.doe"
}
```

Then use it during import:

```bash
rin-migrate import --source jira-export.json \
  --project PROJECT_KEY \
  --user-mapping user-mapping.json
```

## Troubleshooting

### Common Issues

| Issue | Solution |
|-------|----------|
| API rate limiting | Use `--throttle` flag to slow down requests |
| Missing custom fields | Export field definitions first with `rin-migrate jira fields` |
| Attachment import failures | Check file size limits and use `--max-attachment-size` flag |
| Status mapping errors | Create more detailed status mapping in your mapping file |
| User assignment failures | Create missing users in Rinna or use user mapping file |

### Error Codes

| Error Code | Description | Resolution |
|------------|-------------|------------|
| JIRA-001 | API authentication failure | Check credentials and API token |
| JIRA-002 | Project not found | Verify Jira project key |
| JIRA-003 | Export format error | Use supported export format |
| JIRA-004 | User mapping error | Check user mapping file format |
| JIRA-005 | Field mapping error | Check field mapping configuration |

### Logs and Diagnostics

Migration logs are stored in:
- `~/.rinna/logs/migration.log`
- `~/.rinna/logs/jira-export.log`

Enable debug mode for verbose logging:
```bash
rin-migrate import --source jira-export.json --project PROJECT_KEY --debug
```

## Post-Migration Steps

1. Verify work item counts match between systems
2. Test workflows and transitions in Rinna
3. Confirm user assignments are correct
4. Validate that relationships (parent/child, links) are preserved
5. Check attachment and comment migration

## Resources

- [Jira API Documentation](https://developer.atlassian.com/cloud/jira/platform/rest/v3/intro/)
- [Rinna Data Model Reference](../reference/data-model.md)
- [Workflow State Mapping Guide](../workflow.md)
- [Common Migration Patterns](./migration-patterns.md)
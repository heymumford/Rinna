# Migrating from Azure DevOps to Rinna

This guide provides detailed steps for migrating projects and work items from Azure DevOps to Rinna.

## Prerequisites

Before starting the migration:

- Rinna server is set up and running
- Admin access to both Azure DevOps and Rinna
- [Rinna CLI](../rin-cli.md) installed and configured
- Personal Access Token (PAT) from Azure DevOps with appropriate scopes
- User mapping between Azure DevOps and Rinna users
- Backup of Azure DevOps data (recommended)

## Migration Planning

### 1. Assess Migration Scope

Determine what to migrate from Azure DevOps:

- Project structure
- Work items (active and/or completed)
- Iterations (Sprints)
- Area Paths
- Queries
- Work item links and relationships
- Attachments and comments
- Test plans and test cases

### 2. Data Mapping

| Azure DevOps Concept | Rinna Equivalent | Notes |
|----------------------|------------------|-------|
| Project | Project | Direct mapping |
| Work Item Type | Work Item Type | See mapping table below |
| State | Workflow State | See mapping table below |
| Priority | Priority | See mapping table below |
| Area Path | Metadata | Stored as metadata |
| Iteration Path | Metadata + Tag | Both as metadata and tag |
| Acceptance Criteria | Metadata | Stored as metadata |
| Story Points | Metadata | Stored as metadata |
| Parent/Child | Parent/Child | Direct relationship |
| Related Work Items | Links | Direct relationship |
| Attachments | Attachments | Direct mapping |
| Comments | Comments | Direct mapping |
| Tags | Tags | Direct mapping |
| Custom Fields | Metadata | Stored as custom metadata |

## Standard Mapping Tables

### Work Item Type Mapping

| Azure DevOps Work Item Type | Rinna Work Item Type |
|-----------------------------|----------------------|
| Epic | EPIC |
| Feature | FEATURE |
| User Story | STORY |
| Bug | BUG |
| Task | TASK |
| Issue | TASK |
| Test Case | TEST |
| Impediment | IMPEDIMENT |

### State Mapping

| Azure DevOps State | Rinna Workflow State |
|--------------------|----------------------|
| New | FOUND |
| Active | IN_PROGRESS |
| Committed | TRIAGED |
| Open | FOUND |
| Ready | TRIAGED |
| In Progress | IN_PROGRESS |
| Resolved | IN_TEST |
| Ready for Test | IN_TEST |
| Closed | DONE |
| Done | DONE |
| Removed | DONE |
| Released | RELEASED |

### Priority Mapping

| Azure DevOps Priority | Rinna Priority |
|----------------------|---------------|
| 1 | CRITICAL |
| 2 | HIGH |
| 3 | MEDIUM |
| 4 | LOW |

## Migration Steps

### 1. Export Data from Azure DevOps

#### Option A: Using Azure DevOps API (Recommended)

```bash
# Export all work items from an Azure DevOps project
rin-migrate export azure \
  --org "organization" \
  --project "project-name" \
  --token "your-personal-access-token" \
  --output azure-export.json

# Export with additional options
rin-migrate export azure \
  --org "organization" \
  --project "project-name" \
  --token "your-personal-access-token" \
  --include-attachments \
  --include-comments \
  --states "all" \
  --created-after "2023-01-01" \
  --output azure-export.json
```

#### Option B: Using CSV Export (Limited)

1. In Azure DevOps, go to **Boards** > **Work Items**
2. Create a query that includes all work items you want to export
3. Select **Export to CSV**
4. Save the CSV file

### 2. Prepare Custom Mapping (Optional)

Create a custom mapping file `azure-mapping.json` if the default mappings don't fit your needs:

```json
{
  "typeMapping": {
    "Epic": "EPIC",
    "Feature": "FEATURE",
    "User Story": "STORY",
    "Bug": "BUG",
    "Task": "TASK",
    "Issue": "TASK",
    "Test Case": "TEST",
    "Impediment": "IMPEDIMENT"
  },
  "statusMapping": {
    "New": "FOUND",
    "Ready": "TRIAGED",
    "Active": "IN_PROGRESS",
    "In Progress": "IN_PROGRESS",
    "Resolved": "IN_TEST",
    "Ready for Test": "IN_TEST",
    "Closed": "DONE",
    "Done": "DONE"
  },
  "priorityMapping": {
    "1": "CRITICAL",
    "2": "HIGH",
    "3": "MEDIUM",
    "4": "LOW"
  },
  "fieldMapping": {
    "System.Title": "title",
    "System.Description": "description",
    "Microsoft.VSTS.Scheduling.StoryPoints": "metadata.storyPoints",
    "Microsoft.VSTS.Common.AcceptanceCriteria": "metadata.acceptanceCriteria",
    "System.AreaPath": "metadata.areaPath",
    "System.IterationPath": "metadata.iterationPath"
  }
}
```

### 3. Create Target Project in Rinna

```bash
# Create new project in Rinna
rin admin project create --key PROJECT_KEY --name "Project Name" --description "Migrated from Azure DevOps"
```

### 4. Import Data into Rinna

```bash
# Import from Azure DevOps export with default mappings
rin-migrate import --source azure-export.json --project PROJECT_KEY

# Import with custom mappings
rin-migrate import --source azure-export.json --project PROJECT_KEY --mapping azure-mapping.json

# Dry-run to preview changes without importing
rin-migrate import --source azure-export.json --project PROJECT_KEY --dry-run
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
rin admin integration add azure \
  --org "organization" \
  --project "project-name" \
  --token "your-personal-access-token" \
  --rinna-project PROJECT_KEY \
  --sync-interval 15m \
  --bidirectional

# Test synchronization
rin admin integration test azure --project PROJECT_KEY
```

## Advanced Options

### Area Path and Iteration Path Handling

Azure DevOps uses hierarchical paths for organization. Here's how to handle them:

1. Export the path structures:
   ```bash
   rin-migrate export azure-structure \
     --org "organization" \
     --project "project-name" \
     --token "your-personal-access-token" \
     --output structure.json
   ```

2. Import the structure into Rinna metadata:
   ```bash
   rin admin metadata import-structure --file structure.json --project PROJECT_KEY
   ```

### Custom Fields

Azure DevOps allows custom fields. Identify and map them:

1. Export field definitions:
   ```bash
   rin-migrate azure fields \
     --org "organization" \
     --project "project-name" \
     --token "your-personal-access-token"
   ```

2. Create corresponding metadata definitions in Rinna:
   ```bash
   rin admin metadata create --name customField --type STRING
   ```

### Query and Dashboard Migration

To migrate saved queries and dashboards:

```bash
# Export queries
rin-migrate export azure-queries \
  --org "organization" \
  --project "project-name" \
  --token "your-personal-access-token" \
  --output queries.json

# Import as Rinna views
rin-migrate import queries.json --as-view --project PROJECT_KEY
```

### Test Case Migration

For test cases and test plans:

```bash
# Export test plans
rin-migrate export azure-testplans \
  --org "organization" \
  --project "project-name" \
  --token "your-personal-access-token" \
  --output testplans.json

# Import as Rinna test specifications
rin-migrate import testplans.json --as-tests --project PROJECT_KEY
```

### User Mapping

Create a user mapping file `user-mapping.json`:

```json
{
  "john.doe@company.com": "jdoe",
  "jane.smith@company.com": "jsmith",
  "mike.johnson@company.com": "mjohnson"
}
```

Then use it during import:

```bash
rin-migrate import --source azure-export.json \
  --project PROJECT_KEY \
  --user-mapping user-mapping.json
```

## Troubleshooting

### Common Issues

| Issue | Solution |
|-------|----------|
| API rate limiting | Use `--throttle` flag to slow down requests |
| Missing custom fields | Export field definitions first with `rin-migrate azure fields` |
| Hierarchy preservation issues | Use `--preserve-hierarchy` flag during import |
| HTML content in descriptions | Use `--convert-html-to-markdown` flag |
| Test case migration failures | Export test plans separately with dedicated command |

### Error Codes

| Error Code | Description | Resolution |
|------------|-------------|------------|
| AZURE-001 | API authentication failure | Check PAT token and permissions |
| AZURE-002 | Project not found | Verify Azure DevOps project name |
| AZURE-003 | Export format error | Use supported export format |
| AZURE-004 | User mapping error | Check user mapping file format |
| AZURE-005 | Field mapping error | Check field mapping configuration |

### Logs and Diagnostics

Migration logs are stored in:
- `~/.rinna/logs/migration.log`
- `~/.rinna/logs/azure-export.log`

Enable debug mode for verbose logging:
```bash
rin-migrate import --source azure-export.json --project PROJECT_KEY --debug
```

## Post-Migration Steps

1. Verify work item counts match between systems
2. Test workflows and transitions in Rinna
3. Confirm user assignments are correct
4. Validate that hierarchies and relationships are preserved
5. Check attachment and comment migration
6. Update team processes to use Rinna instead of Azure DevOps

## Resources

- [Azure DevOps REST API Reference](https://docs.microsoft.com/en-us/rest/api/azure/devops/)
- [Rinna Data Model Reference](../reference/data-model.md)
- [Workflow State Mapping Guide](../workflow.md)
- [Common Migration Patterns](./migration-patterns.md)
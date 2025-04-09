# Migration Guide

This guide provides comprehensive strategies for migrating to Rinna from other project management and workflow tools.

## Contents
- [Choosing a Migration Strategy](#choosing-a-migration-strategy)
- [Migration Guides by Source System](#migration-guides-by-source-system)
- [Data Mapping Reference](#data-mapping-reference)
- [Migration Tools](#migration-tools)
- [Post-Migration Steps](#post-migration-steps)

## Choosing a Migration Strategy

Rinna offers three primary migration strategies:

### 1. Complete Migration
A full migration where all historical data and active work items are transferred to Rinna.

**Best for:** Teams fully switching to Rinna with historical reporting needs.

**Process Overview:**
1. Export all data from source system
2. Transform data using Rinna mapping tools
3. Import into Rinna with full history preservation
4. Verify data integrity
5. Deactivate source system

### 2. Hybrid Migration
Active work items are migrated to Rinna while completed items remain in the legacy system.

**Best for:** Teams needing to maintain historical data in original system.

**Process Overview:**
1. Export active (non-completed) work items from source system
2. Transform data using Rinna mapping tools
3. Import active items into Rinna
4. Set up bidirectional synchronization (if needed)
5. Keep source system for historical reporting

### 3. Fresh Start Migration
Only basic project configuration is migrated, with new work items created in Rinna.

**Best for:** Teams wanting a clean break from legacy processes.

**Process Overview:**
1. Export project configuration from source system
2. Create matching projects in Rinna
3. Set up workflow states and permissions
4. Create new work items in Rinna
5. Keep source system available for reference only

## Migration Guides by Source System

Detailed guides for specific source systems:

- [Migrating from Jira](jira-migration.md)
- [Migrating from Azure DevOps](azure-devops-migration.md)
- [Migrating from GitHub Issues](github-issues-migration.md)
- [Migrating from Trello](trello-migration.md)
- [Migrating from GitLab Issues](gitlab-issues-migration.md)
- [Migrating from Linear](linear-migration.md)

## Data Mapping Reference

This reference provides standard mappings between common systems and Rinna's data model.

### Work Item Type Mapping

| Source System | Source Type | Rinna Type |
|---------------|-------------|------------|
| Jira | Epic | EPIC |
| Jira | Story | STORY |
| Jira | Bug | BUG |
| Jira | Task | TASK |
| Azure DevOps | Epic | EPIC |
| Azure DevOps | User Story | STORY |
| Azure DevOps | Bug | BUG |
| Azure DevOps | Task | TASK |
| GitHub | Issue | TASK |
| GitHub | Issue (bug label) | BUG |
| GitHub | Issue (enhancement label) | FEATURE |
| Trello | Card | TASK |
| GitLab | Issue | TASK |
| GitLab | Issue (bug label) | BUG |
| Linear | Issue | TASK |

### Status Mapping

| Source System | Source Status | Rinna Status |
|---------------|---------------|--------------|
| Jira | To Do | FOUND |
| Jira | In Progress | IN_PROGRESS |
| Jira | In Review | IN_TEST |
| Jira | Done | DONE |
| Azure DevOps | New | FOUND |
| Azure DevOps | Active | IN_PROGRESS |
| Azure DevOps | Resolved | IN_TEST |
| Azure DevOps | Closed | DONE |
| GitHub | Open | TO_DO |
| GitHub | In Progress | IN_PROGRESS |
| GitHub | Review | IN_TEST |
| GitHub | Closed | DONE |
| Trello | To Do | FOUND |
| Trello | Doing | IN_PROGRESS |
| Trello | Testing | IN_TEST |
| Trello | Done | DONE |
| GitLab | Open | TO_DO |
| GitLab | In Progress | IN_PROGRESS |
| GitLab | In Review | IN_TEST |
| GitLab | Closed | DONE |
| Linear | Todo | TO_DO |
| Linear | In Progress | IN_PROGRESS |
| Linear | In Review | IN_TEST |
| Linear | Done | DONE |

### Priority Mapping

| Source System | Source Priority | Rinna Priority |
|---------------|----------------|----------------|
| Jira | Highest | CRITICAL |
| Jira | High | HIGH |
| Jira | Medium | MEDIUM |
| Jira | Low | LOW |
| Jira | Lowest | TRIVIAL |
| Azure DevOps | 1 | CRITICAL |
| Azure DevOps | 2 | HIGH |
| Azure DevOps | 3 | MEDIUM |
| Azure DevOps | 4 | LOW |
| GitHub | High priority | HIGH |
| GitHub | Medium priority | MEDIUM |
| GitHub | Low priority | LOW |
| Trello | Red label | HIGH |
| Trello | Yellow label | MEDIUM |
| Trello | Green label | LOW |
| GitLab | High | HIGH |
| GitLab | Medium | MEDIUM |
| GitLab | Low | LOW |
| Linear | Urgent | CRITICAL |
| Linear | High | HIGH |
| Linear | Medium | MEDIUM |
| Linear | Low | LOW |

## Migration Tools

Rinna provides several tools to assist with migration:

### rin-migrate CLI Tool

The `rin-migrate` command-line tool supports importing data from various sources:

```bash
# Basic migration from Jira
rin-migrate from jira --url https://your-jira.atlassian.net --project KEY

# Migration from Azure DevOps
rin-migrate from azure --org organization --project project-name 

# Migration from GitHub Issues
rin-migrate from github --repo owner/repository

# Migration from CSV file with custom mapping
rin-migrate from csv --file export.csv --mapping mapping.json

# Dry-run migration (no actual import)
rin-migrate from jira --url https://your-jira.atlassian.net --project KEY --dry-run

# Migration with specific mapping overrides
rin-migrate from jira --url https://your-jira.atlassian.net --project KEY --mapping custom-jira-mapping.json
```

### Mapping Files

Custom JSON mapping files allow you to define how fields from source systems map to Rinna:

```json
{
  "typeMapping": {
    "Story": "STORY",
    "Enhancement": "FEATURE",
    "Custom Issue": "TASK"
  },
  "statusMapping": {
    "Backlog": "FOUND",
    "Ready for Dev": "TRIAGED",
    "In Dev": "IN_PROGRESS",
    "QA": "IN_TEST",
    "Ready for Release": "DONE"
  },
  "priorityMapping": {
    "P0": "CRITICAL",
    "P1": "HIGH",
    "P2": "MEDIUM",
    "P3": "LOW",
    "P4": "TRIVIAL"
  },
  "fieldMapping": {
    "summary": "title",
    "description": "description",
    "acceptance_criteria": "metadata.acceptanceCriteria",
    "story_points": "metadata.storyPoints",
    "custom_field_10023": "metadata.businessValue"
  }
}
```

### Rinna Web Import

For smaller migrations, Rinna's web interface provides a guided import process:

1. Navigate to Project Settings > Import
2. Select source system
3. Provide credentials or upload export file
4. Configure field mappings 
5. Preview and confirm import

## Post-Migration Steps

### 1. Validation

After migrating, verify data integrity:

```bash
# Run validation check
rin-migrate validate

# Check specific errors
rin-migrate validate --errors-only

# Export validation report
rin-migrate validate --export validation-report.json
```

### 2. User Training

- Schedule training sessions for team members
- Provide links to Rinna documentation
- Highlight differences from previous system
- Create cheat sheets for common operations

### 3. Process Adjustment

- Review and update team workflows
- Configure notifications and integrations
- Set up recurring reports
- Establish feedback channels for migration issues

### 4. Legacy System Handling

Depending on migration strategy:

- **Complete Migration**: Archive legacy system after validation period
- **Hybrid Migration**: Configure bidirectional sync if needed
- **Fresh Start**: Keep legacy system accessible for historical reference

## Common Challenges and Solutions

| Challenge | Solution |
|-----------|----------|
| Missing custom fields | Create custom metadata fields in Rinna before import |
| Workflow state mismatch | Use custom mapping files to handle special cases |
| User assignment issues | Ensure users exist in Rinna before migration |
| Attachment migration | Use `--with-attachments` flag with rin-migrate |
| Comment history | Use `--with-comments` flag to preserve discussions |
| Failed migrations | Run with `--resume` flag to continue from last checkpoint |

## Support

If you encounter issues during migration:

- Check the migration log at `~/.rinna/logs/migration.log`
- Reference the detailed error codes in [Migration Error Guide](migration-errors.md)
- Contact support at migration-support@rinna.org
- Open an issue in the support portal with tag "migration"
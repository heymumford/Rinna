# Migration Error Guide

This document provides a comprehensive list of error codes that may occur during migration, along with their causes and resolutions.

## Error Code Format

Migration error codes follow the format:

`SOURCE-CATEGORY-CODE`

Where:
- `SOURCE` identifies the source system (JIRA, AZURE, GITHUB, etc.)
- `CATEGORY` identifies the error category (AUTH, DATA, MAPPING, etc.)
- `CODE` is a numeric identifier

## General Errors (GEN)

| Error Code | Description | Possible Causes | Resolution |
|------------|-------------|-----------------|------------|
| GEN-INIT-001 | Migration initialization failed | Configuration file missing or invalid | Check configuration file exists and is valid JSON/YAML |
| GEN-INIT-002 | Invalid project key | Project key doesn't exist or is invalid | Verify the project key and create the project if needed |
| GEN-AUTH-001 | Authentication failed | Invalid API token or credentials | Check credentials and token permissions |
| GEN-FILE-001 | Invalid source file | Source file is corrupted or in wrong format | Verify the file format and try re-exporting |
| GEN-MAP-001 | Invalid mapping file | Mapping file syntax error | Check JSON syntax in mapping file |
| GEN-NET-001 | Network error | Connection timeout or network issue | Check network connectivity and try again |
| GEN-IO-001 | I/O error | File system permissions issue | Check file permissions and disk space |
| GEN-MEM-001 | Memory limit exceeded | Large import exceeding memory limits | Split import into smaller batches or increase memory |
| GEN-SYS-001 | Unexpected system error | Various system-level issues | Check logs for details and contact support |

## Jira-Specific Errors (JIRA)

| Error Code | Description | Possible Causes | Resolution |
|------------|-------------|-----------------|------------|
| JIRA-AUTH-001 | Jira authentication failed | Invalid username, token, or permissions | Verify Jira credentials and token scope |
| JIRA-AUTH-002 | Token expired | API token has expired | Generate a new API token in Jira |
| JIRA-PROJ-001 | Project not found | Invalid project key | Verify the Jira project key |
| JIRA-PROJ-002 | No permission to access project | User lacks permissions | Use admin user or grant necessary permissions |
| JIRA-API-001 | API rate limit exceeded | Too many requests | Use `--throttle` parameter to slow requests |
| JIRA-API-002 | API version incompatible | Using deprecated endpoints | Update API version in configuration |
| JIRA-DATA-001 | Custom field extraction failed | Unknown custom field | Export field list first with `rin-migrate jira fields` |
| JIRA-DATA-002 | Issue history retrieval failed | History API limitations | Try without `--include-history` |
| JIRA-DATA-003 | Attachment retrieval failed | Attachment too large or inaccessible | Check attachment limits or use `--skip-attachments` |
| JIRA-MAP-001 | Status mapping failed | Custom workflow not mapped | Add custom status in mapping file |
| JIRA-MAP-002 | Issue type mapping failed | Custom issue type not mapped | Add custom type in mapping file |
| JIRA-MAP-003 | User mapping failed | User not found in mapping | Update user mapping file or create user |

## Azure DevOps Errors (AZURE)

| Error Code | Description | Possible Causes | Resolution |
|------------|-------------|-----------------|------------|
| AZURE-AUTH-001 | Azure DevOps authentication failed | Invalid PAT token | Verify token and check it has correct scopes |
| AZURE-AUTH-002 | PAT token expired | Token has expired | Generate new Personal Access Token |
| AZURE-PROJ-001 | Project not found | Invalid project name | Verify Azure DevOps project name |
| AZURE-PROJ-002 | Organization not found | Invalid organization name | Verify Azure DevOps organization name |
| AZURE-API-001 | API rate limit exceeded | Too many requests | Use `--throttle` parameter to slow requests |
| AZURE-DATA-001 | Work item retrieval failed | Work item ID not accessible | Check permissions or exclude specific IDs |
| AZURE-DATA-002 | Area path retrieval failed | Invalid area path structure | Export structure first |
| AZURE-DATA-003 | Iteration path retrieval failed | Invalid iteration structure | Export structure first |
| AZURE-DATA-004 | Attachment retrieval failed | Attachment too large | Check size limits or use `--skip-attachments` |
| AZURE-MAP-001 | State mapping failed | Custom workflow not mapped | Add custom state in mapping file |
| AZURE-MAP-002 | Work item type mapping failed | Custom type not mapped | Add custom type in mapping file |
| AZURE-MAP-003 | Field mapping failed | Custom field not mapped | Add field mapping to configuration |
| AZURE-HIER-001 | Hierarchy preservation failed | Invalid parent-child structure | Use `--ignore-hierarchy` to skip hierarchy |

## GitHub Errors (GITHUB)

| Error Code | Description | Possible Causes | Resolution |
|------------|-------------|-----------------|------------|
| GITHUB-AUTH-001 | GitHub authentication failed | Invalid token | Verify GitHub token and scope |
| GITHUB-AUTH-002 | Token lacks permissions | Insufficient scope | Generate token with `repo` scope |
| GITHUB-REPO-001 | Repository not found | Invalid repository path | Check owner/repo format |
| GITHUB-REPO-002 | Repository access denied | Permission issue | Use token with repository access |
| GITHUB-API-001 | API rate limit exceeded | Too many requests | Use `--throttle` parameter or authenticated requests |
| GITHUB-DATA-001 | Issue retrieval failed | Issue access issue | Check permissions |
| GITHUB-DATA-002 | Project board retrieval failed | Project access issue | Verify project permissions |
| GITHUB-DATA-003 | Labels retrieval failed | Label access issue | Check repository access |
| GITHUB-MAP-001 | Label mapping failed | Missing label mapping | Add custom label mapping |
| GITHUB-MAP-002 | Project column mapping failed | Custom column not mapped | Add column in mapping file |
| GITHUB-MAP-003 | User mapping failed | GitHub user not mapped | Update user mapping file |

## Trello Errors (TRELLO)

| Error Code | Description | Possible Causes | Resolution |
|------------|-------------|-----------------|------------|
| TRELLO-AUTH-001 | Trello authentication failed | Invalid API key or token | Verify Trello API key and token |
| TRELLO-AUTH-002 | Token expired | Trello token expired | Generate new token |
| TRELLO-BRD-001 | Board not found | Invalid board ID | Verify Trello board ID |
| TRELLO-API-001 | API rate limit exceeded | Too many requests | Use `--throttle` parameter to slow requests |
| TRELLO-DATA-001 | Card retrieval failed | Card access issue | Check permissions |
| TRELLO-DATA-002 | Attachment retrieval failed | Attachment too large | Check size limits or use `--skip-attachments` |
| TRELLO-MAP-001 | List mapping failed | Custom list not mapped | Add list mapping |
| TRELLO-MAP-002 | Label mapping failed | Custom label not mapped | Add label mapping |
| TRELLO-MAP-003 | Member mapping failed | Trello member not mapped | Update user mapping file |

## GitLab Errors (GITLAB)

| Error Code | Description | Possible Causes | Resolution |
|------------|-------------|-----------------|------------|
| GITLAB-AUTH-001 | GitLab authentication failed | Invalid token | Verify GitLab token |
| GITLAB-PROJ-001 | Project not found | Invalid project path | Check project path format |
| GITLAB-API-001 | API rate limit exceeded | Too many requests | Use `--throttle` parameter to slow requests |
| GITLAB-DATA-001 | Issue retrieval failed | Issue access issue | Check permissions |
| GITLAB-DATA-002 | Milestone retrieval failed | Milestone access issue | Verify project permissions |
| GITLAB-MAP-001 | Label mapping failed | Missing label mapping | Add custom label mapping |
| GITLAB-MAP-002 | User mapping failed | GitLab user not mapped | Update user mapping file |

## Import Errors (IMPORT)

| Error Code | Description | Possible Causes | Resolution |
|------------|-------------|-----------------|------------|
| IMPORT-INIT-001 | Import initialization failed | Configuration issue | Check import configuration |
| IMPORT-PROJ-001 | Target project not found | Project doesn't exist | Create project before import |
| IMPORT-DATA-001 | Data validation failed | Invalid data format | Check exported data structure |
| IMPORT-DATA-002 | Duplicate detection failed | Identity field missing | Configure identity field mapping |
| IMPORT-DATA-003 | Required field missing | Mandatory field not provided | Check required field mapping |
| IMPORT-DATA-004 | Field type mismatch | Data type incompatible | Check field type mapping |
| IMPORT-REL-001 | Relationship creation failed | Invalid relationship | Verify relationship mapping |
| IMPORT-BATCH-001 | Batch size exceeded | Too many items in batch | Reduce batch size with `--batch-size` |
| IMPORT-USER-001 | User creation failed | User already exists | Use `--skip-users` or fix mapping |
| IMPORT-ATT-001 | Attachment import failed | Size or format issue | Check attachment settings |

## Verification Errors (VERIFY)

| Error Code | Description | Possible Causes | Resolution |
|------------|-------------|-----------------|------------|
| VERIFY-COUNT-001 | Count mismatch | Items not fully imported | Check import logs for failures |
| VERIFY-REL-001 | Relationship mismatch | Relationships not imported | Verify relationship mappings |
| VERIFY-META-001 | Metadata mismatch | Custom fields not imported | Check metadata field mappings |
| VERIFY-ATT-001 | Attachment count mismatch | Some attachments not imported | Check attachment failures |
| VERIFY-STATE-001 | State mapping mismatch | States incorrectly mapped | Verify state mapping configuration |

## Sync Errors (SYNC)

| Error Code | Description | Possible Causes | Resolution |
|------------|-------------|-----------------|------------|
| SYNC-INIT-001 | Sync initialization failed | Configuration issue | Check sync configuration |
| SYNC-AUTH-001 | Sync authentication failed | Expired credentials | Update integration credentials |
| SYNC-CONF-001 | Sync conflict detected | Item modified in both systems | Resolve conflict manually |
| SYNC-FIELD-001 | Field sync failed | Field mapping issue | Check field mappings |
| SYNC-LOCK-001 | Item locked in source system | Item being modified | Wait and retry or force update |

## Troubleshooting Steps

When encountering migration errors, follow these steps:

1. **Check logs**: Review detailed logs at `~/.rinna/logs/migration.log`
2. **Verify inputs**: Confirm source files and mapping configurations
3. **Run with debug**: Add `--debug` flag for verbose output
4. **Use dry run**: Test with `--dry-run` to validate without importing
5. **Isolate issues**: Try migrating smaller batches of data
6. **Check permissions**: Verify API tokens have necessary permissions
7. **Update mappings**: Adjust mapping files to handle custom fields/types
8. **Check connectivity**: Verify network access to source system
9. **Incremental approach**: Start with basic data, then add complexity

## Getting Support

If you cannot resolve migration errors after troubleshooting:

1. Generate a diagnostic report:
   ```bash
   rin-migrate diagnostics --output diagnostic-report.zip
   ```

2. Submit the diagnostic report along with:
   - Migration command used
   - Error codes encountered
   - Steps already attempted
   - Source and target system details

Contact support at migration-support@rinna.org or open an issue in the support portal with tag "migration".
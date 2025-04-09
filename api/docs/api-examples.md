# Rinna API Examples

This guide provides detailed examples for using the Rinna API effectively. Each endpoint is demonstrated with complete request/response examples in multiple formats.

## Table of Contents

- [Authentication](#authentication)
- [Projects](#projects)
- [Work Items](#work-items)
- [Releases](#releases)
- [Webhooks](#webhooks)
- [Error Handling](#error-handling)
- [Advanced Usage Patterns](#advanced-usage-patterns)

## Authentication

All API requests require authentication using an API token passed in the `Authorization` header.

```
Authorization: Bearer ri-dev-f72a159e4bdc
```

### Token Validation

```bash
# Check if a token is valid
curl -X POST "https://your-rinna-instance/api/v1/auth/token/validate" \
  -H "Content-Type: application/json" \
  -d '{
    "token": "ri-dev-f72a159e4bdc"
  }'
```

Response:

```json
{
  "valid": true,
  "projectId": "billing-system"
}
```

## Projects

### List Projects

#### Basic Listing

```bash
# List all projects
curl -X GET "https://your-rinna-instance/api/v1/projects" \
  -H "Authorization: Bearer ri-dev-f72a159e4bdc"
```

Response:

```json
{
  "items": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "key": "RINNA",
      "name": "Rinna Project Management",
      "description": "Internal project management system",
      "active": true,
      "createdAt": "2025-02-15T14:30:45Z",
      "updatedAt": "2025-02-20T09:15:22Z"
    },
    {
      "id": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
      "key": "BILLING",
      "name": "Billing System",
      "description": "Customer billing management",
      "active": true,
      "createdAt": "2025-01-10T08:45:30Z",
      "updatedAt": "2025-02-18T11:22:40Z"
    }
  ],
  "totalCount": 2,
  "page": 1,
  "pageSize": 10
}
```

#### With Filtering

```bash
# List only active projects
curl -X GET "https://your-rinna-instance/api/v1/projects?active=true" \
  -H "Authorization: Bearer ri-dev-f72a159e4bdc"
```

#### With Pagination

```bash
# Get page 2 with 5 items per page
curl -X GET "https://your-rinna-instance/api/v1/projects?page=2&size=5" \
  -H "Authorization: Bearer ri-dev-f72a159e4bdc"
```

### Create Project

```bash
# Create a new project
curl -X POST "https://your-rinna-instance/api/v1/projects" \
  -H "Authorization: Bearer ri-dev-f72a159e4bdc" \
  -H "Content-Type: application/json" \
  -d '{
    "key": "INFRA",
    "name": "Infrastructure Team",
    "description": "Cloud infrastructure management",
    "metadata": {
      "department": "IT",
      "costCenter": "CC-123"
    }
  }'
```

Response:

```json
{
  "id": "7d8f3ab4-c2e5-47d6-9a1b-8fc7e41fea23",
  "key": "INFRA",
  "name": "Infrastructure Team",
  "description": "Cloud infrastructure management",
  "active": true,
  "metadata": {
    "department": "IT",
    "costCenter": "CC-123"
  },
  "createdAt": "2025-04-08T13:45:22Z",
  "updatedAt": "2025-04-08T13:45:22Z"
}
```

### Get Project

```bash
# Get project by key
curl -X GET "https://your-rinna-instance/api/v1/projects/INFRA" \
  -H "Authorization: Bearer ri-dev-f72a159e4bdc"
```

Response:

```json
{
  "id": "7d8f3ab4-c2e5-47d6-9a1b-8fc7e41fea23",
  "key": "INFRA",
  "name": "Infrastructure Team",
  "description": "Cloud infrastructure management",
  "active": true,
  "metadata": {
    "department": "IT",
    "costCenter": "CC-123"
  },
  "createdAt": "2025-04-08T13:45:22Z",
  "updatedAt": "2025-04-08T13:45:22Z"
}
```

### Update Project

```bash
# Update a project
curl -X PUT "https://your-rinna-instance/api/v1/projects/INFRA" \
  -H "Authorization: Bearer ri-dev-f72a159e4bdc" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Infrastructure & Cloud Team",
    "description": "Cloud infrastructure and platform management",
    "metadata": {
      "department": "IT",
      "costCenter": "CC-456"
    }
  }'
```

Response:

```json
{
  "id": "7d8f3ab4-c2e5-47d6-9a1b-8fc7e41fea23",
  "key": "INFRA",
  "name": "Infrastructure & Cloud Team",
  "description": "Cloud infrastructure and platform management",
  "active": true,
  "metadata": {
    "department": "IT",
    "costCenter": "CC-456"
  },
  "createdAt": "2025-04-08T13:45:22Z",
  "updatedAt": "2025-04-08T14:22:45Z"
}
```

### Get Project Work Items

```bash
# Get work items for a project
curl -X GET "https://your-rinna-instance/api/v1/projects/INFRA/workitems" \
  -H "Authorization: Bearer ri-dev-f72a159e4bdc"
```

Response:

```json
{
  "items": [
    {
      "id": "9c4f98e2-1a2b-3c4d-5e6f-7a8b9c0d1e2f",
      "title": "Set up AWS account structure",
      "description": "Create organization and sub-accounts for dev/test/prod",
      "type": "FEATURE",
      "priority": "HIGH",
      "status": "IN_DEV",
      "assignee": "john.smith",
      "projectId": "7d8f3ab4-c2e5-47d6-9a1b-8fc7e41fea23",
      "metadata": {
        "estimatedHours": "24"
      },
      "createdAt": "2025-04-08T14:30:00Z",
      "updatedAt": "2025-04-08T15:45:30Z"
    }
  ],
  "totalCount": 1,
  "page": 1,
  "pageSize": 10
}
```

#### With Filtering

```bash
# Get in-progress work items for a project
curl -X GET "https://your-rinna-instance/api/v1/projects/INFRA/workitems?status=IN_DEV" \
  -H "Authorization: Bearer ri-dev-f72a159e4bdc"
```

## Work Items

### List Work Items

#### Basic Listing

```bash
# List all work items
curl -X GET "https://your-rinna-instance/api/v1/workitems" \
  -H "Authorization: Bearer ri-dev-f72a159e4bdc"
```

Response:

```json
{
  "items": [
    {
      "id": "9c4f98e2-1a2b-3c4d-5e6f-7a8b9c0d1e2f",
      "title": "Set up AWS account structure",
      "description": "Create organization and sub-accounts for dev/test/prod",
      "type": "FEATURE",
      "priority": "HIGH",
      "status": "IN_DEV",
      "assignee": "john.smith",
      "projectId": "7d8f3ab4-c2e5-47d6-9a1b-8fc7e41fea23",
      "metadata": {
        "estimatedHours": "24"
      },
      "createdAt": "2025-04-08T14:30:00Z",
      "updatedAt": "2025-04-08T15:45:30Z"
    },
    {
      "id": "123e4567-e89b-12d3-a456-426614174000",
      "title": "Implement user authentication",
      "description": "Add OAuth2 support for user login",
      "type": "FEATURE",
      "priority": "HIGH",
      "status": "TESTING",
      "assignee": "jane.doe",
      "projectId": "550e8400-e29b-41d4-a716-446655440000",
      "metadata": {
        "estimatedHours": "16"
      },
      "createdAt": "2025-03-15T10:20:30Z",
      "updatedAt": "2025-04-05T11:35:20Z"
    }
  ],
  "totalCount": 2,
  "page": 1,
  "pageSize": 10
}
```

#### With Multiple Filters

```bash
# List high priority bugs assigned to specific user
curl -X GET "https://your-rinna-instance/api/v1/workitems?type=BUG&priority=HIGH&assignee=john.smith" \
  -H "Authorization: Bearer ri-dev-f72a159e4bdc"
```

#### With Project Filter

```bash
# List work items for a specific project
curl -X GET "https://your-rinna-instance/api/v1/workitems?project=INFRA" \
  -H "Authorization: Bearer ri-dev-f72a159e4bdc"
```

### Create Work Item

```bash
# Create a new work item
curl -X POST "https://your-rinna-instance/api/v1/workitems" \
  -H "Authorization: Bearer ri-dev-f72a159e4bdc" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Implement SSO for admin portal",
    "description": "Add support for Azure AD single sign-on",
    "type": "FEATURE",
    "priority": "HIGH",
    "projectId": "INFRA",
    "metadata": {
      "requestedBy": "security-team",
      "estimatedHours": "12"
    }
  }'
```

Response:

```json
{
  "id": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "title": "Implement SSO for admin portal",
  "description": "Add support for Azure AD single sign-on",
  "type": "FEATURE",
  "priority": "HIGH",
  "status": "FOUND",
  "projectId": "7d8f3ab4-c2e5-47d6-9a1b-8fc7e41fea23",
  "metadata": {
    "requestedBy": "security-team",
    "estimatedHours": "12"
  },
  "createdAt": "2025-04-08T16:10:45Z",
  "updatedAt": "2025-04-08T16:10:45Z"
}
```

### Get Work Item

```bash
# Get work item by ID
curl -X GET "https://your-rinna-instance/api/v1/workitems/a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d" \
  -H "Authorization: Bearer ri-dev-f72a159e4bdc"
```

Response:

```json
{
  "id": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "title": "Implement SSO for admin portal",
  "description": "Add support for Azure AD single sign-on",
  "type": "FEATURE",
  "priority": "HIGH",
  "status": "FOUND",
  "projectId": "7d8f3ab4-c2e5-47d6-9a1b-8fc7e41fea23",
  "metadata": {
    "requestedBy": "security-team",
    "estimatedHours": "12"
  },
  "createdAt": "2025-04-08T16:10:45Z",
  "updatedAt": "2025-04-08T16:10:45Z"
}
```

### Update Work Item

```bash
# Update a work item
curl -X PUT "https://your-rinna-instance/api/v1/workitems/a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d" \
  -H "Authorization: Bearer ri-dev-f72a159e4bdc" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Implement SSO for admin portal with MFA",
    "assignee": "jane.doe",
    "metadata": {
      "requestedBy": "security-team",
      "estimatedHours": "16",
      "complexity": "medium"
    }
  }'
```

Response:

```json
{
  "id": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "title": "Implement SSO for admin portal with MFA",
  "description": "Add support for Azure AD single sign-on",
  "type": "FEATURE",
  "priority": "HIGH",
  "status": "FOUND",
  "assignee": "jane.doe",
  "projectId": "7d8f3ab4-c2e5-47d6-9a1b-8fc7e41fea23",
  "metadata": {
    "requestedBy": "security-team",
    "estimatedHours": "16",
    "complexity": "medium"
  },
  "createdAt": "2025-04-08T16:10:45Z",
  "updatedAt": "2025-04-08T16:30:22Z"
}
```

### Transition Work Item

```bash
# Transition a work item to a new state
curl -X POST "https://your-rinna-instance/api/v1/workitems/a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d/transitions" \
  -H "Authorization: Bearer ri-dev-f72a159e4bdc" \
  -H "Content-Type: application/json" \
  -d '{
    "targetStatus": "IN_DEV",
    "comment": "Starting implementation with Azure AD libraries"
  }'
```

Response:

```json
{
  "id": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "title": "Implement SSO for admin portal with MFA",
  "description": "Add support for Azure AD single sign-on",
  "type": "FEATURE",
  "priority": "HIGH",
  "status": "IN_DEV",
  "assignee": "jane.doe",
  "projectId": "7d8f3ab4-c2e5-47d6-9a1b-8fc7e41fea23",
  "metadata": {
    "requestedBy": "security-team",
    "estimatedHours": "16",
    "complexity": "medium"
  },
  "createdAt": "2025-04-08T16:10:45Z",
  "updatedAt": "2025-04-08T16:45:10Z"
}
```

## Releases

### List Releases

```bash
# List all releases
curl -X GET "https://your-rinna-instance/api/v1/releases" \
  -H "Authorization: Bearer ri-dev-f72a159e4bdc"
```

Response:

```json
{
  "items": [
    {
      "id": "98765432-abcd-efgh-ijkl-1234567890ab",
      "name": "Q2 2025 Release",
      "version": "2.4.0",
      "description": "Major feature release for Q2",
      "status": "IN_PROGRESS",
      "startDate": "2025-04-01",
      "endDate": "2025-06-30",
      "projectKey": "RINNA",
      "metadata": {
        "releaseManager": "alex.walker",
        "priority": "strategic"
      },
      "createdAt": "2025-03-15T10:20:30Z",
      "updatedAt": "2025-04-01T09:15:45Z"
    }
  ],
  "totalCount": 1,
  "page": 1,
  "pageSize": 10
}
```

#### With Filtering

```bash
# List in-progress releases for a specific project
curl -X GET "https://your-rinna-instance/api/v1/releases?status=IN_PROGRESS&project=RINNA" \
  -H "Authorization: Bearer ri-dev-f72a159e4bdc"
```

### Create Release

```bash
# Create a new release
curl -X POST "https://your-rinna-instance/api/v1/releases" \
  -H "Authorization: Bearer ri-dev-f72a159e4bdc" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Infrastructure Platform 1.0",
    "version": "1.0.0",
    "description": "Initial platform release",
    "status": "PLANNED",
    "startDate": "2025-05-01",
    "endDate": "2025-05-15",
    "projectKey": "INFRA",
    "metadata": {
      "releaseManager": "john.smith",
      "riskLevel": "medium"
    }
  }'
```

Response:

```json
{
  "id": "abcdef12-3456-7890-abcd-ef1234567890",
  "name": "Infrastructure Platform 1.0",
  "version": "1.0.0",
  "description": "Initial platform release",
  "status": "PLANNED",
  "startDate": "2025-05-01",
  "endDate": "2025-05-15",
  "projectKey": "INFRA",
  "metadata": {
    "releaseManager": "john.smith",
    "riskLevel": "medium"
  },
  "createdAt": "2025-04-08T17:10:30Z",
  "updatedAt": "2025-04-08T17:10:30Z"
}
```

### Get Release

```bash
# Get release by ID
curl -X GET "https://your-rinna-instance/api/v1/releases/abcdef12-3456-7890-abcd-ef1234567890" \
  -H "Authorization: Bearer ri-dev-f72a159e4bdc"
```

Response:

```json
{
  "id": "abcdef12-3456-7890-abcd-ef1234567890",
  "name": "Infrastructure Platform 1.0",
  "version": "1.0.0",
  "description": "Initial platform release",
  "status": "PLANNED",
  "startDate": "2025-05-01",
  "endDate": "2025-05-15",
  "projectKey": "INFRA",
  "metadata": {
    "releaseManager": "john.smith",
    "riskLevel": "medium"
  },
  "createdAt": "2025-04-08T17:10:30Z",
  "updatedAt": "2025-04-08T17:10:30Z"
}
```

### Update Release

```bash
# Update a release
curl -X PUT "https://your-rinna-instance/api/v1/releases/abcdef12-3456-7890-abcd-ef1234567890" \
  -H "Authorization: Bearer ri-dev-f72a159e4bdc" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "IN_PROGRESS",
    "endDate": "2025-05-30",
    "metadata": {
      "releaseManager": "john.smith",
      "riskLevel": "high",
      "changeApprovalID": "CAB-2025-042"
    }
  }'
```

Response:

```json
{
  "id": "abcdef12-3456-7890-abcd-ef1234567890",
  "name": "Infrastructure Platform 1.0",
  "version": "1.0.0",
  "description": "Initial platform release",
  "status": "IN_PROGRESS",
  "startDate": "2025-05-01",
  "endDate": "2025-05-30",
  "projectKey": "INFRA",
  "metadata": {
    "releaseManager": "john.smith",
    "riskLevel": "high",
    "changeApprovalID": "CAB-2025-042"
  },
  "createdAt": "2025-04-08T17:10:30Z",
  "updatedAt": "2025-04-08T17:25:15Z"
}
```

### Get Release Work Items

```bash
# Get work items in a release
curl -X GET "https://your-rinna-instance/api/v1/releases/abcdef12-3456-7890-abcd-ef1234567890/workitems" \
  -H "Authorization: Bearer ri-dev-f72a159e4bdc"
```

Response:

```json
{
  "items": [
    {
      "id": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
      "title": "Implement SSO for admin portal with MFA",
      "description": "Add support for Azure AD single sign-on",
      "type": "FEATURE",
      "priority": "HIGH",
      "status": "IN_DEV",
      "assignee": "jane.doe",
      "projectId": "7d8f3ab4-c2e5-47d6-9a1b-8fc7e41fea23",
      "releaseId": "abcdef12-3456-7890-abcd-ef1234567890",
      "metadata": {
        "requestedBy": "security-team",
        "estimatedHours": "16",
        "complexity": "medium"
      },
      "createdAt": "2025-04-08T16:10:45Z",
      "updatedAt": "2025-04-08T16:45:10Z"
    }
  ],
  "totalCount": 1,
  "page": 1,
  "pageSize": 10
}
```

## Webhooks

### GitHub Webhook

```bash
# GitHub webhook payload example (sent by GitHub to your webhook URL)
curl -X POST "https://your-rinna-instance/api/v1/webhooks/github?project=INFRA" \
  -H "Content-Type: application/json" \
  -H "X-GitHub-Event: pull_request" \
  -H "X-Hub-Signature-256: sha256=..." \
  -d '{
    "action": "opened",
    "pull_request": {
      "title": "Implement SSO feature",
      "body": "This PR implements the Single Sign-On feature for the admin portal.\n\nReferences: INFRA-42",
      "user": {
        "login": "jane.doe"
      },
      "state": "open",
      "html_url": "https://github.com/org/repo/pull/123"
    },
    "repository": {
      "name": "repo",
      "full_name": "org/repo",
      "owner": {
        "login": "org"
      }
    }
  }'
```

Response:

```json
{
  "success": true,
  "message": "Webhook processed successfully",
  "workItemId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "event": "pull_request"
}
```

### Custom Webhook

```bash
# Custom webhook for external integrations
curl -X POST "https://your-rinna-instance/api/v1/webhooks/custom?project=INFRA" \
  -H "Authorization: Bearer ri-dev-f72a159e4bdc" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Update network security groups",
    "description": "Update NSGs to comply with new security policies",
    "type": "CHORE",
    "priority": "MEDIUM",
    "metadata": {
      "source": "security_scanner",
      "compliance": "required",
      "deadline": "2025-06-01"
    }
  }'
```

Response:

```json
{
  "success": true,
  "message": "Work item created successfully",
  "workItemId": "fedcba98-7654-3210-fedc-ba9876543210"
}
```

## Error Handling

The API uses standard HTTP status codes and returns error details in a consistent format:

### Not Found Example

```bash
# Request a non-existent work item
curl -X GET "https://your-rinna-instance/api/v1/workitems/non-existent-id" \
  -H "Authorization: Bearer ri-dev-f72a159e4bdc"
```

Response (404):

```json
{
  "code": 404,
  "message": "Work item not found",
  "details": ["Item with id 'non-existent-id' does not exist"]
}
```

### Validation Error Example

```bash
# Attempt to create a work item with missing required fields
curl -X POST "https://your-rinna-instance/api/v1/workitems" \
  -H "Authorization: Bearer ri-dev-f72a159e4bdc" \
  -H "Content-Type: application/json" \
  -d '{
    "description": "This work item has no title"
  }'
```

Response (400):

```json
{
  "code": 400,
  "message": "Invalid request",
  "details": ["Field 'title' is required"]
}
```

### Invalid Transition Example

```bash
# Attempt an invalid workflow transition
curl -X POST "https://your-rinna-instance/api/v1/workitems/a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d/transitions" \
  -H "Authorization: Bearer ri-dev-f72a159e4bdc" \
  -H "Content-Type: application/json" \
  -d '{
    "targetStatus": "DONE",
    "comment": "Skipping testing phase"
  }'
```

Response (422):

```json
{
  "code": 422,
  "message": "Invalid transition",
  "details": ["Cannot transition from 'IN_DEV' to 'DONE'. Valid transitions are: 'TESTING'"]
}
```

## Advanced Usage Patterns

### Bulk Work Item Creation

```bash
# Create multiple work items at once
curl -X POST "https://your-rinna-instance/api/v1/workitems/bulk" \
  -H "Authorization: Bearer ri-dev-f72a159e4bdc" \
  -H "Content-Type: application/json" \
  -d '{
    "items": [
      {
        "title": "Set up CI/CD pipeline",
        "type": "FEATURE",
        "priority": "HIGH",
        "projectId": "INFRA"
      },
      {
        "title": "Configure monitoring alerts",
        "type": "CHORE",
        "priority": "MEDIUM",
        "projectId": "INFRA"
      },
      {
        "title": "Update IAM policies",
        "type": "CHORE",
        "priority": "HIGH",
        "projectId": "INFRA"
      }
    ]
  }'
```

Response:

```json
{
  "success": true,
  "created": 3,
  "failed": 0,
  "items": [
    {
      "id": "11111111-1111-1111-1111-111111111111",
      "title": "Set up CI/CD pipeline"
    },
    {
      "id": "22222222-2222-2222-2222-222222222222",
      "title": "Configure monitoring alerts"
    },
    {
      "id": "33333333-3333-3333-3333-333333333333",
      "title": "Update IAM policies"
    }
  ]
}
```

### Advanced Filtering

```bash
# Complex filtering for work items
curl -X GET "https://your-rinna-instance/api/v1/workitems/search" \
  -H "Authorization: Bearer ri-dev-f72a159e4bdc" \
  -H "Content-Type: application/json" \
  -d '{
    "project": "INFRA",
    "filters": [
      {
        "field": "status",
        "operator": "in",
        "value": ["IN_DEV", "TESTING"]
      },
      {
        "field": "priority",
        "operator": "eq",
        "value": "HIGH"
      },
      {
        "field": "metadata.estimatedHours",
        "operator": "gt",
        "value": "8"
      }
    ],
    "sort": [
      {"field": "priority", "direction": "desc"},
      {"field": "createdAt", "direction": "asc"}
    ],
    "page": 1,
    "pageSize": 20
  }'
```

Response:

```json
{
  "items": [
    {
      "id": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
      "title": "Implement SSO for admin portal with MFA",
      "description": "Add support for Azure AD single sign-on",
      "type": "FEATURE",
      "priority": "HIGH",
      "status": "IN_DEV",
      "assignee": "jane.doe",
      "projectId": "7d8f3ab4-c2e5-47d6-9a1b-8fc7e41fea23",
      "metadata": {
        "requestedBy": "security-team",
        "estimatedHours": "16",
        "complexity": "medium"
      },
      "createdAt": "2025-04-08T16:10:45Z",
      "updatedAt": "2025-04-08T16:45:10Z"
    }
  ],
  "totalCount": 1,
  "page": 1,
  "pageSize": 20
}
```

### Client SDK Usage (Java)

```java
// Initialize the client
RinnaClient client = RinnaClient.builder()
    .apiUrl("https://your-rinna-instance/api/v1")
    .apiToken("ri-dev-f72a159e4bdc")
    .build();

// Create a work item
WorkItemCreateRequest workItem = new WorkItemCreateRequest.Builder()
    .title("Implement database migration script")
    .description("Create script to migrate data from MySQL to PostgreSQL")
    .type(WorkItemType.FEATURE)
    .priority(Priority.MEDIUM)
    .projectId("INFRA")
    .addMetadata("estimatedHours", "8")
    .build();

// Submit the request
try {
    WorkItem created = client.workItems().create(workItem);
    System.out.println("Created work item: " + created.getId());
    
    // Transition the work item
    client.workItems()
        .transition(created.getId(), WorkflowState.IN_DEV, "Starting implementation");
    
    // Fetch and print work items for project
    WorkItemListResponse projectItems = client.projects()
        .getWorkItems("INFRA", null, 1, 10);
    
    System.out.println("Project has " + projectItems.getTotalCount() + " work items");
    projectItems.getItems().forEach(item -> 
        System.out.println(" - " + item.getTitle() + " (" + item.getStatus() + ")"));
        
} catch (RinnaApiException e) {
    System.err.println("API Error: " + e.getMessage());
    e.getDetails().forEach(detail -> System.err.println(" - " + detail));
}
```

### Client SDK Usage (Go)

```go
package main

import (
    "context"
    "fmt"
    "log"
    
    "github.com/heymumford/rinna/client"
)

func main() {
    // Initialize the client
    c := client.NewClient("https://your-rinna-instance/api/v1", "ri-dev-f72a159e4bdc")
    
    // Create a work item
    workItem, err := c.CreateWorkItem(context.Background(), client.WorkItemCreateRequest{
        Title:       "Update Docker containers to latest base images",
        Description: "Security update for all Docker containers",
        Type:        client.WorkItemTypeChore,
        Priority:    client.PriorityHigh,
        ProjectID:   "INFRA",
        Metadata: map[string]string{
            "estimatedHours": "4",
            "securityImpact": "high",
        },
    })
    
    if err != nil {
        log.Fatalf("Failed to create work item: %v", err)
    }
    
    fmt.Printf("Created work item: %s (%s)\n", workItem.Title, workItem.ID)
    
    // Transition the work item
    err = c.TransitionWorkItem(context.Background(), workItem.ID, client.WorkItemTransitionRequest{
        ToState: client.WorkflowStateInDev,
        Comment: "Starting the security update process",
    })
    
    if err != nil {
        log.Fatalf("Failed to transition work item: %v", err)
    }
    
    // List work items for project
    items, err := c.GetProjectWorkItems(context.Background(), "INFRA", "", 1, 10)
    if err != nil {
        log.Fatalf("Failed to get project work items: %v", err)
    }
    
    fmt.Printf("Project has %d work items:\n", items.TotalCount)
    for _, item := range items.Items {
        fmt.Printf(" - %s (%s): %s\n", item.ID, item.Status, item.Title)
    }
}
```
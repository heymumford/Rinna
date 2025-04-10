# API Endpoint Data Formats

This document provides detailed specifications for all request and response formats used in the Rinna API. 

## Table of Contents

- [Common Structures](#common-structures)
  - [Metadata](#metadata)
  - [Pagination](#pagination)
  - [Date and Time Formats](#date-and-time-formats)
  - [Identifiers](#identifiers)
- [Authentication](#authentication)
  - [Token Format](#token-format)
  - [Token Validation Request/Response](#token-validation-requestresponse)
- [Projects](#projects)
  - [Project Object](#project-object)
  - [Project List Response](#project-list-response)
  - [Project Create Request](#project-create-request)
  - [Project Update Request](#project-update-request)
- [Work Items](#work-items)
  - [Work Item Object](#work-item-object)
  - [Work Item List Response](#work-item-list-response)
  - [Work Item Create Request](#work-item-create-request)
  - [Work Item Update Request](#work-item-update-request)
  - [Work Item Transition Request](#work-item-transition-request)
  - [Work Item Search Request](#work-item-search-request)
  - [Work Item Bulk Create Request](#work-item-bulk-create-request)
- [Releases](#releases)
  - [Release Object](#release-object)
  - [Release List Response](#release-list-response)
  - [Release Create Request](#release-create-request)
  - [Release Update Request](#release-update-request)
- [Webhooks](#webhooks)
  - [Webhook Configuration Object](#webhook-configuration-object)
  - [Webhook Event Formats](#webhook-event-formats)
  - [Webhook Response Format](#webhook-response-format)
- [OAuth Integration](#oauth-integration)
  - [OAuth Provider Object](#oauth-provider-object)
  - [OAuth Token Object](#oauth-token-object)
  - [OAuth Authorization Response](#oauth-authorization-response)
- [Error Responses](#error-responses)
  - [Error Object](#error-object)
  - [Validation Error](#validation-error)
  - [Authentication Error](#authentication-error)
  - [Rate Limit Error](#rate-limit-error)
- [Advanced Features](#advanced-features)
  - [Filtering Format](#filtering-format)
  - [Sorting Format](#sorting-format)
  - [Searching Format](#searching-format)

## Common Structures

### Metadata

The metadata field is a flexible key-value map that allows custom attributes to be associated with various objects.

```json
"metadata": {
  "key1": "value1",
  "key2": "value2"
}
```

- Keys must be strings, maximum 64 characters
- Values must be strings, maximum 255 characters
- Maximum 20 metadata entries per object

### Pagination

All list endpoints return paginated results with the following structure:

```json
{
  "items": [...],
  "totalCount": 100,
  "page": 1,
  "pageSize": 10
}
```

| Field | Type | Description |
|-------|------|-------------|
| items | array | Array of objects matching the request |
| totalCount | integer | Total number of items matching the filters |
| page | integer | Current page number (1-based) |
| pageSize | integer | Number of items per page |

**Pagination Parameters:**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| page | integer | 1 | Page number to retrieve (1-based) |
| size | integer | 10 | Number of items per page (max 100) |

### Date and Time Formats

- All date-time values are in ISO 8601 format in UTC timezone: `YYYY-MM-DDThh:mm:ssZ`
  - Example: `2025-04-08T13:45:22Z`
- Date-only values are in ISO 8601 format: `YYYY-MM-DD`
  - Example: `2025-04-08`

### Identifiers

- **UUIDs**: Most objects use UUIDs as primary identifiers
  - Format: UUID v4 (8-4-4-4-12 hexadecimal digits)
  - Example: `550e8400-e29b-41d4-a716-446655440000`
- **Project Keys**: Projects also have a short, human-readable key
  - Format: 2-10 uppercase alphanumeric characters
  - Example: `INFRA`, `RINNA`

## Authentication

### Token Format

```
ri-<type>-<base64(encrypted(claims))>
```

| Part | Description |
|------|-------------|
| ri- | Fixed prefix for all Rinna API tokens |
| type | Token type: `dev`, `test`, or `prod` |
| claims | Base64-encoded encrypted claims payload |

The claims contain:
- Token ID (unique identifier)
- Project ID (associated project)
- Issuance and expiration timestamps
- Format version
- Scope (permissions)
- User ID (if applicable)

### Token Validation Request/Response

**Request:**

```json
{
  "token": "ri-dev-f72a159e4bdc"
}
```

**Response:**

```json
{
  "valid": true,
  "projectId": "INFRA",
  "userId": "john.smith",
  "scopes": ["read", "write"],
  "expiresAt": "2025-05-08T16:10:45Z"
}
```

| Field | Type | Description |
|-------|------|-------------|
| valid | boolean | Whether the token is valid |
| projectId | string | The project key associated with the token |
| userId | string | The user ID associated with the token (if any) |
| scopes | array | Array of permission scopes granted to the token |
| expiresAt | string | When the token expires (ISO 8601 format) |

## Projects

### Project Object

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "key": "RINNA",
  "name": "Rinna Project Management",
  "description": "Internal project management system",
  "active": true,
  "metadata": {
    "department": "Engineering",
    "target": "Internal"
  },
  "createdAt": "2025-02-15T14:30:45Z",
  "updatedAt": "2025-02-20T09:15:22Z"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| id | string (UUID) | Yes | Unique identifier for the project |
| key | string | Yes | Short, human-readable project identifier (2-10 uppercase alphanumeric characters) |
| name | string | Yes | Display name for the project (max 100 characters) |
| description | string | No | Detailed description of the project (max 1000 characters) |
| active | boolean | No | Whether the project is active (default: true) |
| metadata | object | No | Custom attributes for the project |
| createdAt | string | Yes | ISO 8601 datetime when the project was created |
| updatedAt | string | Yes | ISO 8601 datetime when the project was last updated |

### Project List Response

```json
{
  "items": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "key": "RINNA",
      "name": "Rinna Project Management",
      "description": "Internal project management system",
      "active": true,
      "metadata": {
        "department": "Engineering",
        "target": "Internal"
      },
      "createdAt": "2025-02-15T14:30:45Z",
      "updatedAt": "2025-02-20T09:15:22Z"
    },
    {
      "id": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
      "key": "BILLING",
      "name": "Billing System",
      "description": "Customer billing management",
      "active": true,
      "metadata": {
        "department": "Finance",
        "target": "Revenue"
      },
      "createdAt": "2025-01-10T08:45:30Z",
      "updatedAt": "2025-02-18T11:22:40Z"
    }
  ],
  "totalCount": 2,
  "page": 1,
  "pageSize": 10
}
```

**Filter Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| active | boolean | Filter by active status |

### Project Create Request

```json
{
  "key": "INFRA",
  "name": "Infrastructure Team",
  "description": "Cloud infrastructure management",
  "active": true,
  "metadata": {
    "department": "IT",
    "costCenter": "CC-123"
  }
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| key | string | Yes | Short, human-readable project identifier (2-10 uppercase alphanumeric characters) |
| name | string | Yes | Display name for the project (max 100 characters) |
| description | string | No | Detailed description of the project (max 1000 characters) |
| active | boolean | No | Whether the project is active (default: true) |
| metadata | object | No | Custom attributes for the project |

### Project Update Request

```json
{
  "name": "Infrastructure & Cloud Team",
  "description": "Cloud infrastructure and platform management",
  "active": true,
  "metadata": {
    "department": "IT",
    "costCenter": "CC-456"
  }
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| name | string | No | Display name for the project (max 100 characters) |
| description | string | No | Detailed description of the project (max 1000 characters) |
| active | boolean | No | Whether the project is active |
| metadata | object | No | Custom attributes for the project (replaces existing metadata) |

## Work Items

### Work Item Object

```json
{
  "id": "9c4f98e2-1a2b-3c4d-5e6f-7a8b9c0d1e2f",
  "title": "Set up AWS account structure",
  "description": "Create organization and sub-accounts for dev/test/prod",
  "type": "FEATURE",
  "priority": "HIGH",
  "status": "IN_DEV",
  "assignee": "john.smith",
  "projectId": "7d8f3ab4-c2e5-47d6-9a1b-8fc7e41fea23",
  "releaseId": "abcdef12-3456-7890-abcd-ef1234567890",
  "metadata": {
    "estimatedHours": "24",
    "complexity": "medium"
  },
  "createdAt": "2025-04-08T14:30:00Z",
  "updatedAt": "2025-04-08T15:45:30Z"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| id | string (UUID) | Yes | Unique identifier for the work item |
| title | string | Yes | Title of the work item (max 200 characters) |
| description | string | No | Detailed description of the work item (max 5000 characters) |
| type | string (enum) | Yes | Type of work item: `BUG`, `FEATURE`, `CHORE` |
| priority | string (enum) | Yes | Priority of the work item: `LOW`, `MEDIUM`, `HIGH` |
| status | string (enum) | Yes | Current status: `FOUND`, `TRIAGED`, `IN_DEV`, `TESTING`, `DONE`, `CLOSED` |
| assignee | string | No | User ID of the assignee |
| projectId | string (UUID) | Yes | ID of the project this work item belongs to |
| releaseId | string (UUID) | No | ID of the release this work item is targeted for |
| metadata | object | No | Custom attributes for the work item |
| createdAt | string | Yes | ISO 8601 datetime when the work item was created |
| updatedAt | string | Yes | ISO 8601 datetime when the work item was last updated |

### Work Item List Response

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
        "estimatedHours": "24",
        "complexity": "medium"
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
        "estimatedHours": "16",
        "complexity": "medium"
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

**Filter Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| status | string (enum) | Filter by work item status |
| priority | string (enum) | Filter by priority |
| type | string (enum) | Filter by work item type |
| project | string | Filter by project key |
| release | string | Filter by release ID |
| assignee | string | Filter by assignee |

### Work Item Create Request

```json
{
  "title": "Implement SSO for admin portal",
  "description": "Add support for Azure AD single sign-on",
  "type": "FEATURE",
  "priority": "HIGH",
  "projectId": "INFRA",
  "releaseId": "abcdef12-3456-7890-abcd-ef1234567890",
  "assignee": "jane.doe",
  "metadata": {
    "requestedBy": "security-team",
    "estimatedHours": "12"
  }
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| title | string | Yes | Title of the work item (max 200 characters) |
| description | string | No | Detailed description of the work item (max 5000 characters) |
| type | string (enum) | Yes | Type of work item: `BUG`, `FEATURE`, `CHORE` |
| priority | string (enum) | Yes | Priority of the work item: `LOW`, `MEDIUM`, `HIGH` |
| projectId | string | Yes | Key or ID of the project this work item belongs to |
| releaseId | string (UUID) | No | ID of the release this work item is targeted for |
| assignee | string | No | User ID of the assignee |
| metadata | object | No | Custom attributes for the work item |

### Work Item Update Request

```json
{
  "title": "Implement SSO for admin portal with MFA",
  "description": "Add support for Azure AD single sign-on with multi-factor authentication",
  "type": "FEATURE",
  "priority": "HIGH",
  "assignee": "jane.doe",
  "releaseId": "abcdef12-3456-7890-abcd-ef1234567890",
  "metadata": {
    "requestedBy": "security-team",
    "estimatedHours": "16",
    "complexity": "medium"
  }
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| title | string | No | Title of the work item (max 200 characters) |
| description | string | No | Detailed description of the work item (max 5000 characters) |
| type | string (enum) | No | Type of work item: `BUG`, `FEATURE`, `CHORE` |
| priority | string (enum) | No | Priority of the work item: `LOW`, `MEDIUM`, `HIGH` |
| assignee | string | No | User ID of the assignee |
| releaseId | string (UUID) | No | ID of the release this work item is targeted for |
| metadata | object | No | Custom attributes for the work item (replaces existing metadata) |

### Work Item Transition Request

```json
{
  "targetStatus": "IN_DEV",
  "comment": "Starting implementation with Azure AD libraries"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| targetStatus | string (enum) | Yes | Status to transition to: `FOUND`, `TRIAGED`, `IN_DEV`, `TESTING`, `DONE`, `CLOSED` |
| comment | string | No | Comment explaining the transition (max 1000 characters) |

### Work Item Search Request

```json
{
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
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| project | string | No | Project key to filter by |
| filters | array | No | Array of filter criteria |
| sort | array | No | Array of sort criteria |
| page | integer | No | Page number (default: 1) |
| pageSize | integer | No | Items per page (default: 10, max: 100) |

### Work Item Bulk Create Request

```json
{
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
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| items | array | Yes | Array of work item create requests |

**Response:**

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

## Releases

### Release Object

```json
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
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| id | string (UUID) | Yes | Unique identifier for the release |
| name | string | Yes | Name of the release (max 100 characters) |
| version | string | Yes | Version number of the release (max 50 characters) |
| description | string | No | Detailed description of the release (max 1000 characters) |
| status | string (enum) | Yes | Status of the release: `PLANNED`, `IN_PROGRESS`, `RELEASED`, `CANCELLED` |
| startDate | string (date) | No | Planned start date (YYYY-MM-DD) |
| endDate | string (date) | No | Planned end date (YYYY-MM-DD) |
| projectKey | string | Yes | Key of the project this release belongs to |
| metadata | object | No | Custom attributes for the release |
| createdAt | string | Yes | ISO 8601 datetime when the release was created |
| updatedAt | string | Yes | ISO 8601 datetime when the release was last updated |

### Release List Response

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
    },
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
  ],
  "totalCount": 2,
  "page": 1,
  "pageSize": 10
}
```

**Filter Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| status | string (enum) | Filter by release status: `PLANNED`, `IN_PROGRESS`, `RELEASED`, `CANCELLED` |
| project | string | Filter by project key |

### Release Create Request

```json
{
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
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| name | string | Yes | Name of the release (max 100 characters) |
| version | string | Yes | Version number of the release (max 50 characters) |
| description | string | No | Detailed description of the release (max 1000 characters) |
| status | string (enum) | Yes | Status of the release: `PLANNED`, `IN_PROGRESS`, `RELEASED`, `CANCELLED` |
| startDate | string (date) | No | Planned start date (YYYY-MM-DD) |
| endDate | string (date) | No | Planned end date (YYYY-MM-DD) |
| projectKey | string | Yes | Key of the project this release belongs to |
| metadata | object | No | Custom attributes for the release |

### Release Update Request

```json
{
  "name": "Infrastructure Platform 1.0",
  "version": "1.0.0",
  "description": "Initial platform release with security features",
  "status": "IN_PROGRESS",
  "startDate": "2025-05-01",
  "endDate": "2025-05-30",
  "metadata": {
    "releaseManager": "john.smith",
    "riskLevel": "high",
    "changeApprovalID": "CAB-2025-042"
  }
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| name | string | No | Name of the release (max 100 characters) |
| version | string | No | Version number of the release (max 50 characters) |
| description | string | No | Detailed description of the release (max 1000 characters) |
| status | string (enum) | No | Status of the release: `PLANNED`, `IN_PROGRESS`, `RELEASED`, `CANCELLED` |
| startDate | string (date) | No | Planned start date (YYYY-MM-DD) |
| endDate | string (date) | No | Planned end date (YYYY-MM-DD) |
| metadata | object | No | Custom attributes for the release (replaces existing metadata) |

## Webhooks

### Webhook Configuration Object

```json
{
  "source": "github",
  "secret": "webhook-secret-key-123",
  "enabled": true,
  "projectKey": "INFRA",
  "securityMode": "strict",
  "allowedIPs": ["192.168.1.0/24"],
  "rateLimitPerMinute": 60,
  "metadata": {
    "repository": "org/repo",
    "branchFilter": "main,develop"
  }
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| source | string (enum) | Yes | Webhook source: `github`, `gitlab`, `bitbucket`, `custom` |
| secret | string | Yes | Secret used for webhook verification |
| enabled | boolean | No | Whether the webhook is enabled (default: true) |
| projectKey | string | Yes | Project key associated with the webhook |
| securityMode | string (enum) | No | Security validation mode: `standard`, `strict` (default: standard) |
| allowedIPs | array | No | IP addresses or CIDR ranges allowed to send webhooks |
| rateLimitPerMinute | integer | No | Rate limit for this webhook (requests per minute) |
| metadata | object | No | Additional metadata for webhook configuration |

### Webhook Event Formats

**GitHub Pull Request Event:**

```json
{
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
}
```

**Required Headers:**

| Header | Description |
|--------|-------------|
| X-GitHub-Event | Event type (e.g., `pull_request`, `push`, `issues`) |
| X-Hub-Signature-256 | HMAC-SHA256 signature for payload verification |
| X-GitHub-Delivery | Unique identifier for the webhook delivery |

**Custom Webhook:**

```json
{
  "title": "Update network security groups",
  "description": "Update NSGs to comply with new security policies",
  "type": "CHORE",
  "priority": "MEDIUM",
  "metadata": {
    "source": "security_scanner",
    "compliance": "required",
    "deadline": "2025-06-01"
  }
}
```

### Webhook Response Format

```json
{
  "success": true,
  "message": "Webhook processed successfully",
  "workItemId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "event": "pull_request"
}
```

| Field | Type | Description |
|-------|------|-------------|
| success | boolean | Whether the webhook was processed successfully |
| message | string | Human-readable message |
| workItemId | string | ID of the created or updated work item (if applicable) |
| event | string | Type of event that was processed (if applicable) |

## OAuth Integration

### OAuth Provider Object

```json
{
  "id": "github",
  "name": "GitHub",
  "description": "Connect to GitHub repositories and issues",
  "icon_url": "https://example.com/icons/github.png",
  "auth_url_template": "https://github.com/login/oauth/authorize?client_id={client_id}&redirect_uri={redirect_uri}&state={state}&scope={scopes}",
  "token_url": "https://github.com/login/oauth/access_token",
  "scopes": [
    {
      "id": "repo",
      "name": "Repository",
      "description": "Full control of private repositories"
    },
    {
      "id": "user",
      "name": "User",
      "description": "Read user information"
    }
  ],
  "configuration_fields": [
    {
      "name": "org",
      "label": "Organization",
      "required": true,
      "type": "string"
    }
  ],
  "documentation_url": "https://docs.example.com/integrations/github"
}
```

### OAuth Token Object

```json
{
  "access_token": "gho_16C7e42F292c6912E7710c838347Ae178B4a",
  "token_type": "Bearer",
  "refresh_token": "ghr_1B4a2e67897e6B4a2e12866e1B4a2e67897e6B4a2e12866e1B4a2e67897e6B4a2e12866e",
  "expiry": "2025-05-08T16:10:45Z",
  "scopes": ["repo", "user"],
  "provider": "github",
  "user_id": "john.smith",
  "project_id": "INFRA",
  "created_at": "2025-04-08T16:10:45Z",
  "updated_at": "2025-04-08T16:10:45Z",
  "metadata": {
    "organization": "example-org",
    "installation_id": "12345678"
  }
}
```

### OAuth Authorization Response

```json
{
  "authorization_url": "https://github.com/login/oauth/authorize?client_id=123456&redirect_uri=http://localhost:8080/api/v1/oauth/callback&state=abcdef123456",
  "state": "abcdef123456"
}
```

## Error Responses

### Error Object

```json
{
  "code": 404,
  "message": "Work item not found",
  "details": ["Item with id 'non-existent-id' does not exist"],
  "request_id": "req-123456",
  "timestamp": "2025-04-08T16:10:45Z"
}
```

| Field | Type | Description |
|-------|------|-------------|
| code | integer | HTTP status code |
| message | string | Human-readable error message |
| details | array | Detailed error information |
| request_id | string | Unique request identifier for tracing |
| timestamp | string | When the error occurred (ISO 8601 format) |

### Validation Error

```json
{
  "code": 400,
  "message": "Invalid request",
  "details": [
    "Field 'title' is required",
    "Field 'priority' must be one of: LOW, MEDIUM, HIGH"
  ],
  "request_id": "req-123456",
  "timestamp": "2025-04-08T16:10:45Z"
}
```

### Authentication Error

```json
{
  "code": 401,
  "message": "Unauthorized",
  "details": ["Invalid or expired API token"],
  "request_id": "req-123456",
  "timestamp": "2025-04-08T16:10:45Z"
}
```

### Rate Limit Error

```json
{
  "code": 429,
  "message": "Rate limit exceeded",
  "details": ["Maximum request rate exceeded. Please retry after 60 seconds."],
  "request_id": "req-123456",
  "timestamp": "2025-04-08T16:10:45Z",
  "rate_limit": {
    "limit": 60,
    "remaining": 0,
    "reset": 1722880245,
    "retry_after": 60
  }
}
```

## Advanced Features

### Filtering Format

```json
{
  "field": "status",
  "operator": "in",
  "value": ["IN_DEV", "TESTING"]
}
```

**Available Operators:**

| Operator | Description | Example |
|----------|-------------|---------|
| eq | Equal to | `{"field": "priority", "operator": "eq", "value": "HIGH"}` |
| ne | Not equal to | `{"field": "priority", "operator": "ne", "value": "LOW"}` |
| gt | Greater than | `{"field": "metadata.estimatedHours", "operator": "gt", "value": "8"}` |
| gte | Greater than or equal to | `{"field": "metadata.estimatedHours", "operator": "gte", "value": "8"}` |
| lt | Less than | `{"field": "metadata.estimatedHours", "operator": "lt", "value": "24"}` |
| lte | Less than or equal to | `{"field": "metadata.estimatedHours", "operator": "lte", "value": "24"}` |
| in | In array | `{"field": "status", "operator": "in", "value": ["IN_DEV", "TESTING"]}` |
| nin | Not in array | `{"field": "status", "operator": "nin", "value": ["DONE", "CLOSED"]}` |
| contains | String contains | `{"field": "title", "operator": "contains", "value": "API"}` |
| startsWith | String starts with | `{"field": "title", "operator": "startsWith", "value": "Implement"}` |
| endsWith | String ends with | `{"field": "title", "operator": "endsWith", "value": "feature"}` |

### Sorting Format

```json
{
  "field": "priority",
  "direction": "desc"
}
```

**Directions:**

- `asc`: Ascending order (A-Z, 0-9)
- `desc`: Descending order (Z-A, 9-0)

### Searching Format

```json
{
  "query": "security authentication",
  "fields": ["title", "description"],
  "fuzzy": true
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| query | string | Yes | Search query |
| fields | array | No | Fields to search (default: all text fields) |
| fuzzy | boolean | No | Whether to perform fuzzy matching (default: false) |
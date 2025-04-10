# Rinna API Examples

This guide provides detailed examples for using the Rinna API effectively. Each endpoint is demonstrated with complete request/response examples in multiple formats.

## Table of Contents

- [Authentication](#authentication)
- [Projects](#projects)
- [Work Items](#work-items)
- [Releases](#releases)
- [Security Features](#security-features)
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

## Security Features

### OAuth Authorization

```bash
# Request an OAuth authorization URL for GitHub
curl -X GET "https://your-rinna-instance/api/v1/oauth/authorize/github?project_id=INFRA&user_id=john.smith" \
  -H "Authorization: Bearer ri-dev-f72a159e4bdc"
```

Response:

```json
{
  "authorization_url": "https://github.com/login/oauth/authorize?client_id=abc123&state=def456&scope=repo,user&redirect_uri=https://your-rinna-instance/api/v1/oauth/callback",
  "state": "def456"
}
```

### OAuth Callback Handling

```bash
# OAuth callback from GitHub
curl -X GET "https://your-rinna-instance/api/v1/oauth/callback?code=abc123&state=def456"
```

Response:

```json
{
  "access_token": "gho_abc123def456",
  "token_type": "Bearer",
  "refresh_token": "ghr_789xyz",
  "expiry": "2025-05-10T14:30:00Z",
  "scopes": ["repo", "user"],
  "provider": "github",
  "user_id": "john.smith",
  "project_id": "INFRA",
  "created_at": "2025-04-10T14:30:00Z",
  "updated_at": "2025-04-10T14:30:00Z"
}
```

### Rate Limit Information

```bash
# Get rate limit information
curl -X GET "https://your-rinna-instance/api/v1/rate-limits" \
  -H "Authorization: Bearer ri-dev-f72a159e4bdc"
```

Response:

```json
{
  "limit": 300,
  "remaining": 297,
  "reset": 1692203400,
  "endpoint_limits": {
    "/api/v1/projects": {
      "limit": 300,
      "remaining": 297
    },
    "/api/v1/workitems": {
      "limit": 300,
      "remaining": 299
    }
  }
}
```

### Webhook Configuration

```bash
# Configure a GitHub webhook
curl -X POST "https://your-rinna-instance/api/v1/webhooks/configure/github" \
  -H "Authorization: Bearer ri-dev-f72a159e4bdc" \
  -H "Content-Type: application/json" \
  -d '{
    "project_key": "INFRA",
    "secret": "your-webhook-secret",
    "enabled": true,
    "security_mode": "strict",
    "rate_limit": 60
  }'
```

Response:

```json
{
  "source": "github",
  "project_key": "INFRA",
  "secret": "your-webhook-secret",
  "enabled": true,
  "security_mode": "strict",
  "rate_limit": 60,
  "metadata": {
    "created_at": "2025-04-10T16:00:00Z"
  }
}
```

### Security Context

```bash
# Get security context information
curl -X GET "https://your-rinna-instance/api/v1/security/context" \
  -H "Authorization: Bearer ri-dev-f72a159e4bdc" \
  -H "X-Request-ID": "custom-request-id-12345"
```

Response:

```json
{
  "request_id": "custom-request-id-12345",
  "ip": "192.168.1.100",
  "user_agent": "curl/7.68.0",
  "method": "GET",
  "path": "/api/v1/security/context",
  "user_id": "john.smith",
  "project_id": "INFRA",
  "token_type": "API_KEY",
  "duration": "11ms"
}
```

## Webhooks

### GitHub Webhook

```bash
# GitHub webhook payload (sent by GitHub to your webhook URL)
curl -X POST "https://your-rinna-instance/api/v1/webhooks/github?project=INFRA" \
  -H "Content-Type: application/json" \
  -H "X-GitHub-Event: pull_request" \
  -H "X-GitHub-Delivery": "72d3162e-cc78-11e3-81ab-4c9367dc0958" \
  -H "X-Hub-Signature-256: sha256=hash-of-payload-using-webhook-secret" \
  -d '{
    "action": "opened",
    "pull_request": {
      "title": "Implement SSO feature",
      "body": "References: INFRA-42",
      "user": {
        "login": "jane.doe"
      },
      "html_url": "https://github.com/org/repo/pull/123"
    },
    "repository": {
      "full_name": "org/repo"
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

## Error Handling

The API uses standard HTTP status codes and returns error details in a consistent format:

### Not Found Example (404)

```json
{
  "code": 404,
  "message": "Work item not found",
  "details": ["Item with id 'non-existent-id' does not exist"],
  "request_id": "custom-request-id-12345",
  "timestamp": "2025-04-10T16:30:00Z"
}
```

### Validation Error Example (400)

```json
{
  "code": 400,
  "message": "Invalid request",
  "details": ["Field 'title' is required", "Field 'projectId' is required"],
  "request_id": "custom-request-id-67890",
  "timestamp": "2025-04-10T16:35:00Z"
}
```

### Rate Limit Exceeded Example (429)

```json
{
  "code": 429,
  "message": "Rate limit exceeded",
  "details": ["Request limit reached. Please try again later."],
  "request_id": "custom-request-id-abcde",
  "timestamp": "2025-04-10T16:40:00Z"
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
      }
    ]
  }'
```

### Client SDK Usage (Go)

```go
// Initialize the client
client := rinna.NewClient("https://your-rinna-instance/api/v1", "ri-dev-f72a159e4bdc")

// Create a work item
workItem, err := client.CreateWorkItem(context.Background(), rinna.WorkItemCreateRequest{
    Title:       "Update Docker containers",
    Description: "Security update for all Docker containers",
    Type:        rinna.WorkItemTypeChore,
    Priority:    rinna.PriorityHigh,
    ProjectID:   "INFRA",
})

// Handle rate limiting with exponential backoff
if err != nil {
    if apiErr, ok := err.(*rinna.APIError); ok && apiErr.StatusCode == 429 {
        retryAfter, _ := strconv.Atoi(apiErr.Headers.Get("Retry-After"))
        time.Sleep(time.Duration(retryAfter) * time.Second)
        // Retry the request
    }
}
```

## Additional Resources

For detailed examples of specific security features, see:

- [Security Integration Examples](./examples/security-examples.md)
- [OAuth Integration Examples](./examples/oauth-examples.md)
- [Rate Limiting Examples](./examples/rate-limiting-examples.md)
- [Webhook Security Examples](./examples/webhook-security-examples.md)
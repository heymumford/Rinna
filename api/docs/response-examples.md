# API Response Examples

This document provides detailed response examples for all Rinna API endpoints. These examples can be used for integration testing, client library development, and understanding the expected response format.

## Table of Contents

- [Health Endpoints](#health-endpoints)
- [Projects Endpoints](#projects-endpoints)
- [Work Items Endpoints](#work-items-endpoints)
- [Releases Endpoints](#releases-endpoints)
- [Webhooks Endpoints](#webhooks-endpoints)
- [Error Responses](#error-responses)

## Health Endpoints

### GET /health

Response:

```json
{
  "status": "OK",
  "version": "1.3.2",
  "timestamp": "2025-04-08T12:34:56Z",
  "components": [
    {
      "name": "database",
      "status": "UP",
      "message": "Connected",
      "details": {
        "latency": "3ms",
        "connections": "5"
      }
    },
    {
      "name": "javaService",
      "status": "UP",
      "message": "Running",
      "details": {
        "memoryUsage": "512MB",
        "uptime": "3d 12h"
      }
    },
    {
      "name": "cacheService",
      "status": "DEGRADED",
      "message": "High load",
      "details": {
        "hitRate": "68%",
        "evictions": "125"
      }
    }
  ]
}
```

### GET /health/live

Response:

```json
{
  "status": "UP"
}
```

### GET /health/ready

Response:

```json
{
  "status": "READY"
}
```

## Projects Endpoints

### GET /projects

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

### POST /projects

Request:

```json
{
  "key": "INFRA",
  "name": "Infrastructure Team",
  "description": "Cloud infrastructure management",
  "metadata": {
    "department": "IT",
    "costCenter": "CC-123"
  }
}
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

### GET /projects/{key}

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

### PUT /projects/{key}

Request:

```json
{
  "name": "Infrastructure & Cloud Team",
  "description": "Cloud infrastructure and platform management",
  "metadata": {
    "department": "IT",
    "costCenter": "CC-456"
  }
}
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

### GET /projects/{key}/workitems

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
        "estimatedHours": "24",
        "complexity": "medium"
      },
      "createdAt": "2025-04-08T14:30:00Z",
      "updatedAt": "2025-04-08T15:45:30Z"
    },
    {
      "id": "8e7d6c5b-4a3b-2c1d-0e9f-8a7b6c5d4e3f",
      "title": "Configure VPC networking",
      "description": "Set up VPC, subnets, and security groups",
      "type": "FEATURE",
      "priority": "HIGH",
      "status": "FOUND",
      "assignee": null,
      "projectId": "7d8f3ab4-c2e5-47d6-9a1b-8fc7e41fea23",
      "metadata": {
        "estimatedHours": "16",
        "complexity": "high"
      },
      "createdAt": "2025-04-08T14:35:00Z",
      "updatedAt": "2025-04-08T14:35:00Z"
    }
  ],
  "totalCount": 2,
  "page": 1,
  "pageSize": 10
}
```

## Work Items Endpoints

### GET /workitems

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

### POST /workitems

Request:

```json
{
  "title": "Implement SSO for admin portal",
  "description": "Add support for Azure AD single sign-on",
  "type": "FEATURE",
  "priority": "HIGH",
  "projectId": "INFRA",
  "metadata": {
    "requestedBy": "security-team",
    "estimatedHours": "12"
  }
}
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

### GET /workitems/{id}

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

### PUT /workitems/{id}

Request:

```json
{
  "title": "Implement SSO for admin portal with MFA",
  "assignee": "jane.doe",
  "metadata": {
    "requestedBy": "security-team",
    "estimatedHours": "16",
    "complexity": "medium"
  }
}
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

### POST /workitems/{id}/transitions

Request:

```json
{
  "targetStatus": "IN_DEV",
  "comment": "Starting implementation with Azure AD libraries"
}
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

## Releases Endpoints

### GET /releases

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

### POST /releases

Request:

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

### GET /releases/{id}

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

### PUT /releases/{id}

Request:

```json
{
  "status": "IN_PROGRESS",
  "endDate": "2025-05-30",
  "metadata": {
    "releaseManager": "john.smith",
    "riskLevel": "high",
    "changeApprovalID": "CAB-2025-042"
  }
}
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

### GET /releases/{id}/workitems

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

## Webhooks Endpoints

### POST /webhooks/github

Request:

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

Response:

```json
{
  "success": true,
  "message": "Webhook processed successfully",
  "workItemId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "event": "pull_request"
}
```

### POST /webhooks/custom

Request:

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

Response:

```json
{
  "success": true,
  "message": "Work item created successfully",
  "workItemId": "fedcba98-7654-3210-fedc-ba9876543210"
}
```

## Error Responses

### Not Found (404)

```json
{
  "code": 404,
  "message": "Work item not found",
  "details": ["Item with id 'non-existent-id' does not exist"]
}
```

### Bad Request (400)

```json
{
  "code": 400,
  "message": "Invalid request",
  "details": ["Field 'title' is required"]
}
```

### Unauthorized (401)

```json
{
  "code": 401,
  "message": "Unauthorized",
  "details": ["Invalid or expired API token"]
}
```

### Forbidden (403)

```json
{
  "code": 403,
  "message": "Forbidden",
  "details": ["Insufficient permissions to access this resource"]
}
```

### Unprocessable Entity (422)

```json
{
  "code": 422,
  "message": "Invalid transition",
  "details": ["Cannot transition from 'IN_DEV' to 'DONE'. Valid transitions are: 'TESTING'"]
}
```

### Internal Server Error (500)

```json
{
  "code": 500,
  "message": "Internal server error",
  "details": ["An unexpected error occurred while processing your request"]
}
```
# Rinna API Security Guide

This guide provides comprehensive information on implementing secure API interactions with the Rinna API.

## Authentication

### Token-Based Authentication

The Rinna API uses a robust token-based authentication system with the following features:

- **Encrypted Tokens**: All tokens are encrypted using AES-GCM with a configurable encryption key
- **Token Claims**: Tokens contain metadata like project ID, expiration date, scope, and more
- **Token Rotation**: Automatic warning and rotation before tokens expire
- **Secure Storage**: Tokens are stored securely with proper file permissions
- **Format Versioning**: Token format versioning for future enhancements
- **Token Revocation**: Support for immediate token revocation

#### Token Format

```
ri-<type>-<base64(encrypted(claims))>
```

Where:
- `ri-` is the token prefix
- `<type>` indicates the token type (dev, test, prod)
- The claims section contains encrypted metadata:
  - Token ID (unique identifier)
  - Project ID (associated project)
  - Issuance and expiration timestamps
  - Format version
  - Scope (permissions)
  - User ID (if applicable)

#### Best Practices

1. **Secure Storage**: Never hardcode tokens in your application code. Store them securely in environment variables or a secrets manager.
2. **Limited Scope**: Always request the minimum scope required for your operation.
3. **Token Rotation**: Implement token rotation in your application. Never use tokens indefinitely.
4. **Monitoring**: Monitor token usage and implement alerting for suspicious activity.
5. **Revocation**: Implement token revocation for security incidents or when tokens are no longer needed.

### OAuth Integration

For third-party service integration, Rinna supports OAuth 2.0 with the following providers:

- GitHub
- GitLab
- Azure DevOps
- Jira
- Bitbucket

#### OAuth Flow Implementation

1. **Authorization**: Redirect your user to the Rinna OAuth authorization endpoint
   ```
   GET /api/v1/oauth/authorize/{provider}
   ```

2. **Callback Handling**: Implement a callback endpoint to receive the authorization code
   ```
   GET /api/v1/oauth/callback?code={code}&state={state}
   ```

3. **Token Management**: Store the received OAuth tokens securely and refresh them when needed

#### Security Considerations

- Always validate the `state` parameter in the callback to prevent CSRF attacks
- Store OAuth tokens securely (encrypted at rest)
- Refresh tokens when they expire
- Implement proper revocation when tokens are no longer needed

## Rate Limiting

The Rinna API implements rate limiting to ensure fair usage and system stability.

### Rate Limit Headers

All responses include the following rate limit headers:

- `X-RateLimit-Limit`: Maximum number of requests allowed in the current window
- `X-RateLimit-Remaining`: Number of requests remaining in the current window
- `X-RateLimit-Reset`: Unix timestamp when the rate limit resets

### Handling Rate Limiting

When a request exceeds the rate limit, the server responds with a `429 Too Many Requests` status code and includes a `Retry-After` header indicating the number of seconds to wait before retrying.

#### Best Practices

1. **Exponential Backoff**: Implement exponential backoff for retries
2. **Respect Headers**: Always respect the `Retry-After` header
3. **Monitor Usage**: Keep track of your rate limit usage to avoid hitting limits
4. **Request Batching**: Batch requests when possible to reduce API calls
5. **Caching**: Implement client-side caching to avoid unnecessary requests

Example implementation:

```javascript
async function fetchWithRateLimitHandling(url, token) {
  try {
    const response = await fetch(url, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    
    if (response.status === 429) {
      const retryAfter = parseInt(response.headers.get('Retry-After')) || 60;
      const retryMs = retryAfter * 1000;
      
      console.log(`Rate limited. Retrying after ${retryAfter} seconds`);
      
      // Wait and retry with exponential backoff
      await new Promise(resolve => setTimeout(resolve, retryMs));
      return fetchWithRateLimitHandling(url, token);
    }
    
    return await response.json();
  } catch (error) {
    console.error('Error fetching data:', error);
    throw error;
  }
}
```

## Webhook Security

### Webhook Signature Verification

The Rinna API uses different signature methods depending on the webhook source:

- **GitHub**: HMAC-SHA256 signature using `X-Hub-Signature-256` header
- **GitLab**: Token comparison via `X-Gitlab-Token` header
- **Bitbucket**: Signature verification via `X-Hub-Signature` header
- **Custom webhooks**: Configurable signature methods

### Implementing Secure Webhook Receivers

#### GitHub Webhook Verification

```javascript
const crypto = require('crypto');

function verifyGitHubWebhook(payload, signature, secret) {
  if (!payload || !signature || !secret) {
    return false;
  }
  
  const hmac = crypto.createHmac('sha256', secret);
  const digest = 'sha256=' + hmac.update(payload).digest('hex');
  
  return crypto.timingSafeEqual(
    Buffer.from(signature),
    Buffer.from(digest)
  );
}
```

#### GitLab Webhook Verification

```javascript
function verifyGitLabWebhook(token, secret) {
  if (!token || !secret) {
    return false;
  }
  
  return crypto.timingSafeEqual(
    Buffer.from(token),
    Buffer.from(secret)
  );
}
```

#### Bitbucket Webhook Verification

```javascript
function verifyBitbucketWebhook(payload, signature, secret) {
  if (!payload || !signature || !secret) {
    return false;
  }
  
  const hmac = crypto.createHmac('sha1', secret);
  const digest = hmac.update(payload).digest('hex');
  
  return crypto.timingSafeEqual(
    Buffer.from(signature),
    Buffer.from(digest)
  );
}
```

### Advanced Security Features

The Rinna webhook system includes several advanced security features:

1. **Replay Protection**: Nonce tracking prevents webhook replay attacks
2. **Rate Limiting**: Configurable per-source, per-project rate limits
3. **IP Whitelisting**: Allow trusted IPs to bypass certain security checks
4. **Secret Rotation**: Support for rotating webhook secrets with grace periods
5. **Timestamp Validation**: Rejects requests with outdated timestamps
6. **Security Logging**: Comprehensive logging of all security-related events

## General Security Best Practices

### TLS/HTTPS

Always use HTTPS for all API interactions. Never send tokens or sensitive data over unencrypted connections.

### Input Validation

Always validate and sanitize input before processing. Never trust client-provided data.

```javascript
function validateWorkItem(workItem) {
  const errors = [];
  
  if (!workItem.title || workItem.title.length > 200) {
    errors.push('Title is required and must be less than 200 characters');
  }
  
  if (!['BUG', 'FEATURE', 'CHORE'].includes(workItem.type)) {
    errors.push('Type must be one of: BUG, FEATURE, CHORE');
  }
  
  if (!['LOW', 'MEDIUM', 'HIGH'].includes(workItem.priority)) {
    errors.push('Priority must be one of: LOW, MEDIUM, HIGH');
  }
  
  if (!workItem.projectKey) {
    errors.push('Project key is required');
  }
  
  return errors;
}
```

### Error Handling

Implement proper error handling that doesn't expose sensitive information.

```javascript
async function handleApiError(response) {
  try {
    const errorData = await response.json();
    
    // Log detailed error for debugging
    console.error('API error:', {
      status: response.status,
      message: errorData.message,
      details: errorData.details,
      requestId: errorData.request_id
    });
    
    // Return user-friendly error
    return {
      error: true,
      message: errorData.message || 'An unexpected error occurred'
    };
  } catch (e) {
    // Handle case where response is not valid JSON
    return {
      error: true,
      message: `Request failed with status ${response.status}`
    };
  }
}
```

### Request Tracing

Include the `X-Request-ID` header in all requests to help with debugging and tracking.

```javascript
async function apiRequest(url, options = {}) {
  const requestId = generateRequestId();
  
  const response = await fetch(url, {
    ...options,
    headers: {
      ...options.headers,
      'X-Request-ID': requestId
    }
  });
  
  // Store the request ID for logging
  console.log(`API request ${requestId} completed with status ${response.status}`);
  
  return response;
}

function generateRequestId() {
  return 'req-' + Math.random().toString(36).substring(2, 15);
}
```

### Content Security Policy

When integrating Rinna API with a web application, implement a strong Content Security Policy to prevent XSS attacks.

```http
Content-Security-Policy: default-src 'self'; script-src 'self'; connect-src 'self' localhost:8080;
```

## Security Checklist

Before deploying your Rinna API integration to production, verify the following:

- [ ] All API requests use HTTPS
- [ ] Authentication tokens are stored securely
- [ ] Token rotation is implemented
- [ ] Rate limit handling is implemented
- [ ] Webhook signatures are verified
- [ ] Input validation is implemented
- [ ] Error handling doesn't expose sensitive information
- [ ] Request tracing is implemented
- [ ] Content Security Policy is configured (for web applications)
- [ ] Security logging and monitoring is implemented
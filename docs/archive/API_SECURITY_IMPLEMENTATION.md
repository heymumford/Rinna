# API Security Implementation in Rinna

## Overview

This document describes the comprehensive security features implemented for the Rinna API. The security implementation includes token management, rate limiting, comprehensive security logging, and multiple layers of protection for the API endpoints.

## Key Components

### 1. Rate Limiting and Throttling

We have implemented a flexible rate-limiting system for the API to protect against abuse, DoS attacks, and to ensure fair resource allocation:

#### Features
- **Time-based rate limits**: Configurable limits per minute with separate tracking for different endpoints
- **IP-based rate limiting**: Different limits can be applied to specific IP addresses
- **Project-based rate limiting**: Different limits can be applied to specific projects
- **Path-based rate limiting**: Different limits can be applied to specific API paths
- **Burst allowance**: Configurable burst allowance for handling temporary spikes
- **IP whitelisting**: Certain trusted IPs or CIDR ranges can be exempted from rate limits
- **Transparent headers**: Standard rate limit headers indicate limits, usage, and reset time
- **Penalty behavior**: When a rate limit is exceeded, a configurable penalty time is applied

#### Implementation
- Rate limiting implemented in middleware for consistent application across all endpoints
- In-memory token bucket implementation with periodic cleanup to prevent memory leaks
- HTTP headers added to every response to indicate limit status:
  - `X-RateLimit-Limit`: Maximum allowed requests per time window
  - `X-RateLimit-Remaining`: Remaining requests in the current time window
  - `X-RateLimit-Reset`: Time when the rate limit window resets (Unix timestamp)
  - `Retry-After`: Seconds to wait before trying again (when rate limited)

### 2. Comprehensive Security Logging

We've implemented detailed security logging for the API to help with monitoring, auditing, and detecting potential security incidents:

#### Features
- **Request ID tracking**: Every request gets a unique identifier for tracing through the system
- **Sensitive path detection**: Certain paths (auth, oauth, token) are automatically marked for higher security scrutiny
- **Parameter redaction**: Sensitive data like tokens, passwords, and credentials are automatically redacted in logs
- **Body hashing**: Request body is hashed for traceability without storing sensitive content
- **Differentiated log levels**: Log level is adjusted based on the type of request and response status
- **Structured logging**: All logs are structured for easier parsing and analysis
- **Security context**: A security context is attached to each request with relevant security information

#### Implementation
- Security logging middleware captures details at the start and end of each request
- Log format includes client IP, authentication info, request path, status code, duration
- For sensitive operations, extra details are logged including token type and project ID
- Error responses are logged with more detail for troubleshooting security issues
- Security logger integrates with existing logging infrastructure but uses a dedicated prefix

### 3. Secure Token Management

The API implements a robust token management system for API clients:

#### Features
- **Secure token generation**: Tokens are securely generated with appropriate entropy
- **Token encryption**: Tokens include encrypted claims for secure information storage
- **Token expiration**: All tokens have an expiration date to limit the security impact
- **Token scope**: Tokens include scope restrictions to limit the operations they can perform
- **Token validation**: Tokens are validated on each request with comprehensive checks
- **Token storage**: Secure on-disk storage with encryption for persisting tokens

#### Implementation
- Tokens use AES-GCM encryption for confidentiality, integrity and authenticity protection
- Token validation middleware applies to protected endpoints
- Encrypted token storage class handles persistent storage with appropriate permissions
- Token format includes version, project ID, user ID, scope, and other metadata
- Cache system reduces validation overhead while maintaining security

### 4. Webhook Security

Webhooks are secured with multiple mechanisms:

#### Features
- **HMAC signature verification**: All webhook payloads must be signed
- **Provider-specific security**: Each provider (GitHub, GitLab, etc.) has tailored security
- **Replay protection**: Nonce tracking prevents replay attacks
- **Timestamp validation**: Old webhooks are rejected to prevent replays
- **Rate limiting**: Special rate limits for webhook endpoints

#### Implementation
- Webhook authentication middleware verifies signatures before processing
- Webhook source is added to security context for auditing and tracing
- Different signature algorithms are supported based on the webhook source

### 5. OAuth Integration

OAuth 2.0 integration for third-party services:

#### Features
- **Multiple provider support**: GitHub, GitLab, Jira, Azure DevOps, and Bitbucket
- **Secure token storage**: OAuth tokens stored with encryption
- **Token refresh**: Automatic refresh of expired tokens
- **State parameter**: Protection against CSRF attacks

#### Implementation
- OAuth manager handles the OAuth flow for each provider
- Token storage securely persists tokens with appropriate encryption
- OAuth endpoints implement the standard OAuth 2.0 flow

## Configuration

### Rate Limiting Configuration

```yaml
rate_limit:
  enabled: true
  default_limit: 300      # Default: 300 requests per minute 
  burst_limit: 50         # Allow 50 burst requests
  default_penalty_time: 60  # 60 seconds penalty when rate limit is exceeded
  
  # IP addresses/CIDR ranges that are exempt from rate limiting
  ip_whitelist:
    - "127.0.0.1"         # Local development
    - "192.168.0.0/16"    # Internal network
    
  # Custom limits for specific IPs, projects, and API paths
  custom_ip_limits:
    "203.0.113.1": 100    # Limit this IP to 100 requests per minute
  
  custom_project_limits:
    "admin-project": 600  # Admin project gets higher limits
    
  custom_path_limits:
    "/api/v1/auth/*": 60        # Auth endpoints: 60 requests per minute
    "/api/v1/webhooks/*": 600   # Webhooks: 600 requests per minute
```

### Security Logging Configuration

```yaml
logging:
  level: "info"
  format: "json"
  file_enabled: true
  file_path: "/var/log/rinna/api.log"
  rotation: true
  max_size: 100  # 100 MB
  max_age: 30    # 30 days
  max_backups: 10
  security_logging: true
  redact_paths:
    - "/api/v1/auth/"
    - "/api/v1/oauth/"
    - "/api/v1/token/"
  custom_fields:
    service: "rinna-api"
    component: "api-server"
```

## Security Best Practices

The API implementation follows these security best practices:

1. **Defense in Depth**: Multiple security layers protect the API
2. **Least Privilege**: Tokens and permissions follow the principle of least privilege
3. **Secure by Default**: Security features are enabled by default with safe configurations
4. **Fail Secure**: When security checks fail, the system errs on the side of caution
5. **Rate Limiting**: Protects against brute force and DoS attacks
6. **Comprehensive Logging**: Security events are logged for auditing and troubleshooting
7. **Input Validation**: All inputs are validated before processing
8. **CORS Protection**: Cross-Origin Resource Sharing is configured securely
9. **Error Handling**: Errors are handled gracefully without exposing sensitive information
10. **Secrets Management**: API tokens, OAuth tokens, and other secrets are stored securely

## Testing

The security features are extensively tested:

1. **Unit Tests**: Each security component has unit tests
2. **Integration Tests**: Tests verify the interaction between security components
3. **End-to-End Tests**: Scripts verify the security features from a client perspective

## Future Enhancements

Potential future security enhancements:

1. **JWT Support**: Add JWT token support with signature verification
2. **API Key Rotation**: Implement automatic key rotation for long-lived API keys
3. **IP Geolocation Restrictions**: Add geographic restrictions for API access
4. **Anomaly Detection**: Implement anomaly detection for security monitoring
5. **Dynamic Rate Limiting**: Adjust rate limits based on server load and traffic patterns
6. **API Security Scanning**: Integrate with security scanning tools for continuous validation
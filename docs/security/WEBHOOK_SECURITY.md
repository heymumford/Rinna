# Webhook Security in Rinna

This document describes the security mechanisms implemented for webhooks in the Rinna system.

## Overview

Webhooks provide a way for external systems to notify Rinna about events, allowing real-time processing of information from various sources. The Rinna webhook system currently supports the following providers:

- GitHub
- GitLab
- Bitbucket
- Custom webhooks

Because webhooks accept HTTP requests from external sources, they require robust security measures to prevent unauthorized access, replay attacks, and other potential security issues.

## Security Features

### 1. Signature Verification

All webhook requests are verified using cryptographic signatures:

| Provider   | Signature Method | Header                | Verification Method           |
|------------|------------------|----------------------|-------------------------------|
| GitHub     | HMAC-SHA256      | X-Hub-Signature-256  | Compare SHA256 HMAC           |
| GitLab     | Token            | X-Gitlab-Token       | Direct token comparison       |
| Bitbucket  | HMAC-SHA1        | X-Hub-Signature      | Compare SHA1 HMAC             |
| Custom     | HMAC-SHA256*     | X-Webhook-Signature  | Algorithm specified in header |

*\* Custom webhooks can specify the algorithm using the `X-Webhook-Algorithm` header.*

The signature verification ensures that:
- The request comes from the expected source
- The request payload has not been tampered with
- The request is from an authorized sender

### 2. Rate Limiting

To prevent denial-of-service attacks, webhooks implement rate limiting based on:

- Client IP address
- Project ID
- Webhook provider

Rate limits can be configured per environment and include:
- Configurable limits per minute/hour
- IP whitelist for trusted sources
- Rate limit headers in responses

When rate limits are exceeded, the system returns a 429 status code with a Retry-After header.

### 3. Replay Protection

To prevent replay attacks, webhooks implement two replay protection mechanisms:

#### 3.1. Nonce Tracking

Each webhook request contains a unique identifier (nonce):

| Provider   | Nonce Header          |
|------------|----------------------|
| GitHub     | X-GitHub-Delivery    |
| GitLab     | X-Gitlab-Event-UUID  |
| Bitbucket  | X-Request-UUID       |
| Custom     | X-Webhook-Nonce      |

The system tracks nonces to ensure each webhook is processed only once.

#### 3.2. Timestamp Validation

When available, the system validates request timestamps to prevent replay of old requests:

| Provider   | Timestamp Header          | Format             |
|------------|---------------------------|-------------------|
| GitHub     | N/A (uses Delivery ID)    | N/A               |
| GitLab     | X-Gitlab-Event-Timestamp  | Unix timestamp    |
| Bitbucket  | X-Event-Time              | RFC3339           |
| Custom     | X-Webhook-Timestamp       | Unix timestamp    |

Requests with timestamps older than the configured tolerance (default: 15 minutes) are rejected.

### 4. Secret Management

Webhook secrets are securely managed:

- Secrets are stored in the Java backend service
- Secrets can be rotated with configurable grace periods
- Secrets support version tracking to allow smooth rotation
- Secrets are cached with configurable expiration to reduce backend calls
- Development mode uses different secrets than production

### 5. Security Levels

The system supports multiple security levels that can be configured per webhook:

- **Strict**: Enforces all security measures (signatures, timestamps, nonces)
- **Standard**: Enforces signatures but relaxes some timestamp requirements
- **Relaxed**: Only enforces basic signature validation (for compatibility)

## Implementation Details

### Security Middleware

The webhook security is implemented as HTTP middleware that:

1. Extracts and validates the webhook signature
2. Applies rate limiting based on source and project
3. Verifies nonces to prevent replay attacks
4. Logs all security-related events
5. Provides detailed error messages for debugging

### Configuration

Security settings can be configured in the central configuration:

```yaml
auth:
  webhookSecretExpiry: 1440  # Cache expiry in minutes (24 hours)
  securityLevel: "standard"  # Default security level
  allowedSources:
    - "github"
    - "gitlab"
    - "bitbucket"
    - "custom"
  
security:
  webhook_token_expiration_days: 365
  enable_cors: true
  ipWhitelist:
    - "192.168.1.100"
    - "10.0.0.0/24"
```

## Webhook Registration Process

1. Create a webhook configuration in Rinna
2. Rinna generates a secure secret
3. Configure the webhook in the external system with the provided secret
4. The external system starts sending events to Rinna
5. Rinna validates the events using the registered secret

## Secret Rotation Process

1. Request a secret rotation for a webhook
2. Rinna generates a new secret while keeping the old one active
3. Update the external system with the new secret
4. During the grace period, both old and new secrets are valid
5. After the grace period, only the new secret is valid

## Best Practices

1. **Use HTTPS**: Always use HTTPS for webhook endpoints
2. **Rotate Secrets Regularly**: Implement a rotation schedule for webhook secrets
3. **Monitor Failures**: Set up alerts for repeated webhook authentication failures
4. **Rate Limit Endpoints**: Configure appropriate rate limits for each webhook source
5. **Implement Retries**: Configure webhook senders to retry on temporary failures
6. **IP Restrictions**: When possible, restrict webhook sources to known IP ranges

## Example Webhook Test

To test webhook signature verification:

```bash
# Generate signature for GitHub webhook
SECRET="your-webhook-secret"
PAYLOAD='{"event":"test"}'
SIGNATURE=$(echo -n "$PAYLOAD" | openssl dgst -sha256 -hmac "$SECRET" | cut -d' ' -f2)

# Send test webhook
curl -X POST "https://api.example.com/api/v1/webhooks/github?project=test-project" \
  -H "Content-Type: application/json" \
  -H "X-GitHub-Event: ping" \
  -H "X-GitHub-Delivery: $(uuidgen)" \
  -H "X-Hub-Signature-256: sha256=$SIGNATURE" \
  -d "$PAYLOAD"
```

## Troubleshooting

If you encounter webhook signature verification failures:

1. Verify the correct secret is configured in both systems
2. Ensure no whitespace or encoding issues in the secret
3. Check that the correct signature algorithm is being used
4. Verify the webhook headers match the expected format
5. Check the system time synchronization
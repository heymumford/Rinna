# OAuth Integration Guide

Rinna provides robust OAuth 2.0 integration capabilities for connecting to third-party services like GitHub, GitLab, Jira, Azure DevOps, and Bitbucket. This guide explains how to configure and use these integrations.

## Configuration

OAuth providers are configured in the `oauth` section of your configuration file:

```yaml
oauth:
  token_encryption_key: "your-secure-encryption-key"  # Used to encrypt stored tokens
  
  # GitHub configuration
  github:
    enabled: true
    client_id: "your-github-client-id"
    client_secret: "your-github-client-secret"
    redirect_url: "http://your-app/api/v1/oauth/callback"
    scopes:
      - "repo"
      - "user:email"
  
  # GitLab configuration
  gitlab:
    enabled: true
    client_id: "your-gitlab-client-id"
    client_secret: "your-gitlab-client-secret"
    redirect_url: "http://your-app/api/v1/oauth/callback"
    server_url: "https://gitlab.com"  # Or your GitLab instance URL
    scopes:
      - "api"
      - "read_user"
  
  # Jira configuration
  jira:
    enabled: true
    client_id: "your-jira-client-id"
    client_secret: "your-jira-client-secret"
    redirect_url: "http://your-app/api/v1/oauth/callback"
    server_url: "https://your-instance.atlassian.net"
    scopes:
      - "read:jira-work"
      - "read:jira-user"
  
  # Azure DevOps configuration
  azure:
    enabled: true
    client_id: "your-azure-client-id"
    client_secret: "your-azure-client-secret"
    redirect_url: "http://your-app/api/v1/oauth/callback"
    scopes:
      - "vso.code"
      - "vso.project"
      - "vso.work"
  
  # Bitbucket configuration
  bitbucket:
    enabled: false  # Disabled by default
    client_id: ""
    client_secret: ""
    redirect_url: ""
    scopes: []
```

## Creating OAuth Applications

For each provider you want to integrate with, you'll need to create an OAuth application on their platform.

### GitHub

1. Go to your GitHub account settings
2. Select "Developer settings" > "OAuth Apps" > "New OAuth App"
3. Fill in the details:
   - Application name: "Rinna Workflow Manager"
   - Homepage URL: Your application's homepage
   - Authorization callback URL: Your redirect URL (e.g., `http://your-app/api/v1/oauth/callback`)
4. Register the application
5. Copy the client ID and generate a client secret
6. Add these to your Rinna configuration

### GitLab

1. Go to your GitLab instance (or gitlab.com)
2. Navigate to your profile settings > "Applications"
3. Create a new application:
   - Name: "Rinna Workflow Manager"
   - Redirect URI: Your redirect URL
   - Scopes: Select "api" and "read_user"
4. Save the application
5. Copy the application ID and secret
6. Add these to your Rinna configuration

### Jira (Atlassian)

1. Go to https://developer.atlassian.com/console/myapps/
2. Create a new OAuth 2.0 integration
3. Configure the application:
   - App name: "Rinna Workflow Manager"
   - Callback URL: Your redirect URL
   - Permissions: Select Jira API permissions
4. Create the application
5. Copy the client ID and secret
6. Add these to your Rinna configuration

### Azure DevOps

1. Go to https://app.vsaex.visualstudio.com/app/register
2. Register a new application:
   - Company name: Your company
   - Application name: "Rinna Workflow Manager"
   - Application website: Your application's homepage
   - Authorization callback URL: Your redirect URL
   - Authorized scopes: Select appropriate scopes
3. Register the application
4. Copy the application ID and secret
5. Add these to your Rinna configuration

### Bitbucket

1. Go to Bitbucket settings > "OAuth"
2. Add a consumer:
   - Name: "Rinna Workflow Manager"
   - Callback URL: Your redirect URL
   - Permissions: Select appropriate permissions
3. Save the consumer
4. Copy the key and secret
5. Add these to your Rinna configuration

## Using the OAuth API

The OAuth API provides endpoints for initiating authorization, handling callbacks, and managing tokens.

### API Endpoints

- `GET /api/v1/oauth/providers` - List available OAuth providers
- `GET /api/v1/oauth/providers/{provider}` - Get details for a specific provider
- `GET /api/v1/oauth/authorize/{provider}` - Initiate OAuth flow for a provider
- `GET /api/v1/oauth/callback` - Handle OAuth callback (used internally)
- `GET /api/v1/oauth/tokens` - List OAuth tokens for a project
- `GET /api/v1/oauth/tokens/{provider}` - Get a specific OAuth token
- `DELETE /api/v1/oauth/tokens/{provider}` - Revoke an OAuth token

### Authentication

All OAuth endpoints except for `/api/v1/oauth/callback` require authentication.

### Initiating OAuth Flow

To initiate an OAuth authorization flow:

```
GET /api/v1/oauth/authorize/{provider}?project=project_id&user_id=user_id
```

Parameters:
- `provider`: OAuth provider (github, gitlab, jira, azure-devops, bitbucket)
- `project`: Project ID to associate the token with
- `user_id`: User ID to associate the token with
- `redirect_uri` (optional): URI to redirect after authorization

Response:
```json
{
  "authorization_url": "https://github.com/login/oauth/authorize?client_id=...&state=...",
  "state": "random-state-token",
  "provider": "github",
  "expires_in": "1800"
}
```

If `redirect_uri` is provided, the API will redirect directly to the authorization URL.

### Managing Tokens

To list tokens for a project:

```
GET /api/v1/oauth/tokens?project=project_id
```

To get a specific token:

```
GET /api/v1/oauth/tokens/{provider}?project=project_id&user_id=user_id
```

To revoke a token:

```
DELETE /api/v1/oauth/tokens/{provider}?project=project_id&user_id=user_id
```

## Integration Examples

### JavaScript Example

```javascript
// Initiate OAuth flow
async function authorizeGitHub(projectId, userId) {
  const response = await fetch(`/api/v1/oauth/authorize/github?project=${projectId}&user_id=${userId}`, {
    headers: {
      'Authorization': `Bearer ${yourAuthToken}`
    }
  });
  
  const data = await response.json();
  
  // Redirect to the authorization URL
  window.location.href = data.authorization_url;
}

// List OAuth tokens
async function listTokens(projectId) {
  const response = await fetch(`/api/v1/oauth/tokens?project=${projectId}`, {
    headers: {
      'Authorization': `Bearer ${yourAuthToken}`
    }
  });
  
  return await response.json();
}
```

### Using OAuth for API Calls

After obtaining a token, you can use it to make API calls to the third-party service. The Rinna API handles token management, including refreshing expired tokens, so you can focus on your integration logic.

## Security Considerations

- The OAuth tokens are encrypted at rest using AES-GCM
- Token encryption uses the key specified in the configuration
- Expired tokens are automatically cleaned up
- The state parameter is used to prevent CSRF attacks
- Tokens are scoped to a specific project and user

## Troubleshooting

Common issues and solutions:

1. **Missing redirect URI**: Ensure the redirect URI in your configuration matches the one registered with the OAuth provider.

2. **Invalid client credentials**: Double-check your client ID and secret.

3. **Token not found**: Make sure you're using the correct project ID and user ID.

4. **Token expired**: Tokens are automatically refreshed when possible. If a token can't be refreshed, you'll need to re-authorize.

5. **Insufficient scopes**: If you can't perform certain actions, you may need to re-authorize with additional scopes.

For more advanced troubleshooting, check the API server logs.
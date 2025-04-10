# OAuth Integration Examples

This document provides practical examples for integrating with Rinna's OAuth endpoints for third-party service authentication.

## Overview

Rinna supports OAuth 2.0 integration with various providers like GitHub, Jira, Azure DevOps, GitLab, and Bitbucket. This allows you to authorize Rinna to interact with these services on your behalf.

The OAuth flow typically follows these steps:

1. Get an authorization URL from Rinna
2. Redirect the user to the authorization URL
3. The user authorizes access on the provider's site
4. The provider redirects back to your application with an authorization code
5. Exchange the authorization code for access tokens
6. Use the access tokens to make API requests to the provider

## Authorization Code Flow Examples

### Step 1: Get Authorization URL

```javascript
// JavaScript Example
async function getAuthorizationURL(provider, projectId, userId, redirectUri) {
    try {
        const response = await fetch(
            `https://api.rinna.io/api/v1/oauth/authorize/${provider}?project_id=${projectId}&user_id=${userId}&redirect_uri=${encodeURIComponent(redirectUri)}`,
            {
                method: 'GET',
                headers: {
                    'Authorization': 'Bearer ri-your-api-key',
                    'Content-Type': 'application/json'
                }
            }
        );
        
        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(`Failed to get authorization URL: ${errorData.message}`);
        }
        
        const data = await response.json();
        return {
            authorizationUrl: data.authorization_url,
            state: data.state
        };
    } catch (error) {
        console.error('Error getting authorization URL:', error);
        throw error;
    }
}
```

```go
// Go Example
func getAuthorizationURL(provider, projectID, userID, redirectURI string) (string, string, error) {
    client := &http.Client{Timeout: 10 * time.Second}
    
    // Build URL with query parameters
    url := fmt.Sprintf(
        "https://api.rinna.io/api/v1/oauth/authorize/%s?project_id=%s&user_id=%s",
        provider, projectID, userID,
    )
    
    if redirectURI != "" {
        url += "&redirect_uri=" + url.QueryEscape(redirectURI)
    }
    
    // Create request
    req, err := http.NewRequest("GET", url, nil)
    if err != nil {
        return "", "", fmt.Errorf("failed to create request: %w", err)
    }
    
    // Add headers
    req.Header.Set("Authorization", "Bearer ri-your-api-key")
    req.Header.Set("Content-Type", "application/json")
    
    // Send request
    resp, err := client.Do(req)
    if err != nil {
        return "", "", fmt.Errorf("request failed: %w", err)
    }
    defer resp.Body.Close()
    
    // Check for errors
    if resp.StatusCode != http.StatusOK {
        body, _ := ioutil.ReadAll(resp.Body)
        return "", "", fmt.Errorf("API error (%d): %s", resp.StatusCode, body)
    }
    
    // Parse response
    var result struct {
        AuthorizationURL string `json:"authorization_url"`
        State           string `json:"state"`
    }
    
    if err := json.NewDecoder(resp.Body).Decode(&result); err != nil {
        return "", "", fmt.Errorf("failed to parse response: %w", err)
    }
    
    return result.AuthorizationURL, result.State, nil
}
```

### Step 2: Redirect User to Authorization URL

```javascript
// JavaScript (Browser) Example
function redirectToAuthorizationURL(authUrl) {
    // Store state in local storage or cookies for verification later
    const urlData = new URL(authUrl);
    const state = urlData.searchParams.get('state');
    localStorage.setItem('oauth_state', state);
    
    // Redirect the user
    window.location.href = authUrl;
}

// Usage example
async function startOAuthFlow(provider, projectId, userId) {
    const redirectUri = 'https://yourapplication.com/oauth/callback';
    const { authorizationUrl } = await getAuthorizationURL(provider, projectId, userId, redirectUri);
    redirectToAuthorizationURL(authorizationUrl);
}
```

### Step 3: Handle OAuth Callback

```javascript
// JavaScript Example (Express.js)
const express = require('express');
const app = express();

app.get('/oauth/callback', async (req, res) => {
    const { code, state } = req.query;
    
    // Verify state to prevent CSRF attacks
    const storedState = localStorage.getItem('oauth_state');
    if (!state || state !== storedState) {
        return res.status(400).send('Invalid state parameter');
    }
    
    try {
        // Exchange code for tokens
        const tokens = await exchangeCodeForTokens(code, state);
        
        // Store tokens securely (never in localStorage for production apps)
        // This is just an example; use a secure storage method in production
        localStorage.setItem('oauth_tokens', JSON.stringify(tokens));
        
        res.send('Authorization successful! You can close this window.');
    } catch (error) {
        console.error('OAuth callback error:', error);
        res.status(500).send('Authorization failed: ' + error.message);
    }
});

async function exchangeCodeForTokens(code, state) {
    const response = await fetch(`https://api.rinna.io/api/v1/oauth/callback?code=${code}&state=${state}`, {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json'
        }
    });
    
    if (!response.ok) {
        const errorData = await response.json();
        throw new Error(`Token exchange failed: ${errorData.message}`);
    }
    
    return response.json();
}
```

```go
// Go Example (net/http)
func handleOAuthCallback(w http.ResponseWriter, r *http.Request) {
    // Get query parameters
    code := r.URL.Query().Get("code")
    state := r.URL.Query().Get("state")
    
    if code == "" || state == "" {
        http.Error(w, "Missing code or state parameter", http.StatusBadRequest)
        return
    }
    
    // Verify state (retrieve from session/cookie in real implementation)
    // This is placeholder code - implement proper session handling in production
    expectedState := getStateFromSession(r)
    if state != expectedState {
        http.Error(w, "Invalid state parameter", http.StatusBadRequest)
        return
    }
    
    // Exchange code for tokens
    tokens, err := exchangeCodeForTokens(code, state)
    if err != nil {
        http.Error(w, fmt.Sprintf("Token exchange failed: %v", err), http.StatusInternalServerError)
        return
    }
    
    // Store tokens securely (use proper session storage in production)
    storeTokensInSession(w, r, tokens)
    
    // Render success page
    w.Header().Set("Content-Type", "text/html")
    w.Write([]byte("<html><body><h1>Authorization Successful!</h1><p>You can close this window.</p></body></html>"))
}

func exchangeCodeForTokens(code, state string) (map[string]interface{}, error) {
    client := &http.Client{Timeout: 10 * time.Second}
    
    // Build URL with query parameters
    url := fmt.Sprintf(
        "https://api.rinna.io/api/v1/oauth/callback?code=%s&state=%s",
        code, state,
    )
    
    // Create request
    req, err := http.NewRequest("GET", url, nil)
    if err != nil {
        return nil, fmt.Errorf("failed to create request: %w", err)
    }
    
    // Add headers
    req.Header.Set("Content-Type", "application/json")
    
    // Send request
    resp, err := client.Do(req)
    if err != nil {
        return nil, fmt.Errorf("request failed: %w", err)
    }
    defer resp.Body.Close()
    
    // Check for errors
    if resp.StatusCode != http.StatusOK {
        body, _ := ioutil.ReadAll(resp.Body)
        return nil, fmt.Errorf("API error (%d): %s", resp.StatusCode, body)
    }
    
    // Parse response
    var tokens map[string]interface{}
    if err := json.NewDecoder(resp.Body).Decode(&tokens); err != nil {
        return nil, fmt.Errorf("failed to parse response: %w", err)
    }
    
    return tokens, nil
}
```

## Managing OAuth Tokens

### Listing OAuth Tokens

```javascript
// JavaScript Example
async function listOAuthTokens(projectId, provider = null) {
    try {
        let url = `https://api.rinna.io/api/v1/oauth/tokens?project_id=${projectId}`;
        if (provider) {
            url += `&provider=${provider}`;
        }
        
        const response = await fetch(url, {
            method: 'GET',
            headers: {
                'Authorization': 'Bearer ri-your-api-key',
                'Content-Type': 'application/json'
            }
        });
        
        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(`Failed to list tokens: ${errorData.message}`);
        }
        
        return response.json();
    } catch (error) {
        console.error('Error listing OAuth tokens:', error);
        throw error;
    }
}
```

### Revoking OAuth Tokens

```javascript
// JavaScript Example
async function revokeOAuthToken(provider, projectId, userId) {
    try {
        const response = await fetch(
            `https://api.rinna.io/api/v1/oauth/tokens/${provider}?project_id=${projectId}&user_id=${userId}`,
            {
                method: 'DELETE',
                headers: {
                    'Authorization': 'Bearer ri-your-api-key',
                    'Content-Type': 'application/json'
                }
            }
        );
        
        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(`Failed to revoke token: ${errorData.message}`);
        }
        
        return true;
    } catch (error) {
        console.error('Error revoking OAuth token:', error);
        throw error;
    }
}
```

## Listing Available OAuth Providers

```javascript
// JavaScript Example
async function listOAuthProviders() {
    try {
        const response = await fetch('https://api.rinna.io/api/v1/oauth/providers', {
            method: 'GET',
            headers: {
                'Authorization': 'Bearer ri-your-api-key',
                'Content-Type': 'application/json'
            }
        });
        
        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(`Failed to list providers: ${errorData.message}`);
        }
        
        return response.json();
    } catch (error) {
        console.error('Error listing OAuth providers:', error);
        throw error;
    }
}
```

## Complete OAuth Integration Example

```javascript
// JavaScript/React Example
import React, { useState, useEffect } from 'react';

function OAuthIntegration() {
    const [providers, setProviders] = useState([]);
    const [tokens, setTokens] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    
    // Project and user info (would come from your app's state/context)
    const projectId = 'RINNA';
    const userId = 'user123';
    
    // Get providers and tokens on component mount
    useEffect(() => {
        async function fetchData() {
            try {
                setLoading(true);
                
                // Fetch providers and tokens in parallel
                const [providersData, tokensData] = await Promise.all([
                    listOAuthProviders(),
                    listOAuthTokens(projectId)
                ]);
                
                setProviders(providersData);
                setTokens(tokensData);
                setError(null);
            } catch (err) {
                setError(err.message);
                console.error('Failed to fetch OAuth data:', err);
            } finally {
                setLoading(false);
            }
        }
        
        fetchData();
    }, [projectId]);
    
    // Start OAuth flow for a provider
    const handleConnectProvider = async (providerId) => {
        try {
            const redirectUri = `${window.location.origin}/oauth/callback`;
            const { authorizationUrl } = await getAuthorizationURL(providerId, projectId, userId, redirectUri);
            
            // Store the provider being connected in sessionStorage
            sessionStorage.setItem('connecting_provider', providerId);
            
            // Redirect to authorization URL
            window.location.href = authorizationUrl;
        } catch (err) {
            setError(`Failed to start OAuth flow: ${err.message}`);
            console.error('OAuth flow error:', err);
        }
    };
    
    // Revoke token for a provider
    const handleDisconnectProvider = async (providerId) => {
        try {
            await revokeOAuthToken(providerId, projectId, userId);
            
            // Update tokens list
            const updatedTokens = tokens.filter(token => token.provider !== providerId);
            setTokens(updatedTokens);
            setError(null);
        } catch (err) {
            setError(`Failed to disconnect provider: ${err.message}`);
            console.error('Revoke token error:', err);
        }
    };
    
    // Check if a provider is connected
    const isProviderConnected = (providerId) => {
        return tokens.some(token => token.provider === providerId);
    };
    
    if (loading) {
        return <div>Loading...</div>;
    }
    
    return (
        <div className="oauth-integration">
            <h2>Connected Services</h2>
            
            {error && (
                <div className="error-message">{error}</div>
            )}
            
            <div className="providers-list">
                {providers.map(provider => (
                    <div key={provider.id} className="provider-card">
                        <img src={provider.icon_url} alt={provider.name} />
                        <div className="provider-info">
                            <h3>{provider.name}</h3>
                            <p>{provider.description}</p>
                        </div>
                        
                        {isProviderConnected(provider.id) ? (
                            <button 
                                onClick={() => handleDisconnectProvider(provider.id)}
                                className="disconnect-button"
                            >
                                Disconnect
                            </button>
                        ) : (
                            <button 
                                onClick={() => handleConnectProvider(provider.id)}
                                className="connect-button"
                            >
                                Connect
                            </button>
                        )}
                    </div>
                ))}
            </div>
            
            {tokens.length > 0 && (
                <div className="tokens-section">
                    <h3>Connected Accounts</h3>
                    <table className="tokens-table">
                        <thead>
                            <tr>
                                <th>Provider</th>
                                <th>Connected On</th>
                                <th>Expires</th>
                                <th>Scopes</th>
                            </tr>
                        </thead>
                        <tbody>
                            {tokens.map(token => (
                                <tr key={`${token.provider}-${token.user_id}`}>
                                    <td>{token.provider}</td>
                                    <td>{new Date(token.created_at).toLocaleString()}</td>
                                    <td>{token.expiry ? new Date(token.expiry).toLocaleString() : 'Never'}</td>
                                    <td>{token.scopes.join(', ')}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}
        </div>
    );
}

export default OAuthIntegration;
```

## OAuth Callback Handler Component

```javascript
// JavaScript/React Example - Callback Handler
import React, { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';

function OAuthCallback() {
    const [status, setStatus] = useState('Processing...');
    const location = useLocation();
    const navigate = useNavigate();
    
    useEffect(() => {
        async function handleCallback() {
            try {
                // Get code and state from URL query parameters
                const params = new URLSearchParams(location.search);
                const code = params.get('code');
                const state = params.get('state');
                
                if (!code || !state) {
                    setStatus('Error: Missing code or state parameter');
                    return;
                }
                
                // Exchange code for tokens
                const tokens = await exchangeCodeForTokens(code, state);
                
                // Store tokens securely (this is simplified for the example)
                // In a real app, you should use a secure storage method
                sessionStorage.setItem('oauth_tokens', JSON.stringify(tokens));
                
                // Get the provider being connected from sessionStorage
                const provider = sessionStorage.getItem('connecting_provider');
                sessionStorage.removeItem('connecting_provider');
                
                setStatus(`Successfully connected to ${provider}!`);
                
                // Redirect back to integrations page after a delay
                setTimeout(() => {
                    navigate('/integrations');
                }, 3000);
            } catch (error) {
                console.error('OAuth callback error:', error);
                setStatus(`Error: ${error.message}`);
            }
        }
        
        handleCallback();
    }, [location, navigate]);
    
    return (
        <div className="oauth-callback">
            <h2>Connecting Service</h2>
            <div className="status-message">{status}</div>
        </div>
    );
}

export default OAuthCallback;
```

## Best Practices for OAuth Integration

1. **Always validate the state parameter** to prevent CSRF attacks
2. **Store tokens securely**, using secure cookies, server-side sessions, or encrypted storage
3. **Implement token refresh** for providers with expiring tokens
4. **Handle errors gracefully** with clear user messaging
5. **Provide a way to revoke access** for users to disconnect services
6. **Use HTTPS** for all OAuth-related traffic
7. **Request only the necessary scopes** to follow the principle of least privilege
8. **Validate redirect URIs** to prevent open redirector vulnerabilities
9. **Keep OAuth credentials confidential** and never expose them in client-side code
10. **Monitor token usage** and revoke tokens that show suspicious activity

For more information on secure OAuth implementation, refer to the [API Security Guide](../api-security-guide.md).
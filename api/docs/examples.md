# Rinna API Examples

This document provides a comprehensive set of examples for using the Rinna API across different programming languages and use cases.

## Authentication Examples

### Obtaining an API Token

```bash
# Request an API token
curl -X POST "http://localhost:8080/api/v1/auth/token/generate" \
  -H "Content-Type: application/json" \
  -d '{
    "project_id": "RINNA",
    "user_id": "developer1",
    "scopes": ["read", "write"]
  }'
```

```javascript
// JavaScript example
async function getToken() {
  const response = await fetch('http://localhost:8080/api/v1/auth/token/generate', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      project_id: 'RINNA',
      user_id: 'developer1',
      scopes: ['read', 'write']
    })
  });
  
  const data = await response.json();
  return data.token;
}
```

```python
# Python example
import requests

def get_token():
    response = requests.post(
        "http://localhost:8080/api/v1/auth/token/generate",
        json={
            "project_id": "RINNA",
            "user_id": "developer1",
            "scopes": ["read", "write"]
        }
    )
    
    return response.json()["token"]
```

```go
// Go example
package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"net/http"
)

type TokenRequest struct {
	ProjectID string   `json:"project_id"`
	UserID    string   `json:"user_id"`
	Scopes    []string `json:"scopes"`
}

type TokenResponse struct {
	Token string `json:"token"`
}

func getToken() (string, error) {
	req := TokenRequest{
		ProjectID: "RINNA",
		UserID:    "developer1",
		Scopes:    []string{"read", "write"},
	}
	
	body, err := json.Marshal(req)
	if err != nil {
		return "", err
	}
	
	resp, err := http.Post(
		"http://localhost:8080/api/v1/auth/token/generate",
		"application/json",
		bytes.NewBuffer(body),
	)
	
	if err != nil {
		return "", err
	}
	defer resp.Body.Close()
	
	var tokenResp TokenResponse
	err = json.NewDecoder(resp.Body).Decode(&tokenResp)
	if err != nil {
		return "", err
	}
	
	return tokenResp.Token, nil
}
```

## Work Item Examples

### Creating a Work Item

```bash
# Create a work item via curl
curl -X POST "http://localhost:8080/api/v1/workitems" \
  -H "Authorization: Bearer ri-api-key" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Implement payment gateway",
    "type": "FEATURE",
    "priority": "HIGH",
    "projectKey": "RINNA",
    "description": "Integrate with Stripe payment API"
  }'
```

```javascript
// JavaScript example
async function createWorkItem(token) {
  const response = await fetch('http://localhost:8080/api/v1/workitems', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      title: 'Implement payment gateway',
      type: 'FEATURE',
      priority: 'HIGH',
      projectKey: 'RINNA',
      description: 'Integrate with Stripe payment API'
    })
  });
  
  return await response.json();
}
```

```python
# Python example
import requests

def create_work_item(token):
    response = requests.post(
        "http://localhost:8080/api/v1/workitems",
        headers={
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json"
        },
        json={
            "title": "Implement payment gateway",
            "type": "FEATURE",
            "priority": "HIGH",
            "projectKey": "RINNA",
            "description": "Integrate with Stripe payment API"
        }
    )
    
    return response.json()
```

### Updating a Work Item Status

```bash
# Transition a work item to a new state
curl -X POST "http://localhost:8080/api/v1/workitems/123/transitions" \
  -H "Authorization: Bearer ri-api-key" \
  -H "Content-Type: application/json" \
  -d '{
    "targetStatus": "IN_DEV",
    "comment": "Starting implementation of payment gateway"
  }'
```

## OAuth Integration Examples

### Initializing OAuth Authorization Flow

```javascript
// JavaScript example
function initiateOAuth() {
  window.location.href = 'http://localhost:8080/api/v1/oauth/authorize/github?project_id=RINNA&user_id=developer1&redirect_uri=http://localhost:3000/callback';
}
```

```python
# Python example
def get_oauth_url():
    params = {
        "project_id": "RINNA",
        "user_id": "developer1",
        "redirect_uri": "http://localhost:3000/callback"
    }
    
    response = requests.get(
        "http://localhost:8080/api/v1/oauth/authorize/github",
        params=params,
        headers={"Authorization": f"Bearer {token}"}
    )
    
    return response.json()["authorization_url"]
```

### Handling OAuth Callback

```javascript
// JavaScript callback handler
async function handleOAuthCallback() {
  // Get URL parameters
  const urlParams = new URLSearchParams(window.location.search);
  const code = urlParams.get('code');
  const state = urlParams.get('state');
  
  // Exchange code for token
  const response = await fetch(`http://localhost:8080/api/v1/oauth/callback?code=${code}&state=${state}`);
  const tokenData = await response.json();
  
  // Store the token
  localStorage.setItem('oauth_token', tokenData.access_token);
}
```

## Webhook Security Examples

### Configuring Webhook Security

```bash
# Configure webhook security settings
curl -X POST "http://localhost:8080/api/v1/webhooks/configure/github" \
  -H "Authorization: Bearer ri-api-key" \
  -H "Content-Type: application/json" \
  -d '{
    "secret": "webhook-secret-key-123",
    "projectKey": "RINNA",
    "securityMode": "strict",
    "allowedIPs": ["192.168.1.0/24"]
  }'
```

### Verifying Webhook Signatures (GitHub)

```javascript
// Node.js webhook receiver
const crypto = require('crypto');
const express = require('express');
const app = express();

app.use(express.json({
  verify: (req, res, buf) => {
    req.rawBody = buf;
  }
}));

app.post('/webhooks/github', (req, res) => {
  const secret = 'webhook-secret-key-123';
  const signature = req.headers['x-hub-signature-256'];
  
  // Verify signature
  const hmac = crypto.createHmac('sha256', secret);
  const digest = 'sha256=' + hmac.update(req.rawBody).digest('hex');
  
  if (!signature || !crypto.timingSafeEqual(Buffer.from(signature), Buffer.from(digest))) {
    return res.status(401).send('Invalid signature');
  }
  
  // Process webhook
  console.log('Webhook verified:', req.body);
  res.status(200).send('OK');
});

app.listen(3000);
```

## Rate Limiting Examples

### Getting Rate Limit Information

```bash
# Get current rate limit information
curl -X GET "http://localhost:8080/api/v1/rate-limits" \
  -H "Authorization: Bearer ri-api-key"
```

```javascript
// JavaScript example
async function getRateLimits(token) {
  const response = await fetch('http://localhost:8080/api/v1/rate-limits', {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  
  return await response.json();
}
```

### Handling Rate Limiting in Clients

```javascript
// JavaScript example with rate limit handling
async function fetchWithRateLimitHandling(url, token) {
  try {
    const response = await fetch(url, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    
    if (response.status === 429) {
      const retryAfter = response.headers.get('Retry-After');
      const retryMs = parseInt(retryAfter) * 1000;
      
      console.log(`Rate limited. Retrying after ${retryAfter} seconds`);
      
      // Wait and retry
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

## Comprehensive API Client Example

```javascript
// JavaScript API client class
class RinnaApiClient {
  constructor(baseUrl = 'http://localhost:8080/api/v1') {
    this.baseUrl = baseUrl;
    this.token = null;
  }
  
  // Set authentication token
  setToken(token) {
    this.token = token;
  }
  
  // Get headers with authentication
  getHeaders() {
    return {
      'Authorization': `Bearer ${this.token}`,
      'Content-Type': 'application/json'
    };
  }
  
  // Generic request method with rate limit handling
  async request(endpoint, method = 'GET', data = null) {
    const url = `${this.baseUrl}${endpoint}`;
    const options = {
      method,
      headers: this.getHeaders()
    };
    
    if (data && (method === 'POST' || method === 'PUT')) {
      options.body = JSON.stringify(data);
    }
    
    try {
      const response = await fetch(url, options);
      
      // Handle rate limiting
      if (response.status === 429) {
        const retryAfter = response.headers.get('Retry-After') || '60';
        const retryMs = parseInt(retryAfter) * 1000;
        
        console.log(`Rate limited. Retrying after ${retryAfter} seconds`);
        
        // Wait and retry
        await new Promise(resolve => setTimeout(resolve, retryMs));
        return this.request(endpoint, method, data);
      }
      
      // Handle other errors
      if (!response.ok) {
        const errorData = await response.json().catch(() => null);
        throw new Error(errorData?.message || `HTTP error ${response.status}`);
      }
      
      // Return data
      return await response.json();
    } catch (error) {
      console.error('API request failed:', error);
      throw error;
    }
  }
  
  // Authentication methods
  async generateToken(projectId, userId, scopes = ['read', 'write']) {
    const response = await fetch(`${this.baseUrl}/auth/token/generate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ project_id: projectId, user_id: userId, scopes })
    });
    
    const data = await response.json();
    this.token = data.token;
    return data.token;
  }
  
  // Work item methods
  async getWorkItems(filters = {}) {
    const queryParams = new URLSearchParams(filters).toString();
    return this.request(`/workitems?${queryParams}`);
  }
  
  async getWorkItem(id) {
    return this.request(`/workitems/${id}`);
  }
  
  async createWorkItem(workItem) {
    return this.request('/workitems', 'POST', workItem);
  }
  
  async updateWorkItem(id, updates) {
    return this.request(`/workitems/${id}`, 'PUT', updates);
  }
  
  async transitionWorkItem(id, targetStatus, comment) {
    return this.request(`/workitems/${id}/transitions`, 'POST', {
      targetStatus,
      comment
    });
  }
  
  // Project methods
  async getProjects(filters = {}) {
    const queryParams = new URLSearchParams(filters).toString();
    return this.request(`/projects?${queryParams}`);
  }
  
  async getProject(key) {
    return this.request(`/projects/${key}`);
  }
  
  async createProject(project) {
    return this.request('/projects', 'POST', project);
  }
  
  // Release methods
  async getReleases(filters = {}) {
    const queryParams = new URLSearchParams(filters).toString();
    return this.request(`/releases?${queryParams}`);
  }
  
  async getRelease(id) {
    return this.request(`/releases/${id}`);
  }
  
  async createRelease(release) {
    return this.request('/releases', 'POST', release);
  }
  
  // OAuth methods
  getAuthorizationUrl(provider, projectId, userId, redirectUri) {
    const params = new URLSearchParams({
      project_id: projectId,
      user_id: userId,
      redirect_uri: redirectUri
    }).toString();
    
    return `${this.baseUrl}/oauth/authorize/${provider}?${params}`;
  }
  
  // Rate limit methods
  async getRateLimits() {
    return this.request('/rate-limits');
  }
  
  // Webhook methods
  async configureWebhook(source, config) {
    return this.request(`/webhooks/configure/${source}`, 'POST', config);
  }
}

// Usage
const client = new RinnaApiClient();
await client.generateToken('RINNA', 'developer1');

// Get work items
const workItems = await client.getWorkItems({ status: 'IN_DEV' });
console.log('Work items:', workItems);

// Create a work item
const newWorkItem = await client.createWorkItem({
  title: 'Implement search feature',
  type: 'FEATURE',
  priority: 'MEDIUM',
  projectKey: 'RINNA'
});
console.log('Created work item:', newWorkItem);
```
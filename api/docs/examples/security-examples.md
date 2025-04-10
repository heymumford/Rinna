# Rinna API Security Integration Examples

This document provides practical code examples for implementing security features when integrating with the Rinna API.

## Authentication Examples

### API Key Authentication

```go
// Go Example
func apiKeyAuthentication() {
    client := &http.Client{Timeout: 10 * time.Second}
    
    req, err := http.NewRequest("GET", "https://api.rinna.io/api/v1/projects", nil)
    if err != nil {
        log.Fatalf("Error creating request: %v", err)
    }
    
    // Add API key using Bearer token format
    req.Header.Set("Authorization", "Bearer ri-your-api-key")
    
    resp, err := client.Do(req)
    if err != nil {
        log.Fatalf("Error making request: %v", err)
    }
    defer resp.Body.Close()
    
    // Process response
    // ...
}
```

```javascript
// JavaScript Example
async function apiKeyAuthentication() {
    try {
        const response = await fetch('https://api.rinna.io/api/v1/projects', {
            method: 'GET',
            headers: {
                'Authorization': 'Bearer ri-your-api-key',
                'Content-Type': 'application/json'
            }
        });
        
        if (!response.ok) {
            throw new Error(`API error: ${response.status}`);
        }
        
        const data = await response.json();
        return data;
    } catch (error) {
        console.error('Authentication error:', error);
        throw error;
    }
}
```

### OAuth2 Integration

```javascript
// JavaScript Example - OAuth2 Authorization Code Flow
async function startOAuth2Flow(provider, projectId, userId) {
    try {
        // Step 1: Get authorization URL
        const response = await fetch(
            `https://api.rinna.io/api/v1/oauth/authorize/${provider}?project_id=${projectId}&user_id=${userId}`, 
            {
                method: 'GET',
                headers: {
                    'Authorization': 'Bearer ri-your-api-key',
                    'Content-Type': 'application/json'
                }
            }
        );
        
        if (!response.ok) {
            throw new Error(`API error: ${response.status}`);
        }
        
        const data = await response.json();
        
        // Step 2: Redirect user to authorization URL
        // This would typically be done by setting window.location or similar
        console.log(`Redirect user to: ${data.authorization_url}`);
        
        // The OAuth provider will redirect back to your redirect_uri with a code and state parameter
        // You would then exchange this code for tokens in a separate function
    } catch (error) {
        console.error('OAuth flow error:', error);
        throw error;
    }
}

// Handle OAuth callback after user authorizes
async function handleOAuthCallback(code, state) {
    try {
        const response = await fetch(
            `https://api.rinna.io/api/v1/oauth/callback?code=${code}&state=${state}`,
            {
                method: 'GET'
            }
        );
        
        if (!response.ok) {
            throw new Error(`API error: ${response.status}`);
        }
        
        const tokenData = await response.json();
        
        // Store token data securely
        localStorage.setItem('oauth_token', JSON.stringify(tokenData));
        
        return tokenData;
    } catch (error) {
        console.error('OAuth callback error:', error);
        throw error;
    }
}
```

## Rate Limiting Examples

### Monitoring Rate Limits

```go
// Go Example - Monitoring rate limits
func monitorRateLimits(resp *http.Response) {
    limit := resp.Header.Get("X-RateLimit-Limit")
    remaining := resp.Header.Get("X-RateLimit-Remaining")
    reset := resp.Header.Get("X-RateLimit-Reset")
    
    log.Printf("Rate limits: %s/%s requests remaining, resets at %s", remaining, limit, reset)
    
    // Alert if approaching limit
    remainingInt, _ := strconv.Atoi(remaining)
    limitInt, _ := strconv.Atoi(limit)
    
    if remainingInt < int(float64(limitInt) * 0.1) {
        log.Printf("WARNING: Approaching rate limit! Only %s/%s requests remaining", remaining, limit)
    }
}
```

### Handling Rate Limiting (Exponential Backoff)

```python
# Python Example - Handling rate limits with exponential backoff
import requests
import time
import random

def request_with_backoff(url, headers, max_retries=5):
    """Make a request with exponential backoff for rate limiting"""
    retries = 0
    
    while retries <= max_retries:
        response = requests.get(url, headers=headers)
        
        # Check rate limit headers
        limit = response.headers.get('X-RateLimit-Limit')
        remaining = response.headers.get('X-RateLimit-Remaining')
        reset = response.headers.get('X-RateLimit-Reset')
        
        print(f"Rate limits: {remaining}/{limit} remaining, resets at {reset}")
        
        # If rate limited, implement backoff
        if response.status_code == 429:
            retry_after = int(response.headers.get('Retry-After', 60))
            
            # Add jitter to prevent thundering herd problem
            jitter = random.uniform(0, 0.1) * retry_after
            
            # Calculate exponential backoff time
            wait_time = min(
                (2 ** retries) * retry_after + jitter,
                300  # Cap at 5 minutes max
            )
            
            print(f"Rate limited. Retrying in {wait_time:.1f} seconds...")
            time.sleep(wait_time)
            retries += 1
            continue
            
        # Return successful response
        if response.ok:
            return response
            
        # Handle other errors
        if response.status_code >= 400:
            print(f"Error: {response.status_code} - {response.text}")
            
        # If we had a non-429 error, still implement backoff but with different strategy
        if retries < max_retries:
            wait_time = (2 ** retries) + random.uniform(0, 1)
            print(f"Request failed. Retrying in {wait_time:.1f} seconds...")
            time.sleep(wait_time)
            retries += 1
        else:
            response.raise_for_status()
    
    raise Exception("Max retries exceeded")
```

## Webhook Security Examples

### Webhook Signature Verification

```go
// Go Example - GitHub Webhook Signature Verification (SHA-256)
func verifyGitHubWebhook(payload []byte, signature, secret string) bool {
    // GitHub signatures are prefixed with "sha256="
    const signaturePrefix = "sha256="
    
    // Check if signature has the right format
    if len(signature) <= len(signaturePrefix) || !strings.HasPrefix(signature, signaturePrefix) {
        return false
    }
    
    // Extract the actual signature
    signatureHex := signature[len(signaturePrefix):]
    
    // Calculate expected signature
    mac := hmac.New(sha256.New, []byte(secret))
    mac.Write(payload)
    expectedMAC := mac.Sum(nil)
    expectedSignature := hex.EncodeToString(expectedMAC)
    
    // Compare signatures using constant-time comparison to prevent timing attacks
    return hmac.Equal([]byte(signatureHex), []byte(expectedSignature))
}

// Webhook handler
func handleGitHubWebhook(w http.ResponseWriter, r *http.Request) {
    // Read the request body
    payload, err := ioutil.ReadAll(r.Body)
    if err != nil {
        http.Error(w, "Failed to read request body", http.StatusBadRequest)
        return
    }
    
    // Get headers
    signature := r.Header.Get("X-Hub-Signature-256")
    event := r.Header.Get("X-GitHub-Event")
    delivery := r.Header.Get("X-GitHub-Delivery")
    
    // Verify delivery ID exists (nonce verification)
    if delivery == "" {
        http.Error(w, "Missing delivery ID", http.StatusBadRequest)
        return
    }
    
    // Verify signature
    if !verifyGitHubWebhook(payload, signature, "your-webhook-secret") {
        http.Error(w, "Invalid signature", http.StatusUnauthorized)
        return
    }
    
    // Process the webhook based on event type
    switch event {
    case "push":
        // Handle push event
    case "pull_request":
        // Handle pull request event
    default:
        // Handle other events
    }
    
    w.WriteHeader(http.StatusOK)
    w.Write([]byte("Webhook processed"))
}
```

```javascript
// JavaScript/Node.js Example - GitLab Webhook Verification
const crypto = require('crypto');
const express = require('express');
const bodyParser = require('body-parser');

const app = express();

// Need raw body for signature verification
app.use(bodyParser.json({
    verify: (req, res, buf) => {
        req.rawBody = buf;
    }
}));

app.post('/webhooks/gitlab', (req, res) => {
    const token = req.headers['x-gitlab-token'];
    const event = req.headers['x-gitlab-event'];
    
    // Verify token (GitLab uses token-based verification)
    if (token !== process.env.GITLAB_WEBHOOK_SECRET) {
        return res.status(401).send('Invalid token');
    }
    
    // Process the webhook based on event type
    console.log(`Received GitLab event: ${event}`);
    
    // Handle the webhook data
    const webhookData = req.body;
    console.log('Webhook data:', webhookData);
    
    res.status(200).send('Webhook processed');
});

// Bitbucket webhook handler with HMAC-SHA1 verification
app.post('/webhooks/bitbucket', (req, res) => {
    const signature = req.headers['x-hub-signature'];
    const event = req.headers['x-event-key'];
    const requestUUID = req.headers['x-request-uuid'];
    
    // Verify request UUID exists (nonce verification)
    if (!requestUUID) {
        return res.status(400).send('Missing request UUID');
    }
    
    // Verify signature
    const hmac = crypto.createHmac('sha1', process.env.BITBUCKET_WEBHOOK_SECRET);
    const digest = 'sha1=' + hmac.update(req.rawBody).digest('hex');
    
    if (!crypto.timingSafeEqual(Buffer.from(digest), Buffer.from(signature))) {
        return res.status(401).send('Invalid signature');
    }
    
    // Process the webhook based on event type
    console.log(`Received Bitbucket event: ${event}`);
    
    // Handle the webhook data
    const webhookData = req.body;
    console.log('Webhook data:', webhookData);
    
    res.status(200).send('Webhook processed');
});

app.listen(3000, () => {
    console.log('Webhook server running on port 3000');
});
```

## Request Tracing Examples

### Adding Request Tracing Headers

```go
// Go Example - Adding request tracing headers
import (
    "net/http"
    "github.com/google/uuid"
)

func tracedAPIRequest(url string) (*http.Response, error) {
    client := &http.Client{Timeout: 10 * time.Second}
    
    req, err := http.NewRequest("GET", url, nil)
    if err != nil {
        return nil, err
    }
    
    // Add API key authentication
    req.Header.Set("Authorization", "Bearer ri-your-api-key")
    
    // Add request tracing header
    requestID := uuid.New().String()
    req.Header.Set("X-Request-ID", requestID)
    
    // Log the request ID for correlation
    log.Printf("Making API request with ID: %s", requestID)
    
    resp, err := client.Do(req)
    if err != nil {
        return nil, err
    }
    
    // Extract response request ID for correlation
    responseRequestID := resp.Header.Get("X-Request-ID")
    log.Printf("Received response for request ID: %s", responseRequestID)
    
    return resp, nil
}
```

## Security Context Example

```javascript
// JavaScript Example - Getting security context
async function getSecurityContext() {
    try {
        const response = await fetch('https://api.rinna.io/api/v1/security/context', {
            method: 'GET',
            headers: {
                'Authorization': 'Bearer ri-your-api-key',
                'Content-Type': 'application/json',
                'X-Request-ID': generateUUID() // Custom function to generate UUID
            }
        });
        
        if (!response.ok) {
            throw new Error(`API error: ${response.status}`);
        }
        
        const securityContext = await response.json();
        console.log('Security context:', securityContext);
        
        return securityContext;
    } catch (error) {
        console.error('Error fetching security context:', error);
        throw error;
    }
}
```

## Complete Integration Example

```go
// Go Example - Complete API integration with security features
package main

import (
    "bytes"
    "crypto/tls"
    "encoding/json"
    "fmt"
    "io/ioutil"
    "log"
    "net/http"
    "strconv"
    "time"
    
    "github.com/google/uuid"
)

// Secure API client with retry handling, rate limiting, and security features
type SecureAPIClient struct {
    baseURL     string
    apiKey      string
    httpClient  *http.Client
    maxRetries  int
}

// Create a new secure API client
func NewSecureAPIClient(baseURL, apiKey string) *SecureAPIClient {
    return &SecureAPIClient{
        baseURL:    baseURL,
        apiKey:     apiKey,
        httpClient: createSecureHTTPClient(),
        maxRetries: 3,
    }
}

// Create an HTTP client with security configurations
func createSecureHTTPClient() *http.Client {
    return &http.Client{
        Timeout: 10 * time.Second,
        Transport: &http.Transport{
            TLSClientConfig: &tls.Config{
                MinVersion: tls.VersionTLS12,
            },
            MaxIdleConnsPerHost: 20,
            MaxConnsPerHost:     100,
        },
    }
}

// Make a secure API request with retry handling and rate limiting
func (c *SecureAPIClient) Request(method, path string, body interface{}) ([]byte, error) {
    var bodyData []byte
    var err error
    
    // Prepare request body if provided
    if body != nil {
        bodyData, err = json.Marshal(body)
        if err != nil {
            return nil, fmt.Errorf("failed to marshal request body: %w", err)
        }
    }
    
    // Create request
    url := fmt.Sprintf("%s%s", c.baseURL, path)
    req, err := http.NewRequest(method, url, bytes.NewBuffer(bodyData))
    if err != nil {
        return nil, fmt.Errorf("failed to create request: %w", err)
    }
    
    // Add security headers
    requestID := uuid.New().String()
    req.Header.Set("Content-Type", "application/json")
    req.Header.Set("Authorization", fmt.Sprintf("Bearer %s", c.apiKey))
    req.Header.Set("X-Request-ID", requestID)
    req.Header.Set("X-Content-Type-Options", "nosniff")
    
    // Execute request with retry logic
    var resp *http.Response
    var respBody []byte
    
    for retry := 0; retry <= c.maxRetries; retry++ {
        log.Printf("Making request to %s (attempt %d/%d)", url, retry+1, c.maxRetries+1)
        
        resp, err = c.httpClient.Do(req)
        if err != nil {
            log.Printf("Request failed: %v", err)
            if retry == c.maxRetries {
                return nil, fmt.Errorf("request failed after %d retries: %w", c.maxRetries, err)
            }
            
            // Simple backoff strategy
            time.Sleep(time.Duration(retry+1) * time.Second)
            continue
        }
        
        // Check and log rate limit information
        c.checkRateLimits(resp)
        
        // Handle rate limiting
        if resp.StatusCode == 429 {
            resp.Body.Close()
            
            retryAfter := 60 // Default to 60 seconds
            if ra := resp.Header.Get("Retry-After"); ra != "" {
                if raInt, err := strconv.Atoi(ra); err == nil {
                    retryAfter = raInt
                }
            }
            
            // Apply exponential backoff
            delay := time.Duration(retryAfter) * time.Second
            if retry > 0 {
                delay = time.Duration(retryAfter * (1 << retry)) * time.Second
            }
            
            log.Printf("Rate limited. Retrying in %v...", delay)
            time.Sleep(delay)
            continue
        }
        
        // Read response body
        respBody, err = ioutil.ReadAll(resp.Body)
        resp.Body.Close()
        if err != nil {
            log.Printf("Failed to read response body: %v", err)
            if retry == c.maxRetries {
                return nil, fmt.Errorf("failed to read response after %d retries: %w", c.maxRetries, err)
            }
            
            time.Sleep(time.Duration(retry+1) * time.Second)
            continue
        }
        
        // Handle non-success response codes
        if resp.StatusCode >= 400 {
            log.Printf("Request failed with status %d: %s", resp.StatusCode, respBody)
            
            // Return error immediately for client errors except rate limiting (already handled)
            if resp.StatusCode >= 400 && resp.StatusCode < 500 && resp.StatusCode != 429 {
                var errorResp struct {
                    Message string   `json:"message"`
                    Details []string `json:"details"`
                }
                
                if err := json.Unmarshal(respBody, &errorResp); err == nil {
                    return nil, fmt.Errorf("API error (%d): %s - %v", 
                        resp.StatusCode, errorResp.Message, errorResp.Details)
                }
                
                return nil, fmt.Errorf("API error (%d): %s", resp.StatusCode, respBody)
            }
            
            // Retry on server errors
            if resp.StatusCode >= 500 && retry < c.maxRetries {
                time.Sleep(time.Duration(retry+1) * time.Second)
                continue
            }
            
            return nil, fmt.Errorf("API error (%d) after %d retries", resp.StatusCode, retry)
        }
        
        // If we've gotten here, we have a successful response
        break
    }
    
    return respBody, nil
}

// Check and log rate limit information
func (c *SecureAPIClient) checkRateLimits(resp *http.Response) {
    limit := resp.Header.Get("X-RateLimit-Limit")
    remaining := resp.Header.Get("X-RateLimit-Remaining")
    reset := resp.Header.Get("X-RateLimit-Reset")
    
    if limit != "" && remaining != "" {
        log.Printf("Rate limits: %s/%s requests remaining", remaining, limit)
        
        // Alert if approaching limit
        remainingInt, _ := strconv.Atoi(remaining)
        limitInt, _ := strconv.Atoi(limit)
        
        if remainingInt < int(float64(limitInt) * 0.1) {
            log.Printf("WARNING: Approaching rate limit! Only %s/%s requests remaining", remaining, limit)
        }
    }
}

// Example usage
func main() {
    client := NewSecureAPIClient("https://api.rinna.io/api/v1", "ri-your-api-key")
    
    // List projects
    projectsData, err := client.Request("GET", "/projects", nil)
    if err != nil {
        log.Fatalf("Failed to list projects: %v", err)
    }
    
    var projects []map[string]interface{}
    if err := json.Unmarshal(projectsData, &projects); err != nil {
        log.Fatalf("Failed to parse projects: %v", err)
    }
    
    log.Printf("Found %d projects", len(projects))
    
    // Create a work item
    workItem := map[string]interface{}{
        "title":      "Fix login button on home page",
        "type":       "BUG",
        "priority":   "HIGH",
        "projectKey": "RINNA",
    }
    
    workItemData, err := client.Request("POST", "/workitems", workItem)
    if err != nil {
        log.Fatalf("Failed to create work item: %v", err)
    }
    
    var createdItem map[string]interface{}
    if err := json.Unmarshal(workItemData, &createdItem); err != nil {
        log.Fatalf("Failed to parse created work item: %v", err)
    }
    
    log.Printf("Created work item with ID: %s", createdItem["id"])
}
```

## Security Best Practices Summary

When integrating with the Rinna API, remember these key security practices:

1. **Always use HTTPS** for all API requests
2. **Protect your API keys** and never expose them in client-side code
3. **Implement proper rate limit handling** with exponential backoff
4. **Verify webhook signatures** to prevent tampering
5. **Include request IDs** for troubleshooting and tracing
6. **Monitor rate limits** to avoid service disruptions
7. **Implement proper error handling** for all API responses
8. **Keep dependencies updated** to avoid security vulnerabilities
9. **Validate all input and output** to prevent injection attacks
10. **Log securely** without exposing sensitive information

For a complete guide to API security, refer to the [API Security Guide](../api-security-guide.md).
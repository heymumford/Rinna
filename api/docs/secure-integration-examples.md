# Secure API Integration Examples

This document provides practical examples of securely integrating with the Rinna API using various programming languages and frameworks. These examples demonstrate best practices for authentication, error handling, rate limiting, and secure data exchange.

## Go Integration Examples

### Basic API Client with Security Features

```go
package main

import (
	"bytes"
	"context"
	"crypto/tls"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"strconv"
	"time"

	"github.com/google/uuid"
)

// SecureAPIClient provides a secure client for Rinna API
type SecureAPIClient struct {
	BaseURL    string
	APIKey     string
	HTTPClient *http.Client
}

// NewSecureAPIClient creates a new secure API client
func NewSecureAPIClient(baseURL, apiKey string) *SecureAPIClient {
	// Create transport with secure TLS configuration
	transport := &http.Transport{
		TLSClientConfig: &tls.Config{
			MinVersion: tls.VersionTLS12,
		},
		MaxIdleConns:       10,
		IdleConnTimeout:    30 * time.Second,
		DisableCompression: false,
	}

	// Create client with timeout
	client := &http.Client{
		Timeout:   10 * time.Second,
		Transport: transport,
	}

	return &SecureAPIClient{
		BaseURL:    baseURL,
		APIKey:     apiKey,
		HTTPClient: client,
	}
}

// GetWorkItem fetches a work item by ID with security and rate limit handling
func (c *SecureAPIClient) GetWorkItem(ctx context.Context, id string) (map[string]interface{}, error) {
	// Generate request ID for tracing
	requestID := uuid.New().String()

	// Create request with context
	url := fmt.Sprintf("%s/api/v1/workitems/%s", c.BaseURL, id)
	req, err := http.NewRequestWithContext(ctx, "GET", url, nil)
	if err != nil {
		return nil, fmt.Errorf("failed to create request: %w", err)
	}

	// Add security headers
	req.Header.Set("Authorization", "Bearer "+c.APIKey)
	req.Header.Set("X-Request-ID", requestID)
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("Accept", "application/json")

	// Execute request with retry logic for rate limiting
	var resp *http.Response
	maxRetries := 3
	
	for retries := 0; retries <= maxRetries; retries++ {
		resp, err = c.HTTPClient.Do(req)
		if err != nil {
			return nil, fmt.Errorf("request failed: %w", err)
		}

		// Handle rate limiting
		if resp.StatusCode == http.StatusTooManyRequests {
			resp.Body.Close()
			
			// Get retry after header
			retryAfter := 60 // Default to 60 seconds
			if ra := resp.Header.Get("Retry-After"); ra != "" {
				if seconds, err := strconv.Atoi(ra); err == nil {
					retryAfter = seconds
				}
			}
			
			// Calculate backoff with exponential increase
			backoff := time.Duration(retryAfter) * time.Second
			if retries > 0 {
				backoff = time.Duration(retryAfter * (1 << uint(retries))) * time.Second
			}
			
			// Log rate limiting
			fmt.Printf("Rate limited. Retrying in %v (attempt %d/%d)\n", 
				backoff, retries+1, maxRetries)
			
			select {
			case <-time.After(backoff):
				continue // Retry after backoff
			case <-ctx.Done():
				return nil, ctx.Err() // Context cancelled during wait
			}
		}
		
		// Break retry loop if not rate limited
		break
	}
	
	// Ensure body is closed after we're done
	defer resp.Body.Close()
	
	// Log rate limit info for monitoring
	if remaining := resp.Header.Get("X-RateLimit-Remaining"); remaining != "" {
		fmt.Printf("Rate limit remaining: %s\n", remaining)
	}

	// Handle non-successful responses
	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		// Read error body
		body, _ := io.ReadAll(resp.Body)
		
		return nil, fmt.Errorf("API request failed with status %d: %s", 
			resp.StatusCode, string(body))
	}

	// Parse response
	var result map[string]interface{}
	if err := json.NewDecoder(resp.Body).Decode(&result); err != nil {
		return nil, fmt.Errorf("failed to parse response: %w", err)
	}

	return result, nil
}

// CreateWorkItem creates a new work item with security features
func (c *SecureAPIClient) CreateWorkItem(ctx context.Context, item map[string]interface{}) (map[string]interface{}, error) {
	// Generate request ID for tracing
	requestID := uuid.New().String()

	// Marshal the data
	jsonData, err := json.Marshal(item)
	if err != nil {
		return nil, fmt.Errorf("failed to marshal request: %w", err)
	}

	// Create request with context
	url := fmt.Sprintf("%s/api/v1/workitems", c.BaseURL)
	req, err := http.NewRequestWithContext(ctx, "POST", url, bytes.NewBuffer(jsonData))
	if err != nil {
		return nil, fmt.Errorf("failed to create request: %w", err)
	}

	// Add security headers
	req.Header.Set("Authorization", "Bearer "+c.APIKey)
	req.Header.Set("X-Request-ID", requestID)
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("Accept", "application/json")

	// Execute request with retry logic for rate limiting
	var resp *http.Response
	maxRetries := 3
	
	for retries := 0; retries <= maxRetries; retries++ {
		resp, err = c.HTTPClient.Do(req)
		if err != nil {
			return nil, fmt.Errorf("request failed: %w", err)
		}

		// Handle rate limiting
		if resp.StatusCode == http.StatusTooManyRequests {
			resp.Body.Close()
			
			// Get retry after header
			retryAfter := 60 // Default to 60 seconds
			if ra := resp.Header.Get("Retry-After"); ra != "" {
				if seconds, err := strconv.Atoi(ra); err == nil {
					retryAfter = seconds
				}
			}
			
			// Calculate backoff with exponential increase
			backoff := time.Duration(retryAfter) * time.Second
			if retries > 0 {
				backoff = time.Duration(retryAfter * (1 << uint(retries))) * time.Second
			}
			
			select {
			case <-time.After(backoff):
				continue // Retry after backoff
			case <-ctx.Done():
				return nil, ctx.Err() // Context cancelled during wait
			}
		}
		
		// Break retry loop if not rate limited
		break
	}
	
	// Ensure body is closed after we're done
	defer resp.Body.Close()

	// Handle non-successful responses
	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		// Read error body
		body, _ := io.ReadAll(resp.Body)
		
		return nil, fmt.Errorf("API request failed with status %d: %s", 
			resp.StatusCode, string(body))
	}

	// Parse response
	var result map[string]interface{}
	if err := json.NewDecoder(resp.Body).Decode(&result); err != nil {
		return nil, fmt.Errorf("failed to parse response: %w", err)
	}

	return result, nil
}

// Example usage
func main() {
	// Create secure client
	client := NewSecureAPIClient("https://api.example.com", "your-api-key")
	
	// Create context with timeout
	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()
	
	// Get work item
	workItem, err := client.GetWorkItem(ctx, "WI-123")
	if err != nil {
		fmt.Printf("Error getting work item: %v\n", err)
		return
	}
	
	fmt.Printf("Retrieved work item: %v\n", workItem)
	
	// Create work item
	newItem := map[string]interface{}{
		"title":       "Fix security vulnerability",
		"description": "Address the XSS vulnerability in the login form",
		"type":        "BUG",
		"priority":    "HIGH",
		"projectKey":  "RINNA",
	}
	
	createdItem, err := client.CreateWorkItem(ctx, newItem)
	if err != nil {
		fmt.Printf("Error creating work item: %v\n", err)
		return
	}
	
	fmt.Printf("Created work item: %v\n", createdItem)
}
```

## Node.js Integration Examples

### Secure API Client with Axios

```javascript
const axios = require('axios');
const { v4: uuidv4 } = require('uuid');

class SecureApiClient {
  constructor(options) {
    this.baseURL = options.baseURL || 'https://api.rinna.io';
    this.apiKey = options.apiKey;
    
    // Create axios instance with security defaults
    this.client = axios.create({
      baseURL: this.baseURL,
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
      httpsAgent: new https.Agent({
        rejectUnauthorized: true, // Verify SSL certificates
        minVersion: 'TLSv1.2',    // Enforce TLS 1.2+
      }),
    });
    
    // Add request interceptor for authentication and tracing
    this.client.interceptors.request.use((config) => {
      // Add API key authentication
      config.headers['Authorization'] = `Bearer ${this.apiKey}`;
      
      // Add request ID for tracing
      config.headers['X-Request-ID'] = uuidv4();
      
      return config;
    });
    
    // Add response interceptor for rate limit handling
    this.client.interceptors.response.use(
      (response) => {
        // Log rate limit info
        const remaining = response.headers['x-ratelimit-remaining'];
        const limit = response.headers['x-ratelimit-limit'];
        if (remaining && limit) {
          console.debug(`Rate limits: ${remaining}/${limit} remaining`);
        }
        
        return response;
      },
      async (error) => {
        // Handle rate limiting with retries
        if (error.response && error.response.status === 429 && error.config && !error.config.__isRetry) {
          // Get retry time from header or default to 60 seconds
          const retryAfter = parseInt(error.response.headers['retry-after'] || '60', 10);
          console.warn(`Rate limited. Retrying after ${retryAfter} seconds...`);
          
          // Wait for the specified time
          await new Promise(resolve => setTimeout(resolve, retryAfter * 1000));
          
          // Mark this request as a retry and try again
          error.config.__isRetry = true;
          return this.client(error.config);
        }
        
        // Return error for other status codes
        return Promise.reject(error);
      }
    );
  }
  
  // Get a work item by ID
  async getWorkItem(id) {
    try {
      const response = await this.client.get(`/api/v1/workitems/${id}`);
      return response.data;
    } catch (error) {
      this._handleError(error);
    }
  }
  
  // Create a new work item
  async createWorkItem(workItem) {
    try {
      const response = await this.client.post('/api/v1/workitems', workItem);
      return response.data;
    } catch (error) {
      this._handleError(error);
    }
  }
  
  // Helper for consistent error handling
  _handleError(error) {
    if (error.response) {
      // Server responded with error
      const status = error.response.status;
      const data = error.response.data;
      const requestId = error.response.headers['x-request-id'];
      
      // Construct error with request details
      const apiError = new Error(`API Error ${status}: ${data.message || 'Unknown error'}`);
      apiError.status = status;
      apiError.data = data;
      apiError.requestId = requestId;
      
      throw apiError;
    } else if (error.request) {
      // Request was made but no response received
      throw new Error('No response received from API server');
    } else {
      // Error setting up the request
      throw error;
    }
  }
}

// Example usage
async function main() {
  const client = new SecureApiClient({
    baseURL: 'https://api.rinna.io',
    apiKey: 'your-api-key'
  });
  
  try {
    // Get work item
    const workItem = await client.getWorkItem('WI-123');
    console.log('Retrieved work item:', workItem);
    
    // Create work item
    const newItem = {
      title: 'Fix security vulnerability',
      description: 'Address the XSS vulnerability in the login form',
      type: 'BUG',
      priority: 'HIGH',
      projectKey: 'RINNA'
    };
    
    const createdItem = await client.createWorkItem(newItem);
    console.log('Created work item:', createdItem);
  } catch (error) {
    console.error('API Error:', error.message);
    if (error.requestId) {
      console.error('Request ID:', error.requestId);
    }
    if (error.data) {
      console.error('Error details:', error.data);
    }
  }
}

main();
```

### Secure Webhook Handler

```javascript
const express = require('express');
const crypto = require('crypto');
const bodyParser = require('body-parser');

const app = express();
const port = 3000;

// Webhook secret (store in environment variables in production)
const WEBHOOK_SECRET = process.env.WEBHOOK_SECRET || 'your-webhook-secret';

// Create raw body buffer for HMAC verification
app.use(bodyParser.json({
  verify: (req, res, buf) => {
    req.rawBody = buf;
  }
}));

// Middleware to verify GitHub webhook signatures
function verifyGitHubSignature(req, res, next) {
  const signature = req.headers['x-hub-signature-256'];
  
  if (!signature) {
    return res.status(401).send('No signature found');
  }
  
  // Create expected signature
  const hmac = crypto.createHmac('sha256', WEBHOOK_SECRET);
  const digest = 'sha256=' + hmac.update(req.rawBody).digest('hex');
  
  // Use timing-safe comparison
  if (!crypto.timingSafeEqual(Buffer.from(digest), Buffer.from(signature))) {
    return res.status(401).send('Invalid signature');
  }
  
  // Verify GitHub event
  if (!req.headers['x-github-event']) {
    return res.status(400).send('Missing event type');
  }
  
  // Verify uniqueness with delivery ID
  if (!req.headers['x-github-delivery']) {
    return res.status(400).send('Missing delivery ID');
  }
  
  next();
}

// GitHub webhook endpoint with verification
app.post('/webhooks/github', verifyGitHubSignature, (req, res) => {
  const event = req.headers['x-github-event'];
  const delivery = req.headers['x-github-delivery'];
  const payload = req.body;
  
  console.log(`Received GitHub ${event} event (${delivery})`);
  
  // Process the event based on type
  switch (event) {
    case 'push':
      handlePushEvent(payload);
      break;
      
    case 'pull_request':
      handlePullRequestEvent(payload);
      break;
      
    case 'issues':
      handleIssueEvent(payload);
      break;
      
    default:
      console.log(`Unhandled event type: ${event}`);
  }
  
  // Always return 200 quickly to acknowledge receipt
  res.status(200).send('Event received');
});

// Event handler functions
function handlePushEvent(payload) {
  const repo = payload.repository?.full_name;
  const branch = payload.ref?.replace('refs/heads/', '');
  console.log(`Push to ${repo}/${branch} by ${payload.pusher?.name}`);
  
  // Process commits
  const commits = payload.commits || [];
  commits.forEach(commit => {
    console.log(`Commit ${commit.id.substr(0, 7)}: ${commit.message}`);
    
    // Process work item references in commit message
    processWorkItemReferences(commit.message);
  });
}

function handlePullRequestEvent(payload) {
  const action = payload.action;
  const prNumber = payload.number;
  const repo = payload.repository?.full_name;
  
  console.log(`PR #${prNumber} ${action} in ${repo}`);
  
  // Process work item references in PR title and description
  if (payload.pull_request) {
    processWorkItemReferences(payload.pull_request.title);
    processWorkItemReferences(payload.pull_request.body);
  }
}

function handleIssueEvent(payload) {
  const action = payload.action;
  const issueNumber = payload.issue?.number;
  const repo = payload.repository?.full_name;
  
  console.log(`Issue #${issueNumber} ${action} in ${repo}`);
  
  // Process work item references in issue title and body
  if (payload.issue) {
    processWorkItemReferences(payload.issue.title);
    processWorkItemReferences(payload.issue.body);
  }
}

// Helper function to extract and process work item references
function processWorkItemReferences(text) {
  if (!text) return;
  
  // Look for patterns like WI-123 or RINNA-456
  const regex = /\b([A-Z]+-\d+)\b/g;
  const matches = text.match(regex) || [];
  
  if (matches.length > 0) {
    console.log(`Found work item references: ${matches.join(', ')}`);
    // Process each work item reference...
  }
}

// Start the server
app.listen(port, () => {
  console.log(`Webhook server running on port ${port}`);
});
```

## Python Integration Examples

### Secure API Client

```python
import json
import time
import uuid
import requests
from typing import Dict, Any, Optional, List, Union
from urllib3.util import Retry
from requests.adapters import HTTPAdapter


class SecureAPIClient:
    """Secure client for interacting with the Rinna API."""
    
    def __init__(
        self, 
        base_url: str, 
        api_key: str,
        timeout: int = 10,
        max_retries: int = 3
    ):
        """
        Initialize the secure API client.
        
        Args:
            base_url: Base URL of the API
            api_key: API key for authentication
            timeout: Request timeout in seconds
            max_retries: Maximum number of retries for failed requests
        """
        self.base_url = base_url.rstrip('/')
        self.api_key = api_key
        
        # Create session with security settings
        self.session = requests.Session()
        
        # Configure retry with backoff strategy
        retry_strategy = Retry(
            total=max_retries,
            backoff_factor=1,
            status_forcelist=[429, 500, 502, 503, 504],
            allowed_methods=["GET", "POST", "PUT", "DELETE", "PATCH"],
            respect_retry_after_header=True
        )
        
        adapter = HTTPAdapter(max_retries=retry_strategy)
        self.session.mount("https://", adapter)
        
        # Set default headers
        self.session.headers.update({
            "Content-Type": "application/json",
            "Accept": "application/json",
            "Authorization": f"Bearer {self.api_key}"
        })
        
        self.timeout = timeout
    
    def _handle_response(self, response: requests.Response) -> Dict[str, Any]:
        """
        Handle API response, checking for errors and rate limits.
        
        Args:
            response: Response object from requests
            
        Returns:
            Parsed JSON response
            
        Raises:
            Exception: If the response contains an error
        """
        # Check if rate limit headers are present
        if 'X-RateLimit-Remaining' in response.headers:
            remaining = response.headers['X-RateLimit-Remaining']
            limit = response.headers.get('X-RateLimit-Limit', 'unknown')
            print(f"Rate limit: {remaining}/{limit} remaining")
        
        # Check for error responses
        if not response.ok:
            error_data = {}
            try:
                error_data = response.json()
            except:
                error_data = {"message": response.text}
            
            request_id = response.headers.get('X-Request-ID', 'unknown')
            
            raise Exception(
                f"API error {response.status_code}: {error_data.get('message', 'Unknown error')} "
                f"(Request ID: {request_id})"
            )
        
        # Parse and return the response
        try:
            return response.json()
        except ValueError:
            # Return empty dict if response is empty
            return {}
    
    def get_work_item(self, item_id: str) -> Dict[str, Any]:
        """
        Get a work item by ID.
        
        Args:
            item_id: ID of the work item
            
        Returns:
            Work item data
        """
        # Generate request ID for traceability
        request_id = str(uuid.uuid4())
        
        # Make the request with request ID header
        response = self.session.get(
            f"{self.base_url}/api/v1/workitems/{item_id}",
            headers={"X-Request-ID": request_id},
            timeout=self.timeout
        )
        
        return self._handle_response(response)
    
    def create_work_item(self, work_item: Dict[str, Any]) -> Dict[str, Any]:
        """
        Create a new work item.
        
        Args:
            work_item: Work item data
            
        Returns:
            Created work item data
        """
        # Generate request ID for traceability
        request_id = str(uuid.uuid4())
        
        # Make the request with request ID header
        response = self.session.post(
            f"{self.base_url}/api/v1/workitems",
            json=work_item,
            headers={"X-Request-ID": request_id},
            timeout=self.timeout
        )
        
        return self._handle_response(response)
    
    def update_work_item(self, item_id: str, updates: Dict[str, Any]) -> Dict[str, Any]:
        """
        Update an existing work item.
        
        Args:
            item_id: ID of the work item
            updates: Updated work item data
            
        Returns:
            Updated work item data
        """
        # Generate request ID for traceability
        request_id = str(uuid.uuid4())
        
        # Make the request with request ID header
        response = self.session.put(
            f"{self.base_url}/api/v1/workitems/{item_id}",
            json=updates,
            headers={"X-Request-ID": request_id},
            timeout=self.timeout
        )
        
        return self._handle_response(response)
    
    def list_work_items(
        self, 
        project_key: Optional[str] = None, 
        status: Optional[str] = None,
        priority: Optional[str] = None,
        page: int = 1,
        size: int = 20
    ) -> List[Dict[str, Any]]:
        """
        List work items with optional filtering.
        
        Args:
            project_key: Filter by project key
            status: Filter by status
            priority: Filter by priority
            page: Page number for pagination
            size: Page size for pagination
            
        Returns:
            List of work items
        """
        # Generate request ID for traceability
        request_id = str(uuid.uuid4())
        
        # Build query parameters
        params = {"page": page, "size": size}
        if project_key:
            params["project"] = project_key
        if status:
            params["status"] = status
        if priority:
            params["priority"] = priority
        
        # Make the request with request ID header
        response = self.session.get(
            f"{self.base_url}/api/v1/workitems",
            params=params,
            headers={"X-Request-ID": request_id},
            timeout=self.timeout
        )
        
        return self._handle_response(response)


# Example usage
if __name__ == "__main__":
    # Create secure client
    client = SecureAPIClient(
        base_url="https://api.rinna.io",
        api_key="your-api-key"
    )
    
    try:
        # Get a work item
        work_item = client.get_work_item("WI-123")
        print(f"Retrieved work item: {work_item['id']}")
        
        # Create a new work item
        new_item = {
            "title": "Fix security vulnerability",
            "description": "Address the XSS vulnerability in the login form",
            "type": "BUG",
            "priority": "HIGH",
            "projectKey": "RINNA"
        }
        
        created_item = client.create_work_item(new_item)
        print(f"Created work item: {created_item['id']}")
        
        # List work items
        items = client.list_work_items(
            project_key="RINNA",
            status="IN_DEV",
            priority="HIGH"
        )
        print(f"Found {len(items)} work items")
        
    except Exception as e:
        print(f"Error: {str(e)}")
```

## Best Practices Summary

The examples above demonstrate these key security best practices:

1. **Authentication**: 
   - Use Bearer token authentication
   - Keep API keys secure
   - Never hard-code keys in source code

2. **Request Security**:
   - Use HTTPS only
   - Enforce TLS 1.2+
   - Set reasonable timeouts
   - Include request IDs for traceability
   - Use secure headers

3. **Rate Limiting**:
   - Monitor rate limit headers
   - Implement exponential backoff
   - Handle 429 responses gracefully
   - Respect Retry-After headers

4. **Data Validation**:
   - Validate request and response data
   - Use proper content types
   - Handle errors appropriately
   - Don't expose sensitive data in logs

5. **Webhook Security**:
   - Verify signature with HMAC
   - Use timing-safe comparison
   - Validate event sources
   - Implement nonce/replay protection
   - Process events asynchronously

By following these examples and best practices, you can ensure your integration with the Rinna API is secure, reliable, and performant.
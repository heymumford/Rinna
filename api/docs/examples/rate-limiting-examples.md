# Rate Limiting Examples

This document provides practical code examples for implementing rate limit handling when integrating with the Rinna API.

## Understanding Rate Limiting in Rinna's API

Rinna employs a token bucket rate limiting system with the following features:

- Each client has limits based on IP address, project, and endpoint
- Rate limits reset automatically every minute
- Different endpoints have different rate limit thresholds
- When a limit is exceeded, a `429 Too Many Requests` response is returned
- The `Retry-After` header indicates how long to wait before retrying

## Rate Limit Headers

Every API response from Rinna includes headers to help monitor your rate limit status:

| Header | Description | Example |
|--------|-------------|---------|
| `X-RateLimit-Limit` | Maximum number of requests allowed per minute | `300` |
| `X-RateLimit-Remaining` | Number of requests remaining in the current window | `295` |
| `X-RateLimit-Reset` | Unix timestamp when the rate limit will reset | `1614556800` |

When rate limited, responses also include:

| Header | Description | Example |
|--------|-------------|---------|
| `Retry-After` | Seconds to wait before retrying the request | `30` |

## Basic Rate Limit Handling

### JavaScript Example

```javascript
// JavaScript - Simple rate limit monitoring
async function fetchWithRateLimitMonitoring(url, options = {}) {
    try {
        const response = await fetch(url, {
            ...options,
            headers: {
                ...options.headers,
                'Authorization': 'Bearer ri-your-api-key',
                'Content-Type': 'application/json'
            }
        });
        
        // Monitor rate limits
        const rateLimit = {
            limit: response.headers.get('X-RateLimit-Limit'),
            remaining: response.headers.get('X-RateLimit-Remaining'),
            reset: response.headers.get('X-RateLimit-Reset')
        };
        
        console.log(`Rate limits: ${rateLimit.remaining}/${rateLimit.limit} remaining`);
        
        // Alert if approaching limit (less than 10% remaining)
        if (rateLimit.remaining && rateLimit.limit) {
            const remainingPercent = (parseInt(rateLimit.remaining) / parseInt(rateLimit.limit)) * 100;
            if (remainingPercent < 10) {
                console.warn(`Warning: Approaching rate limit! Only ${rateLimit.remaining} requests remaining`);
            }
        }
        
        // Check if rate limited
        if (response.status === 429) {
            const retryAfter = response.headers.get('Retry-After') || '60';
            console.warn(`Rate limited! Retry after ${retryAfter} seconds`);
            // Handle rate limiting here
        }
        
        return response;
    } catch (error) {
        console.error('Request failed:', error);
        throw error;
    }
}

// Usage
fetchWithRateLimitMonitoring('https://api.rinna.io/api/v1/projects')
    .then(response => response.json())
    .then(data => console.log('Projects:', data))
    .catch(error => console.error('Error:', error));
```

### Python Example

```python
import requests
import time

def fetch_with_rate_limit_monitoring(url, headers=None, method='GET', data=None):
    if headers is None:
        headers = {}
    
    # Add authentication
    headers['Authorization'] = 'Bearer ri-your-api-key'
    headers['Content-Type'] = 'application/json'
    
    # Make the request
    response = requests.request(method, url, headers=headers, json=data)
    
    # Monitor rate limits
    rate_limit = {
        'limit': response.headers.get('X-RateLimit-Limit'),
        'remaining': response.headers.get('X-RateLimit-Remaining'),
        'reset': response.headers.get('X-RateLimit-Reset')
    }
    
    print(f"Rate limits: {rate_limit['remaining']}/{rate_limit['limit']} remaining")
    
    # Alert if approaching limit
    if rate_limit['remaining'] and rate_limit['limit']:
        remaining_percent = (int(rate_limit['remaining']) / int(rate_limit['limit'])) * 100
        if remaining_percent < 10:
            print(f"Warning: Approaching rate limit! Only {rate_limit['remaining']} requests remaining")
    
    # Check if rate limited
    if response.status_code == 429:
        retry_after = int(response.headers.get('Retry-After', 60))
        print(f"Rate limited! Retry after {retry_after} seconds")
        # Handle rate limiting here
    
    return response

# Usage
response = fetch_with_rate_limit_monitoring('https://api.rinna.io/api/v1/projects')
if response.ok:
    projects = response.json()
    print(f"Found {len(projects)} projects")
```

## Implementing Exponential Backoff

### JavaScript Example

```javascript
// JavaScript - Fetch with exponential backoff
async function fetchWithBackoff(url, options = {}, maxRetries = 3) {
    let retries = 0;
    
    while (true) {
        try {
            const response = await fetch(url, {
                ...options,
                headers: {
                    ...options.headers,
                    'Authorization': 'Bearer ri-your-api-key',
                    'Content-Type': 'application/json'
                }
            });
            
            // Log rate limit info
            const rateLimit = {
                limit: response.headers.get('X-RateLimit-Limit'),
                remaining: response.headers.get('X-RateLimit-Remaining'),
                reset: response.headers.get('X-RateLimit-Reset')
            };
            
            console.log(`Rate limits: ${rateLimit.remaining}/${rateLimit.limit} remaining`);
            
            // Handle rate limiting
            if (response.status === 429) {
                if (retries >= maxRetries) {
                    throw new Error(`Rate limited after ${maxRetries} retries`);
                }
                
                // Get retry time from header or default to 60 seconds
                const retryAfter = parseInt(response.headers.get('Retry-After') || '60', 10);
                
                // Calculate exponential backoff with jitter
                const jitter = Math.random() * 0.3;
                const backoffTime = Math.min(
                    // Base time + exponential factor + jitter
                    (retryAfter * (Math.pow(2, retries))) * (1 + jitter),
                    // Maximum backoff cap of 5 minutes
                    300
                );
                
                console.warn(`Rate limited! Retrying in ${backoffTime.toFixed(1)} seconds (attempt ${retries + 1}/${maxRetries})`);
                
                // Wait before retrying
                await new Promise(resolve => setTimeout(resolve, backoffTime * 1000));
                
                retries++;
                continue;
            }
            
            return response;
        } catch (error) {
            if (retries >= maxRetries) {
                throw error;
            }
            
            // For network errors, also implement backoff
            const backoffTime = Math.pow(2, retries) + Math.random();
            console.warn(`Request failed. Retrying in ${backoffTime.toFixed(1)} seconds (attempt ${retries + 1}/${maxRetries})`);
            
            await new Promise(resolve => setTimeout(resolve, backoffTime * 1000));
            
            retries++;
        }
    }
}

// Example usage with async/await
async function getProjects() {
    try {
        const response = await fetchWithBackoff('https://api.rinna.io/api/v1/projects');
        
        if (!response.ok && response.status !== 429) {  // 429 already handled in fetchWithBackoff
            throw new Error(`API error: ${response.status}`);
        }
        
        const data = await response.json();
        return data;
    } catch (error) {
        console.error('Failed to get projects:', error);
        throw error;
    }
}
```

### Go Example

```go
package main

import (
    "encoding/json"
    "fmt"
    "io/ioutil"
    "math"
    "math/rand"
    "net/http"
    "strconv"
    "time"
)

// FetchWithBackoff makes a request with exponential backoff for rate limits
func FetchWithBackoff(url string, maxRetries int) ([]byte, error) {
    client := &http.Client{Timeout: 10 * time.Second}
    
    // Initialize random seed
    rand.Seed(time.Now().UnixNano())
    
    var resp *http.Response
    var err error
    var body []byte
    
    for retry := 0; retry <= maxRetries; retry++ {
        // Create request
        req, err := http.NewRequest("GET", url, nil)
        if err != nil {
            return nil, fmt.Errorf("error creating request: %w", err)
        }
        
        // Add API key using Bearer token format
        req.Header.Set("Authorization", "Bearer ri-your-api-key")
        req.Header.Set("Content-Type", "application/json")
        
        // Make request
        resp, err = client.Do(req)
        if err != nil {
            if retry < maxRetries {
                // Calculate backoff for network errors
                backoffTime := time.Duration(math.Pow(2, float64(retry)) + rand.Float64()) * time.Second
                fmt.Printf("Request failed. Retrying in %.1f seconds (attempt %d/%d)\n", 
                          backoffTime.Seconds(), retry+1, maxRetries)
                time.Sleep(backoffTime)
                continue
            }
            return nil, fmt.Errorf("request failed after %d retries: %w", maxRetries, err)
        }
        
        // Check and log rate limit information
        limit := resp.Header.Get("X-RateLimit-Limit")
        remaining := resp.Header.Get("X-RateLimit-Remaining")
        reset := resp.Header.Get("X-RateLimit-Reset")
        
        fmt.Printf("Rate limits: %s/%s requests remaining\n", remaining, limit)
        
        // Handle rate limiting
        if resp.StatusCode == 429 {
            resp.Body.Close()
            
            if retry >= maxRetries {
                return nil, fmt.Errorf("rate limited after %d retries", maxRetries)
            }
            
            // Get retry time from header or default to 60 seconds
            retryAfter := 60
            if ra := resp.Header.Get("Retry-After"); ra != "" {
                if raInt, err := strconv.Atoi(ra); err == nil {
                    retryAfter = raInt
                }
            }
            
            // Calculate exponential backoff with jitter
            jitter := rand.Float64() * 0.3
            backoffSeconds := math.Min(
                float64(retryAfter) * math.Pow(2, float64(retry)) * (1 + jitter),
                300, // Cap at 5 minutes
            )
            
            backoffTime := time.Duration(backoffSeconds * float64(time.Second))
            fmt.Printf("Rate limited! Retrying in %.1f seconds (attempt %d/%d)\n", 
                      backoffTime.Seconds(), retry+1, maxRetries)
            
            time.Sleep(backoffTime)
            continue
        }
        
        // Read response body
        defer resp.Body.Close()
        body, err = ioutil.ReadAll(resp.Body)
        if err != nil {
            if retry < maxRetries {
                backoffTime := time.Duration(math.Pow(2, float64(retry))) * time.Second
                fmt.Printf("Failed to read response. Retrying in %v seconds\n", backoffTime.Seconds())
                time.Sleep(backoffTime)
                continue
            }
            return nil, fmt.Errorf("failed to read response after %d retries: %w", maxRetries, err)
        }
        
        // Handle other error status codes
        if resp.StatusCode >= 400 && resp.StatusCode != 429 {
            if retry < maxRetries {
                // Only retry server errors
                if resp.StatusCode >= 500 {
                    backoffTime := time.Duration(math.Pow(2, float64(retry))) * time.Second
                    fmt.Printf("Server error: %d. Retrying in %v seconds\n", resp.StatusCode, backoffTime.Seconds())
                    time.Sleep(backoffTime)
                    continue
                }
            }
            
            return nil, fmt.Errorf("API error: %d - %s", resp.StatusCode, body)
        }
        
        // Successfully got a good response
        break
    }
    
    return body, nil
}

// Example usage
func GetProjects() ([]map[string]interface{}, error) {
    body, err := FetchWithBackoff("https://api.rinna.io/api/v1/projects", 3)
    if err != nil {
        return nil, err
    }
    
    var projects []map[string]interface{}
    if err := json.Unmarshal(body, &projects); err != nil {
        return nil, fmt.Errorf("failed to parse projects: %w", err)
    }
    
    return projects, nil
}
```

## Request Throttling and Queuing

For high-volume applications, implementing a request queue with throttling helps to stay within rate limits:

### JavaScript Rate Limiting Queue

```javascript
// JavaScript - Rate limiting queue
class RateLimitQueue {
    constructor(options = {}) {
        this.requestsPerMinute = options.requestsPerMinute || 250; // Slightly below the limit
        this.interval = options.interval || 60000; // 1 minute in milliseconds
        this.minimumGap = options.minimumGap || 50; // Minimum ms between requests
        
        this.queue = [];
        this.inProgress = 0;
        this.requestsMade = 0;
        this.lastRequestTime = 0;
        
        // Reset counter every interval
        setInterval(() => {
            this.requestsMade = 0;
        }, this.interval);
    }
    
    // Add a request to the queue
    enqueue(requestFn) {
        return new Promise((resolve, reject) => {
            this.queue.push({
                requestFn,
                resolve,
                reject
            });
            
            this.processQueue();
        });
    }
    
    // Process the queue
    async processQueue() {
        if (this.queue.length === 0 || this.shouldThrottle()) {
            return;
        }
        
        // Calculate time to wait before making next request
        const now = Date.now();
        const timeSinceLastRequest = now - this.lastRequestTime;
        const timeToWait = Math.max(0, this.minimumGap - timeSinceLastRequest);
        
        if (timeToWait > 0) {
            await new Promise(resolve => setTimeout(resolve, timeToWait));
        }
        
        // Get the next request from the queue
        const { requestFn, resolve, reject } = this.queue.shift();
        
        this.inProgress++;
        this.lastRequestTime = Date.now();
        this.requestsMade++;
        
        try {
            const result = await requestFn();
            resolve(result);
        } catch (error) {
            reject(error);
        } finally {
            this.inProgress--;
            this.processQueue();
        }
    }
    
    // Check if we should throttle requests
    shouldThrottle() {
        // If we've made too many requests this interval, throttle
        if (this.requestsMade >= this.requestsPerMinute) {
            return true;
        }
        
        return false;
    }
}

// Usage example
const rateLimitQueue = new RateLimitQueue({ requestsPerMinute: 250 });

async function fetchWithQueue(url, options = {}) {
    return rateLimitQueue.enqueue(async () => {
        const response = await fetch(url, {
            ...options,
            headers: {
                ...options.headers,
                'Authorization': 'Bearer ri-your-api-key',
                'Content-Type': 'application/json'
            }
        });
        
        // Handle rate limits (this shouldn't happen often with the queue)
        if (response.status === 429) {
            const retryAfter = parseInt(response.headers.get('Retry-After') || '60', 10);
            throw new Error(`Rate limited! Retry after ${retryAfter} seconds`);
        }
        
        return response;
    });
}

// Make multiple requests that will be automatically throttled
async function fetchMultipleItems() {
    const itemIds = ['item1', 'item2', 'item3', 'item4', 'item5'];
    
    const promises = itemIds.map(id => 
        fetchWithQueue(`https://api.rinna.io/api/v1/workitems/${id}`)
            .then(response => response.json())
    );
    
    return Promise.all(promises);
}
```

## Batch Requests to Reduce API Calls

Instead of making many individual API calls, use batch operations to reduce the number of requests and stay within rate limits:

```javascript
// JavaScript - Using batch operations
async function fetchBatchWorkItems(itemIds) {
    // Convert individual requests to a single batch request
    const response = await fetch('https://api.rinna.io/api/v1/workitems/batch', {
        method: 'POST',
        headers: {
            'Authorization': 'Bearer ri-your-api-key',
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            ids: itemIds
        })
    });
    
    if (!response.ok) {
        const error = await response.json();
        throw new Error(`Batch request failed: ${error.message}`);
    }
    
    return response.json();
}

// Usage: one request instead of many
const workItems = await fetchBatchWorkItems(['item1', 'item2', 'item3', 'item4', 'item5']);
```

## Client-Side Rate Limiting Strategy

For client applications that need to avoid rate limiting entirely:

```javascript
// JavaScript - Client-side limiter with localStorage persistence
class ClientRateLimiter {
    constructor(options = {}) {
        this.storageKey = options.storageKey || 'rinna_api_rate_limit';
        this.limitPerMinute = options.limitPerMinute || 250;
        this.clearExpiredInterval = options.clearExpiredInterval || 5000; // 5 seconds
        
        // Start the interval to periodically clean up expired timestamps
        this.startClearExpiredInterval();
    }
    
    // Check if the request should be allowed
    canMakeRequest() {
        const now = Date.now();
        const oneMinuteAgo = now - 60000;
        
        // Get stored request timestamps
        const requestTimestamps = this.getRequestTimestamps();
        
        // Filter to only include timestamps from the last minute
        const recentTimestamps = requestTimestamps.filter(timestamp => timestamp > oneMinuteAgo);
        
        // If we haven't hit the limit, allow the request
        return recentTimestamps.length < this.limitPerMinute;
    }
    
    // Track that a request was made
    trackRequest() {
        const now = Date.now();
        const timestamps = this.getRequestTimestamps();
        
        // Add current timestamp
        timestamps.push(now);
        
        // Save updated timestamps
        this.saveRequestTimestamps(timestamps);
        
        return timestamps.length;
    }
    
    // Get request timestamps from storage
    getRequestTimestamps() {
        try {
            const stored = localStorage.getItem(this.storageKey);
            return stored ? JSON.parse(stored) : [];
        } catch (error) {
            console.error('Error retrieving request timestamps:', error);
            return [];
        }
    }
    
    // Save request timestamps to storage
    saveRequestTimestamps(timestamps) {
        try {
            localStorage.setItem(this.storageKey, JSON.stringify(timestamps));
        } catch (error) {
            console.error('Error saving request timestamps:', error);
        }
    }
    
    // Clear expired timestamps
    clearExpiredTimestamps() {
        const now = Date.now();
        const oneMinuteAgo = now - 60000;
        
        const timestamps = this.getRequestTimestamps();
        const validTimestamps = timestamps.filter(timestamp => timestamp > oneMinuteAgo);
        
        if (validTimestamps.length !== timestamps.length) {
            this.saveRequestTimestamps(validTimestamps);
        }
    }
    
    // Start the interval to clear expired timestamps
    startClearExpiredInterval() {
        this.clearIntervalId = setInterval(() => {
            this.clearExpiredTimestamps();
        }, this.clearExpiredInterval);
    }
    
    // Stop the interval
    stopClearExpiredInterval() {
        if (this.clearIntervalId) {
            clearInterval(this.clearIntervalId);
        }
    }
}

// Usage example
const rateLimiter = new ClientRateLimiter();

async function fetchWithClientLimiting(url, options = {}) {
    // Check if we can make the request
    if (!rateLimiter.canMakeRequest()) {
        throw new Error('Rate limit exceeded. Please try again later.');
    }
    
    // Track the request
    rateLimiter.trackRequest();
    
    // Make the request
    return fetch(url, {
        ...options,
        headers: {
            ...options.headers,
            'Authorization': 'Bearer ri-your-api-key',
            'Content-Type': 'application/json'
        }
    });
}
```

## Testing Rate Limiting Behavior

Rinna provides a test endpoint that always returns a 429 response for testing your rate limit handling:

```javascript
// JavaScript - Testing rate limit handling
async function testRateLimitHandling() {
    try {
        console.log('Testing rate limit handling...');
        
        const response = await fetchWithBackoff('https://api.rinna.io/api/v1/test/rate-limit', {}, 5);
        
        console.log('This should not be reached because the test endpoint always returns 429');
        return response;
    } catch (error) {
        console.log('Expected error:', error.message);
        console.log('Rate limit handling test complete');
    }
}
```

## Best Practices for Rate Limit Handling

1. **Monitor rate limit headers** in every response
2. **Implement exponential backoff** with jitter for retries
3. **Add request queuing** for high-volume applications
4. **Use batch operations** when possible to reduce request count
5. **Add alerting** when consistently approaching rate limits
6. **Consider client-side rate limiting** to prevent hitting server limits
7. **Use a circuit breaker pattern** to pause requests when repeatedly rate limited
8. **Add logging and metrics** for rate limit events
9. **Test your rate limit handling** using the test endpoint
10. **Distribute load** evenly over time rather than in bursts

For more details on Rinna's rate limiting, see the [Rate Limiting Documentation](../rate-limiting.md).
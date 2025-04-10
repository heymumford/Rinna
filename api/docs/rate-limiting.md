# Rinna API Rate Limiting

The Rinna API implements comprehensive rate limiting to ensure fair usage, maintain API stability, and protect against abuse. This document explains how rate limiting works and provides best practices for handling rate limits in your application.

## Rate Limit Overview

Rate limits in the Rinna API are based on the following factors:

1. **Client IP Address**: Each client IP has base rate limits
2. **Project-specific Limits**: Different projects may have different rate limits
3. **Endpoint-specific Limits**: Some endpoints may have custom rate limits
4. **Authentication Level**: Authenticated requests may have higher limits than unauthenticated ones

Rate limits are calculated per minute and reset automatically at the start of each minute window.

## Rate Limiting Headers

All API responses include the following headers to help you monitor your rate limit status:

| Header | Description | Example |
|--------|-------------|---------|
| `X-RateLimit-Limit` | Maximum number of requests allowed per minute | `300` |
| `X-RateLimit-Remaining` | Number of requests remaining in the current window | `297` |
| `X-RateLimit-Reset` | Unix timestamp when the rate limit will reset | `1614556800` |

When a rate limit is exceeded, the API returns a `429 Too Many Requests` response with an additional header:

| Header | Description | Example |
|--------|-------------|---------|
| `Retry-After` | Seconds to wait before retrying the request | `60` |

## Default Rate Limits

The following are the default rate limits for different types of operations:

| Request Type | Limit (per minute) |
|--------------|-----------------|
| Standard API calls | 300 |
| Webhook API calls | 60 |
| Authentication operations | 20 |
| Search operations | 60 |
| Bulk operations | 30 |

## Custom Rate Limits

In some cases, you may need higher rate limits for your application. Custom rate limits can be configured for:

- Specific IP addresses or ranges
- Specific projects
- Specific API endpoints
- Specific user accounts

To request custom rate limits, please contact your account manager.

## IP-based Whitelisting

For trusted services, we offer IP-based whitelisting which can bypass or increase certain rate limits. This is particularly useful for:

- Internal services
- Continuous integration systems
- Partner integrations

Requests from whitelisted IPs will receive a special header: `X-RateLimit-Whitelisted: true`

## Handling Rate Limiting

### Best Practices

1. **Monitor Rate Limit Headers**: Track the `X-RateLimit-Remaining` header to avoid hitting limits

2. **Implement Backoff Strategy**: When rate limited, use an exponential backoff strategy:

   ```javascript
   function fetchWithRetry(url, options, maxRetries = 3) {
     let retries = 0;
     
     return new Promise((resolve, reject) => {
       function attempt() {
         fetch(url, options)
           .then(response => {
             // Check if rate limited
             if (response.status === 429) {
               const retryAfter = parseInt(response.headers.get('Retry-After') || '60', 10);
               const delay = Math.min(retryAfter * 1000, (2 ** retries) * 1000);
               
               console.log(`Rate limited. Retrying in ${delay/1000} seconds...`);
               
               if (retries < maxRetries) {
                 retries++;
                 setTimeout(attempt, delay);
               } else {
                 reject(new Error('Maximum retries exceeded'));
               }
             } else {
               resolve(response);
             }
           })
           .catch(reject);
       }
       
       attempt();
     });
   }
   ```

3. **Batch Requests When Possible**: Use bulk endpoints to reduce the number of API calls

4. **Distribute Load**: Spread requests over time rather than sending in bursts

5. **Cache Responses**: Cache responses when appropriate to reduce API calls

### Handling 429 Responses

When you receive a 429 response, your application should:

1. Read the `Retry-After` header
2. Wait at least the specified number of seconds
3. Retry the request
4. Implement exponential backoff for repeated failures

Example in Go:

```go
func requestWithRateLimitHandling(url string, maxRetries int) ([]byte, error) {
    var resp *http.Response
    var err error
    
    client := &http.Client{Timeout: 10 * time.Second}
    req, err := http.NewRequest("GET", url, nil)
    if err != nil {
        return nil, err
    }
    
    req.Header.Set("Authorization", "Bearer YOUR_API_KEY")
    
    for retries := 0; retries <= maxRetries; retries++ {
        resp, err = client.Do(req)
        if err != nil {
            return nil, err
        }
        
        // Check if rate limited
        if resp.StatusCode == 429 {
            resp.Body.Close()
            
            // Get retry after header
            retryAfter := 60 // Default to 60 seconds
            if ra := resp.Header.Get("Retry-After"); ra != "" {
                if raInt, err := strconv.Atoi(ra); err == nil {
                    retryAfter = raInt
                }
            }
            
            // Add exponential backoff
            delay := time.Duration(retryAfter) * time.Second
            if retries > 0 {
                delay = time.Duration(retryAfter * (1 << retries)) * time.Second
            }
            
            fmt.Printf("Rate limited. Retrying in %v...\n", delay)
            time.Sleep(delay)
            continue
        }
        
        // If we get here, we got a non-429 response
        defer resp.Body.Close()
        return ioutil.ReadAll(resp.Body)
    }
    
    return nil, fmt.Errorf("exceeded maximum retries for rate limiting")
}
```

## Monitoring Rate Limits

To help monitor your usage and avoid rate limit issues, we recommend:

- Setting up alerts when your applications approach rate limits
- Logging rate limit headers for monitoring and analysis
- Using our dashboard to visualize your API usage patterns

## Rate Limit Errors

When a rate limit is exceeded, you'll receive a 429 response with a JSON error body:

```json
{
  "code": 429,
  "message": "Rate limit exceeded",
  "details": ["Too many requests"],
  "request_id": "01F3Z4T5J7A8V9X1Y2Z3",
  "timestamp": "2025-04-09T15:42:12Z"
}
```

## Support and Assistance

If you're experiencing rate limit issues or need higher limits, please contact support with:

1. Your project ID
2. The specific endpoints you're calling
3. Your use case and volume requirements

## Frequently Asked Questions

### Are rate limits shared across all applications using the same API key?

Yes. Rate limits are calculated based on the API key, so all applications using the same key share the limit.

### Do webhook deliveries count toward API rate limits?

No. Webhook deliveries have separate rate limits from standard API calls.

### What happens if I repeatedly exceed rate limits?

Consistently exceeding rate limits may trigger automatic restrictions or account review. Implement proper rate limit handling to avoid this.

### How can I test rate limiting behavior?

You can use the `/test/rate-limit` endpoint which always returns a 429 response for testing your handling logic.

### Are there any endpoints exempt from rate limiting?

Health check and status endpoints (`/health`, `/health/live`, `/health/ready`) are exempt from rate limiting.
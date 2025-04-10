# Webhook Security Examples

This document provides detailed examples for implementing secure webhook handlers for different provider integrations with the Rinna API.

## Webhook Security Overview

Webhooks are HTTP callbacks that allow external services to notify your application when certain events occur. Since webhooks are publicly accessible endpoints, proper security measures are essential to verify that incoming requests are legitimate and not from malicious sources.

Key security features to implement for webhooks:

1. **Signature Verification** - Verify payloads are authentic and unmodified
2. **Nonce Checking** - Prevent replay attacks by verifying delivery IDs
3. **IP Validation** - Restrict webhook access to known IP ranges
4. **Proper Error Handling** - Avoid exposing sensitive information in error responses

## GitHub Webhook Integration

GitHub uses HMAC-SHA256 signatures to verify webhook payloads:

```go
// Go Example - GitHub Webhook Handler
package main

import (
    "crypto/hmac"
    "crypto/sha256"
    "encoding/hex"
    "encoding/json"
    "fmt"
    "io/ioutil"
    "log"
    "net/http"
    "strings"
)

// Set your webhook secret
const webhookSecret = "your-webhook-secret"

// DeliveryIDs keeps track of processed webhook deliveries to prevent replay attacks
var deliveryIDs = make(map[string]bool)

func githubWebhookHandler(w http.ResponseWriter, r *http.Request) {
    // 1. Read the request body
    body, err := ioutil.ReadAll(r.Body)
    if err != nil {
        log.Printf("Error reading request body: %v", err)
        http.Error(w, "Failed to read request body", http.StatusBadRequest)
        return
    }
    
    // 2. Get and validate headers
    signature := r.Header.Get("X-Hub-Signature-256")
    event := r.Header.Get("X-GitHub-Event")
    deliveryID := r.Header.Get("X-GitHub-Delivery")
    
    // 3. Verify all required headers are present
    if signature == "" || event == "" || deliveryID == "" {
        log.Printf("Missing required headers: signature=%s, event=%s, deliveryID=%s", 
            signature, event, deliveryID)
        http.Error(w, "Missing required headers", http.StatusBadRequest)
        return
    }
    
    // 4. Verify this isn't a replay attack by checking the delivery ID
    if deliveryIDs[deliveryID] {
        log.Printf("Duplicate delivery ID detected: %s", deliveryID)
        http.Error(w, "Duplicate delivery ID", http.StatusBadRequest)
        return
    }
    
    // 5. Verify signature
    if !verifyGitHubSignature(body, signature, webhookSecret) {
        log.Printf("Invalid signature: %s", signature)
        http.Error(w, "Invalid signature", http.StatusUnauthorized)
        return
    }
    
    // 6. Record the delivery ID to prevent replays
    deliveryIDs[deliveryID] = true
    
    // 7. Parse the payload
    var payload map[string]interface{}
    if err := json.Unmarshal(body, &payload); err != nil {
        log.Printf("Error parsing JSON payload: %v", err)
        http.Error(w, "Invalid JSON payload", http.StatusBadRequest)
        return
    }
    
    // 8. Process the event based on type
    switch event {
    case "push":
        handlePushEvent(payload)
    case "pull_request":
        handlePullRequestEvent(payload)
    case "issues":
        handleIssueEvent(payload)
    default:
        log.Printf("Received unhandled event type: %s", event)
    }
    
    // 9. Return success response
    w.WriteHeader(http.StatusOK)
    fmt.Fprintf(w, "Webhook processed successfully")
}

// Verify the GitHub webhook signature
func verifyGitHubSignature(payload []byte, signature, secret string) bool {
    const signaturePrefix = "sha256="
    
    // Verify signature format
    if !strings.HasPrefix(signature, signaturePrefix) {
        return false
    }
    
    // Extract actual signature
    signature = signature[len(signaturePrefix):]
    
    // Calculate expected signature
    mac := hmac.New(sha256.New, []byte(secret))
    mac.Write(payload)
    expectedMAC := mac.Sum(nil)
    expectedSignature := hex.EncodeToString(expectedMAC)
    
    // Compare signatures using constant-time comparison
    return hmac.Equal([]byte(signature), []byte(expectedSignature))
}

func handlePushEvent(payload map[string]interface{}) {
    // Extract information and process push event
    log.Println("Processing push event")
    
    // Access repository information
    if repo, ok := payload["repository"].(map[string]interface{}); ok {
        repoName := repo["full_name"]
        log.Printf("Repository: %s", repoName)
    }
    
    // Process commits
    if commits, ok := payload["commits"].([]interface{}); ok {
        log.Printf("Number of commits: %d", len(commits))
        
        for i, commit := range commits {
            if commitObj, ok := commit.(map[string]interface{}); ok {
                message := commitObj["message"]
                author := commitObj["author"].(map[string]interface{})["name"]
                log.Printf("Commit %d: \"%s\" by %s", i+1, message, author)
            }
        }
    }
}

func handlePullRequestEvent(payload map[string]interface{}) {
    // Extract information and process pull request event
    log.Println("Processing pull request event")
    
    if action, ok := payload["action"].(string); ok {
        log.Printf("Action: %s", action)
    }
    
    if pr, ok := payload["pull_request"].(map[string]interface{}); ok {
        prNumber := pr["number"]
        prTitle := pr["title"]
        log.Printf("PR #%v: %s", prNumber, prTitle)
    }
}

func handleIssueEvent(payload map[string]interface{}) {
    // Extract information and process issue event
    log.Println("Processing issue event")
    
    if action, ok := payload["action"].(string); ok {
        log.Printf("Action: %s", action)
    }
    
    if issue, ok := payload["issue"].(map[string]interface{}); ok {
        issueNumber := issue["number"]
        issueTitle := issue["title"]
        log.Printf("Issue #%v: %s", issueNumber, issueTitle)
    }
}
```

## GitLab Webhook Integration

GitLab uses a token-based verification system:

```javascript
// Node.js Example - GitLab Webhook Handler
const express = require('express');
const bodyParser = require('body-parser');
const crypto = require('crypto');

const app = express();

// Set your GitLab webhook token
const GITLAB_SECRET_TOKEN = 'your-gitlab-webhook-token';

// Configure Express to parse JSON bodies
app.use(bodyParser.json());

// Store processed delivery IDs to prevent replay attacks
const processedDeliveryIds = new Set();

// GitLab webhook handler
app.post('/webhooks/gitlab', (req, res) => {
    try {
        // 1. Get and validate headers
        const token = req.header('X-Gitlab-Token');
        const event = req.header('X-Gitlab-Event');
        const deliveryId = req.header('X-Gitlab-Event-UUID');
        
        // 2. Log the incoming webhook for debugging
        console.log(`Received GitLab webhook: ${event}, ID: ${deliveryId}`);
        
        // 3. Verify required headers
        if (!token || !event) {
            console.error('Missing required headers');
            return res.status(400).json({ error: 'Missing required headers' });
        }
        
        // 4. Verify token using constant-time comparison
        const isValidToken = crypto.timingSafeEqual(
            Buffer.from(token),
            Buffer.from(GITLAB_SECRET_TOKEN)
        );
        
        if (!isValidToken) {
            console.error('Invalid webhook token');
            return res.status(401).json({ error: 'Invalid webhook token' });
        }
        
        // 5. Check for replay attacks if delivery ID is present
        if (deliveryId) {
            if (processedDeliveryIds.has(deliveryId)) {
                console.error(`Duplicate delivery ID: ${deliveryId}`);
                return res.status(400).json({ error: 'Duplicate delivery ID' });
            }
            
            // Add to processed IDs
            processedDeliveryIds.add(deliveryId);
            
            // Periodically clean up old delivery IDs (in a real implementation)
            // This is simplified for example purposes
            setTimeout(() => {
                processedDeliveryIds.delete(deliveryId);
            }, 24 * 60 * 60 * 1000); // Remove after 24 hours
        }
        
        // 6. Process based on event type
        const payload = req.body;
        
        switch (event) {
            case 'Push Hook':
                handlePushEvent(payload);
                break;
            case 'Merge Request Hook':
                handleMergeRequestEvent(payload);
                break;
            case 'Issue Hook':
                handleIssueEvent(payload);
                break;
            default:
                console.log(`Unhandled event type: ${event}`);
        }
        
        // 7. Return success response
        res.status(200).json({ status: 'success' });
    } catch (error) {
        console.error('Error processing GitLab webhook:', error);
        
        // Return a generic error response to avoid leaking sensitive information
        res.status(500).json({ error: 'Internal server error' });
    }
});

function handlePushEvent(payload) {
    console.log('Processing GitLab push event');
    
    const projectName = payload.project?.name || 'Unknown project';
    const branch = payload.ref?.replace('refs/heads/', '') || 'Unknown branch';
    const commitCount = payload.total_commits_count || 0;
    
    console.log(`Push to ${projectName}/${branch} with ${commitCount} commits`);
    
    // Process commits
    if (payload.commits && Array.isArray(payload.commits)) {
        payload.commits.forEach((commit, index) => {
            console.log(`Commit ${index + 1}: "${commit.message}" by ${commit.author.name}`);
        });
    }
}

function handleMergeRequestEvent(payload) {
    console.log('Processing GitLab merge request event');
    
    const action = payload.object_attributes?.action || 'unknown';
    const mrId = payload.object_attributes?.iid || 'unknown';
    const title = payload.object_attributes?.title || 'unknown';
    const state = payload.object_attributes?.state || 'unknown';
    
    console.log(`Merge request #${mrId} "${title}" ${action} (${state})`);
}

function handleIssueEvent(payload) {
    console.log('Processing GitLab issue event');
    
    const action = payload.object_attributes?.action || 'unknown';
    const issueId = payload.object_attributes?.iid || 'unknown';
    const title = payload.object_attributes?.title || 'unknown';
    
    console.log(`Issue #${issueId} "${title}" ${action}`);
}

// Start server
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`GitLab webhook server listening on port ${PORT}`);
});
```

## Bitbucket Webhook Integration

Bitbucket uses HMAC-SHA1 signatures for webhook verification:

```python
# Python Example - Bitbucket Webhook Handler
from flask import Flask, request, jsonify
import hmac
import hashlib
import json
import time

app = Flask(__name__)

# Set your Bitbucket webhook secret
BITBUCKET_SECRET = b'your-bitbucket-webhook-secret'

# Store processed request UUIDs to prevent replay attacks
processed_uuids = {}
UUID_EXPIRATION = 86400  # 24 hours in seconds

@app.route('/webhooks/bitbucket', methods=['POST'])
def bitbucket_webhook():
    try:
        # 1. Get and validate headers
        signature = request.headers.get('X-Hub-Signature')
        event_key = request.headers.get('X-Event-Key')
        request_uuid = request.headers.get('X-Request-UUID')
        
        # 2. Log the incoming webhook for debugging
        print(f"Received Bitbucket webhook: {event_key}, UUID: {request_uuid}")
        
        # 3. Verify required headers
        if not signature or not event_key:
            print("Missing required headers")
            return jsonify({"error": "Missing required headers"}), 400
        
        # 4. Get the raw payload for signature verification
        payload = request.get_data()
        
        # 5. Verify signature
        if not verify_bitbucket_signature(payload, signature):
            print(f"Invalid signature: {signature}")
            return jsonify({"error": "Invalid signature"}), 401
        
        # 6. Check for replay attacks if UUID is present
        if request_uuid:
            current_time = int(time.time())
            
            # Check if we've seen this UUID before
            if request_uuid in processed_uuids:
                print(f"Duplicate request UUID: {request_uuid}")
                return jsonify({"error": "Duplicate request UUID"}), 400
            
            # Add to processed UUIDs with expiration
            processed_uuids[request_uuid] = current_time
            
            # Clean up old UUIDs (in a real implementation you'd use a background task)
            clean_old_uuids()
        
        # 7. Parse the payload
        payload_data = json.loads(payload)
        
        # 8. Process based on event type
        if event_key.startswith('repo:push'):
            handle_push_event(payload_data)
        elif event_key.startswith('pullrequest:'):
            handle_pull_request_event(payload_data, event_key)
        elif event_key.startswith('issue:'):
            handle_issue_event(payload_data, event_key)
        else:
            print(f"Unhandled event type: {event_key}")
        
        # 9. Return success response
        return jsonify({"status": "success"}), 200
        
    except Exception as e:
        print(f"Error processing Bitbucket webhook: {str(e)}")
        return jsonify({"error": "Internal server error"}), 500

def verify_bitbucket_signature(payload, signature):
    """Verify the Bitbucket webhook signature"""
    # Bitbucket signatures should start with 'sha1='
    if not signature.startswith('sha1='):
        return False
    
    # Extract the signature part
    signature = signature[5:]
    
    # Calculate expected signature
    mac = hmac.new(BITBUCKET_SECRET, msg=payload, digestmod=hashlib.sha1)
    expected_signature = mac.hexdigest()
    
    # Compare signatures using constant-time comparison
    return hmac.compare_digest(signature, expected_signature)

def clean_old_uuids():
    """Clean up expired UUIDs"""
    current_time = int(time.time())
    expired_uuids = []
    
    for uuid, timestamp in processed_uuids.items():
        if current_time - timestamp > UUID_EXPIRATION:
            expired_uuids.append(uuid)
    
    for uuid in expired_uuids:
        del processed_uuids[uuid]

def handle_push_event(payload):
    """Handle Bitbucket push event"""
    print("Processing Bitbucket push event")
    
    try:
        repository = payload.get('repository', {})
        repo_name = repository.get('full_name', 'Unknown')
        
        changes = payload.get('push', {}).get('changes', [])
        for change in changes:
            ref = change.get('ref', {})
            branch = ref.get('name', 'Unknown') if ref else 'Unknown'
            commits = change.get('commits', [])
            
            print(f"Push to {repo_name}/{branch} with {len(commits)} commits")
            
            for commit in commits:
                message = commit.get('message', 'No message')
                author = commit.get('author', {}).get('raw', 'Unknown')
                print(f"Commit: \"{message}\" by {author}")
    
    except Exception as e:
        print(f"Error processing push event: {str(e)}")

def handle_pull_request_event(payload, event_key):
    """Handle Bitbucket pull request event"""
    print(f"Processing Bitbucket pull request event: {event_key}")
    
    try:
        # Extract the action from the event key (e.g., 'pullrequest:created')
        action = event_key.split(':')[1] if ':' in event_key else 'unknown'
        
        pr = payload.get('pullrequest', {})
        pr_id = pr.get('id', 'unknown')
        title = pr.get('title', 'unknown')
        
        print(f"Pull request #{pr_id} \"{title}\" {action}")
    
    except Exception as e:
        print(f"Error processing pull request event: {str(e)}")

def handle_issue_event(payload, event_key):
    """Handle Bitbucket issue event"""
    print(f"Processing Bitbucket issue event: {event_key}")
    
    try:
        # Extract the action from the event key (e.g., 'issue:created')
        action = event_key.split(':')[1] if ':' in event_key else 'unknown'
        
        issue = payload.get('issue', {})
        issue_id = issue.get('id', 'unknown')
        title = issue.get('title', 'unknown')
        
        print(f"Issue #{issue_id} \"{title}\" {action}")
    
    except Exception as e:
        print(f"Error processing issue event: {str(e)}")

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
```

## Custom Webhook Integration

For custom webhook implementations using Rinna's webhook security model:

```go
// Go Example - Custom Webhook Handler
package main

import (
    "crypto/hmac"
    "crypto/sha256"
    "encoding/hex"
    "encoding/json"
    "fmt"
    "io/ioutil"
    "log"
    "net"
    "net/http"
    "strings"
    "time"
)

// Custom webhook configuration
type WebhookConfig struct {
    Secret       string   `json:"secret"`
    AllowedIPs   []string `json:"allowed_ips"`
    RateLimit    int      `json:"rate_limit"`
    SecurityMode string   `json:"security_mode"` // "standard" or "strict"
}

// Store processed event IDs (used for nonce verification)
var processedEventIDs = make(map[string]time.Time)

// Configurations for different webhook sources
var webhookConfigs = map[string]WebhookConfig{
    "custom": {
        Secret:       "your-custom-webhook-secret",
        AllowedIPs:   []string{"192.168.1.0/24", "10.0.0.1"},
        RateLimit:    60,
        SecurityMode: "strict",
    },
}

// Last request times for rate limiting
var lastRequestTimes = make(map[string][]time.Time)

func customWebhookHandler(w http.ResponseWriter, r *http.Request) {
    // 1. Extract source ID from URL path
    pathParts := strings.Split(r.URL.Path, "/")
    if len(pathParts) < 3 {
        http.Error(w, "Invalid URL path", http.StatusBadRequest)
        return
    }
    
    sourceID := pathParts[len(pathParts)-1]
    
    // 2. Check if we have a configuration for this source
    config, exists := webhookConfigs[sourceID]
    if !exists {
        http.Error(w, "Unknown webhook source", http.StatusBadRequest)
        return
    }
    
    // 3. Read the request body
    body, err := ioutil.ReadAll(r.Body)
    if err != nil {
        log.Printf("Error reading request body: %v", err)
        http.Error(w, "Failed to read request body", http.StatusBadRequest)
        return
    }
    
    // 4. Validate IP address if IP restriction is enabled
    if len(config.AllowedIPs) > 0 {
        clientIP := getClientIP(r)
        if !isIPAllowed(clientIP, config.AllowedIPs) {
            log.Printf("Request from unauthorized IP: %s", clientIP)
            http.Error(w, "IP not authorized", http.StatusForbidden)
            return
        }
    }
    
    // 5. Apply rate limiting
    if !checkRateLimit(sourceID, config.RateLimit) {
        log.Printf("Rate limit exceeded for source: %s", sourceID)
        w.Header().Set("Retry-After", "60")
        http.Error(w, "Rate limit exceeded", http.StatusTooManyRequests)
        return
    }
    
    // 6. Get and validate headers
    signature := r.Header.Get("X-Webhook-Signature")
    eventType := r.Header.Get("X-Webhook-Event")
    eventID := r.Header.Get("X-Webhook-ID")
    
    // 7. Verify all required headers are present
    if signature == "" || eventType == "" || eventID == "" {
        log.Printf("Missing required headers: signature=%s, event=%s, eventID=%s", 
            signature, eventType, eventID)
        http.Error(w, "Missing required headers", http.StatusBadRequest)
        return
    }
    
    // 8. Verify signature
    if !verifySignature(body, signature, config.Secret) {
        log.Printf("Invalid signature: %s", signature)
        http.Error(w, "Invalid signature", http.StatusUnauthorized)
        return
    }
    
    // 9. Check for replay attacks using event ID
    if isReplayAttack(eventID) {
        log.Printf("Duplicate event ID detected: %s", eventID)
        http.Error(w, "Duplicate event ID", http.StatusBadRequest)
        return
    }
    
    // 10. Record the event ID to prevent replays
    recordEventID(eventID)
    
    // 11. Parse the payload
    var payload map[string]interface{}
    if err := json.Unmarshal(body, &payload); err != nil {
        log.Printf("Error parsing JSON payload: %v", err)
        http.Error(w, "Invalid JSON payload", http.StatusBadRequest)
        return
    }
    
    // 12. Process the event based on type
    switch eventType {
    case "data.created":
        handleDataCreatedEvent(payload)
    case "data.updated":
        handleDataUpdatedEvent(payload)
    case "data.deleted":
        handleDataDeletedEvent(payload)
    default:
        log.Printf("Received unhandled event type: %s", eventType)
    }
    
    // 13. Return success response
    w.WriteHeader(http.StatusOK)
    fmt.Fprintf(w, "Webhook processed successfully")
}

// Verify the webhook signature
func verifySignature(payload []byte, signature, secret string) bool {
    // Verify signature format (should be in format "sha256=...")
    if !strings.HasPrefix(signature, "sha256=") {
        return false
    }
    
    // Extract actual signature
    signature = signature[7:]
    
    // Calculate expected signature
    mac := hmac.New(sha256.New, []byte(secret))
    mac.Write(payload)
    expectedMAC := mac.Sum(nil)
    expectedSignature := hex.EncodeToString(expectedMAC)
    
    // Compare signatures using constant-time comparison
    return hmac.Equal([]byte(signature), []byte(expectedSignature))
}

// Check if event ID has been seen before (replay attack detection)
func isReplayAttack(eventID string) bool {
    _, exists := processedEventIDs[eventID]
    return exists
}

// Record an event ID with timestamp
func recordEventID(eventID string) {
    processedEventIDs[eventID] = time.Now()
    
    // Clean up old event IDs periodically (in a real implementation)
    // This would be done by a background goroutine
    cleanEventIDs()
}

// Remove event IDs older than 24 hours
func cleanEventIDs() {
    cutoff := time.Now().Add(-24 * time.Hour)
    
    for id, timestamp := range processedEventIDs {
        if timestamp.Before(cutoff) {
            delete(processedEventIDs, id)
        }
    }
}

// Get the client IP address
func getClientIP(r *http.Request) string {
    // Check X-Forwarded-For header first (for proxies)
    forwardedFor := r.Header.Get("X-Forwarded-For")
    if forwardedFor != "" {
        // X-Forwarded-For can contain multiple IPs; use the first one
        ips := strings.Split(forwardedFor, ",")
        if len(ips) > 0 {
            return strings.TrimSpace(ips[0])
        }
    }
    
    // Fall back to RemoteAddr
    ip, _, err := net.SplitHostPort(r.RemoteAddr)
    if err != nil {
        return r.RemoteAddr
    }
    return ip
}

// Check if an IP is in the allowed list
func isIPAllowed(ip string, allowedIPs []string) bool {
    for _, allowed := range allowedIPs {
        // Check for exact match
        if ip == allowed {
            return true
        }
        
        // Check for CIDR match
        if strings.Contains(allowed, "/") {
            _, ipNet, err := net.ParseCIDR(allowed)
            if err != nil {
                continue
            }
            
            parsedIP := net.ParseIP(ip)
            if parsedIP != nil && ipNet.Contains(parsedIP) {
                return true
            }
        }
    }
    
    return false
}

// Check and enforce rate limits
func checkRateLimit(source string, limit int) bool {
    now := time.Now()
    oneMinuteAgo := now.Add(-time.Minute)
    
    // Initialize if needed
    if _, exists := lastRequestTimes[source]; !exists {
        lastRequestTimes[source] = []time.Time{}
    }
    
    // Filter to only include requests from the last minute
    recentRequests := []time.Time{}
    for _, t := range lastRequestTimes[source] {
        if t.After(oneMinuteAgo) {
            recentRequests = append(recentRequests, t)
        }
    }
    
    // Check if we're at the limit
    if len(recentRequests) >= limit {
        return false
    }
    
    // Add current request time
    lastRequestTimes[source] = append(recentRequests, now)
    
    return true
}

// Event handlers
func handleDataCreatedEvent(payload map[string]interface{}) {
    log.Println("Processing data.created event")
    
    if item, ok := payload["item"].(map[string]interface{}); ok {
        itemID := item["id"]
        itemName := item["name"]
        log.Printf("New item created: %v - %v", itemID, itemName)
    }
}

func handleDataUpdatedEvent(payload map[string]interface{}) {
    log.Println("Processing data.updated event")
    
    if item, ok := payload["item"].(map[string]interface{}); ok {
        itemID := item["id"]
        itemName := item["name"]
        changes := payload["changes"]
        log.Printf("Item updated: %v - %v with changes: %v", itemID, itemName, changes)
    }
}

func handleDataDeletedEvent(payload map[string]interface{}) {
    log.Println("Processing data.deleted event")
    
    if item, ok := payload["item"].(map[string]interface{}); ok {
        itemID := item["id"]
        log.Printf("Item deleted: %v", itemID)
    }
}

func main() {
    http.HandleFunc("/webhooks/custom/", customWebhookHandler)
    
    log.Println("Starting webhook server on port 8080...")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatalf("Failed to start server: %v", err)
    }
}
```

## Webhook Configuration Management

Example for configuring webhook settings using the Rinna API:

```javascript
// JavaScript Example - Webhook Configuration
async function configureWebhook(source, config) {
    try {
        const response = await fetch(`https://api.rinna.io/api/v1/webhooks/configure/${source}`, {
            method: 'POST',
            headers: {
                'Authorization': 'Bearer ri-your-api-key',
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(config)
        });
        
        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(`Failed to configure webhook: ${errorData.message}`);
        }
        
        return response.json();
    } catch (error) {
        console.error('Error configuring webhook:', error);
        throw error;
    }
}

// Configure GitHub webhook
async function configureGitHubWebhook(projectKey) {
    // Generate a secure random secret
    const secret = generateSecureSecret(32);
    
    const config = {
        source: 'github',
        project_key: projectKey,
        secret: secret,
        enabled: true,
        security_mode: 'strict',
        allowed_ips: [], // GitHub IPs change, so we rely on signature verification
        rate_limit: 60   // Max 60 webhooks per minute
    };
    
    const result = await configureWebhook('github', config);
    console.log('GitHub webhook configured successfully:', result);
    
    return {
        webhook_url: `https://api.rinna.io/api/v1/webhooks/github?project=${projectKey}`,
        secret: secret
    };
}

// Helper function to generate secure random secret
function generateSecureSecret(length) {
    const chars = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*()-_=+';
    const array = new Uint8Array(length);
    window.crypto.getRandomValues(array);
    
    let secret = '';
    for (let i = 0; i < length; i++) {
        secret += chars[array[i] % chars.length];
    }
    
    return secret;
}
```

## Testing Webhook Handlers

Example for testing webhook handlers with signature verification:

```python
# Python Example - Webhook Testing Tool
import requests
import json
import hmac
import hashlib
import time
import argparse
import sys

def generate_github_signature(payload, secret):
    """Generate GitHub webhook signature"""
    mac = hmac.new(secret.encode(), msg=payload.encode(), digestmod=hashlib.sha256)
    return f"sha256={mac.hexdigest()}"

def generate_gitlab_token(token):
    """Return GitLab webhook token"""
    return token

def generate_bitbucket_signature(payload, secret):
    """Generate Bitbucket webhook signature"""
    mac = hmac.new(secret.encode(), msg=payload.encode(), digestmod=hashlib.sha1)
    return f"sha1={mac.hexdigest()}"

def generate_custom_signature(payload, secret):
    """Generate custom webhook signature"""
    mac = hmac.new(secret.encode(), msg=payload.encode(), digestmod=hashlib.sha256)
    return f"sha256={mac.hexdigest()}"

def test_webhook(webhook_url, webhook_type, secret, payload_file=None):
    """Test a webhook endpoint with proper signature verification"""
    print(f"Testing {webhook_type} webhook at {webhook_url}")
    
    # Load payload from file or use default
    if payload_file:
        try:
            with open(payload_file, 'r') as f:
                payload = f.read()
        except Exception as e:
            print(f"Error reading payload file: {e}")
            sys.exit(1)
    else:
        # Use a simple default payload
        payload_data = {
            "event": "test",
            "timestamp": int(time.time()),
            "data": {
                "message": "This is a test webhook payload",
                "source": webhook_type
            }
        }
        payload = json.dumps(payload_data)
    
    # Set up headers based on webhook type
    headers = {
        'Content-Type': 'application/json'
    }
    
    if webhook_type == 'github':
        headers['X-GitHub-Event'] = 'ping'
        headers['X-GitHub-Delivery'] = f"test-{int(time.time())}"
        headers['X-Hub-Signature-256'] = generate_github_signature(payload, secret)
    
    elif webhook_type == 'gitlab':
        headers['X-Gitlab-Event'] = 'Push Hook'
        headers['X-Gitlab-Token'] = generate_gitlab_token(secret)
        headers['X-Gitlab-Event-UUID'] = f"test-{int(time.time())}"
    
    elif webhook_type == 'bitbucket':
        headers['X-Event-Key'] = 'repo:push'
        headers['X-Request-UUID'] = f"test-{int(time.time())}"
        headers['X-Hub-Signature'] = generate_bitbucket_signature(payload, secret)
    
    elif webhook_type == 'custom':
        headers['X-Webhook-Event'] = 'data.created'
        headers['X-Webhook-ID'] = f"test-{int(time.time())}"
        headers['X-Webhook-Signature'] = generate_custom_signature(payload, secret)
    
    else:
        print(f"Unsupported webhook type: {webhook_type}")
        sys.exit(1)
    
    # Print request details
    print("\nRequest:")
    print(f"URL: {webhook_url}")
    print("Headers:")
    for key, value in headers.items():
        if key.lower().find('secret') != -1 or key.lower().find('token') != -1 or key.lower().find('signature') != -1:
            print(f"  {key}: {value[:10]}... (truncated)")
        else:
            print(f"  {key}: {value}")
    print(f"Payload (first 100 chars): {payload[:100]}...")
    
    try:
        # Send the webhook request
        response = requests.post(
            webhook_url,
            headers=headers,
            data=payload,
            timeout=10
        )
        
        # Print response details
        print("\nResponse:")
        print(f"Status Code: {response.status_code}")
        print("Headers:")
        for key, value in response.headers.items():
            print(f"  {key}: {value}")
        
        try:
            # Try to parse JSON response
            json_response = response.json()
            print(f"Body (JSON): {json.dumps(json_response, indent=2)}")
        except:
            # Fall back to text response
            print(f"Body: {response.text[:500]}")
        
        # Check response status
        if response.status_code >= 200 and response.status_code < 300:
            print("\n✅ Webhook test successful!")
        else:
            print(f"\n❌ Webhook test failed with status code {response.status_code}")
        
    except Exception as e:
        print(f"\n❌ Error sending webhook: {e}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Test webhook endpoints with proper signature verification')
    parser.add_argument('--url', required=True, help='The webhook URL to test')
    parser.add_argument('--type', required=True, choices=['github', 'gitlab', 'bitbucket', 'custom'], help='The type of webhook')
    parser.add_argument('--secret', required=True, help='The webhook secret or token')
    parser.add_argument('--payload', help='Path to a JSON file containing the webhook payload')
    
    args = parser.parse_args()
    
    test_webhook(args.url, args.type, args.secret, args.payload)
```

## Best Practices for Webhook Security

1. **Always verify signatures** using the provider's recommended method
2. **Implement nonce checking** to prevent replay attacks
3. **Use IP filtering** when the provider has a known IP range
4. **Store webhook secrets securely** and never in version control
5. **Implement rate limiting** to prevent abuse
6. **Use proper error handling** to avoid information leakage
7. **Log webhook events** for audit purposes, but sanitize sensitive data
8. **Implement request timeouts** to prevent hanging requests
9. **Consider queue-based processing** for reliability
10. **Test your webhook handlers** thoroughly, especially edge cases

## Security Considerations

- **Constant-time comparison**: Always use constant-time comparison when verifying signatures to prevent timing attacks
- **Secret rotation**: Rotate webhook secrets periodically
- **Secure storage**: Store webhook secrets in a secure environment variable or secret manager
- **Authentication**: Consider adding additional authentication for webhook endpoints when possible
- **Content validation**: Always validate the content of webhook payloads before processing
- **Process isolation**: Run webhook processing in isolated environments for enhanced security
- **HTTPS only**: Only accept webhook requests over HTTPS to prevent eavesdropping

For complete information on Rinna's webhook security features, refer to the [API Security Guide](../api-security-guide.md) and [Webhook Security Documentation](../WEBHOOK_SECURITY.md).
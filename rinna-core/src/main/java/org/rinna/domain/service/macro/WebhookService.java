package org.rinna.domain.service.macro;

import org.rinna.domain.model.macro.WebhookConfig;

import java.util.List;
import java.util.Map;

/**
 * Service interface for managing incoming and outgoing webhooks.
 */
public interface WebhookService {
    
    /**
     * Processes an incoming webhook and triggers appropriate macros.
     *
     * @param path the webhook path
     * @param headers the request headers
     * @param body the request body
     * @param sourceIp the source IP address
     * @return true if the webhook was processed successfully
     */
    boolean processIncomingWebhook(String path, Map<String, String> headers, String body, String sourceIp);
    
    /**
     * Sends an outgoing webhook.
     *
     * @param url the target URL
     * @param method the HTTP method (GET, POST, etc.)
     * @param headers the request headers
     * @param body the request body
     * @param config the webhook configuration
     * @return the response from the target system
     */
    WebhookResponse sendOutgoingWebhook(String url, String method, Map<String, String> headers, 
            String body, WebhookConfig config);
    
    /**
     * Registers a webhook trigger.
     *
     * @param path the webhook path
     * @param description a description of this webhook
     * @param config the webhook configuration
     * @return the generated secret key for this webhook
     */
    String registerWebhookTrigger(String path, String description, WebhookConfig config);
    
    /**
     * Unregisters a webhook trigger.
     *
     * @param path the webhook path
     * @return true if the webhook was unregistered
     */
    boolean unregisterWebhookTrigger(String path);
    
    /**
     * Lists all registered webhook triggers.
     *
     * @return a list of webhook triggers
     */
    List<WebhookTriggerInfo> listWebhookTriggers();
    
    /**
     * Gets the URL for a registered webhook.
     *
     * @param path the webhook path
     * @return the full webhook URL
     */
    String getWebhookUrl(String path);
    
    /**
     * Sets the base URL for webhooks.
     *
     * @param baseUrl the base URL
     */
    void setBaseUrl(String baseUrl);
    
    /**
     * Gets the webhook rate limit status.
     *
     * @param path the webhook path
     * @return the rate limit status
     */
    RateLimitStatus getRateLimitStatus(String path);
    
    /**
     * Represents information about a registered webhook trigger.
     */
    class WebhookTriggerInfo {
        private String path;
        private String description;
        private String fullUrl;
        private String secretKeyHint;
        private boolean enabled;
        private int hitCount;
        private long lastHitTimestamp;
        
        // Getters and setters
        
        public String getPath() {
            return path;
        }
        
        public void setPath(String path) {
            this.path = path;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public String getFullUrl() {
            return fullUrl;
        }
        
        public void setFullUrl(String fullUrl) {
            this.fullUrl = fullUrl;
        }
        
        public String getSecretKeyHint() {
            return secretKeyHint;
        }
        
        public void setSecretKeyHint(String secretKeyHint) {
            this.secretKeyHint = secretKeyHint;
        }
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public int getHitCount() {
            return hitCount;
        }
        
        public void setHitCount(int hitCount) {
            this.hitCount = hitCount;
        }
        
        public long getLastHitTimestamp() {
            return lastHitTimestamp;
        }
        
        public void setLastHitTimestamp(long lastHitTimestamp) {
            this.lastHitTimestamp = lastHitTimestamp;
        }
    }
    
    /**
     * Represents rate limit status for a webhook.
     */
    class RateLimitStatus {
        private int limit;
        private int remaining;
        private long resetTimestamp;
        
        public RateLimitStatus(int limit, int remaining, long resetTimestamp) {
            this.limit = limit;
            this.remaining = remaining;
            this.resetTimestamp = resetTimestamp;
        }
        
        public int getLimit() {
            return limit;
        }
        
        public int getRemaining() {
            return remaining;
        }
        
        public long getResetTimestamp() {
            return resetTimestamp;
        }
    }
    
    /**
     * Represents the response from a webhook call.
     */
    class WebhookResponse {
        private int statusCode;
        private Map<String, String> headers;
        private String body;
        private long responseTimeMs;
        private boolean successful;
        private String errorMessage;
        
        public WebhookResponse() {
        }
        
        public WebhookResponse(int statusCode, Map<String, String> headers, String body, long responseTimeMs) {
            this.statusCode = statusCode;
            this.headers = headers;
            this.body = body;
            this.responseTimeMs = responseTimeMs;
            this.successful = statusCode >= 200 && statusCode < 300;
        }
        
        public static WebhookResponse error(String errorMessage) {
            WebhookResponse response = new WebhookResponse();
            response.setStatusCode(0);
            response.setSuccessful(false);
            response.setErrorMessage(errorMessage);
            return response;
        }
        
        public int getStatusCode() {
            return statusCode;
        }
        
        public void setStatusCode(int statusCode) {
            this.statusCode = statusCode;
        }
        
        public Map<String, String> getHeaders() {
            return headers;
        }
        
        public void setHeaders(Map<String, String> headers) {
            this.headers = headers;
        }
        
        public String getBody() {
            return body;
        }
        
        public void setBody(String body) {
            this.body = body;
        }
        
        public long getResponseTimeMs() {
            return responseTimeMs;
        }
        
        public void setResponseTimeMs(long responseTimeMs) {
            this.responseTimeMs = responseTimeMs;
        }
        
        public boolean isSuccessful() {
            return successful;
        }
        
        public void setSuccessful(boolean successful) {
            this.successful = successful;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }
}
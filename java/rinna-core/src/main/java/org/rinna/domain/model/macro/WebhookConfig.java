package org.rinna.domain.model.macro;

import java.util.Objects;

/**
 * Configuration settings for webhooks, including rate limiting.
 */
public class WebhookConfig {
    
    /**
     * Default values for webhook configuration.
     */
    public static final class Defaults {
        public static final int DEFAULT_RATE_LIMIT = 60;          // 60 requests per period
        public static final int DEFAULT_RATE_LIMIT_PERIOD = 3600; // 1 hour (in seconds)
        public static final int DEFAULT_TIMEOUT = 10000;          // 10 seconds (in milliseconds)
        public static final int DEFAULT_MAX_RETRY_COUNT = 3;      // 3 retries
        public static final int DEFAULT_RETRY_DELAY = 5000;       // 5 seconds (in milliseconds)
    }
    
    private String id;                    // Unique identifier for this configuration
    private String name;                  // User-friendly name
    private String description;           // Description of this webhook configuration
    private WebhookAuthentication auth;   // Authentication settings
    private boolean enabled;              // Is this webhook enabled
    
    // Rate limiting settings
    private int rateLimit;                // Maximum number of requests per period
    private int rateLimitPeriod;          // Rate limit period in seconds
    private boolean rateLimitEnabled;     // Is rate limiting enabled
    
    // Timeout and retry settings
    private int timeout;                  // Timeout in milliseconds
    private int maxRetryCount;            // Maximum number of retries
    private int retryDelay;               // Delay between retries in milliseconds
    private boolean retryEnabled;         // Is retry enabled
    
    // Logging settings
    private boolean logRequestBody;       // Log the request body
    private boolean logResponseBody;      // Log the response body
    private boolean logHeaders;           // Log the headers
    
    /**
     * Default constructor.
     */
    public WebhookConfig() {
        this.auth = new WebhookAuthentication();
        this.enabled = true;
        this.rateLimit = Defaults.DEFAULT_RATE_LIMIT;
        this.rateLimitPeriod = Defaults.DEFAULT_RATE_LIMIT_PERIOD;
        this.rateLimitEnabled = true;
        this.timeout = Defaults.DEFAULT_TIMEOUT;
        this.maxRetryCount = Defaults.DEFAULT_MAX_RETRY_COUNT;
        this.retryDelay = Defaults.DEFAULT_RETRY_DELAY;
        this.retryEnabled = true;
        this.logRequestBody = false;
        this.logResponseBody = false;
        this.logHeaders = false;
    }
    
    /**
     * Constructor with name and description.
     *
     * @param name the name
     * @param description the description
     */
    public WebhookConfig(String name, String description) {
        this();
        this.name = name;
        this.description = description;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public WebhookAuthentication getAuth() {
        return auth;
    }
    
    public void setAuth(WebhookAuthentication auth) {
        this.auth = auth != null ? auth : new WebhookAuthentication();
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public int getRateLimit() {
        return rateLimit;
    }
    
    public void setRateLimit(int rateLimit) {
        this.rateLimit = rateLimit > 0 ? rateLimit : Defaults.DEFAULT_RATE_LIMIT;
    }
    
    public int getRateLimitPeriod() {
        return rateLimitPeriod;
    }
    
    public void setRateLimitPeriod(int rateLimitPeriod) {
        this.rateLimitPeriod = rateLimitPeriod > 0 ? rateLimitPeriod : Defaults.DEFAULT_RATE_LIMIT_PERIOD;
    }
    
    public boolean isRateLimitEnabled() {
        return rateLimitEnabled;
    }
    
    public void setRateLimitEnabled(boolean rateLimitEnabled) {
        this.rateLimitEnabled = rateLimitEnabled;
    }
    
    public int getTimeout() {
        return timeout;
    }
    
    public void setTimeout(int timeout) {
        this.timeout = timeout > 0 ? timeout : Defaults.DEFAULT_TIMEOUT;
    }
    
    public int getMaxRetryCount() {
        return maxRetryCount;
    }
    
    public void setMaxRetryCount(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount >= 0 ? maxRetryCount : Defaults.DEFAULT_MAX_RETRY_COUNT;
    }
    
    public int getRetryDelay() {
        return retryDelay;
    }
    
    public void setRetryDelay(int retryDelay) {
        this.retryDelay = retryDelay > 0 ? retryDelay : Defaults.DEFAULT_RETRY_DELAY;
    }
    
    public boolean isRetryEnabled() {
        return retryEnabled;
    }
    
    public void setRetryEnabled(boolean retryEnabled) {
        this.retryEnabled = retryEnabled;
    }
    
    public boolean isLogRequestBody() {
        return logRequestBody;
    }
    
    public void setLogRequestBody(boolean logRequestBody) {
        this.logRequestBody = logRequestBody;
    }
    
    public boolean isLogResponseBody() {
        return logResponseBody;
    }
    
    public void setLogResponseBody(boolean logResponseBody) {
        this.logResponseBody = logResponseBody;
    }
    
    public boolean isLogHeaders() {
        return logHeaders;
    }
    
    public void setLogHeaders(boolean logHeaders) {
        this.logHeaders = logHeaders;
    }
    
    /**
     * Calculates requests per second based on rate limit settings.
     *
     * @return the maximum requests per second
     */
    public double getRequestsPerSecond() {
        return (double) rateLimit / rateLimitPeriod;
    }
    
    /**
     * Creates a new WebhookConfig with default rate limits.
     *
     * @param name the name
     * @return a new WebhookConfig instance
     */
    public static WebhookConfig withDefaultRateLimits(String name) {
        return new WebhookConfig(name, "Default configuration with standard rate limits");
    }
    
    /**
     * Creates a new WebhookConfig with high rate limits for trusted systems.
     *
     * @param name the name
     * @return a new WebhookConfig instance
     */
    public static WebhookConfig forTrustedSystem(String name) {
        WebhookConfig config = new WebhookConfig(name, "High rate limits for trusted systems");
        config.setRateLimit(600);  // 600 requests per hour (10 per minute)
        return config;
    }
    
    /**
     * Creates a new WebhookConfig with low rate limits for untrusted systems.
     *
     * @param name the name
     * @return a new WebhookConfig instance
     */
    public static WebhookConfig forUntrustedSystem(String name) {
        WebhookConfig config = new WebhookConfig(name, "Low rate limits for untrusted systems");
        config.setRateLimit(6);    // 6 requests per hour (0.1 per minute)
        return config;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WebhookConfig that = (WebhookConfig) o;
        return enabled == that.enabled &&
                rateLimit == that.rateLimit &&
                rateLimitPeriod == that.rateLimitPeriod &&
                rateLimitEnabled == that.rateLimitEnabled &&
                timeout == that.timeout &&
                maxRetryCount == that.maxRetryCount &&
                retryDelay == that.retryDelay &&
                retryEnabled == that.retryEnabled &&
                logRequestBody == that.logRequestBody &&
                logResponseBody == that.logResponseBody &&
                logHeaders == that.logHeaders &&
                Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(description, that.description) &&
                Objects.equals(auth, that.auth);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, auth, enabled, rateLimit, rateLimitPeriod,
                rateLimitEnabled, timeout, maxRetryCount, retryDelay, retryEnabled,
                logRequestBody, logResponseBody, logHeaders);
    }
}
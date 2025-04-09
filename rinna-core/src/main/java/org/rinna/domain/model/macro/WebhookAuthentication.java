package org.rinna.domain.model.macro;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents authentication and security settings for webhooks.
 */
public class WebhookAuthentication {
    
    /**
     * Authentication methods for webhooks.
     */
    public enum AuthMethod {
        NONE,           // No authentication
        API_KEY,        // API key in header or query parameter
        HMAC_SHA256,    // HMAC-SHA256 signature
        BASIC_AUTH,     // HTTP Basic authentication
        BEARER_TOKEN,   // Bearer token authentication
        CUSTOM_HEADER   // Custom header authentication
    }
    
    private String id;                        // Unique ID for this authentication configuration
    private AuthMethod method;                // Authentication method
    private String secretKey;                 // Secret key for authentication
    private String headerName;                // Header name for API key or signature
    private Map<String, String> parameters;   // Additional parameters
    private boolean validateSsl;              // Whether to validate SSL certificates
    private String ipAllowList;               // Comma-separated list of allowed IP addresses or CIDR ranges
    
    /**
     * Default constructor.
     */
    public WebhookAuthentication() {
        this.id = UUID.randomUUID().toString();
        this.method = AuthMethod.NONE;
        this.parameters = new HashMap<>();
        this.validateSsl = true;
    }
    
    /**
     * Constructor with authentication method.
     *
     * @param method the authentication method
     */
    public WebhookAuthentication(AuthMethod method) {
        this();
        this.method = method;
    }
    
    /**
     * Constructor with authentication method and secret key.
     *
     * @param method the authentication method
     * @param secretKey the secret key
     */
    public WebhookAuthentication(AuthMethod method, String secretKey) {
        this(method);
        this.secretKey = secretKey;
    }
    
    /**
     * Constructor with authentication method, secret key, and header name.
     *
     * @param method the authentication method
     * @param secretKey the secret key
     * @param headerName the header name
     */
    public WebhookAuthentication(AuthMethod method, String secretKey, String headerName) {
        this(method, secretKey);
        this.headerName = headerName;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public AuthMethod getMethod() {
        return method;
    }
    
    public void setMethod(AuthMethod method) {
        this.method = method != null ? method : AuthMethod.NONE;
    }
    
    public String getSecretKey() {
        return secretKey;
    }
    
    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }
    
    public String getHeaderName() {
        return headerName;
    }
    
    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }
    
    public Map<String, String> getParameters() {
        return parameters;
    }
    
    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters != null ? parameters : new HashMap<>();
    }
    
    /**
     * Gets a parameter value.
     *
     * @param key the parameter key
     * @return the value, or null if not found
     */
    public String getParameter(String key) {
        return parameters.get(key);
    }
    
    /**
     * Sets a parameter value.
     *
     * @param key the parameter key
     * @param value the value to set
     */
    public void setParameter(String key, String value) {
        if (key != null && value != null) {
            this.parameters.put(key, value);
        }
    }
    
    public boolean isValidateSsl() {
        return validateSsl;
    }
    
    public void setValidateSsl(boolean validateSsl) {
        this.validateSsl = validateSsl;
    }
    
    public String getIpAllowList() {
        return ipAllowList;
    }
    
    public void setIpAllowList(String ipAllowList) {
        this.ipAllowList = ipAllowList;
    }
    
    /**
     * Creates a new WebhookAuthentication for API key authentication.
     *
     * @param secretKey the API key
     * @param headerName the header name
     * @return a new WebhookAuthentication instance
     */
    public static WebhookAuthentication apiKey(String secretKey, String headerName) {
        WebhookAuthentication auth = new WebhookAuthentication(AuthMethod.API_KEY, secretKey);
        auth.setHeaderName(headerName != null ? headerName : "X-API-Key");
        return auth;
    }
    
    /**
     * Creates a new WebhookAuthentication for HMAC-SHA256 authentication.
     *
     * @param secretKey the secret key
     * @param headerName the header name for the signature
     * @return a new WebhookAuthentication instance
     */
    public static WebhookAuthentication hmacSha256(String secretKey, String headerName) {
        WebhookAuthentication auth = new WebhookAuthentication(AuthMethod.HMAC_SHA256, secretKey);
        auth.setHeaderName(headerName != null ? headerName : "X-Signature");
        return auth;
    }
    
    /**
     * Creates a new WebhookAuthentication for Basic authentication.
     *
     * @param username the username
     * @param password the password
     * @return a new WebhookAuthentication instance
     */
    public static WebhookAuthentication basicAuth(String username, String password) {
        WebhookAuthentication auth = new WebhookAuthentication(AuthMethod.BASIC_AUTH);
        auth.setParameter("username", username);
        auth.setParameter("password", password);
        return auth;
    }
    
    /**
     * Creates a new WebhookAuthentication for Bearer token authentication.
     *
     * @param token the bearer token
     * @return a new WebhookAuthentication instance
     */
    public static WebhookAuthentication bearerToken(String token) {
        return new WebhookAuthentication(AuthMethod.BEARER_TOKEN, token);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WebhookAuthentication that = (WebhookAuthentication) o;
        return validateSsl == that.validateSsl &&
                Objects.equals(id, that.id) &&
                method == that.method &&
                Objects.equals(secretKey, that.secretKey) &&
                Objects.equals(headerName, that.headerName) &&
                Objects.equals(parameters, that.parameters) &&
                Objects.equals(ipAllowList, that.ipAllowList);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, method, secretKey, headerName, parameters, validateSsl, ipAllowList);
    }
}
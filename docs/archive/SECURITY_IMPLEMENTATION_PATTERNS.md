# Security Implementation Patterns

This document provides reusable security implementation patterns for the Rinna project. These patterns serve as ready-to-use templates for implementing security features consistently across the codebase.

## Table of Contents

1. [Authentication Patterns](#authentication-patterns)
2. [Authorization Patterns](#authorization-patterns)
3. [Input Validation Patterns](#input-validation-patterns)
4. [Output Encoding Patterns](#output-encoding-patterns)
5. [Secure Communication Patterns](#secure-communication-patterns)
6. [Webhook Security Patterns](#webhook-security-patterns)
7. [Password Security Patterns](#password-security-patterns)
8. [Token Management Patterns](#token-management-patterns)
9. [Logging and Monitoring Patterns](#logging-and-monitoring-patterns)
10. [Error Handling Patterns](#error-handling-patterns)

## Authentication Patterns

### CLI User Authentication

```java
/**
 * Authenticate a user with username and password.
 *
 * @param username The username
 * @param password The password
 * @return true if authentication was successful, false otherwise
 */
public boolean authenticateUser(String username, String password) {
    AuthenticationService authService = new AuthenticationService();
    authService.initialize();
    
    boolean success = authService.login(username, password);
    if (success) {
        // Optionally log successful authentication
        LOGGER.info("User {} authenticated successfully", username);
    } else {
        // Optionally log failed authentication
        LOGGER.warn("Authentication failed for user {}", username);
    }
    
    return success;
}
```

### API Token Authentication

```go
/**
 * Middleware to authenticate API requests with token.
 */
func TokenAuthMiddleware(authService *AuthService) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        // Extract token from Authorization header
        authHeader := r.Header.Get("Authorization")
        if authHeader == "" || !strings.HasPrefix(authHeader, "Bearer ") {
            http.Error(w, "Unauthorized: Missing or invalid Authorization header", http.StatusUnauthorized)
            return
        }
        
        token := strings.TrimPrefix(authHeader, "Bearer ")
        
        // Validate token
        projectID, err := authService.ValidateToken(r.Context(), token)
        if err != nil {
            http.Error(w, "Unauthorized: Invalid token", http.StatusUnauthorized)
            log.WithFields(log.Fields{
                "error": err.Error(),
                "token": maskToken(token), // Only log masked token for security
            }).Warn("Token validation failed")
            return
        }
        
        // Store token and project ID in context
        ctx := context.WithValue(r.Context(), tokenKey{}, token)
        ctx = context.WithValue(ctx, projectKey{}, projectID)
        
        // Call next handler
        next.ServeHTTP(w, r.WithContext(ctx))
    }
}

/**
 * Mask a token for logging purposes.
 */
func maskToken(token string) string {
    if len(token) < 8 {
        return "***"
    }
    return token[:4] + "..." + token[len(token)-4:]
}
```

## Authorization Patterns

### Permission Check

```java
/**
 * Check if the current user has permission to perform an operation.
 *
 * @param operation The operation to check
 * @return true if the user has permission, false otherwise
 */
public boolean hasPermission(String operation) {
    AuthenticationService authService = new AuthenticationService();
    AuthorizationService authzService = new AuthorizationService(authService);
    
    // Check if user is authenticated
    String currentUser = authService.getCurrentUser();
    if (currentUser == null) {
        LOGGER.warn("Permission check failed: No authenticated user");
        return false;
    }
    
    // Check permission
    boolean hasPermission = authzService.hasPermission(operation);
    if (!hasPermission) {
        LOGGER.warn("User {} does not have permission for operation: {}", 
                   currentUser, operation);
    }
    
    return hasPermission;
}
```

### Admin Access Check

```java
/**
 * Check if the current user has admin access to a specific area.
 *
 * @param area The administrative area
 * @return true if the user has admin access, false otherwise
 */
public boolean hasAdminAccess(String area) {
    AuthenticationService authService = new AuthenticationService();
    AuthorizationService authzService = new AuthorizationService(authService);
    
    // Check if user is authenticated
    String currentUser = authService.getCurrentUser();
    if (currentUser == null) {
        LOGGER.warn("Admin access check failed: No authenticated user");
        return false;
    }
    
    // Check if user is a full admin
    if (authService.isCurrentUserAdmin()) {
        return true;
    }
    
    // Check for area-specific admin access
    boolean hasAccess = authzService.hasAdminAccess(area);
    if (!hasAccess) {
        LOGGER.warn("User {} does not have admin access to area: {}", 
                   currentUser, area);
    }
    
    return hasAccess;
}
```

## Input Validation Patterns

### String Validation

```java
/**
 * Validate that a string is not null or empty and meets length requirements.
 *
 * @param input The input string to validate
 * @param minLength The minimum acceptable length
 * @param maxLength The maximum acceptable length
 * @return true if the input is valid, false otherwise
 */
public boolean validateString(String input, int minLength, int maxLength) {
    if (input == null || input.isEmpty()) {
        return false;
    }
    
    int length = input.length();
    return length >= minLength && length <= maxLength;
}
```

### ID Validation

```java
/**
 * Validate that a string represents a valid UUID.
 *
 * @param id The ID to validate
 * @return true if the ID is a valid UUID, false otherwise
 */
public boolean validateUUID(String id) {
    if (id == null || id.isEmpty()) {
        return false;
    }
    
    try {
        UUID uuid = UUID.fromString(id);
        return true;
    } catch (IllegalArgumentException e) {
        return false;
    }
}
```

### Request Parameter Validation

```go
/**
 * Validate request parameters.
 */
func validateParams(params map[string]string, required []string) error {
    for _, param := range required {
        value, exists := params[param]
        if !exists || strings.TrimSpace(value) == "" {
            return fmt.Errorf("missing required parameter: %s", param)
        }
    }
    return nil
}
```

## Output Encoding Patterns

### HTML Encoding

```java
/**
 * Encode a string for safe display in HTML.
 *
 * @param input The input string to encode
 * @return The HTML-encoded string
 */
public String encodeForHTML(String input) {
    if (input == null) {
        return "";
    }
    
    return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
}
```

### JSON Encoding

```java
/**
 * Safely encode an object as JSON.
 *
 * @param object The object to encode
 * @return The JSON string, or an empty object if an error occurs
 */
public String toJSON(Object object) {
    if (object == null) {
        return "{}";
    }
    
    try {
        return objectMapper.writeValueAsString(object);
    } catch (JsonProcessingException e) {
        LOGGER.error("Error encoding object to JSON: {}", e.getMessage());
        return "{}";
    }
}
```

## Secure Communication Patterns

### HTTPS Client

```java
/**
 * Create a secure HTTP client with certificate validation.
 *
 * @return A configured HTTP client
 */
public HttpClient createSecureHttpClient() {
    return HttpClient.newBuilder()
        .sslContext(SSLContext.getDefault())
        .connectTimeout(Duration.ofSeconds(10))
        .build();
}
```

### API Token Headers

```java
/**
 * Add authentication headers to an HTTP request.
 *
 * @param request The request builder
 * @param token The authentication token
 * @return The updated request builder
 */
public HttpRequest.Builder addAuthHeaders(HttpRequest.Builder request, String token) {
    return request.header("Authorization", "Bearer " + token)
                 .header("Content-Type", "application/json");
}
```

## Webhook Security Patterns

### Signature Verification

```go
/**
 * Verify a webhook signature.
 */
func verifyWebhookSignature(payload []byte, signature string, secret string) bool {
    // Calculate expected signature
    mac := hmac.New(sha256.New, []byte(secret))
    mac.Write(payload)
    expectedSignature := "sha256=" + hex.EncodeToString(mac.Sum(nil))
    
    // Compare signatures using constant-time comparison
    return hmac.Equal([]byte(expectedSignature), []byte(signature))
}
```

### Webhook Handler

```go
/**
 * Generic webhook handler with signature verification.
 */
func webhookHandler(w http.ResponseWriter, r *http.Request) {
    // Read the request body
    body, err := io.ReadAll(r.Body)
    if err != nil {
        http.Error(w, "Error reading request body", http.StatusBadRequest)
        return
    }
    
    // Restore the request body for later use
    r.Body = io.NopCloser(bytes.NewBuffer(body))
    
    // Get the signature
    signature := r.Header.Get("X-Hub-Signature-256")
    if signature == "" || !strings.HasPrefix(signature, "sha256=") {
        http.Error(w, "Invalid signature", http.StatusUnauthorized)
        return
    }
    signature = strings.TrimPrefix(signature, "sha256=")
    
    // Get the webhook secret
    secret := getWebhookSecret(r.Context(), r.URL.Query().Get("project"))
    
    // Verify the signature
    if !verifyWebhookSignature(body, signature, secret) {
        http.Error(w, "Invalid signature", http.StatusUnauthorized)
        return
    }
    
    // Process the webhook
    // ...
}
```

## Password Security Patterns

### Password Hashing

```java
/**
 * Hash a password securely.
 *
 * @param password The password to hash
 * @return The hashed password
 */
public String hashPassword(String password) {
    try {
        // Generate random salt
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        
        // Hash with PBKDF2WithHmacSHA512
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 10000, 512);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
        byte[] hash = factory.generateSecret(spec).getEncoded();
        
        // Combine salt and hash
        byte[] combined = new byte[salt.length + hash.length];
        System.arraycopy(salt, 0, combined, 0, salt.length);
        System.arraycopy(hash, 0, combined, salt.length, hash.length);
        
        // Encode with Base64
        return Base64.getEncoder().encodeToString(combined);
    } catch (Exception e) {
        LOGGER.error("Error hashing password: {}", e.getMessage());
        throw new SecurityException("Error hashing password", e);
    }
}
```

### Password Verification

```java
/**
 * Verify a password against a stored hash.
 *
 * @param password The password to verify
 * @param storedHash The stored hash
 * @return true if the password matches, false otherwise
 */
public boolean verifyPassword(String password, String storedHash) {
    try {
        // Decode the stored hash
        byte[] combined = Base64.getDecoder().decode(storedHash);
        
        // Extract salt and hash
        byte[] salt = new byte[16];
        byte[] hash = new byte[combined.length - 16];
        System.arraycopy(combined, 0, salt, 0, 16);
        System.arraycopy(combined, 16, hash, 0, hash.length);
        
        // Hash the provided password with the same salt
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 10000, 8 * hash.length);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
        byte[] testHash = factory.generateSecret(spec).getEncoded();
        
        // Compare the hashes
        return constantTimeEquals(hash, testHash);
    } catch (Exception e) {
        LOGGER.error("Error verifying password: {}", e.getMessage());
        return false;
    }
}

/**
 * Constant-time comparison of two byte arrays to prevent timing attacks.
 */
private boolean constantTimeEquals(byte[] a, byte[] b) {
    if (a.length != b.length) {
        return false;
    }
    
    int result = 0;
    for (int i = 0; i < a.length; i++) {
        result |= a[i] ^ b[i];
    }
    
    return result == 0;
}
```

## Token Management Patterns

### Token Generation

```java
/**
 * Generate a secure authentication token.
 *
 * @param tokenType The token type (dev, test, prod)
 * @return The generated token
 */
public String generateToken(String tokenType) {
    // Validate token type
    if (!Arrays.asList("dev", "test", "prod").contains(tokenType)) {
        throw new IllegalArgumentException("Invalid token type: " + tokenType);
    }
    
    // Generate a secure random identifier
    UUID uuid = UUID.randomUUID();
    
    // Format the token
    return String.format("ri-%s-%s", tokenType, uuid.toString());
}
```

### Token Validation

```go
/**
 * Validate a token and return the associated project ID.
 */
func validateToken(ctx context.Context, token string) (string, error) {
    // Validate token format
    if !strings.HasPrefix(token, "ri-") {
        return "", fmt.Errorf("invalid token format: must start with 'ri-'")
    }
    
    parts := strings.Split(token, "-")
    if len(parts) < 3 {
        return "", fmt.Errorf("invalid token format: must be ri-<type>-<id>")
    }
    
    tokenType := parts[1]
    if tokenType != "dev" && tokenType != "test" && tokenType != "prod" {
        return "", fmt.Errorf("invalid token type: %s", tokenType)
    }
    
    // Validate token with service
    // ... (implementation specific to your token validation service)
    
    return "project-id", nil
}
```

## Logging and Monitoring Patterns

### Security Event Logging

```java
/**
 * Log a security event.
 *
 * @param eventType The type of security event
 * @param username The username associated with the event
 * @param details Additional details about the event
 */
public void logSecurityEvent(String eventType, String username, String details) {
    String timestamp = Instant.now().toString();
    String logEntry = String.format("%s|%s|%s|%s", timestamp, eventType, username, details);
    
    // Log to file
    try {
        Files.write(
            Paths.get(SECURITY_LOG_FILE),
            (logEntry + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND
        );
    } catch (IOException e) {
        LOGGER.error("Error writing to security log: {}", e.getMessage());
    }
    
    // Log to system logger
    LOGGER.info("Security event: {} - User: {} - Details: {}", eventType, username, details);
}
```

### Request Logging

```go
/**
 * Middleware for logging HTTP requests.
 */
func LoggingMiddleware(next http.Handler) http.Handler {
    return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
        start := time.Now()
        
        // Create a response wrapper to capture the status code
        rw := &responseWriter{ResponseWriter: w}
        
        // Call the next handler
        next.ServeHTTP(rw, r)
        
        // Log the request
        duration := time.Since(start)
        logger.WithFields(log.Fields{
            "method":     r.Method,
            "path":       r.URL.Path,
            "status":     rw.status,
            "duration":   duration,
            "ip":         r.RemoteAddr,
            "user_agent": r.UserAgent(),
        }).Info("HTTP Request")
    })
}

// responseWriter wraps the http.ResponseWriter to capture the status code
type responseWriter struct {
    http.ResponseWriter
    status int
}

// WriteHeader captures the status code
func (rw *responseWriter) WriteHeader(status int) {
    rw.status = status
    rw.ResponseWriter.WriteHeader(status)
}
```

## Error Handling Patterns

### Secure Exception Handling

```java
/**
 * Handle an exception securely without exposing sensitive information.
 *
 * @param e The exception to handle
 * @return A user-friendly error message
 */
public String handleException(Exception e) {
    // Generate a unique error reference
    String errorRef = UUID.randomUUID().toString().substring(0, 8);
    
    // Log the full exception with the reference
    LOGGER.error("Error reference {}: {}", errorRef, e.getMessage(), e);
    
    // Return a generic message with the reference
    return String.format("An unexpected error occurred (Ref: %s). Please contact support.", errorRef);
}
```

### API Error Responses

```go
/**
 * Send a standardized error response.
 */
func sendErrorResponse(w http.ResponseWriter, statusCode int, message string, details ...string) {
    // Create error response
    errorResponse := struct {
        Code    int      `json:"code"`
        Message string   `json:"message"`
        Details []string `json:"details,omitempty"`
    }{
        Code:    statusCode,
        Message: message,
        Details: details,
    }
    
    // Set content type and status code
    w.Header().Set("Content-Type", "application/json")
    w.WriteHeader(statusCode)
    
    // Write response
    if err := json.NewEncoder(w).Encode(errorResponse); err != nil {
        log.WithError(err).Error("Failed to encode error response")
        w.WriteHeader(http.StatusInternalServerError)
        w.Write([]byte(`{"code":500,"message":"Internal server error"}`))
    }
}
```
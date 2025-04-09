# Security Implementation Guidelines

This document provides comprehensive guidelines for implementing security features in the Rinna project. It covers both the API server (Go) and CLI (Java) components, detailing authentication mechanisms, authorization policies, token handling, and secure coding practices.

## Table of Contents

1. [Authentication](#authentication)
   - [CLI Authentication](#cli-authentication)
   - [API Authentication](#api-authentication)
   - [Token Management](#token-management)
   - [Password Security](#password-security)

2. [Authorization](#authorization)
   - [CLI Authorization](#cli-authorization)
   - [API Authorization](#api-authorization)
   - [Role-Based Access Control](#role-based-access-control)

3. [Webhook Security](#webhook-security)
   - [Signature Verification](#signature-verification)
   - [Platform-Specific Considerations](#platform-specific-considerations)

4. [Secure Storage](#secure-storage)
   - [Configuration Files Security](#configuration-files-security)
   - [Secure Storage of Secrets](#secure-storage-of-secrets)

5. [Communication Security](#communication-security)
   - [HTTPS/TLS](#httpstls)
   - [API-CLI Communication](#api-cli-communication)

6. [Security Testing](#security-testing)
   - [Automated Security Testing](#automated-security-testing)
   - [Manual Security Testing](#manual-security-testing)

7. [Security Incident Response](#security-incident-response)
   - [Logging and Monitoring](#logging-and-monitoring)
   - [Incident Handling](#incident-handling)

8. [Development Guidelines](#development-guidelines)
   - [Secure Coding Practices](#secure-coding-practices)
   - [Dependency Management](#dependency-management)

## Authentication

### CLI Authentication

The Rinna CLI uses a file-based authentication system with secure password hashing. The implementation is in `org.rinna.cli.security.AuthenticationService`.

#### Implementing Authentication in CLI Components

1. **User Authentication**:
   ```java
   // Get the authentication service
   AuthenticationService authService = new AuthenticationService();
   
   // Authenticate a user
   boolean authenticated = authService.login(username, password);
   if (authenticated) {
       // User is now authenticated
       String currentUser = authService.getCurrentUser();
       boolean isAdmin = authService.isCurrentUserAdmin();
   } else {
       // Authentication failed
       System.err.println("Authentication failed");
   }
   ```

2. **Command Authorization Check**:
   ```java
   // Check if the user has permission to execute a command
   AuthorizationService authzService = new AuthorizationService(authService);
   if (authzService.hasPermission("view")) {
       // Execute the command
   } else {
       System.err.println("Permission denied");
   }
   ```

3. **Admin Operations**:
   ```java
   // Check if the user is an admin
   if (authService.isCurrentUserAdmin()) {
       // Perform admin operation
   } else {
       System.err.println("Admin privileges required");
   }
   ```

4. **Area-Specific Admin Access**:
   ```java
   // Check if the user has admin access to a specific area
   AuthorizationService authzService = new AuthorizationService(authService);
   if (authzService.hasAdminAccess("audit")) {
       // Perform audit admin operation
   } else {
       System.err.println("Audit admin privileges required");
   }
   ```

### API Authentication

The Rinna API uses token-based authentication with middleware to validate requests. The implementation is in `internal/middleware/auth.go`.

#### Implementing Authentication in API Components

1. **Token Validation Middleware**:
   ```go
   // Create an auth service
   authService := middleware.NewAuthService(javaClient, &config.AuthConfig{
       TokenExpiry: 30, // minutes
       DevMode:     false,
   })

   // Register the middleware
   router.Use(middleware.TokenAuthentication(authService))
   ```

2. **Accessing Authentication Information**:
   ```go
   func MyHandler(w http.ResponseWriter, r *http.Request) {
       // Get the token and project ID from the context
       token := middleware.GetToken(r.Context())
       projectID := middleware.GetProjectID(r.Context())
       
       // Use the authentication information
       fmt.Printf("Authenticated request for project %s with token %s\n", projectID, token)
   }
   ```

3. **Authentication Bypass**:
   In development/test environments only:
   ```go
   // Create an auth service with dev mode enabled
   authService := middleware.NewAuthService(nil, &config.AuthConfig{
       TokenExpiry: 30,
       DevMode:     true, // Enable dev mode
   })
   ```

### Token Management

Both CLI and API need to manage authentication tokens securely.

#### Token Generation

Tokens should be generated with sufficient entropy and follow a standardized format:

```
ri-<type>-<identifier>
```

Where:
- `ri` is the Rinna prefix
- `<type>` is the token type (dev, test, prod)
- `<identifier>` is a unique identifier (UUID or other secure random value)

Example implementation:
```java
// Generate a token in Java
String tokenType = "prod"; // or "dev", "test"
String identifier = UUID.randomUUID().toString();
String token = "ri-" + tokenType + "-" + identifier;
```

#### Token Validation

1. **Format Validation**:
   ```go
   // Validate token format in Go
   if !strings.HasPrefix(token, "ri-") {
       return "", fmt.Errorf("invalid token format: must start with 'ri-'")
   }

   parts := strings.Split(token, "-")
   if len(parts) < 3 {
       return "", fmt.Errorf("invalid token format: must be ri-<type>-<id>")
   }

   tokenType := parts[1]
   switch tokenType {
   case "dev", "test", "prod":
       // Valid token types
   default:
       return "", fmt.Errorf("invalid token type: %s", tokenType)
   }
   ```

2. **Signature Validation** (for webhook tokens):
   ```go
   // Validate webhook signature in Go
   mac := hmac.New(sha256.New, []byte(secret))
   mac.Write(payload)
   expectedSignature := "sha256=" + hex.EncodeToString(mac.Sum(nil))

   if !hmac.Equal([]byte(expectedSignature), []byte(signature)) {
       return fmt.Errorf("invalid webhook signature")
   }
   ```

### Password Security

The CLI uses secure password hashing with PBKDF2WithHmacSHA512.

#### Implementing Password Security

1. **Password Hashing**:
   ```java
   // Hash a password
   String hashedPassword = SecurityConfig.hashPassword(password);
   ```

2. **Password Verification**:
   ```java
   // Verify a password
   boolean isValid = SecurityConfig.verifyPassword(password, storedHash);
   ```

3. **Security Considerations**:
   - Use PBKDF2WithHmacSHA512 with at least 10,000 iterations
   - Use a random salt of at least 16 bytes
   - Store the salt with the hash
   - Use constant-time comparison to prevent timing attacks

## Authorization

### CLI Authorization

The CLI uses a permission-based authorization system with role-based access control.

#### Permission Model

1. **Operations**:
   - `view`: View work items and projects
   - `list`: List work items and projects
   - `add`: Create new work items
   - `update`: Update existing work items
   - `delete`: Delete work items
   - `transition`: Transition work items between states
   - `admin`: Perform administrative operations

2. **Admin Areas**:
   - `audit`: Access audit logs and settings
   - `compliance`: Manage compliance settings
   - `monitor`: Access monitoring dashboard
   - `backup`: Perform backup operations
   - `recovery`: Perform recovery operations
   - `user`: Manage users and permissions

#### Implementing Authorization

1. **Check Permission**:
   ```java
   // Check if the user has permission to perform an operation
   boolean hasPermission = authorizationService.hasPermission("update");
   ```

2. **Grant Permission**:
   ```java
   // Grant permission to a user (admin only)
   boolean granted = authorizationService.grantPermission(username, "update");
   ```

3. **Revoke Permission**:
   ```java
   // Revoke permission from a user (admin only)
   boolean revoked = authorizationService.revokePermission(username, "update");
   ```

4. **Check Admin Access**:
   ```java
   // Check if the user has admin access to a specific area
   boolean hasAccess = authorizationService.hasAdminAccess("audit");
   ```

### API Authorization

The API uses project-based authorization with token scoping.

#### Implementing API Authorization

1. **Project-Based Authorization**:
   ```go
   func MyHandler(w http.ResponseWriter, r *http.Request) {
       // Get the project ID from the context
       projectID := middleware.GetProjectID(r.Context())
       
       // Get the requested project from the URL
       requestedProject := r.URL.Query().Get("project")
       
       // Check if the token has access to the requested project
       if projectID != requestedProject {
           http.Error(w, "Unauthorized: Token not valid for this project", http.StatusForbidden)
           return
       }
       
       // Continue with the request
   }
   ```

2. **Role-Based Operations**:
   ```go
   func AdminOperationHandler(w http.ResponseWriter, r *http.Request) {
       // This would be implemented in the Java service
       // The API server would call the Java service to check if the
       // user associated with the token has admin privileges
       
       // Example implementation:
       isAdmin, err := javaClient.CheckAdminStatus(r.Context(), middleware.GetToken(r.Context()))
       if err != nil || !isAdmin {
           http.Error(w, "Unauthorized: Admin privileges required", http.StatusForbidden)
           return
       }
       
       // Continue with the admin operation
   }
   ```

### Role-Based Access Control

The system supports three primary roles:

1. **Admin**: Full access to all operations
2. **Area Admin**: Administrative access to specific areas
3. **User**: Limited access based on granted permissions

#### Implementing RBAC

1. **User Role**:
   ```java
   // Check if user has basic user role
   boolean isUser = authService.userExists(username);
   ```

2. **Admin Role**:
   ```java
   // Check if user is an admin
   boolean isAdmin = authService.isUserAdmin(username);
   ```

3. **Area Admin Role**:
   ```java
   // Check if user is an area admin
   boolean isAreaAdmin = authzService.hasAdminAccess(username, "audit");
   ```

## Webhook Security

### Signature Verification

Webhooks must be secured with signature verification to ensure the authenticity of requests.

#### Implementing Webhook Security

1. **GitHub Webhook**:
   ```go
   // Validate GitHub webhook signature
   signature := r.Header.Get("X-Hub-Signature-256")
   if signature == "" || !strings.HasPrefix(signature, "sha256=") {
       http.Error(w, "Invalid or missing signature", http.StatusUnauthorized)
       return
   }
   
   // Get the secret
   secret, err := javaClient.GetWebhookSecret(r.Context(), projectKey, "github")
   if err != nil {
       http.Error(w, "Failed to retrieve webhook secret", http.StatusInternalServerError)
       return
   }
   
   // Validate the signature
   mac := hmac.New(sha256.New, []byte(secret))
   mac.Write(payload)
   expectedSignature := "sha256=" + hex.EncodeToString(mac.Sum(nil))
   
   if !hmac.Equal([]byte(expectedSignature), []byte("sha256="+signature)) {
       http.Error(w, "Invalid webhook signature", http.StatusUnauthorized)
       return
   }
   ```

2. **Custom Webhook**:
   ```go
   // Validate custom webhook signature
   signature := r.Header.Get("X-Webhook-Signature")
   if signature == "" {
       http.Error(w, "Invalid or missing signature", http.StatusUnauthorized)
       return
   }
   
   // Get the secret
   secret, err := javaClient.GetWebhookSecret(r.Context(), projectKey, "custom")
   if err != nil {
       http.Error(w, "Failed to retrieve webhook secret", http.StatusInternalServerError)
       return
   }
   
   // Validate the signature
   // ... (similar to GitHub webhook)
   ```

### Platform-Specific Considerations

Different webhook providers use different security mechanisms.

#### GitHub

- Uses HMAC-SHA256 signature
- Signature in `X-Hub-Signature-256` header
- Event type in `X-GitHub-Event` header

#### GitLab

- Uses a token comparison
- Token in `X-Gitlab-Token` header

#### Bitbucket

- Uses HMAC-SHA256 signature
- Signature in `X-Hub-Signature` header

## Secure Storage

### Configuration Files Security

Configuration files containing sensitive information should be properly secured.

#### Implementing Secure Configuration

1. **File Permissions**:
   ```java
   // Set secure file permissions
   File configFile = new File(CONFIG_FILE);
   if (configFile.exists()) {
       try {
           // Set file permissions to owner read/write only
           // This is a platform-specific operation
           // On Unix-like systems:
           Files.setPosixFilePermissions(configFile.toPath(), 
               java.util.Set.of(
                   java.nio.file.attribute.PosixFilePermission.OWNER_READ,
                   java.nio.file.attribute.PosixFilePermission.OWNER_WRITE
               ));
       } catch (IOException e) {
           // Handle error
       }
   }
   ```

2. **Directory Permissions**:
   ```java
   // Create a secure directory
   File configDir = new File(CONFIG_DIR);
   if (!configDir.exists()) {
       configDir.mkdirs();
       try {
           // Set directory permissions to owner read/write/execute only
           Files.setPosixFilePermissions(configDir.toPath(), 
               java.util.Set.of(
                   java.nio.file.attribute.PosixFilePermission.OWNER_READ,
                   java.nio.file.attribute.PosixFilePermission.OWNER_WRITE,
                   java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE
               ));
       } catch (IOException e) {
           // Handle error
       }
   }
   ```

### Secure Storage of Secrets

Sensitive information like API keys and webhook secrets should be stored securely.

#### Implementing Secure Storage

1. **Password Hashing**:
   ```java
   // Hash a password
   String hashedPassword = SecurityConfig.hashPassword(password);
   ```

2. **Token Storage**:
   ```java
   // Store a token securely
   securityConfig.storeAuthToken(username, token);
   ```

3. **Webhook Secret Storage**:
   ```java
   // Store a webhook secret in Java service
   // This would be implemented in the Java service
   // The secret should be stored in an encrypted form
   webhookService.storeSecret(projectKey, source, secret);
   ```

## Communication Security

### HTTPS/TLS

All communication should be secured with HTTPS/TLS.

#### Implementing HTTPS

1. **Go Server**:
   ```go
   // Start the server with TLS
   err := http.ListenAndServeTLS(":8443", "cert.pem", "key.pem", router)
   if err != nil {
       log.Fatal(err)
   }
   ```

2. **Java Client**:
   ```java
   // Create a secure HTTP client
   HttpClient client = HttpClient.newBuilder()
       .sslContext(SSLContext.getDefault())
       .build();
   ```

### API-CLI Communication

Communication between the CLI and API should be secured.

#### Implementing Secure Communication

1. **Java to Go Communication**:
   ```java
   // Create a secure HTTP client
   HttpClient client = HttpClient.newBuilder()
       .sslContext(SSLContext.getDefault())
       .build();
   
   // Add authentication token
   HttpRequest request = HttpRequest.newBuilder()
       .uri(URI.create("https://api.example.com/endpoint"))
       .header("Authorization", "Bearer " + token)
       .build();
   
   // Send request
   HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
   ```

2. **Go to Java Communication**:
   ```go
   // Create a secure HTTP client
   client := &http.Client{
       Timeout: time.Second * 10,
   }
   
   // Create request with authentication
   req, err := http.NewRequest("GET", "https://java.example.com/endpoint", nil)
   if err != nil {
       return err
   }
   
   // Add authentication token
   req.Header.Set("Authorization", "Bearer "+token)
   
   // Send request
   resp, err := client.Do(req)
   if err != nil {
       return err
   }
   defer resp.Body.Close()
   ```

## Security Testing

### Automated Security Testing

Implement automated security testing to identify vulnerabilities.

#### Implementing Security Testing

1. **Authentication Testing**:
   ```java
   @Test
   public void testInvalidCredentials() {
       AuthenticationService authService = new AuthenticationService();
       assertFalse(authService.login("user", "wrongpassword"));
   }
   
   @Test
   public void testInvalidToken() {
       AuthenticationService authService = new AuthenticationService();
       assertNull(authService.validateToken("invalid-token"));
   }
   ```

2. **Authorization Testing**:
   ```java
   @Test
   public void testUnauthorizedAccess() {
       AuthenticationService authService = new AuthenticationService();
       AuthorizationService authzService = new AuthorizationService(authService);
       
       // Login as a regular user
       authService.login("user", "password");
       
       // Try to access admin-only operation
       assertFalse(authzService.hasPermission("admin"));
   }
   ```

3. **Token Validation Testing**:
   ```go
   func TestInvalidTokenFormat(t *testing.T) {
       authService := middleware.NewAuthService(nil, &config.AuthConfig{})
       
       _, err := authService.ValidateToken(context.Background(), "invalid-token")
       if err == nil {
           t.Error("Expected error for invalid token format")
       }
   }
   ```

### Manual Security Testing

Regular manual security testing should be performed.

1. **Penetration Testing**:
   - Perform regular penetration testing
   - Test for common vulnerabilities like SQL injection, XSS, CSRF
   - Test authentication and authorization mechanisms

2. **Code Review**:
   - Review security-critical code
   - Check for common security issues
   - Ensure secure coding practices are followed

## Security Incident Response

### Logging and Monitoring

Implement comprehensive logging and monitoring for security events.

#### Implementing Security Logging

1. **Java Logging**:
   ```java
   // Log a security event
   private void logSecurityEvent(String eventType, String username, String details) {
       String timestamp = java.time.Instant.now().toString();
       String logEntry = timestamp + "|" + eventType + "|" + username + "|" + details;
       
       // Append to log file
       try {
           Files.write(
               Paths.get(SECURITY_LOG_FILE),
               (logEntry + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
               StandardOpenOption.CREATE,
               StandardOpenOption.APPEND
           );
       } catch (IOException e) {
           // Handle error
       }
   }
   ```

2. **Go Logging**:
   ```go
   // Log a security event
   func logSecurityEvent(event string, details map[string]interface{}) {
       logger.WithFields(details).Info("Security event: " + event)
   }
   ```

### Incident Handling

Define procedures for handling security incidents.

1. **Log Collection**:
   - Collect and analyze security logs
   - Look for patterns of suspicious activity

2. **Incident Response**:
   - Define incident response procedures
   - Assign responsibilities for incident handling
   - Document and learn from incidents

## Development Guidelines

### Secure Coding Practices

Follow secure coding practices to prevent vulnerabilities.

1. **Input Validation**:
   ```java
   // Validate user input
   if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
       return false;
   }
   ```

2. **Output Encoding**:
   ```java
   // Encode output for HTML
   String encoded = StringEscapeUtils.escapeHtml4(userInput);
   ```

3. **Error Handling**:
   ```java
   try {
       // Operation that might fail
   } catch (Exception e) {
       // Log the error but don't expose internal details
       logger.error("Operation failed: {}", e.getMessage());
       return "Operation failed";
   }
   ```

4. **Secure Random Numbers**:
   ```java
   // Generate a secure random number
   SecureRandom random = new SecureRandom();
   byte[] bytes = new byte[32];
   random.nextBytes(bytes);
   ```

### Dependency Management

Keep dependencies up-to-date and secure.

1. **Dependency Scanning**:
   - Use tools like OWASP Dependency Check
   - Regularly update dependencies

2. **Vulnerability Disclosure**:
   - Have a process for handling vulnerability reports
   - Promptly address reported vulnerabilities
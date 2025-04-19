# Logging Strategy for Rinna

This document outlines the standardized logging approach for the Rinna project to ensure consistency across all modules and components.

## Logging Framework

Rinna uses SLF4J with Logback as the unified logging framework. All code should use the SLF4J API for logging.

### Dependencies

Include these dependencies in your module's pom.xml:

```xml
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
    <version>2.0.12</version>
</dependency>
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.4.14</version>
</dependency>
```

If you need to bridge JUL (java.util.logging) to SLF4J:

```xml
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>jul-to-slf4j</artifactId>
    <version>2.0.12</version>
</dependency>
```

## Logger Creation

Always obtain a logger instance using the SLF4J LoggerFactory:

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyClass {
    private static final Logger LOGGER = LoggerFactory.getLogger(MyClass.class);
    
    // Class implementation
}
```

## Log Levels and When to Use Them

### ERROR

Use for critical issues that prevent proper functioning or cause system failures.

Examples:
- Unrecoverable failures (database connection errors, file system errors)
- Failed API calls that affect core functionality
- Exceptions that will cause request processing to terminate abnormally

```java
try {
    // Critical operation
} catch (Exception e) {
    LOGGER.error("Failed to process customer transaction: {}", transactionId, e);
}
```

### WARN

Use for potential problems that don't stop execution but require attention.

Examples:
- Configuration issues that might cause problems later
- Deprecated API usage
- Automatic recovery from a failure condition
- Slow performance or higher than normal resource usage

```java
if (configValue == null) {
    LOGGER.warn("Configuration value '{}' not found, using default value: {}", configKey, defaultValue);
}
```

### INFO

Use for important application lifecycle events and significant operations.

Examples:
- Application startup/shutdown
- Service initialization
- User login/logout
- Successful completion of important business processes
- Request beginning/completion for long-running operations

```java
LOGGER.info("Application started successfully");
LOGGER.info("User {} logged in successfully", username);
```

### DEBUG

Use for detailed information useful during development and troubleshooting.

Examples:
- Method entry/exit for important methods
- Parameter values for key operations
- Process steps within complex business logic
- More detailed timing information

```java
LOGGER.debug("Processing work item {}: type={}, status={}", workItem.getId(), 
    workItem.getType(), workItem.getStatus());
```

### TRACE

Use for very fine-grained details, rarely used except in complex debugging.

Examples:
- Method entry/exit for all methods
- Variable values within loops
- Details of internal state changes
- Step-by-step execution flow

```java
LOGGER.trace("Setting repository field {} to {}", fieldName, value);
```

## Best Practices

1. **Use parameterized logging** to avoid string concatenation when the log might be filtered out:
   ```java
   // Good
   LOGGER.debug("Processing item {}", itemId);
   
   // Avoid
   LOGGER.debug("Processing item " + itemId); // String concatenation happens even if debug is disabled
   ```

2. **Include exception objects** in the log method call:
   ```java
   try {
       // Some operation
   } catch (Exception e) {
       LOGGER.error("Failed to complete operation", e);
   }
   ```

3. **Use the appropriate log level** based on the guidance above

4. **Log actionable information** that helps troubleshoot issues

5. **Don't log sensitive information** like credentials, PII, or payment details

## Configuration

- Log configuration is in `logback.xml` files in the resources directory
- Default log level is INFO for application code
- Third-party libraries default to WARN to reduce noise
- Logs are written to both console and rolling files in `~/.rinna/logs/`

## Updating Existing Code

When updating existing code that uses java.util.logging:

1. Add the SLF4J logger:
   ```java
   private static final Logger LOGGER = LoggerFactory.getLogger(MyClass.class);
   ```

2. Replace JUL logging calls with SLF4J equivalents:
   - `LOGGER.severe()` → `LOGGER.error()`
   - `LOGGER.warning()` → `LOGGER.warn()`
   - `LOGGER.info()` → `LOGGER.info()`
   - `LOGGER.fine()/finer()` → `LOGGER.debug()`
   - `LOGGER.finest()` → `LOGGER.trace()`
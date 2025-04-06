# Cross-Language Logging Standards

This document outlines the standardized logging approach used across all languages in the Rinna project.

## Core Principles

1. **Consistent log levels** across all languages
2. **Structured logging** capabilities 
3. **Contextual information** using fields/MDC
4. **Configurable output** to console and files
5. **Support for log rotation**

## Log Levels

All Rinna components use these standardized log levels:

| Level | Purpose | Examples |
|-------|---------|----------|
| **ERROR** | Critical issues that prevent proper functioning | Database connection failures, API failures, unrecoverable exceptions |
| **WARN** | Potential problems that don't stop execution | Configuration issues, deprecation warnings, automatic recovery from failures |
| **INFO** | Important application lifecycle events | Startup/shutdown, major operations successful completion, user login/logout |
| **DEBUG** | Detailed information for development | Method entry/exit, parameter values, process steps in business logic |
| **TRACE** | Very fine-grained diagnostic details | Variable values within loops, internal state changes, step-by-step execution flow |

## Language-Specific Implementation

### Java (SLF4J + Logback)

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class MyClass {
    private static final Logger LOGGER = LoggerFactory.getLogger(MyClass.class);
    
    public void doSomething() {
        // Basic logging
        LOGGER.info("Starting operation");
        
        try {
            // With parameters
            LOGGER.debug("Processing item {}", itemId);
            
            // With multiple parameters
            LOGGER.info("User {} performed action {} on resource {}", 
                        username, action, resourceId);
            
            // With context using MDC (Mapped Diagnostic Context)
            MDC.put("requestId", requestId);
            MDC.put("userId", userId);
            LOGGER.info("Request processed successfully");
            MDC.clear();
            
        } catch (Exception e) {
            // With exception
            LOGGER.error("Failed to complete operation", e);
        }
    }
}
```

Configuration is in `logback.xml`:

```xml
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${user.home}/.rinna/logs/rinna-core.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>${user.home}/.rinna/logs/rinna-core.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxFileSize>10MB</maxFileSize>
            <maxHistory>30</maxHistory>
            <totalSizeCap>100MB</totalSizeCap>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <!-- Root logger -->
    <root level="INFO">
        <appender-ref ref="CONSOLE" />
        <appender-ref ref="FILE" />
    </root>
    
    <!-- Specific loggers -->
    <logger name="org.rinna.domain" level="INFO" />
</configuration>
```

### Go

```go
import (
    "github.com/heymumford/rinna/api/pkg/logger"
)

func main() {
    // Configure the logger (optional)
    logger.Configure(logger.Config{
        Level:      logger.InfoLevel,
        TimeFormat: time.RFC3339,
        LogFile:    "/home/user/.rinna/logs/rinna-api.log",
        ShowCaller: true,
    })
    
    // Basic logging
    logger.Info("Starting API server on %s", serverAddr)
    
    // With fields (structured logging)
    logger.WithField("port", port).Info("Server listening")
    
    // Multiple fields
    logger.WithFields(map[string]interface{}{
        "requestId": requestID,
        "method":    r.Method,
        "path":      r.URL.Path,
    }).Info("Request received")
    
    // Error with formatting
    if err != nil {
        logger.Error("Failed to connect to database: %v", err)
    }
    
    // Component-specific logger
    authLogger := logger.WithPrefix("auth")
    authLogger.Debug("Validating token: %s", token)
    
    // Levels
    logger.Trace("Very detailed debug info")
    logger.Debug("Debugging information")
    logger.Info("Standard information")
    logger.Warn("Warning message")
    logger.Error("Error message")
    logger.Fatal("Fatal error, application will exit")
}
```

### Python

```python
from rinna_logger import get_logger

# Get a logger for the current module
logger = get_logger(__name__)

def process_request(request_id, user_id, data):
    # Basic logging
    logger.info("Processing request")
    
    # With parameters
    logger.debug("Request data: %s", data)
    
    # With fields (structured context)
    ctx_logger = logger.with_field("request_id", request_id)
    ctx_logger.info("Request validated")
    
    # Multiple context fields
    ctx = {
        "request_id": request_id,
        "user_id": user_id,
        "action": "process"
    }
    ctx_logger = logger.with_fields(ctx)
    
    try:
        # Processing logic
        result = process_data(data)
        ctx_logger.info("Request processed successfully")
        return result
    except Exception as e:
        # Log exception with traceback
        ctx_logger.exception("Failed to process request")
        raise
```

## Configuration

### Default Log Locations

- Java logs: `~/.rinna/logs/rinna-core.log`
- Go logs: `~/.rinna/logs/rinna-api.log`
- Python logs: `~/.rinna/logs/rinna-python.log`

### Log Format

Standard log format across all languages:

```
timestamp [level] [component] message {context fields} [file:line]
```

Example:
```
2025-04-05T12:34:56Z [INFO] [auth] User login successful user_id=1234 client_ip=192.168.1.1 [auth.go:42]
```

## Environment Variables

Common environment variables for log configuration:

- `RINNA_LOG_LEVEL`: Set logging level (`ERROR`, `WARN`, `INFO`, `DEBUG`, `TRACE`)
- `RINNA_LOG_DIR`: Override default log directory
- `RINNA_LOG_FORMAT`: Set log format (`text`, `json`)

## Best Practices

1. **Use appropriate levels**:
   - ERROR: Critical issues that prevent proper functioning
   - WARN: Potential problems that need attention
   - INFO: Important lifecycle events and operations
   - DEBUG: Detailed information for troubleshooting
   - TRACE: Very detailed diagnostic information

2. **Include context**:
   - In Java: Use SLF4J's MDC or parameterized logging
   - In Go: Use WithField() or WithFields()
   - In Python: Use with_field() or with_fields()

3. **Structured data**:
   - Log machine-processable data when possible
   - Use context fields instead of string concatenation

4. **Don't log sensitive information**:
   - No passwords, tokens, or PII
   - Mask or truncate sensitive fields

5. **Performance considerations**:
   - Use parameterized logging to avoid string concatenation cost
   - Check isDebugEnabled() before expensive operations in Java
   - Logger.Debug() won't evaluate args if debug is disabled in Go

## Migration Guidelines

When updating code that uses different logging approaches:

### Standard Go log package
```go
// Before
log.Printf("Processing request ID: %s", requestID)

// After
logger.Info("Processing request ID: %s", requestID)
```

### fmt.Printf debugging
```go
// Before
fmt.Printf("Debug: value=%v\n", value)

// After
logger.Debug("Debug: value=%v", value)
```

### Java System.out
```java
// Before
System.out.println("Starting process");

// After
LOGGER.info("Starting process");
```

### Java java.util.logging
```java
// Before
LOGGER.log(Level.SEVERE, "Failed to connect", e);

// After
LOGGER.error("Failed to connect", e);
```

### Python print statements
```python
# Before
print(f"Processing {item_id}")

# After
logger.info("Processing %s", item_id)
```
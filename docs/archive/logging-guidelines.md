# Logging Guidelines

This document was developed with analytical assistance from AI tools including Claude 3.7 Sonnet, Claude Code, and Google Gemini Deep Research, which were used as paid services. All intellectual property rights remain exclusively with the copyright holder Eric C. Mumford (@heymumford). Licensed under the Mozilla Public License 2.0.

## Overview

Effective logging is crucial for system observability, troubleshooting, and performance monitoring. This document establishes guidelines for consistent, useful logging practices across all Rinna components, regardless of programming language.

## Core Principles

1. **Purposeful**: Each log message should serve a clear purpose
2. **Consistent**: Follow consistent patterns and levels across the codebase
3. **Contextual**: Include relevant context with each message
4. **Secure**: Never log sensitive information
5. **Performant**: Minimize logging overhead in production
6. **Searchable**: Structure logs to facilitate searching and filtering

## Log Levels

Use the following standard log levels consistently:

| Level | Purpose | Examples |
|-------|---------|----------|
| **FATAL** | Critical errors causing system shutdown | Database connection permanently lost, critical configuration missing |
| **ERROR** | Runtime errors that require attention | Exception during request processing, component initialization failure |
| **WARN** | Potential issues that don't prevent operation | Deprecated API usage, retrying a failed operation, slow query warning |
| **INFO** | Significant application events | System startup/shutdown, user login/logout, job start/end |
| **DEBUG** | Detailed information for development and troubleshooting | Method entry/exit, configuration values, intermediate calculation results |
| **TRACE** | Extremely detailed diagnostic information | Raw HTTP requests/responses, SQL queries with parameters |

## Log Message Format

### Standard Format

```
[TIMESTAMP] [LEVEL] [SERVICE] [COMPONENT] [REQUEST_ID] [USER_ID] [MESSAGE] [METADATA]
```

Example:
```
[2025-04-06T21:45:25.123Z] [INFO] [api-service] [UserController] [req-abc123] [user-456] User profile updated successfully {"changes": ["email", "preferences"]}
```

### Structured Logging

When possible, use structured logging in JSON format:

```json
{
  "timestamp": "2025-04-06T21:45:25.123Z",
  "level": "INFO",
  "service": "api-service",
  "component": "UserController",
  "request_id": "req-abc123",
  "user_id": "user-456",
  "message": "User profile updated successfully",
  "metadata": {
    "changes": ["email", "preferences"],
    "duration_ms": 156
  }
}
```

## Cross-Language Guidelines

### Java

Use SLF4J with Logback as the implementation:

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyClass {
    private static final Logger logger = LoggerFactory.getLogger(MyClass.class);
    
    public void doSomething() {
        logger.info("Operation started");
        try {
            // Do work
            logger.debug("Intermediate result: {}", result);
        } catch (Exception e) {
            logger.error("Operation failed", e);
        }
    }
}
```

### Golang

Use the standard log package or structured logging libraries like zap:

```go
import (
    "go.uber.org/zap"
)

func main() {
    logger, _ := zap.NewProduction()
    defer logger.Sync()
    
    logger.Info("Server starting",
        zap.String("service", "api-service"),
        zap.Int("port", 8080))
    
    // Log with error
    logger.Error("Failed to connect to database",
        zap.String("db", "users"),
        zap.Error(err))
}
```

### Python

Use the standard logging module:

```python
import logging

# Configure once at application startup
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s [%(levelname)s] %(name)s - %(message)s',
    datefmt='%Y-%m-%dT%H:%M:%S%z'
)

logger = logging.getLogger(__name__)

def process_item(item):
    logger.info("Processing item %s", item.id)
    try:
        # Process item
        logger.debug("Item details: %s", item.details)
    except Exception as e:
        logger.error("Failed to process item %s: %s", item.id, str(e), exc_info=True)
```

### Shell Scripts

Use a consistent logging function:

```bash
#!/bin/bash

LOG_LEVEL=${LOG_LEVEL:-INFO}

log() {
    local level=$1
    local message=$2
    
    # Only log if the level is appropriate
    case $LOG_LEVEL in
        DEBUG) log_allowed=1 ;;
        INFO) [[ $level != "DEBUG" ]] && log_allowed=1 ;;
        WARN) [[ $level != "DEBUG" && $level != "INFO" ]] && log_allowed=1 ;;
        ERROR) [[ $level == "ERROR" ]] && log_allowed=1 ;;
        *) log_allowed=0 ;;
    esac
    
    if [[ $log_allowed -eq 1 ]]; then
        echo "$(date '+%Y-%m-%dT%H:%M:%S%z') [$level] $message"
    fi
}

log "INFO" "Script started"
log "DEBUG" "Debug information"
log "ERROR" "Something went wrong"
```

## Context Guidelines

Include the following context when appropriate:

1. **Request IDs**: For tracing requests across microservices
2. **User IDs**: For user-specific actions (anonymize if necessary)
3. **Operation Duration**: For performance monitoring
4. **Resource IDs**: For operations on specific resources
5. **Application Version**: For correlating issues with releases

## MDC (Mapped Diagnostic Context)

Use MDC (or equivalent) to automatically include context in logs:

```java
// Java example
MDC.put("requestId", request.getRequestId());
MDC.put("userId", user.getId());
try {
    // All logs in this block will include requestId and userId
    logger.info("Processing user request");
} finally {
    MDC.clear(); // Clear the context when done
}
```

## Error Logging

When logging exceptions:

1. Include the full stack trace
2. Add context about the operation being performed
3. Include relevant variable values (but no sensitive data)
4. Use consistent formatting for similar errors

Example:
```java
try {
    // Operation
} catch (Exception e) {
    logger.error("Failed to process payment for order {}: {}", 
                 orderId, e.getMessage(), e);
}
```

## Security Considerations

### Never Log

1. **Credentials**: Passwords, API keys, tokens
2. **Personal Data**: Social Security numbers, credit card numbers, etc.
3. **Authentication Details**: Session IDs, cookies
4. **Sensitive Business Data**: Internal pricing, strategy documents

### Data Masking

For potentially sensitive fields, use masking:

```
User email: j***@e***.com
Credit card: ************1234
```

## Performance Considerations

1. Use guard clauses to avoid unnecessary string formatting:
   ```java
   // Good
   if (logger.isDebugEnabled()) {
       logger.debug("Complex calculation result: {}", calculateExpensiveValue());
   }
   
   // Bad - calculateExpensiveValue() is called even if debug is disabled
   logger.debug("Complex calculation result: {}", calculateExpensiveValue());
   ```

2. Use parameterized logging instead of string concatenation:
   ```java
   // Good
   logger.info("User {} performed action {}", userId, action);
   
   // Bad - string concatenation happens regardless of log level
   logger.info("User " + userId + " performed action " + action);
   ```

3. Be mindful of log volume in tight loops:
   ```java
   // Avoid logging in tight loops
   for (Item item : thousandsOfItems) {
       process(item);
       // Don't do this for every iteration
       // logger.debug("Processed item {}", item.getId());
   }
   
   // Instead, log summaries
   logger.info("Processed {} items in {} ms", items.size(), duration);
   ```

## Configuration

### Log Configuration Files

Store logging configuration in external files, not in code:

- Java: `logback.xml` or `log4j2.xml`
- Python: `logging.conf`
- Golang: Configuration structs

### Environment-Specific Settings

| Environment | Default Level | Rotation | Retention |
|-------------|---------------|----------|-----------|
| Development | DEBUG | Daily | 7 days |
| Testing | INFO | Daily | 14 days |
| Staging | INFO | Daily | 30 days |
| Production | WARN | Hourly | 90 days |

### Feature Flags

Use feature flags to enable detailed debugging for specific components:

```java
if (featureFlags.isEnabled("debug-payment-service")) {
    logger.debug("Payment processing debug: {}", detailedInfo);
}
```

## Monitoring and Alerting

1. Set up alerts for ERROR and FATAL log messages
2. Monitor log volume and patterns for anomalies
3. Create dashboards for key metrics derived from logs
4. Establish log-based SLOs (Service Level Objectives)

## Rotation and Retention

1. Configure log rotation based on size or time
2. Establish retention periods based on data needs and compliance requirements
3. Archive logs before deletion for long-term storage if needed
4. Ensure proper permissions on log files

## Conclusion

Following these guidelines will ensure consistent, useful logs across all Rinna components, enabling effective troubleshooting, monitoring, and system observability.
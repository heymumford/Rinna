# Rinna Logging Guide for Developers

This guide provides detailed instructions for working with the Rinna logging system across all language components. Our logging strategy is designed to support both local development and cloud deployment scenarios while maintaining consistency across our polyglot architecture.

## Table of Contents

- [Overview](#overview)
- [Logging Directory Structure](#logging-directory-structure)
- [Environment Configuration](#environment-configuration)
- [Language-Specific Implementation](#language-specific-implementation)
  - [Java Component](#java-component)
  - [Python Component](#python-component)
  - [Go Component](#go-component)
- [Log Levels](#log-levels)
- [Structured Logging Format](#structured-logging-format)
- [Cross-Component Correlation](#cross-component-correlation)
- [Local Development Tools](#local-development-tools)
- [Local Monitoring](#local-monitoring)
- [Cloud Deployment Considerations](#cloud-deployment-considerations)
- [Testing Logging](#testing-logging)
- [Performance Considerations](#performance-considerations)

## Overview

Rinna uses a unified logging approach across all components with environment-aware configuration. Key principles include:

- Consistent log structure across Java, Python, and Go components
- Environment-based behavior (local vs. cloud)
- Developer-friendly local experience with file and console output
- Cloud-ready integration with Azure Monitor
- Cross-component request correlation
- Container-friendly design

## Logging Directory Structure

Each language component maintains its own logging directory:

```
/java/logs/       # Java component logs
/python/logs/     # Python component logs
/go/logs/         # Go component logs
```

These directories are included in `.gitignore` but have `.gitkeep` files to ensure the directories are created on checkout.

## Environment Configuration

The logging system adapts based on the detected environment:

- **LOCAL**: Developer-friendly file and console logging (default)
- **DEV**: Extended logging with testing features
- **PROD**: Performance-optimized cloud integration

The environment is determined by:

1. `RINNA_ENV` environment variable
2. Configuration file settings
3. Default fallback to LOCAL when not specified

## Language-Specific Implementation

### Java Component

The Java component uses SLF4J with Logback for logging.

#### Configuration

The Logback configuration file (`java/src/main/resources/logback.xml`) contains environment-specific profiles:

```xml
<configuration>
  <!-- Common appenders and defaults -->
  
  <!-- Choose profile based on environment -->
  <if condition='property("RINNA_ENV").equals("PROD")'>
    <then>
      <!-- Production profile -->
    </then>
    <elseif condition='property("RINNA_ENV").equals("DEV")'>
      <!-- Development profile -->
    </elseif>
    <else>
      <!-- Local development profile -->
    </else>
  </if>
</configuration>
```

#### Usage

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.rinna.logging.RinnaLogContext;

public class MyService {
    private static final Logger logger = LoggerFactory.getLogger(MyService.class);
    
    public void doSomething() {
        // Simple logging
        logger.info("Starting operation");
        
        // With context
        RinnaLogContext.with("operation", "doSomething")
            .with("itemId", "123")
            .info(logger, "Operation details");
            
        // Error handling
        try {
            // Business logic
        } catch (Exception e) {
            logger.error("Operation failed", e);
        }
    }
}
```

### Python Component

The Python component uses the standard library logging module with custom handlers.

#### Configuration

Configuration is loaded from `python/rinna/logging_config.py`:

```python
import os
import logging
import logging.config
import json

def configure_logging():
    """Configure logging based on environment"""
    env = os.environ.get('RINNA_ENV', 'LOCAL')
    
    config = {
        # Base configuration
        'version': 1,
        'formatters': {...},
        'handlers': {...},
        'loggers': {...}
    }
    
    # Apply environment-specific settings
    if env == 'PROD':
        # Production settings
        pass
    elif env == 'DEV':
        # Development settings
        pass
    else:
        # Local development settings
        pass
        
    logging.config.dictConfig(config)
```

#### Usage

```python
from rinna.logging import get_logger, log_context

# Get module logger
logger = get_logger(__name__)

def process_item(item_id):
    # Simple logging
    logger.info(f"Processing item {item_id}")
    
    # With context
    with log_context(operation="process", item_id=item_id):
        # Business logic
        logger.info("Operation in progress")
        
    # Error handling
    try:
        # More business logic
    except Exception as e:
        logger.exception("Processing failed")
```

### Go Component

The Go component uses zerolog for structured logging.

#### Configuration

Configuration is in `go/pkg/logger/logger.go`:

```go
package logger

import (
    "os"
    "github.com/rs/zerolog"
    "github.com/rs/zerolog/log"
)

// Setup configures the logger based on environment
func Setup() {
    env := os.Getenv("RINNA_ENV")
    
    // Default configuration
    zerolog.TimeFieldFormat = zerolog.TimeFormatISO8601
    
    // Environment-specific settings
    switch env {
    case "PROD":
        // Production settings
        zerolog.SetGlobalLevel(zerolog.InfoLevel)
        log.Logger = log.With().Caller().Logger()
    case "DEV":
        // Development settings
        zerolog.SetGlobalLevel(zerolog.DebugLevel)
        log.Logger = log.With().Caller().Logger()
    default:
        // Local development settings
        zerolog.SetGlobalLevel(zerolog.DebugLevel)
        log.Logger = log.Output(zerolog.ConsoleWriter{Out: os.Stdout}).With().Caller().Logger()
    }
}
```

#### Usage

```go
package mypackage

import (
    "github.com/rs/zerolog/log"
    "github.com/organization/rinna/pkg/logger"
)

func ProcessItem(itemID string) error {
    // Simple logging
    log.Info().Str("item_id", itemID).Msg("Processing item")
    
    // With context
    ctx := logger.WithContext(context.Background(), "operation", "process")
    logger := log.Ctx(ctx)
    
    // Business logic
    
    // Error handling
    if err != nil {
        logger.Error().Err(err).Msg("Processing failed")
        return err
    }
    
    return nil
}
```

## Log Levels

Use consistent log levels across all components:

| Level | Usage |
|-------|-------|
| ERROR | System errors requiring immediate attention |
| WARN  | Potentially harmful situations that should be addressed |
| INFO  | General operational information |
| DEBUG | Detailed information for troubleshooting |
| TRACE | Highly detailed tracing information (development only) |

Guidelines for level selection:

- **ERROR**: Use for exceptions that prevent normal operation
- **WARN**: Use for recoverable issues or deprecated feature usage
- **INFO**: Use for key application events (startup, shutdown, major operations)
- **DEBUG**: Use for detailed troubleshooting information
- **TRACE**: Use for method entry/exit or detailed variable values

## Structured Logging Format

All logs should follow a consistent structured format:

```json
{
  "timestamp": "2025-04-19T10:15:30.123Z",
  "level": "INFO",
  "component": "java",
  "service": "workflow-service",
  "message": "Processing workflow transition",
  "correlation_id": "abc-123-xyz",
  "context": {
    "workflow_id": "WF-123",
    "from_state": "DRAFT",
    "to_state": "REVIEW"
  }
}
```

Required fields:
- `timestamp`: ISO 8601 format with milliseconds 
- `level`: Log level in uppercase
- `component`: Language component (java, python, go)
- `service`: Specific service or module name
- `message`: Human-readable log message
- `correlation_id`: Unique ID for request tracing (when available)
- `context`: Object containing event-specific data

## Cross-Component Correlation

For tracing requests across components:

1. **Correlation ID Generation**:
   - Generated at system entry points (API, CLI, etc.)
   - Format: UUID v4

2. **Propagation**:
   - HTTP: Include in `X-Correlation-ID` header
   - CLI: Pass as environment variable
   - Message Queue: Include in message metadata

3. **Usage**:
   ```java
   // Java example
   String correlationId = request.getHeader("X-Correlation-ID");
   MDC.put("correlation_id", correlationId);
   ```

   ```python
   # Python example
   correlation_id = request.headers.get("X-Correlation-ID")
   log_context.set("correlation_id", correlation_id)
   ```

   ```go
   // Go example
   correlationID := r.Header.Get("X-Correlation-ID")
   ctx = context.WithValue(ctx, "correlation_id", correlationID)
   ```

## Local Development Tools

The Rinna CLI provides tools for log management:

```bash
# View logs with filtering
rin logs view [--component=<java|python|go>] [--level=<ERROR|WARN|INFO|DEBUG|TRACE>]

# Clear log files
rin logs clear [--component=<java|python|go>]

# Follow logs in real-time
rin logs follow [--component=<java|python|go>]
```

Implementation details are in the CLI component.

## Local Monitoring

For advanced log analysis, you can use the built-in monitoring stack:

```bash
# Start monitoring stack
rin logs monitor

# Access the dashboard at http://localhost:3000
```

This launches a Docker Compose stack with:
- Grafana for visualization
- Loki for log aggregation
- Pre-configured dashboards for Rinna components

The Docker Compose file is located at `docker/monitoring/docker-compose.yml`.

## Cloud Deployment Considerations

When deploying to Azure:

1. **Container Logging**:
   - Log to stdout/stderr instead of files
   - Structured JSON format for all logs
   - Include container metadata in context

2. **Azure Integration**:
   - Use Application Insights SDK for each language
   - Configure connection string via environment variables
   - Enable distributed tracing

3. **Configuration**:
   ```
   RINNA_ENV=PROD
   APPLICATIONINSIGHTS_CONNECTION_STRING=<your-connection-string>
   ```

## Testing Logging

Write tests to verify logging behavior:

1. **Unit Tests**:
   - Verify log messages and levels
   - Test environment detection
   - Test correlation ID propagation

2. **Component Tests**:
   - Verify log file creation
   - Test structured format compliance
   - Validate cross-component correlation

3. **Example Test**:
   ```java
   @Test
   public void testLoggerOutputsToFile() {
       // Arrange
       System.setProperty("RINNA_ENV", "LOCAL");
       File logFile = new File("java/logs/rinna.log");
       logFile.delete();
       
       // Act
       Logger logger = LoggerFactory.getLogger("test");
       logger.info("Test message");
       
       // Assert
       assertTrue(logFile.exists());
       assertTrue(Files.readString(logFile.toPath()).contains("Test message"));
   }
   ```

## Performance Considerations

Optimize logging performance:

1. **Check Level Before Logging**:
   ```java
   // Good practice
   if (logger.isDebugEnabled()) {
       logger.debug("Complex message: " + generateComplexMessage());
   }
   ```

2. **Use Parameterized Logging**:
   ```java
   // Good practice
   logger.info("Processing item {}", itemId);
   
   // Avoid
   logger.info("Processing item " + itemId);
   ```

3. **Sampling in Production**:
   - Log only a percentage of high-volume events
   - Configure sampling rates in environment settings

4. **Async Logging**:
   - Use asynchronous appenders/handlers in production
   - Configure appropriate buffer sizes

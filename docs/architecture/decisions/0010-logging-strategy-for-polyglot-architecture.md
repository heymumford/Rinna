# 0010: Logging Strategy for Polyglot Architecture

## Status

Proposed

## Context

Rinna is a polyglot workflow management system consisting of components written in Java, Go, and Python. As we develop Rinna with an eye toward both local development and eventual containerized deployment to Azure cloud, we need a coherent logging strategy that addresses the following concerns:

- **Polyglot consistency**: How to maintain a consistent logging approach across Java, Go, and Python components
- **Environment awareness**: Supporting both local development and cloud deployment scenarios
- **Developer experience**: Making logs easily accessible for developers working locally
- **Cloud readiness**: Integration with Azure Monitor and Application Insights
- **Containerization**: Supporting log aggregation in containerized environments
- **Cross-service correlation**: Tracing requests across different components
- **Performance impact**: Minimizing overhead in high-throughput scenarios

The logging approach should also align with Rinna's clean architecture principles and respect the separation of concerns.

## Decision

We will implement a hybrid, environment-aware logging strategy with the following key characteristics:

### 1. Directory Structure

- Each language-specific component will maintain its own logs directory:
  ```
  /java/logs/
  /python/logs/
  /go/logs/
  ```

- No central `/logs` directory at the project root to maintain language-specific separation

### 2. Environment-Based Configuration

- Logging behavior will adapt based on the detected environment:
  - **LOCAL**: File-based logging with developer-friendly formatting
  - **DEV**: Extended logging with both file and centralized outputs
  - **PROD**: Optimized, structured logging with Azure integration

- Environment detection through:
  - `RINNA_ENV` environment variable
  - Configuration file settings
  - Default fallback to LOCAL when not specified

### 3. Logging Interface

- Each language component will implement a consistent logging interface:
  - **Java**: SLF4J + Logback with custom appenders
  - **Python**: Standard library logging with custom handlers
  - **Go**: zerolog or zap with environment-specific sinks

- Common log levels across all components:
  - ERROR: System errors requiring immediate attention
  - WARN: Potentially harmful situations
  - INFO: General operational information
  - DEBUG: Detailed information for debugging
  - TRACE: Highly detailed tracing information

### 4. Structured Logging

- JSON structured logging format for machine processing
- Configurable human-readable format for local development
- Required fields in all log records:
  - timestamp
  - service name
  - component
  - log level
  - message
  - correlation ID (when available)

### 5. Local Development Support

- Console output with color coding for improved readability
- File-based logs with appropriate rotation
- CLI utilities for viewing and managing logs:
  - `rin logs view [--component] [--level]`
  - `rin logs clear`
  - `rin logs monitor` (launches local monitoring dashboard)

### 6. Cloud Integration

- Azure Application Insights integration for cloud deployments
- Standard output logging for containerized environments
- Container metadata included in log context

### 7. Cross-Language Correlation

- Correlation ID generation at system entry points
- ID propagation across language boundaries
- Context carrying for related log entries

## Consequences

### Positive

- Developers get a consistent experience across all language components
- Local development remains simple with files and console output
- Smooth transition path to cloud deployment
- Support for request tracing across components
- Clean separation of concerns aligning with architecture

### Negative

- Implementation complexity across three languages
- Additional dependencies for each component
- Potential performance overhead of structured logging
- Training required for developers to understand the system

### Mitigations

- Create shared libraries/utilities for each language to minimize duplication
- Implement log sampling for high-volume production environments
- Provide comprehensive documentation and examples
- Create automated tests to verify logging behavior

## Implementation Plan

The implementation will proceed in phases:

1. **Phase 1**: Core logging infrastructure
   - Implement basic logging infrastructure in each language
   - Create environment detection mechanism
   - Establish file-based logging for local development

2. **Phase 2**: Local development experience
   - Implement CLI commands for log management
   - Create console formatting for improved readability
   - Build local monitoring solution with Docker Compose

3. **Phase 3**: Cross-language correlation
   - Implement correlation ID generation and propagation
   - Add context carrying across boundaries
   - Create testing utilities for correlation verification

4. **Phase 4**: Cloud integration
   - Integrate Azure Application Insights
   - Configure container logging
   - Implement log sampling for performance

All phases will follow TDD principles with comprehensive testing.

## References

- [The Twelve-Factor App: Logs](https://12factor.net/logs)
- [Azure Application Insights Documentation](https://docs.microsoft.com/en-us/azure/azure-monitor/app/app-insights-overview)
- [Structured Logging Best Practices](https://www.honeycomb.io/blog/structured-logging-best-practices)
- [OpenTelemetry Specification](https://github.com/open-telemetry/opentelemetry-specification)

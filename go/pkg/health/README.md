# Health Check API

This package provides a standardized health check API for the Rinna application. It includes:

- `/health` - Provides detailed health information
- `/health/live` - Liveness probe for Kubernetes
- `/health/ready` - Readiness probe for Kubernetes

## Features

- Customizable dependency checking
- Standard response format
- Support for Kubernetes liveness and readiness probes
- Detailed service status reporting

## Usage

```go
// Create a dependency checker
javaChecker := &MyJavaChecker{} // implements health.DependencyChecker

// Create the health handler
healthHandler := health.NewHandler(javaChecker)

// Register the routes on your router
router := mux.NewRouter()
healthHandler.RegisterRoutes(router)
```

## Implementing a Dependency Checker

To check the health of a dependency, implement the `DependencyChecker` interface:

```go
type MyDependencyChecker struct {
    // your fields here
}

func (c *MyDependencyChecker) CheckHealth() health.ServiceStatus {
    // Perform your health check logic
    
    // Return status
    return health.ServiceStatus{
        Status:    "ok", // or "error", "degraded", etc.
        Message:   "Optional message explaining the status",
        Timestamp: time.Now().Format(time.RFC3339),
    }
}
```

## Response Format

The `/health` endpoint returns:

```json
{
  "status": "ok",
  "timestamp": "2025-01-01T00:00:00Z",
  "version": "0.1.0",
  "commitSha": "abc123",
  "buildTime": "2025-01-01T00:00:00Z",
  "goVersion": "go1.20",
  "hostname": "server1",
  "services": {
    "java": {
      "status": "ok",
      "timestamp": "2025-01-01T00:00:00Z"
    }
  }
}
```

The `/health/live` endpoint returns:

```json
{
  "status": "ok",
  "timestamp": "2025-01-01T00:00:00Z"
}
```

The `/health/ready` endpoint returns:

```json
{
  "status": "ok", // or "degraded" if any service is not healthy
  "timestamp": "2025-01-01T00:00:00Z",
  "services": {
    "java": {
      "status": "ok",
      "timestamp": "2025-01-01T00:00:00Z"
    }
  }
}
```

## Status Codes

- `/health` always returns `200 OK`
- `/health/live` always returns `200 OK`
- `/health/ready` returns:
  - `200 OK` if all dependencies are healthy
  - `503 Service Unavailable` if any dependency is unhealthy
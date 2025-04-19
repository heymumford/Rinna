# Rinna Configuration System

Rinna uses a unified configuration system across all languages (Java, Go, Python) to maintain a single source of truth for all settings.

> **Note**: The current project version is **1.10.2**. All version references in configuration files should match this version.

## Core Principles

1. **Single Source of Truth**: All configuration starts from a central YAML file.
2. **Environment-Specific Overrides**: Different environments (dev, staging, prod) can override settings.
3. **User-Specific Overrides**: Developers can customize settings without affecting others.
4. **Environment Variables**: Highest precedence for runtime configuration and secrets.
5. **Language-Specific Adapters**: Each language gets its idiomatic configuration interface.

## Configuration Files

The configuration system uses these files:

| File | Purpose | Version Controlled | 
|------|---------|-------------------|
| `config/rinna.yaml` | Main configuration with defaults | Yes |
| `config/rinna-{env}.yaml` | Environment-specific overrides | Yes |
| `~/.rinna/config/rinna-{env}.yaml` | User-specific overrides | No |
| `~/.rinna/config/{language}/...` | Generated language-specific files | No |

## Configuration Hierarchy

Configuration values are resolved in this order (highest precedence first):

1. Environment variables with `RINNA_` prefix
2. User environment config (`~/.rinna/config/rinna-{env}.yaml`)
3. Project environment config (`config/rinna-{env}.yaml`)
4. Main config (`config/rinna.yaml`)
5. Default values in code

## Using the Configuration CLI

The `rin config` command provides a unified interface to the configuration system:

```bash
# View the effective configuration
rin config view

# Get a specific value
rin config get go.api.port

# Set a user-specific value
rin config set java.backend.port 8091

# Generate language-specific config files
rin config generate

# Create environment configs
rin config create-env staging
rin config create-user development

# Validate configuration
rin config validate
```

## Environment-Specific Configuration

Switch between environments using the `RINNA_ENV` environment variable:

```bash
# Use staging environment
export RINNA_ENV=staging
rin config view

# Use production environment
export RINNA_ENV=production
rin config view
```

## Sensitive Configuration

**Never** store sensitive values in version-controlled configuration files. Instead:

1. Use environment variables:
   ```bash
   export RINNA_JAVA_DOCUMENT_SERVICE_DOCMOSIS_LICENSE_KEY="your-key"
   ```

2. Store in user config files:
   ```bash
   rin config set java.document_service.docmosis.license_key "your-key"
   ```

## Using Configuration in Code

### Java

```java
import org.rinna.config.RinnaConfig;

// Get an instance of the config
RinnaConfig config = RinnaConfig.getInstance();

// Get configuration values
int port = config.getInt("java.backend.port", 8090);
boolean enableSwagger = config.getBoolean("java.backend.enable_swagger", true);
String apiUrl = config.getString("go.api.url");
```

### Go

```go
import "github.com/heymumford/rinna/api/pkg/config"

// Load configuration
cfg, err := config.GetConfig()
if err != nil {
    log.Fatalf("Failed to load configuration: %v", err)
}

// Access configuration values
port := cfg.Go.API.Port
javaUrl := cfg.Go.Backend.JavaURL
```

### Python

```python
from rinna_config import config

# Access configuration values
api_key = config.get("python.diagrams.lucidchart.api_key")
output_dir = config.get_path("python.diagrams.output_dir")
enabled = config.get_bool("java.backend.enable_swagger", True)
```

## Adding New Configuration

When adding new configuration parameters:

1. Add them to `config/rinna.yaml` with sensible defaults
2. Update the appropriate language-specific configurations
3. Document any required environment variables

## Common Configuration Keys

| Key | Description | Default |
|-----|-------------|---------|
| `project.name` | Project name | "Rinna" |
| `project.version` | Project version | "1.10.2" |
| `project.environment` | Current environment | "development" |
| `java.backend.port` | Java backend port | 8090 |
| `go.api.port` | Go API server port | 8080 |
| `go.backend.java_url` | URL to Java backend | "http://localhost:8090/api/v1" |

See `config/rinna.yaml` for the complete configuration schema.
# Rinna Project Naming Conventions

This document defines standardized naming conventions for the Rinna project across all languages and file types.

## General Principles

1. Be consistent within each language ecosystem
2. Use descriptive, meaningful names
3. Avoid abbreviations unless universally understood
4. Be explicit about file/class purpose through naming

## Java Conventions

### Package Structure

```
org.rinna.domain.model        // Domain entities/models
org.rinna.domain.repository   // Repository interfaces
org.rinna.domain.service      // Service interfaces
org.rinna.adapter.service     // Service implementations
org.rinna.adapter.repository  // Repository implementations
org.rinna.config              // Configuration code
org.rinna.api                 // API-related code
```

### Classes and Interfaces

| Type | Convention | Example |
|------|------------|---------|
| Interface | Descriptive name | `WorkItem.java`, `Repository.java` |
| Implementation | Prefix with implementation type | `DefaultWorkItem.java`, `InMemoryRepository.java` |
| DTO/Request | Suffix with purpose | `WorkItemCreateRequest.java` |
| Exception | Suffix with Exception | `InvalidTransitionException.java` |

### Test Classes

| Type | Convention | Example |
|------|------------|---------|
| Unit Tests | Suffix with Test | `WorkflowServiceTest.java` |
| Integration Tests | Suffix with IT | `WorkflowServiceIT.java` |
| BDD Tests | Suffix with Runner/Steps | `WorkflowRunner.java`, `WorkflowSteps.java` |

## Go Conventions

### File Naming

| Type | Convention | Example |
|------|------------|---------|
| Implementation | snake_case.go | `work_item.go` |
| Test files | snake_case_test.go | `work_item_test.go` |
| Test helpers | snake_case_helper.go | `test_helper.go` |

### Package Naming

| Type | Convention | Example |
|------|------------|---------|
| Models/Entities | Singular form | `model` not `models` |
| Collections | Plural form | `handlers` not `handler` |
| Utility | Purpose-based name | `health`, `config`, `logger` |

### Directory Structure

```
/api
  /cmd          // Application entry points
  /internal     // Application-specific packages
    /handlers   // HTTP handlers
    /middleware // HTTP middleware
    /models     // Internal data models
  /pkg          // Reusable packages
    /health     // Health check functionality
    /config     // Configuration utilities
    /logger     // Logging functionality
  /test         // Tests that cross package boundaries
```

## Shell Scripts

| Type | Convention | Example |
|------|------------|---------|
| All scripts | kebab-case.sh | `run-tests.sh`, `start-services.sh` |
| Test scripts | test-*.sh | `test-unit.sh`, `test-integration.sh` |
| Build scripts | build-*.sh | `build-api.sh`, `build-java.sh` |
| Utility scripts | utility-*.sh | `utility-version.sh` |

## Configuration Files

| Type | Convention | Example |
|------|------------|---------|
| Properties | lowercase.properties | `application.properties` |
| YAML | kebab-case.yaml/yml | `config.yaml`, `docker-compose.yml` |
| XML | kebab-case.xml | `logback.xml`, `pom.xml` |

## Documentation

| Type | Convention | Example |
|------|------------|---------|
| Markdown | UPPERCASE.md | `README.md`, `CONTRIBUTING.md` |
| User guides | kebab-case.md | `user-guide.md`, `installation-guide.md` |
| Technical docs | kebab-case.md | `architecture.md`, `api-specification.md` |

## Module Structure

```
/home/emumford/NativeLinuxProjects/Rinna/  // Root project directory
  /src                                      // Main source code (consolidated)
    /main
      /java
        /org/rinna/...                      // Java packages
      /resources                            // Application resources
    /test
      /java
        /org/rinna/...                      // Test packages
      /resources                            // Test resources
  /api                                      // Go API code
  /bin                                      // Shell scripts and utilities
  /docs                                     // Documentation
  /config                                   // Configuration files
```

## Enforcement

These conventions will be enforced through:

1. Code review practices
2. Checkstyle rules (Java)
3. golangci-lint configuration (Go)
4. Shell script linting
5. Git pre-commit hooks

## Migration

1. New code should follow these conventions immediately
2. Existing code will be migrated according to the plan in `structure-plan.md`
3. IDEs should be configured to use these conventions for new files
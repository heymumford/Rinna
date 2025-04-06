# Package Structure Refactoring

## Overview

The Rinna project has undergone a significant package structure refactoring to flatten the hierarchy and improve maintainability. This document explains the changes made and provides guidance for working with both the old and new package structures.

## Changes Made

- Domain entities moved from `org.rinna.domain.entity` to `org.rinna.domain.model` 
- Service interfaces moved from `org.rinna.domain.usecase` to `org.rinna.domain.service`
- Service implementations moved from `org.rinna.service.impl` to `org.rinna.adapter.service`
- Repository implementations moved from `org.rinna.persistence` to `org.rinna.adapter.repository`

## New Package Structure

```
org.rinna
  ├── adapter       # Framework adapters (outside layer)
  │   ├── repository  # Concrete repository implementations
  │   └── service     # Service implementations
  ├── config        # Application configuration
  ├── domain        # Core domain (inner layer)
  │   ├── model       # Domain model entities
  │   ├── repository  # Repository interfaces
  │   └── service     # Service interfaces
```

## Transition Status

| Module | Status | Notes |
|--------|--------|-------|
| rinna-core | ✅ Complete | All tests passing with new structure |
| src (main) | 🔄 In Progress | Work in progress |
| API | 🔄 In Progress | Planned for next phase |
| CLI | 🔄 In Progress | Planned for next phase |

## Working with the New Structure

### Importing Classes

Use the new package structure for imports:

```java
// OLD
import org.rinna.domain.entity.WorkItem;
import org.rinna.domain.usecase.ItemService;
import org.rinna.service.impl.DefaultItemService;
import org.rinna.persistence.InMemoryItemRepository;

// NEW 
import org.rinna.domain.model.WorkItem;
import org.rinna.domain.service.ItemService;
import org.rinna.adapter.service.DefaultItemService;
import org.rinna.adapter.repository.InMemoryItemRepository;
```

### Creating Services

```java
// Create repositories
var itemRepo = new InMemoryItemRepository();
var metadataRepo = new InMemoryMetadataRepository();

// Create service
ItemService itemService = new DefaultItemService(itemRepo);
```

### Using Rinna's Initialization

The simplest way to get started is to use Rinna's factory initialization:

```java
// Initialize with default implementations
Rinna rinna = Rinna.initialize();

// Access services
ItemService itemService = rinna.items();
WorkflowService workflowService = rinna.workflow();
ReleaseService releaseService = rinna.releases();
QueueService queueService = rinna.queue();
```

## Migration Scripts

Several scripts have been created to help with the migration:

- `bin/migration/fix-test-imports.sh`: Updates import statements in test files
- `bin/migration/setup-src-structure.sh`: Sets up the correct directory structure for the src directory

## Recommendations

1. **For New Code**: Always use the new package structure
2. **For Existing Code**: 
   - When making changes to existing files, update import statements to use the new package structure
   - Run tests to ensure compatibility

## Next Steps

1. Complete the migration of the src (main) module
2. Migrate the API module
3. Migrate the CLI module
4. Remove legacy compatibility classes

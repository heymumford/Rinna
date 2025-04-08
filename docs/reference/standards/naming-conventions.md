# Naming Conventions

This document was developed with analytical assistance from AI tools including Claude 3.7 Sonnet, Claude Code, and Google Gemini Deep Research, which were used as paid services. All intellectual property rights remain exclusively with the copyright holder Eric C. Mumford (@heymumford). Licensed under the Mozilla Public License 2.0.

## Overview

Consistent naming conventions are crucial for code readability, maintainability, and collaboration. This document defines the standard naming conventions to be used across all Rinna components, regardless of programming language.

## General Principles

1. **Clarity**: Names should clearly communicate purpose and meaning
2. **Consistency**: Similar concepts should use similar names throughout the codebase
3. **Conciseness**: Names should be concise while remaining descriptive
4. **Convention**: Follow established conventions for each language/framework
5. **Context**: Consider the context when choosing names

## Cross-Language Guidelines

### Terminology Consistency

Use consistent terminology across all components:

| Concept | Preferred Term | Avoid |
|---------|---------------|-------|
| User account | `user` | `account`, `profile` |
| Authentication | `auth` | `login`, `security` |
| Configuration | `config` | `settings`, `prefs` |
| Database | `db` | `database`, `store` |
| Initialization | `init` | `setup`, `bootstrap` |
| Utility functions | `utils` | `helpers`, `tools` |

### Abbreviations

- Use only widely accepted abbreviations
- Document any non-obvious abbreviations in project glossary
- Be consistent with abbreviation usage
- Prefer full words for clarity when space permits

**Accepted Abbreviations:**

| Abbreviation | Full Form |
|--------------|-----------|
| `config` | Configuration |
| `db` | Database |
| `pkg` | Package |
| `impl` | Implementation |
| `auth` | Authentication |
| `repo` | Repository |
| `util` | Utility |
| `ctx` | Context |
| `req` | Request |
| `res` | Response |
| `msg` | Message |
| `svc` | Service |

## Language-Specific Conventions

### Java

#### Class Names
- **PascalCase** (e.g., `WorkItemProcessor`)
- Nouns or noun phrases
- Descriptive and specific
- No abbreviations except for common ones (e.g., `HTTP`, `XML`)

#### Interface Names
- **PascalCase** (e.g., `ItemRepository`)
- Nouns, noun phrases, or adjectives
- May use `able` suffix for capability interfaces (e.g., `Serializable`)
- Avoid `I` prefix

#### Method Names
- **camelCase** (e.g., `processWorkItem`)
- Verb or verb phrases
- Boolean methods should ask questions (e.g., `isValid`, `hasPermission`)

#### Variable Names
- **camelCase** (e.g., `userAccount`)
- Nouns or noun phrases
- Clear and descriptive

#### Constants
- **UPPER_SNAKE_CASE** (e.g., `MAX_RETRY_COUNT`)
- Typically static final fields

#### Enum Values
- **UPPER_SNAKE_CASE** (e.g., `COMPLETED`, `IN_PROGRESS`)

#### Package Names
- **lowercase** (e.g., `org.rinna.domain.service`)
- Reverse domain name convention
- Hierarchical structure reflecting component organization

#### Example

```java
package org.rinna.domain.service;

public class WorkItemProcessor implements ItemProcessor {
    public static final int MAX_RETRY_COUNT = 3;
    
    private final ItemRepository itemRepository;
    
    public WorkItemProcessor(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }
    
    public ProcessingResult processWorkItem(WorkItem workItem) {
        // Implementation
    }
    
    public boolean isEligibleForProcessing(WorkItem workItem) {
        // Implementation
    }
}
```

### Go

#### Package Names
- **lowercase**, single word (e.g., `config`, `repository`)
- Short and concise
- No underscores or mixedCaps

#### Interface Names
- **PascalCase** (e.g., `ItemProcessor`)
- No `I` prefix or `er` suffix unless it makes semantic sense

#### Struct Names
- **PascalCase** (e.g., `WorkItem`)

#### Method Names
- **PascalCase** for exported methods (e.g., `ProcessItem`)
- **camelCase** for unexported methods (e.g., `validateItem`)

#### Variable Names
- **camelCase** for local variables (e.g., `itemCount`)
- **PascalCase** for exported variables (e.g., `DefaultTimeout`)
- **camelCase** for unexported package variables (e.g., `maxRetryCount`)

#### Constants
- **PascalCase** for exported constants (e.g., `MaxRetryCount`)
- **camelCase** for unexported constants (e.g., `defaultTimeout`)

#### Example

```go
package repository

import "context"

// MaxRetryCount defines the maximum number of retries for repository operations
const MaxRetryCount = 3

// ItemRepository defines the interface for item storage operations
type ItemRepository interface {
    FindByID(ctx context.Context, id string) (*Item, error)
    Save(ctx context.Context, item *Item) error
}

// postgresItemRepository implements ItemRepository for PostgreSQL
type postgresItemRepository struct {
    db *sql.DB
}

// NewPostgresItemRepository creates a new PostgreSQL-backed item repository
func NewPostgresItemRepository(db *sql.DB) ItemRepository {
    return &postgresItemRepository{db: db}
}

// FindByID retrieves an item by its ID
func (r *postgresItemRepository) FindByID(ctx context.Context, id string) (*Item, error) {
    // Implementation
}
```

### Python

#### Module Names
- **snake_case** (e.g., `work_item_processor.py`)
- Short, lowercase
- Avoid underscores if possible

#### Class Names
- **PascalCase** (e.g., `WorkItemProcessor`)
- Nouns or noun phrases

#### Function and Method Names
- **snake_case** (e.g., `process_work_item`)
- Verb or verb phrases
- Use `is_` or `has_` prefix for boolean functions

#### Variable Names
- **snake_case** (e.g., `work_item`)
- Descriptive nouns

#### Constants
- **UPPER_SNAKE_CASE** (e.g., `MAX_RETRY_COUNT`)

#### Private Members
- Prefix with single underscore (e.g., `_internal_method`)
- Double underscore for name mangling when needed

#### Example

```python
# work_item_processor.py

MAX_RETRY_COUNT = 3

class WorkItemProcessor:
    def __init__(self, item_repository):
        self._item_repository = item_repository
    
    def process_work_item(self, work_item):
        # Implementation
        pass
    
    def is_eligible_for_processing(self, work_item):
        # Implementation
        return True
    
    def _validate_work_item(self, work_item):
        # Internal validation logic
        pass
```

### Shell Scripts

#### File Names
- **kebab-case** (e.g., `process-work-items.sh`)
- Add `.sh` extension for clarity

#### Function Names
- **snake_case** (e.g., `process_work_item`)
- Verb or verb phrases

#### Variable Names
- **snake_case** for local variables (e.g., `work_item_id`)
- **UPPER_SNAKE_CASE** for environment variables and constants (e.g., `MAX_ITEMS`)

#### Example

```bash
#!/bin/bash

# Constants
MAX_ITEMS=100
CONFIG_FILE="/etc/rinna/config.yaml"

# Functions
function process_work_item() {
    local item_id=$1
    # Implementation
}

function validate_config() {
    if [[ ! -f "$CONFIG_FILE" ]]; then
        echo "Error: Config file not found"
        exit 1
    fi
}

# Main script
validate_config
item_count=0

while read -r line; do
    item_id=$(echo "$line" | cut -d',' -f1)
    process_work_item "$item_id"
    ((item_count++))
done < "$INPUT_FILE"

echo "Processed $item_count items"
```

## Database Naming Conventions

### Tables
- **snake_case**, plural nouns (e.g., `work_items`, `users`)
- Use full words, not abbreviations

### Columns
- **snake_case**, singular nouns (e.g., `first_name`, `created_at`)
- Primary keys: `id` or `{table_name}_id` for foreign keys
- Boolean columns: prefix with `is_`, `has_`, or `can_` (e.g., `is_active`)
- Date/timestamp columns: suffix with `_at` or `_date` (e.g., `created_at`, `birth_date`)

### Indexes
- Format: `idx_{table}_{column(s)}` (e.g., `idx_work_items_status`)
- For multi-column indexes, include all columns (e.g., `idx_users_last_name_first_name`)

### Foreign Keys
- Format: `fk_{table}_{ref_table}_{column}` (e.g., `fk_work_items_users_assignee_id`)

## RESTful API Naming

### Endpoints
- Use nouns, not verbs (e.g., `/api/work-items`, not `/api/get-work-items`)
- Use kebab-case for multi-word resources (e.g., `/api/work-items`)
- Use plural for collection endpoints
- Hierarchical relationships: `/api/users/{id}/work-items`

### Query Parameters
- Use camelCase (e.g., `?sortBy=createdAt&orderDirection=desc`)
- Boolean parameters: use `is` prefix (e.g., `?isActive=true`)
- Pagination parameters: `page`, `limit`, `offset`

### Response Fields
- Use camelCase for JSON fields
- Use descriptive names that match domain model where possible
- Use ISO formats for dates and times

## File and Directory Naming

### Source Code Directories
- **lowercase** or **kebab-case** (e.g., `src/main/java`, `domain-services`)
- Reflect package/module structure

### Resource Directories
- **lowercase** (e.g., `config`, `resources`, `templates`)
- Clear and descriptive

### Configuration Files
- **kebab-case** with appropriate extension (e.g., `application-dev.yml`)
- Environment-specific suffix when applicable (e.g., `-dev`, `-prod`)

### Test Files
- Match the name of the class being tested with a `Test` suffix (e.g., `WorkItemProcessorTest.java`)
- Follow language-specific testing framework conventions

## Special Cases

### Domain-Specific Language (DSL)

For domain-specific terminology, maintain a glossary of terms to ensure consistency across the codebase. Prefer domain terms over technical terms when they express the concept more clearly.

### Temporary or Local Variables

For short-lived, locally scoped variables with obvious purpose:
- Loop counters can use short names (`i`, `j`, `k`)
- Lambda parameters can use short names for simple cases
- Obvious context variables can use conventional names (e.g., `e` for exceptions in catch blocks)

## Refactoring Guidance

When refactoring existing code to comply with these naming conventions:

1. Address one namespace/package at a time to minimize disruption
2. Ensure thorough tests before and after renaming
3. Update all references, including documentation
4. Avoid mixing renaming with functional changes
5. Consider using automated refactoring tools

## Conclusion

Consistent naming conventions improve code readability and maintainability. All team members should follow these guidelines for new code and apply them during refactoring of existing code. When in doubt, prioritize clarity and consistency over brevity.
# Admin Operations Commands

This document provides detailed information about the Rinna CLI's administrative operations commands for viewing, analyzing, and managing operation tracking data.

## Overview

The `rin admin operations` commands provide tools to:

1. View historical operation data for auditing and analysis
2. Generate statistics and metrics about command usage
3. Monitor system performance and user activity
4. Manage operation history through retention policies

These commands help administrators understand how the system is used, identify performance bottlenecks, track user activity, and ensure compliance with auditing requirements.

## Available Commands

### Listing Operations

The `list` command displays operations from the history:

```bash
# Basic usage
rin admin operations list

# Limit the number of operations shown
rin admin operations list --limit 20

# Filter by command name
rin admin operations list --command add

# Filter by operation type
rin admin operations list --type UPDATE

# Filter by status
rin admin operations list --status COMPLETED

# Filter by date range
rin admin operations list --from "2025-04-01" --to "2025-04-09"

# Filter by user
rin admin operations list --user johndoe

# Format output as JSON
rin admin operations list --format json

# Show verbose output with parameters and results
rin admin operations list --verbose
```

### Operation Details

The `detail` command provides in-depth information about a specific operation:

```bash
# View details for an operation by ID
rin admin operations detail OP-123456

# Include child operations
rin admin operations detail OP-123456 --include-children

# Format output as JSON
rin admin operations detail OP-123456 --format json

# Export to file
rin admin operations detail OP-123456 --output operation-details.json
```

### Operations Dashboard

The `dashboard` command launches a real-time PUI-based dashboard for operations:

```bash
# Start the dashboard
rin admin operations dashboard

# Filter dashboard by command
rin admin operations dashboard --command workflow

# Filter by date range
rin admin operations dashboard --from "2025-04-01" --to "2025-04-09"

# Focus on specific operation types
rin admin operations dashboard --type CREATE,UPDATE
```

### Operation Statistics

The `stats` command generates statistics about operations:

```bash
# View overall operation statistics
rin admin operations stats

# Group statistics by command
rin admin operations stats --by command

# Group statistics by operation type
rin admin operations stats --by type

# Group statistics by user
rin admin operations stats --by user

# Group statistics by date with daily intervals
rin admin operations stats --by date --interval daily

# Group by performance metrics
rin admin operations stats --by performance

# Format output as JSON
rin admin operations stats --format json

# Export to file
rin admin operations stats --output operation-stats.json
```

### Managing Operation History

The `clean` command manages operation history retention:

```bash
# Preview operations that would be deleted
rin admin operations clean --older-than 90 --dry-run

# Delete operations older than a specified number of days
rin admin operations clean --older-than 90

# Clean operations of a specific type
rin admin operations clean --older-than 90 --type READ

# Clean operations of a specific command
rin admin operations clean --older-than 90 --command list
```

## Example Output

### List Operations

```
+------------+-------------+---------------+-------------------------------+-------------+
| OPERATION  | COMMAND     | TYPE          | TIMESTAMP                     | STATUS      |
+------------+-------------+---------------+-------------------------------+-------------+
| OP-123456  | add         | CREATE        | 2025-04-09 10:15:22           | COMPLETED   |
| OP-123457  | view        | READ          | 2025-04-09 10:17:35           | COMPLETED   |
| OP-123458  | workflow    | UPDATE        | 2025-04-09 10:20:11           | COMPLETED   |
| OP-123459  | find        | SEARCH        | 2025-04-09 10:22:47           | COMPLETED   |
| OP-123460  | update      | UPDATE        | 2025-04-09 10:25:33           | FAILED      |
+------------+-------------+---------------+-------------------------------+-------------+
```

### Operation Detail

```
Operation: OP-123458
Command: workflow
Type: UPDATE
Status: COMPLETED
Start: 2025-04-09 10:20:11
End: 2025-04-09 10:20:13
Duration: 2.1s
User: johndoe
Client: terminal (localhost)

Parameters:
  itemId: WI-123
  targetState: IN_PROGRESS
  comment: "Starting implementation"
  
Result:
  success: true
  previousState: TODO
  newState: IN_PROGRESS
  transitionTime: 2025-04-09 10:20:12
  
Child Operations:
  - OP-123458-1: workflow-validate (VALIDATE)
    Status: COMPLETED
    Duration: 0.3s
    
  - OP-123458-2: workflow-transition (UPDATE)
    Status: COMPLETED
    Duration: 1.1s
    
  - OP-123458-3: workflow-display (READ)
    Status: COMPLETED
    Duration: 0.4s
```

### Operation Statistics

```
Command Usage Statistics (Last 30 days)
+-------------+-------------+------------+--------------+------------------+
| COMMAND     | COUNT       | AVG TIME   | SUCCESS RATE | MOST COMMON USER |
+-------------+-------------+------------+--------------+------------------+
| list        | 325         | 0.45s      | 99.7%        | johndoe (128)    |
| view        | 287         | 0.32s      | 100%         | johndoe (102)    |
| add         | 156         | 0.98s      | 94.2%        | janedoe (53)     |
| update      | 124         | 0.87s      | 95.2%        | janedoe (49)     |
| workflow    | 118         | 1.24s      | 97.5%        | johndoe (42)     |
| find        | 87          | 0.65s      | 98.9%        | johndoe (35)     |
+-------------+-------------+------------+--------------+------------------+
```

## Integration with PUI

For a more interactive experience, you can use the PUI-based operations monitor:

```bash
# Start the operations monitor
./run-operations-monitor.sh
```

This launches a real-time terminal-based dashboard that displays:

- Live operation tracking with hierarchical visualization
- Command execution flow and timing
- Performance metrics and status information
- Historical operation data with filtering and search

## Configuration

Operation tracking behavior can be configured in `.rinna.yaml`:

```yaml
# Operation tracking configuration
operation_tracking:
  # Enable or disable operation tracking
  enabled: true
  
  # Retention period in days
  retention_days: 90
  
  # Parameter redaction for sensitive data
  redact_parameters:
    - password
    - token
    - secret
  
  # Maximum result size in bytes
  max_result_size: 2048
  
  # Tracking detail level (MINIMAL, NORMAL, DETAILED)
  detail_level: NORMAL
```

## Related Documentation

- [CLI Operation Tracking](cli-operation-tracking.md) - Comprehensive documentation on operation tracking
- [PUI Architecture](pui-architecture.md) - Information about the Pragmatic User Interface architecture
- [Service Management](service-management.md) - Service architecture documentation
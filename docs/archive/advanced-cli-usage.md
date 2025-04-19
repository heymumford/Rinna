# Rinna CLI: Advanced Command Usage

This guide provides detailed information on the advanced command-line features of Rinna. It's intended for users who are already familiar with the basic commands and want to leverage the full power of the CLI.

## Table of Contents
- [Search and Filtering Tools](#search-and-filtering-tools)
  - [Grep Command](#grep-command)
  - [Find Command](#find-command)
  - [Backlog Command](#backlog-command)
  - [LS Command](#ls-command)
- [Critical Path Analysis](#critical-path-analysis)
- [Statistics and Reporting](#statistics-and-reporting)
  - [Stats Command](#stats-command)
  - [Report Command](#report-command)
- [Advanced Administration](#advanced-administration)
  - [Admin Command](#admin-command)
  - [Operations Command](#operations-command)
- [Output Formats](#output-formats)
- [Command Combinations](#command-combinations)

## Search and Filtering Tools

Rinna provides powerful search and filtering capabilities that follow Unix/Linux command principles, making them familiar and powerful for developers.

### Grep Command

The `grep` command allows you to search for text patterns within work items, similar to the Unix `grep` utility.

#### Basic Usage:
```bash
rin grep <pattern>                # Basic search (case-insensitive by default)
rin grep -s <pattern>             # Case-sensitive search
rin grep -w <pattern>             # Match whole words only
rin grep -c <num> <pattern>       # Show context lines around matches
rin grep --count-only <pattern>   # Show only match counts
rin grep --format=json <pattern>  # Output in JSON format
rin grep --format=csv <pattern>   # Output in CSV format
```

#### Examples:
```bash
# Find items mentioning "payment gateway"
rin grep "payment gateway"

# Case-sensitive search for "API"
rin grep -s "API"

# Show 3 lines of context around matches for "error"
rin grep -c 3 "error"

# Search with exact matches only
rin grep -w "login"

# Get JSON format results for automation
rin grep --format=json "database"
```

### Find Command

The `find` command provides structured searching based on work item attributes.

#### Basic Usage:
```bash
rin find --type=<type>              # Find by work item type
rin find --status=<status>          # Find by status
rin find --priority=<priority>      # Find by priority
rin find --assignee=<user>          # Find by assignee
rin find --created-after=<date>     # Find items created after date
rin find --updated-before=<date>    # Find items updated before date
rin find --sort=<field>             # Sort results by field
rin find --limit=<num>              # Limit results
```

#### Examples:
```bash
# Find all high priority bugs
rin find --type=BUG --priority=HIGH

# Find work in progress assigned to me
rin find --status=IN_PROGRESS --assignee=me

# Find recently updated work items
rin find --updated-after=2025-01-01 --sort=updated --limit=10

# Find work items without an assignee
rin find --assignee=none --status=TO_DO
```

### Backlog Command

The `backlog` command shows items in the backlog, with various filtering options.

#### Basic Usage:
```bash
rin backlog                       # Show all backlog items
rin backlog --priority=<priority> # Filter by priority
rin backlog --type=<type>         # Filter by type
rin backlog --sort=<field>        # Sort results
rin backlog --format=json         # Output in JSON format
```

#### Examples:
```bash
# Show high priority backlog items
rin backlog --priority=HIGH

# Show feature backlog items sorted by priority
rin backlog --type=FEATURE --sort=priority

# Export backlog to JSON
rin backlog --format=json > backlog.json
```

### LS Command

The `ls` command displays work items in a directory-like format, making it easy to see an overview.

#### Basic Usage:
```bash
rin ls                       # List work items in current context
rin ls --sort=<field>        # Sort by field (title, priority, etc.)
rin ls --reverse             # Reverse sort order
rin ls --long                # Show detailed information
rin ls --filter=<pattern>    # Filter by pattern
```

#### Examples:
```bash
# Show all work items sorted by priority
rin ls --sort=priority

# Show detailed information about open bugs
rin ls --long --type=BUG --status=OPEN

# List recently modified items
rin ls --sort=updated --reverse
```

## Critical Path Analysis

The `path` command provides critical path analysis, identifying dependencies and bottlenecks in your workflow.

#### Basic Usage:
```bash
rin path                    # Show critical path for the project
rin path --blockers         # Show only blocking items
rin path --item=<id>        # Show dependencies for specific item
rin path --format=json      # Output in JSON format
rin path --verbose          # Show detailed information
```

#### Examples:
```bash
# View the critical path for the current project
rin path

# Show only blocking items that impact the critical path
rin path --blockers

# Analyze dependencies for a specific work item
rin path --item=WI-123

# Get JSON output for further processing
rin path --format=json > critical-path.json

# Get detailed view with additional metrics
rin path --verbose
```

## Statistics and Reporting

Rinna provides robust statistics and reporting features for project insights.

### Stats Command

The `stats` command offers various views of project statistics.

#### Basic Usage:
```bash
rin stats                     # Show summary statistics (default)
rin stats dashboard           # Show statistics dashboard
rin stats all                 # Show all available statistics
rin stats distribution        # Show item distributions with charts
rin stats detail <type>       # Show detailed stats for a specific area
rin stats --format=json       # Output in JSON format
rin stats --limit=<num>       # Limit output to top N items
```

#### Detailed Stats Types:
```bash
rin stats detail completion    # Completion metrics
rin stats detail workflow      # Workflow metrics
rin stats detail priority      # Priority metrics
rin stats detail assignments   # Assignment metrics
```

#### Examples:
```bash
# Show a visual dashboard of project metrics
rin stats dashboard

# View distribution of work items by type, state, priority
rin stats distribution

# See detailed workflow metrics
rin stats detail workflow

# Export statistics as JSON for reporting
rin stats --format=json > project-stats.json

# Show top 5 contributors in distribution charts
rin stats distribution --limit=5
```

### Report Command

The `report` command generates various types of reports.

#### Basic Usage:
```bash
rin report generate                     # Generate default report
rin report generate --type=<type>       # Generate specific report type
rin report generate --format=<format>   # Set output format
rin report list                         # List available report types
```

#### Report Types:
```bash
rin report generate --type=summary      # Project summary report
rin report generate --type=burndown     # Sprint burndown chart
rin report generate --type=velocity     # Team velocity report
rin report generate --type=backlog      # Backlog report
```

#### Examples:
```bash
# Generate a burndown chart for the current sprint
rin report generate --type=burndown

# Create a PDF summary report
rin report generate --type=summary --format=pdf

# Generate a velocity report for the last 3 sprints
rin report generate --type=velocity --periods=3
```

## Advanced Administration

### Admin Command

The `admin` command provides administrative functions for managing Rinna.

#### Basic Usage:
```bash
rin admin audit <subcommand>           # Audit log management
rin admin backup <subcommand>          # Backup management
rin admin compliance <subcommand>      # Compliance settings
rin admin diagnostics <subcommand>     # Run diagnostics
rin admin monitor <subcommand>         # System monitoring
rin admin recovery <subcommand>        # System recovery
```

#### Examples:
```bash
# List audit logs
rin admin audit list

# Configure audit retention
rin admin audit configure --retention=90

# Export audit logs to CSV
rin admin audit export --format=csv

# Run system diagnostics
rin admin diagnostics run

# View system metrics
rin admin monitor metrics --type=system

# Start a full backup
rin admin backup start --type=full

# Create a recovery plan
rin admin recovery plan --from=latest
```

### Operations Command

The `operations` command allows monitoring and management of system operations.

#### Basic Usage:
```bash
rin operations list                   # List recent operations
rin operations show <id>              # Show details of an operation
rin operations stats                  # Show operation statistics
rin operations active                 # Show currently active operations
rin operations cancel <id>            # Cancel an operation
```

#### Examples:
```bash
# View recent operations
rin operations list

# Check details of a specific operation
rin operations show op_12345

# See performance statistics of operations
rin operations stats

# View and manage currently running operations
rin operations active
```

## Output Formats

Most Rinna commands support multiple output formats:

```bash
--format=text     # Plain text output (default)
--format=json     # JSON output for scripting
--format=csv      # CSV output for spreadsheets
--format=table    # Tabular output for readability
```

## Command Combinations

You can combine various Rinna commands for powerful workflows:

```bash
# Find blocking bugs and generate a report
rin find --type=BUG --status=IN_PROGRESS | rin report generate --type=custom

# Identify critical path items and assign them to the right team
rin path --blockers | rin bulk assign --team=core

# Check status of high-priority items and send a notification
rin find --priority=HIGH | rin stats custom | rin notify team
```

By mastering these advanced commands, you can efficiently manage complex projects, gain valuable insights, and automate workflows in Rinna.
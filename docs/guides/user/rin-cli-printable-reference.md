# Rinna CLI Quick Reference Card

## Core Commands

| Command | Description | Example |
|---------|-------------|---------|
| **Work Item Commands** | | |
| `add` | Create a new work item | `rin add "Fix login bug" --type=BUG --priority=HIGH` |
| `view <id>` | View details of a work item | `rin view WI-123` |
| `list [filters]` | List work items with filters | `rin list --type=BUG --status=IN_PROGRESS` |
| `update <id>` | Update a work item | `rin update WI-123 --status=IN_PROGRESS` |
| `done <id>` | Mark a work item as complete | `rin done WI-123` |
| `edit <id>` | Interactively edit a work item | `rin edit WI-123` |
| `comment <id> <text>` | Add a comment to a work item | `rin comment WI-123 "Fixed the issue"` |
| `history <id>` | Show history of a work item | `rin history WI-123` |
| `bug <title>` | Quick creation of bug work items | `rin bug "Critical login failure" --priority=HIGH` |

## Search & Filtering Commands

| Command | Description | Example |
|---------|-------------|---------|
| `grep <pattern>` | Search work items for text | `rin grep "payment gateway"` |
| `find [options]` | Find work items by criteria | `rin find --type=BUG --assigned=me` |
| `backlog` | Show items in the backlog | `rin backlog --sort=priority` |
| `cat <id>` | Display work item content | `rin cat WI-123` |
| `ls [options]` | List items directory-style | `rin ls --sort=priority` |

## Workflow Commands

| Command | Description | Example |
|---------|-------------|---------|
| `workflow states` | Show available workflow states | `rin workflow states` |
| `workflow transition` | Transition a work item | `rin workflow transition WI-123 --to-state=IN_PROGRESS` |
| `path` | Show critical path for project | `rin path --blockers` |
| `import <source>` | Import work items | `rin import jira --project=PROJ` |
| `undo` | Undo previous operations | `rin undo` |
| `schedule` | Manage scheduled operations | `rin schedule report --type=weekly` |

## Service Commands

| Command | Description | Example |
|---------|-------------|---------|
| `server status` | Check service status | `rin server status` |
| `server start` | Start services | `rin server start` |
| `server stop` | Stop services | `rin server stop` |
| `server restart` | Restart services | `rin server restart` |

## Communication Commands

| Command | Description | Example |
|---------|-------------|---------|
| `msg <user> <message>` | Send a message to a user | `rin msg johndoe "Meeting at 3pm"` |
| `notify list` | List notifications | `rin notify list` |
| `notify unread` | Show unread notifications | `rin notify unread` |
| `notify markall` | Mark all as read | `rin notify markall` |

## Report Commands

| Command | Description | Example |
|---------|-------------|---------|
| `stats` | Show project statistics | `rin stats` |
| `stats distribution` | Show item distributions | `rin stats distribution` |
| `report generate` | Generate a report | `rin report generate --type=burndown` |

## Admin Commands

| Command | Description | Example |
|---------|-------------|---------|
| `admin audit` | Manage audit logs | `rin admin audit list --user=johndoe` |
| `admin backup` | Manage backups | `rin admin backup start --type=full` |
| `admin compliance` | Manage compliance | `rin admin compliance report financial` |
| `admin diagnostics` | Run system diagnostics | `rin admin diagnostics run` |
| `admin monitor` | View system metrics | `rin admin monitor metrics --type=system` |
| `admin recovery` | Manage system recovery | `rin admin recovery plan --from=latest` |
| `admin operations` | View/manage operations | `rin admin operations list` |

## Authentication Commands

| Command | Description | Example |
|---------|-------------|---------|
| `login [username]` | Log in to the system | `rin login` or `rin login johndoe` |
| `logout` | Log out from the system | `rin logout` |
| `access` | Manage user access | `rin access grant johndoe --role=ADMIN` |

## Build Commands

| Command | Description | Example |
|---------|-------------|---------|
| `build` | Build the project | `rin build` |
| `clean` | Clean build artifacts | `rin clean` |
| `test [type]` | Run tests | `rin test` or `rin test unit` |
| `all` | Clean, build, and test | `rin all` |

## Version Commands

| Command | Description | Example |
|---------|-------------|---------|
| `version current` | Show version info | `rin version current` |
| `version major/minor/patch` | Bump version | `rin version minor` |
| `version set <version>` | Set specific version | `rin version set 2.0.0` |
| `version tag` | Create git tag | `rin version tag` |
| `version release` | Create GitHub release | `rin version release` |

## Global Options

| Option | Description | Example |
|--------|-------------|---------|
| `-v, --verbose` | Show detailed output | `rin -v list` |
| `-t, --terse` | Show minimal output | `rin -t build` |
| `-e, --errors` | Show only errors | `rin -e test` |
| `-c, --config <path>` | Use custom config | `rin -c ~/my-config.yaml list` |
| `--no-auto-start` | Don't start services | `rin --no-auto-start list` |
| `--format=FORMAT` | Output format | `rin list --format=json` |
| `-h, --help` | Show help information | `rin --help` or `rin list --help` |
| `--version` | Show CLI version | `rin --version` |

## Operation Tracking

View operation history and analytics with:

```bash
# List recent operations
rin admin operations list

# View operations dashboard
rin admin operations dashboard
```

## For More Help

```bash
# General help
rin --help

# Command-specific help
rin <command> --help
```
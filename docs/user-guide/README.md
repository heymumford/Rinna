<!-- Copyright (c) 2025 [Eric C. Mumford](https://github.com/heymumford) [@heymumford] -->

# Rinna User Guide

## Developer-Centric Workflow Management

Rinna focuses on providing a streamlined, developer-centric approach to workflow management with clear, explicit processes.

## Tools

- [CLI Tool (rin)](rin-cli.md) - Command-Line Interface utility for managing work and automating developer workflows
- [Quick Reference](rin-quick-reference.md) - Concise reference for common developer CLI (Command-Line Interface) commands
- [CLI Printable Reference Card](rin-cli-printable-reference.md) - Printable quick reference card for CLI commands
- [CLI Operation Tracking](cli-operation-tracking.md) - Understanding CLI operation tracking capabilities
- [Document Generation](documents.md) - Generate beautiful reports and documents from your workflow data
- [Service Management](service-management.md) - Understanding the service architecture and management
- [Version Guide](version-guide.md) - Understanding Rinna's versioning system
- [Test-Driven Development (TDD) Workflow](#test-driven-development-workflow) - Using Rinna for effective TDD
- [Troubleshooting Guide](troubleshooting-guide.md) - Solutions for common issues and error codes

## Administration Guides

- [Admin Guide](admin-guide.md) - Comprehensive guide for administrators to set up and configure Rinna
- [Admin CLI Quick Start](admin-cli-quickstart.md) - Essential commands for rapid project setup
- [Admin POM Sample](admin-pom-sample.xml) - Sample Maven POM file for adding Rinna to your project
- [Migration Guide](migration/README.md) - Comprehensive strategies for migrating from other tools
  - [Jira Migration](migration/jira-migration.md) - Detailed steps for migrating from Jira
  - [Azure DevOps Migration](migration/azure-devops-migration.md) - Detailed steps for migrating from Azure DevOps
  - [GitHub Issues Migration](migration/github-issues-migration.md) - Detailed steps for migrating from GitHub Issues

## Metrics and Business Value

- [Workflow Philosophy](workflow-philosophy.md) - Our approach to streamlined workflow management
- [IT Workflow Metrics](metrics/it-workflow-metrics.md) - Framework for demonstrating IT's strategic value

## Work Item Management

### Item Types

- **Goal**: High-level objectives that guide development efforts
- **Feature**: Incremental functionality that delivers user value
- **Bug**: Software issues requiring correction
- **Chore**: Non-functional maintenance tasks to keep the system healthy

### Work Item Relationships

- [Dependencies and Relationships](work-item-relationships.md) - Understanding how work items relate to each other and affect workflow
- [Complex Dependency Management](complex-dependency-management.md) - Advanced techniques for managing dependencies
- [Multi-Team Workflows](multi-team-workflows.md) - Coordinating work across multiple teams
- [Advanced Workflow Scenarios](advanced-workflow-scenarios.md) - Special workflow patterns for complex situations
- [Workflow Limitations FAQ](workflow-limitations-faq.md) - Common questions about workflow constraints

### Developer Workflow Stages

```
Found → Triaged → To Do → In Progress → In Test → Done
```

### Developer-Focused Commands

```bash
# Show all work items assigned to you
bin/rin-cli my-work

# Show what you should work on next (highest priority items)
bin/rin-cli next-task

# Start working on an item (assigns to you and moves to In Progress)
bin/rin-cli start ITEM-1

# Mark an item as ready for testing
bin/rin-cli ready-for-test ITEM-1

# Complete an item
bin/rin-cli done ITEM-1

# View your work history and productivity metrics
bin/rin-cli my-history
```

### Standard Work Item Commands

```bash
# Create items
bin/rin-cli create goal "Improve system reliability"
bin/rin-cli create feature "Add user authentication"
bin/rin-cli create bug "Login fails on Safari"
bin/rin-cli create chore "Update dependencies"

# List items (with filtering)
bin/rin-cli list
bin/rin-cli list --type bug --status "In Progress"
bin/rin-cli list --assignee jdoe

# Update items
bin/rin-cli update ITEM-1 --status "In Progress"
bin/rin-cli update ITEM-1 --assignee jdoe
bin/rin-cli update ITEM-1 --priority high

# Show item details
bin/rin-cli show ITEM-1
```

## Test-Driven Development Workflow

Rinna provides comprehensive support for Test-Driven Development across your entire software development lifecycle.

### Red-Green-Refactor Cycle

The TDD workflow in Rinna follows the classic Red-Green-Refactor pattern:

1. **Red**: Write a failing test that describes the expected behavior
2. **Green**: Implement the minimum code to make the test pass
3. **Refactor**: Clean up the code while ensuring tests still pass

### Running TDD Tests

```bash
# Run all TDD-related tests
bin/rin test --tag=tdd

# Run only positive TDD scenarios
bin/rin test --tag=tdd --tag=positive

# Run only negative TDD scenarios
bin/rin test --tag=tdd --tag=negative

# Run specific engineering scenarios
bin/rin test --tag=tdd --include="*API*"
```

### TDD for Engineering Tasks

Rinna includes specific TDD workflows for common engineering challenges:

- REST API testing with different response codes
- Database migration with rollback capability
- Concurrent access to shared resources
- Caching with proper invalidation
- Timeout and retry handling
- Complex algorithms with parameterized tests
- Memory leak detection
- Event-driven architecture testing
- Configuration changes in running systems
- Backward compatibility in API changes

### TDD Command Integration

Use Rinna's workflow management to track TDD progress:

```bash
# Create a work item with TDD approach
bin/rin-cli create feature "Implement caching system" --approach=tdd

# Mark a test as implemented (Red phase)
bin/rin-cli tdd red ITEM-1 

# Mark implementation as complete (Green phase)
bin/rin-cli tdd green ITEM-1

# Mark refactoring as complete
bin/rin-cli tdd refactor ITEM-1

# Show TDD status for a work item
bin/rin-cli tdd status ITEM-1
```

## Lota (Development Cycle) Management

```bash
# Create a Lota
bin/rin-cli lota create "Sprint 1" --start 2023-06-01 --end 2023-06-14

# Add items to Lota
bin/rin-cli lota add "Sprint 1" ITEM-1 ITEM-2

# Show Lota status with developer progress
bin/rin-cli lota show "Sprint 1"
```

## Building and Testing

Use the Rinna CLI (Command-Line Interface) tool for simplified build and test operations:

```bash
# Clean and build the project
bin/rin clean build

# Run tests with verbose output
bin/rin -v test

# Full workflow with errors-only output
bin/rin -e all
```

## Document Generation

Rinna includes a powerful document generation system that supports Docmosis for beautiful reports:

```bash
# Configure Docmosis license key
bin/rin-cli doc license YOUR_LICENSE_KEY

# Generate a work item report
bin/rin-cli doc generate workitem --id ITEM-1 --format pdf

# Show document configuration
bin/rin-cli doc config

# List available templates
bin/rin-cli doc templates
```

For complete documentation on document generation, see [documents.md](documents.md).

For complete CLI documentation, see [rin-cli.md](rin-cli.md) or run `bin/rin-cli --help`.

## Configuration

For details on configuring Rinna, see the [configuration reference](configuration-reference.md) guide.
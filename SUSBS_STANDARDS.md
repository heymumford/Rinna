# Standardized Utility Shell-Based Solution (SUSBS) Standards

## Overview

This document defines the standards and guidelines for Rinna as a Standardized Utility Shell-Based Solution (SUSBS). These standards ensure consistency, interoperability, and adherence to shell integration best practices throughout the system.

## SUSBS Definition

A Standardized Utility Shell-Based Solution (SUSBS) is a software system designed with the following characteristics:

1. **Shell-First Philosophy**: Primary interface is a command-line/terminal environment
2. **Standardized Command Structure**: Commands follow consistent patterns and naming conventions
3. **Composability**: Components can be combined through standard shell pipelines and redirections
4. **Utility-Focused**: Optimized for developer productivity and automation
5. **Integration-Ready**: Designed to integrate with other shell-based tools and workflows

## Rinna SUSBS Compliance Requirements

### Command Structure

All Rinna commands must follow these structural requirements:

```
rin <command> [subcommand] [options] [arguments]
```

1. **Base Command**: Always `rin` as the entry point
2. **Command Verbs**: Primary actions (add, list, update, etc.)
3. **Subcommands**: Further specification of the command scope
4. **Options**: Prefixed with `--` for long form or `-` for short form
5. **Arguments**: Positional parameters as needed

Example:
```bash
rin add bug --title="Critical issue" --priority=high --assignee=username
```

### Output Standards

1. **Human-Readable Default**: Default output formatted for human readability
2. **Machine-Readable Option**: All commands support `--json` or `--format=json` for structured output
3. **Exit Codes**: Consistent use of standard exit codes (0 for success, non-zero for errors)
4. **Error Reporting**: Errors reported to stderr, normal output to stdout
5. **Predictable Output**: Consistent output formats for similar commands

### Shell Integration

1. **Pipeline Support**: Commands accept stdin when appropriate and produce stdout/stderr
2. **Environment Variables**: Configuration via standardized environment variables with `RINNA_` prefix
3. **Completion Scripts**: Provide shell completion for all commands and options
4. **Signal Handling**: Proper response to standard signals (SIGTERM, SIGINT, etc.)
5. **Non-Interactive Mode**: Support for non-interactive operation in scripts

### SUSBS Compliance Levels

Rinna components must achieve one of the following compliance levels:

1. **SUSBS Core Compliance**: Essential shell integration patterns
2. **SUSBS Extended Compliance**: Advanced shell integration with extended capabilities
3. **SUSBS Full Compliance**: Complete integration with all shell capabilities

## Implementation Guidelines

### Command Design

1. **Verb-Noun Pattern**: Commands should follow verb-noun pattern (`rin add item` not `rin item add`)
2. **Consistent Verbs**: Use consistent verbs across the system (add, remove, list, update, etc.)
3. **Option Consistency**: Same options should have the same meaning across commands
4. **Common Options**: Support for standard options like `--help`, `--verbose`, `--quiet`
5. **Short/Long Options**: Provide both short and long option forms where appropriate

### Data Handling

1. **Text Processing**: Support for standard text processing operations
2. **File Operations**: Consistent handling of file paths and permissions
3. **Configuration Files**: Standard formats for configuration (YAML/JSON)
4. **State Persistence**: Clean approach to state persistence and recovery
5. **Idempotent Operations**: Commands should be idempotent where possible

### Documentation

1. **Man Pages**: Provide man pages for all commands
2. **Help Text**: Comprehensive `--help` output for all commands
3. **Examples**: Include examples in help text and documentation
4. **API Documentation**: Document all APIs and integration points
5. **Shell Examples**: Provide shell script examples for common operations

## PUI Integration with SUSBS

The Pragmatic User Interface (PUI) of Rinna must maintain SUSBS compliance by:

1. **Command Mirroring**: PUI operations should map directly to shell commands
2. **Keyboard Shortcuts**: Support for command-like keyboard shortcuts
3. **Shell Escapes**: Allow dropping to shell for advanced operations
4. **Shell Script Generation**: Support for generating shell scripts from UI operations
5. **Command History**: Visible command history matching shell history

## Testing SUSBS Compliance

1. **Automated Compliance Tests**: Test suite for verifying SUSBS compliance
2. **Shell Integration Tests**: Tests for shell integration features
3. **Cross-Shell Testing**: Verify behavior in different shell environments
4. **Script Compatibility**: Test compatibility with shell scripting patterns
5. **Performance Benchmarks**: Shell performance benchmarks for key operations

## References

- POSIX Shell Command Language Specification
- GNU Coding Standards for Command Line Interfaces
- Unix Philosophy: Design small, composable tools
- Shell Style Guide
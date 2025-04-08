# Rinna Project Standards

This document was developed with analytical assistance from AI tools including Claude 3.7 Sonnet, Claude Code, and Google Gemini Deep Research, which were used as paid services. All intellectual property rights remain exclusively with the copyright holder Eric C. Mumford (@heymumford). Licensed under the Mozilla Public License 2.0.

## Overview

This directory contains the definitive standards and guidelines for the Rinna project. Following these standards ensures consistency, maintainability, and quality across all components of the system.

## Available Standards

| Standard | Description |
|----------|-------------|
| [Code Review Guidelines](code-review-guidelines.md) | Procedures and expectations for effective code reviews |
| [Documentation Requirements](documentation-requirements.md) | Standards for creating and maintaining documentation |
| [Logging Guidelines](logging-guidelines.md) | Best practices for consistent logging across components |
| [Naming Conventions](naming-conventions.md) | Standardized naming patterns for code elements |
| [TODO Review Process](todo-review-process.md) | Managing technical debt through structured TODO tracking |
| [Version Numbering](version-numbering.md) | System for consistent version numbering across project components |

## Applying Standards

These standards apply to all components of the Rinna project:

- Java core modules
- CLI applications
- API services
- Documentation
- Configuration files
- Build scripts
- Test code

## Compliance Checking

Automated compliance checking is available through:

- `bin/rin-check standards`: Verifies compliance with coding standards
- Pre-commit hooks for style checking
- CI pipelines that validate PRs against standards

## Change Process

Standards evolve through the following process:

1. **Proposal**: Submit a PR with proposed changes to standards
2. **Review**: Standards committee reviews the changes
3. **RFC Period**: Two-week feedback period for the team
4. **Adoption**: Approval and merge of standards changes
5. **Announcement**: Team notification of updated standards
6. **Grace Period**: Two-sprint period for bringing code into compliance

## Exceptions

Occasional exceptions to standards may be necessary. The process for exceptions is:

1. Document the exception in code comments
2. Provide justification for the exception
3. Obtain approval from at least two senior team members
4. Record exceptions in the project's technical debt tracker

## Additional Resources

- [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html) - External reference for Java style
- [Effective Go](https://golang.org/doc/effective_go) - External reference for Go style
- [PEP 8](https://www.python.org/dev/peps/pep-0008/) - External reference for Python style
- [Clean Code](https://www.oreilly.com/library/view/clean-code-a/9780136083238/) - Recommended reading on code quality
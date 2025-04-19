# Integration Implementation Documentation

This directory contains documentation related to the integration of Rinna with external systems.

## Parent Documentation
- [Implementation Documentation](../README.md)
- [Documentation Home](../../README.md)

## Integration Overview

Rinna supports integration with various external systems including:
- Issue tracking systems
- CI/CD pipelines
- Enterprise systems
- Notification services

## Contents

- [Azure DevOps Mapping Guide](azure-devops-mapping-guide.md) - Mapping between Rinna and Azure DevOps
- [GitHub Issues Mapping Guide](github-issues-mapping-guide.md) - Mapping between Rinna and GitHub Issues
- [Jira Mapping Guide](jira-mapping-guide.md) - Mapping between Rinna and Jira
- [Enterprise System Integration](enterprise-system-integration.md) - Integration with enterprise systems

## Integration Approach

Rinna uses a canonical model approach for integrations, where:
1. External system data is mapped to Rinna's internal model
2. Changes are tracked and synchronized bidirectionally
3. Integrations preserve semantics while adapting to each system's specific features
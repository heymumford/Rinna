# Workflow State Mapping Feature Specification

## Overview

The Workflow State Mapping feature will enable admin users to create custom mappings between external workflow state models and Rinna's internal workflow, allowing teams to work with terminology and state transitions they're already familiar with while maintaining Rinna's consistent internal model.

## Problem Statement

Organizations often have established workflows and terminology that teams are accustomed to. Forcing teams to adopt entirely new terminology and workflow concepts can create resistance and confusion during migration to a new system. While Rinna maintains its opinionated internal workflow for consistency, there's a need to allow teams to see and interact with the system using familiar terms and states.

## Feature Requirements

### Core Functionality

1. **State Mapping**
   - Map external state names to Rinna's internal workflow states (FOUND, TRIAGED, TO_DO, IN_PROGRESS, IN_TEST, DONE, RELEASED)
   - Support many-to-one mappings (multiple external states can map to a single Rinna state)
   - Enforce validity rules to ensure all required Rinna states have at least one mapping

2. **Action Name Customization**
   - Customize the names of workflow actions (e.g., "Ready for QA" instead of "Ready for Test")
   - Map custom action names to Rinna's internal state transitions
   - Support localization of action names for different languages/regions

3. **Workflow Views**
   - Create customized views of the workflow that follow the mapped states
   - Provide visualization of the custom workflow with the renamed states
   - Toggle between custom view and Rinna's native view

### Administration & Configuration

1. **Admin Configuration UI**
   - Web-based admin interface for defining state mappings
   - Drag-and-drop workflow editor for visual mapping configuration
   - Validation to prevent invalid state transitions or configurations

2. **Mapping Templates**
   - Built-in templates for common external systems (Jira, Azure DevOps, GitLab, etc.)
   - Ability to save and share custom mapping configurations
   - Import/export functionality for mapping configurations

3. **Persistence**
   - Store mapping configurations in Rinna's database
   - Support for multiple mapping profiles per organization
   - Version control for mapping configurations

### Integration & Compatibility

1. **API Integration**
   - Update API endpoints to respect custom state mappings
   - Support querying by both native and mapped state names
   - Metadata to expose mapping information via API

2. **CLI Support**
   - Update CLI commands to use custom state names when configured
   - Support for toggling between native and mapped terminology
   - Help documentation that shows both native and mapped terms

3. **Reporting**
   - Translation layer for reports to show either mapped or native state names
   - Configurable export formats that respect custom mappings
   - Historical tracking that preserves both mapped and native state information

## Technical Design

### Architecture

1. **Translation Layer**
   - Create a bidirectional mapping service that translates between custom and native states
   - Implement as a separate service layer that sits between UI/API and the core workflow service
   - Cache mapping configurations for performance

2. **Database Schema**
   - Create new tables for storing mapping configurations:
     - `workflow_mapping_profile` (name, description, organization_id, is_default)
     - `state_mapping` (profile_id, external_state, internal_state, display_order)
     - `action_mapping` (profile_id, external_action, source_state, target_state, display_name)

3. **API Extensions**
   - Add new endpoints for managing mapping configurations
   - Extend existing endpoints with query parameters for state translation
   - Include mapping metadata in responses when appropriate

### User Experience

1. **Admin Configuration Flow**
   - Step 1: Create new mapping profile
   - Step 2: Select template or start from scratch
   - Step 3: Map external states to Rinna states
   - Step 4: Define custom action names
   - Step 5: Test and validate mapping
   - Step 6: Activate mapping profile

2. **End User Experience**
   - Users see only the mapped state names in UI and CLI
   - State transitions follow the same rules but with custom terminology
   - Users can optionally see both mapped and native terms for learning purposes

### Security

1. **Access Controls**
   - Only users with admin privileges can create or modify mappings
   - Organization-level separation of mapping configurations
   - Audit logging for all mapping configuration changes

## Implementation Strategy

### Phase 1: Core Mapping Framework

1. Create database schema for storing mapping configurations
2. Implement basic translation service for states and actions
3. Update core workflow service to use translation layer
4. Create basic admin API endpoints for managing mappings

### Phase 2: Admin UI and Templates

1. Develop admin UI for mapping configuration
2. Create templates for common external systems
3. Implement validation and testing capabilities
4. Add import/export functionality

### Phase 3: CLI and API Integration

1. Update CLI to support mapped terminology
2. Extend API endpoints with mapping support
3. Add reporting capabilities with mapping awareness
4. Implement caching and performance optimizations

### Phase 4: Advanced Features

1. Add visual workflow editor
2. Implement version control for mapping configurations
3. Create analytics for mapping usage
4. Enhance documentation and training materials

## Compatibility Considerations

While this feature provides terminology customization, it's crucial to maintain Rinna's core workflow integrity:

1. The underlying workflow state transitions remain unchanged
2. All validation rules for state transitions are enforced regardless of terminology
3. Database and internal representations always use native Rinna states
4. Reporting and metrics remain consistent across different terminology mappings

## User Documentation

Documentation will be created covering:

1. How to create and manage workflow mappings
2. Best practices for mapping external systems to Rinna
3. Template usage for common systems
4. Impact of workflow mapping on reporting and analytics
5. Common troubleshooting for workflow mapping

## Future Extensions

Potential future enhancements to consider:

1. Support for mapping field names in addition to states and actions
2. Bidirectional synchronization with external workflow systems
3. Advanced mapping rules with conditions and scripting
4. AI-assisted mapping suggestions based on existing workflow data
5. Integration with CI/CD systems for workflow automation

## Success Metrics

The feature will be deemed successful based on:

1. Reduction in onboarding time for teams migrating from other systems
2. Increased adoption rate among teams with established workflows
3. Reduced training requirements for new Rinna users
4. User satisfaction scores for teams using custom mappings
5. Fewer support requests related to workflow confusion
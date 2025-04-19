# Unified Work Management in Rinna

Rinna takes a fundamentally different approach to work management by treating all types of work - business, product, engineering, and test - as part of a single, unified system. This document explains the philosophy, implementation, and benefits of this approach.

## Core Philosophy

Traditional work management tools separate different types of work:
- Business teams track requirements in one system
- Product teams manage features and user stories in another
- Engineering teams track tasks and bugs in development tools
- Testing teams manage test cases and defects in testing tools

This separation creates artificial boundaries, communication gaps, and coordination challenges. Rinna eliminates these problems by providing a single model for all work types.

## The Universal Work Item

At the heart of Rinna is the `WorkItem` - a flexible, extensible model that can represent any type of work:

```
WorkItem
├── Core Properties (common to all work types)
│   ├── id
│   ├── title
│   ├── description
│   ├── state
│   ├── priority
│   ├── assignee
│   ├── created
│   ├── updated
│   └── ...
├── Type-Specific Extensions
│   ├── Business (acceptance criteria, stakeholders)
│   ├── Product (user stories, features, epics)
│   ├── Engineering (technical details, implementation notes)
│   └── Test (test steps, expected results)
└── Flexible Metadata
    └── Custom attributes for team-specific needs
```

## One Workflow, Many Work Types

All work in Rinna follows the same core workflow:

```
Found → Triaged → To Do → In Progress → In Test → Done → Released
```

This unified approach means:
1. Teams share a common language about work status
2. Dependencies between different work types are clear
3. Cross-functional collaboration is seamless
4. Reporting provides a complete picture of all work

## Work Type Classification

While using a unified model, Rinna allows rich classification to maintain clarity:

1. **Origin Category**
   - `PROD`: Product-focused (features, stories)
   - `ARCH`: Architecture-focused (design decisions)
   - `DEV`: Development-focused (implementation tasks) 
   - `TEST`: Test-focused (test cases, verification)
   - `OPS`: Operations-focused (deployment, monitoring)
   - `DOC`: Documentation-focused
   - `CROSS`: Cross-functional work

2. **CYNEFIN Domain**
   - `OBVIOUS`: Clear cause-effect relationships, best practices apply
   - `COMPLICATED`: Requires expertise to analyze, good practices apply
   - `COMPLEX`: Emergent solutions, experimental approach needed
   - `CHAOTIC`: High uncertainty, novel approaches required

3. **Work Paradigm**
   - Task-based: Simple units of work with clear completion criteria
   - Story-based: User-centered descriptions of needed functionality
   - Experiment-based: Testing hypotheses and learning
   - Goal-based: Outcome-oriented with flexible implementation

## Vocabulary Mapping

Rinna respects that different teams use different terminology. The vocabulary mapping system allows:

1. Presenting the same work item with different terminology based on the viewer
2. Mapping between internal terms and external system vocabulary
3. Customizing terminology to match your organization's language

For example, what engineering calls a "task" might be called a "development item" by product management and an "implementation step" by business stakeholders. Rinna shows each stakeholder the terminology they're comfortable with.

## Cross-Platform Containerization

Consistent with our unified approach to work, Rinna's deployment model is designed to work identically across platforms:

1. Windows, WSL, and native Linux support through containers
2. Minimal host requirements (just Docker/Podman)
3. Consistent volume mapping for persistent data
4. Orchestrated multi-container deployment
5. Health monitoring and self-healing

## Setting Up Unified Work Management

To configure Rinna for unified work management:

1. **Define Work Types**
   ```bash
   rin admin config work-types --add "FEATURE" --category PROD
   rin admin config work-types --add "TASK" --category DEV
   rin admin config work-types --add "TEST" --category TEST
   ```

2. **Configure Vocabulary Mapping**
   ```bash
   # Map between different terms
   rin admin config vocabulary --map "USER_STORY:FEATURE"
   rin admin config vocabulary --map "BUG:DEFECT"
   ```

3. **Set Up Team Views**
   ```bash
   # Create product team view
   rin admin config view --create "PRODUCT" --types "FEATURE,EPIC"
   
   # Create engineering view
   rin admin config view --create "DEV" --types "TASK,BUG"
   ```

## Unified Reporting

With unified work management, Rinna provides powerful cross-cutting reports:

```bash
# Show all work in progress across types
rin list --state "IN_PROGRESS"

# Show work distribution by type
rin stats --by-type

# Show dependencies across work types
rin dependency-graph --types "FEATURE,TASK,TEST"
```

## POC Status Tracking

Track progress toward your POC milestones with unified status reporting:

```bash
# Set POC milestone goals
rin admin milestone --create "POC_MVP" --target "2025-06-01"

# Track completion percentage across all types
rin milestone --status "POC_MVP"

# Visualize progress
rin dashboard --milestone "POC_MVP"
```

## Benefits of Unified Work Management

1. **Reduced Cognitive Load**: Teams work in one system with consistent terminology
2. **Improved Visibility**: All work is visible through the same interface
3. **Better Dependency Management**: Cross-type dependencies are explicit
4. **Simplified Planning**: Plan across different work types simultaneously
5. **Comprehensive Metrics**: Understand the complete work landscape
6. **Streamlined Process**: Eliminate handoffs between systems
7. **Faster Onboarding**: New team members learn one system, not many

## Further Reading

- [Architecture Decision Record: Unified Work Management](../architecture/decisions/0009-unified-work-management-for-all-work-types.md)
- [Workflow Philosophy](workflow-philosophy.md)
- [Work Item Relationships](work-item-relationships.md)
- [Container Deployment Guide](admin-server-setup.md)
- [IT Workflow Metrics](metrics/it-workflow-metrics.md)
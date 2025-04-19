# Rinna Project Standards

## Work Item Classification Schema

| Category | Values |
|----------|--------|
| Type | Feature, Bug, Chore, Architecture, Documentation |
| Status | Backlog, To_Do, In_Progress, In_Test, Done, Released, Blocked |
| Priority | Critical, High, Medium, Low |
| CYNEFIN Domain | Clear, Complicated, Complex, Chaotic |
| Work Paradigm | Task, Story, Epic, Initiative |
| Cognitive Load | 1-10 scale (10 being highest) |

## ID Naming Convention

`RINNA-[CATEGORY]-[NUMBER]`

Example categories:
- CORE: Core functionality
- ARCH: Architecture
- TEST: Testing infrastructure
- AI: AI integration features
- INFRA: Infrastructure components
- SEC: Security features
- FLEX: Flexibility and feature management
- DOC: Documentation
- RYO: Ryorin-Do integration
- CLI: Command-line interface

## Workflow States

1. **Backlog**: Items awaiting prioritization
2. **Triage**: Items under assessment
3. **To_Do**: Accepted items ready for work
4. **In_Progress**: Items currently being worked on
5. **In_Test**: Items undergoing verification
6. **Done**: Items verified but not yet deployed
7. **Released**: Items deployed to production
8. **Blocked**: Items that cannot proceed due to dependencies

## Team Standards

### Documentation Requirements

Each work item must include:
- Unique ID and descriptive title
- Complete classification metadata
- Detailed description
- 3-5 concrete sub-tasks
- Version information (when released)
- Dependencies (when applicable)

### Development Process

1. Items begin in Backlog or To_Do state
2. Only items with complete classification move to In_Progress
3. All items must pass appropriate tests before moving to Done
4. Items cannot be Released without version information

### Priority Guidelines

- **Critical**: Blocking issues or foundation architecture (high cognitive load)
- **High**: Core functionality essential to product operation
- **Medium**: Important features that enhance product capabilities
- **Low**: Nice-to-have improvements or minor issues

### CYNEFIN Domain Guidance

- **Clear**: Well-understood problems with established solutions (typically low cognitive load)
- **Complicated**: Problems requiring expertise but with knowable solutions
- **Complex**: Problems requiring experimentation and adaptation
- **Chaotic**: Novel problems requiring innovative approaches (typically high cognitive load)

### Paradigm Selection

- **Task**: Small, atomic unit of work (1-3 days)
- **Story**: User-focused functionality (3-10 days)
- **Epic**: Collection of related stories (2-6 weeks)
- **Initiative**: Strategic organizational effort (6+ weeks)

## Testing Standards

- All features require appropriate test coverage
- Test categories must include unit, component, integration, acceptance, and performance tests
- All tests must be properly tagged with JUnit 5 tags

## Dependency Management

- Dependencies must be explicitly documented in work items
- Circular dependencies are not allowed
- Work items with dependencies should use mermaid graphs for visualization

## Version Management

- All released items must reference the version in which they were released
- Version follows semantic versioning format: MAJOR.MINOR.PATCH
- Centralized version.properties is the single source of truth

## Cognitive Load Management

- Team average cognitive load should not exceed 7
- No more than 2 complex items (load ≥ 8) per developer at once
- Load balancing should consider both quantity and complexity

## Ryorin-Do Compliance

All work items should follow the Ryorin-Do cycle:
1. **Intention** (Ishi): Clear objectives and requirements
2. **Execution** (Jikko): Implementation with quality focus
3. **Verification** (Kakunin): Comprehensive testing
4. **Refinement** (Kairyo): Continuous improvement

## Clean Architecture Principles

- All components must respect dependency rule (dependencies point inward)
- Domain logic must be isolated from infrastructure concerns
- Clear interfaces between architecture layers
- Separation of concerns across all components

---

*Generated for Rinna Version: 1.11.0*

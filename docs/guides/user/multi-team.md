# Multi-Team Usage in Rinna

This guide provides detailed information on managing workflow processes across multiple teams using Rinna.

## Team Management Fundamentals

### Creating and Managing Teams

```bash
# Create a new team
rin team create frontend-team --description "Frontend Development Team"

# Add members to a team
rin team add-member frontend-team emma.wilson
rin team add-member frontend-team james.rodriguez
rin team add-members frontend-team alex.smith,taylor.wong

# Set team lead
rin team set-lead frontend-team emma.wilson
```

### Team Configuration

Configure team-specific settings:

```bash
# Set team default project
rin team config frontend-team --default-project PROJ-123

# Configure team notification preferences
rin team config frontend-team --notify-status-changes
rin team config frontend-team --notify-cross-team-dependencies

# Set team capacity
rin team config frontend-team --capacity 40 --capacity-unit points
```

### Team Work Item Assignment

```bash
# Assign an item to a team
rin team assign WI-123 frontend-team

# List team's current work items
rin team list-items frontend-team

# List team's work items by status
rin team list-items frontend-team --status "IN_PROGRESS,IN_TEST"
```

## Team Coordination Patterns

### Matrix Organization

For organizations using a matrix structure with functional and project teams:

```bash
# Create a functional team
rin team create frontend-team --type functional

# Create a project team
rin team create payment-team --type project

# Assign a user to both teams
rin team add-member frontend-team emma.wilson
rin team add-member payment-team emma.wilson

# Create work items with dual team assignment
rin create feature "Payment UI Components" \
  --functional-team frontend-team \
  --project-team payment-team
```

### Squad/Tribe Structure

For organizations using squad/tribe structures:

```bash
# Create a tribe
rin team create customer-experience --type tribe

# Create squads within the tribe
rin team create ui-squad --type squad --parent customer-experience
rin team create onboarding-squad --type squad --parent customer-experience

# View tribe structure
rin team hierarchy customer-experience
```

### Component-Based Teams

For teams organized around system components:

```bash
# Create component-based teams
rin team create database-team --owns "database,data-model"
rin team create api-team --owns "api,middleware"
rin team create frontend-team --owns "ui,client"

# Auto-assign based on components
rin create bug "Database connection pooling issue" \
  --components "database" --auto-assign
```

## Cross-Team Workflows

### Work Item Handoffs

Manage the formal transfer of work between teams:

```bash
# Request handoff to another team
rin handoff request WI-123 backend-team \
  --message "Frontend work complete, ready for API implementation" \
  --required-documents "API.md,REQUIREMENTS.md"

# Check pending handoffs
rin handoff list-pending --team backend-team

# Accept handoff
rin handoff accept WI-123 \
  --message "Starting backend work" \
  --assignee james.rodriguez
```

### Cross-Team Dependencies

Visualize and manage dependencies between teams:

```bash
# Create cross-team dependency
rin link WI-123 DEPENDS_ON WI-456 --cross-team

# View dependencies between teams
rin dependencies --team-1 frontend-team --team-2 backend-team

# Generate cross-team dependency report
rin report cross-team-dependencies
```

## Team Coordination Tools

### Team Synchronization Meetings

Support for coordination meetings:

```bash
# Create a sync meeting
rin meeting create daily-sync \
  --teams "frontend-team,backend-team" \
  --recurrence daily

# Generate meeting agenda
rin meeting agenda daily-sync --date 2025-04-10

# Record meeting notes and action items
rin meeting notes daily-sync --date 2025-04-10 \
  --notes "Discussed API contract issues" \
  --action-items "WI-567: Update API documentation"
```

### Team Dashboards

Create and view team-specific dashboards:

```bash
# Create team dashboard
rin dashboard create frontend-dashboard \
  --team frontend-team \
  --widgets "burndown,blockers,upcoming-items"

# View team dashboard
rin dashboard show frontend-dashboard
```

### Coordination Work Items

Create special work items for team coordination:

```bash
# Create coordination item
rin create coordination "API Contract Agreement" \
  --teams "frontend-team,backend-team" \
  --priority HIGH
```

## Team Metrics and Reporting

### Team Performance Metrics

Track and compare team metrics:

```bash
# View team metrics
rin metrics team frontend-team

# Compare team metrics
rin metrics team-comparison frontend-team backend-team

# Track team velocity over time
rin metrics velocity frontend-team --last 5

# View team workload balance
rin metrics workload frontend-team
```

### Cross-Team Reports

Generate reports focusing on team collaboration:

```bash
# Generate handoff efficiency report
rin report handoffs --last 90days

# Generate inter-team dependency report
rin report team-dependencies

# Create weekly coordination report
rin report coordination --teams "frontend-team,backend-team" \
  --period weekly --output coordination.pdf
```

## Scaled Workflows for Large Organizations

### Program Management

Coordinate multiple teams working on related initiatives:

```bash
# Create a program
rin program create "Digital Transformation" \
  --teams "frontend-team,backend-team,database-team"

# Add objectives to program
rin program add-objective "Digital Transformation" \
  --title "Improve User Experience" \
  --key-results "Reduce page load time by 50%,Increase user satisfaction to 4.5/5"

# Link work items to program
rin program link "Digital Transformation" WI-123,WI-456,WI-789

# View program dashboard
rin program dashboard "Digital Transformation"
```

### Release Trains

Implement SAFe-style release trains for coordinated delivery:

```bash
# Create a release train
rin train create Q2-2025 \
  --teams "frontend-team,backend-team,database-team" \
  --iterations 6 \
  --iteration-length 2weeks

# Plan release train
rin train plan Q2-2025

# Track release train progress
rin train status Q2-2025
```

## Conflict Resolution

### Identifying Conflicts

Detect and resolve conflicts between teams:

```bash
# Detect conflicting changes
rin detect-conflicts --release RELEASE-456

# Identify resource contention
rin detect-resource-contention
```

### Resolution Process

Formalized conflict resolution workflow:

```bash
# Create a resolution item
rin create resolution "API Contract Dispute" \
  --teams "frontend-team,backend-team" \
  --related-items WI-123,WI-456

# Document resolution decision
rin resolution document RES-123 \
  --decision "Teams will use OpenAPI specification" \
  --responsible "architecture-team"

# Track resolution implementation
rin resolution track RES-123
```

## Team-Based Customizations

While maintaining Rinna's core workflow model, certain aspects can be customized per team:

### Custom Fields

Configure team-specific metadata fields:

```bash
# Create team-specific field
rin team field create frontend-team \
  --name "designReviewRequired" \
  --type boolean \
  --default true

# Set field values
rin metadata WI-123 set designReviewRequired false
```

### Validation Rules

Create team-specific validation rules:

```bash
# Create validation rule for frontend team
rin rule create "frontend-design-review" \
  --team frontend-team \
  --condition "type=FEATURE AND transition=IN_TEST->DONE" \
  --validation "metadata.designReviewComplete = true OR metadata.designReviewRequired = false"
```

### Notification Rules

Configure team-specific notifications:

```bash
# Set up notification rules
rin team notification frontend-team \
  --event ITEM_BLOCKED \
  --notify-lead \
  --channels "slack,email"
```

## Best Practices for Multi-Team Workflows

1. **Clear Team Boundaries**: Define clear ownership areas for each team
2. **Minimize Handoffs**: Structure work to reduce the number of handoffs required
3. **Explicit Dependencies**: Make all cross-team dependencies explicit and visible
4. **Regular Coordination**: Schedule regular cross-team synchronization meetings
5. **Shared Metrics**: Use consistent metrics across teams to enable fair comparison
6. **Team Autonomy**: Give teams autonomy within their areas of responsibility
7. **Escalation Paths**: Establish clear processes for resolving cross-team conflicts
8. **Documentation**: Maintain thorough documentation on team interfaces and contracts

For more specific information on handling dependencies between teams, see [Dependencies](dependencies.md).

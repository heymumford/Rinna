# Multi-Team Workflows

This guide provides detailed information on managing workflow processes across multiple teams using Rinna, expanding on the overview in the [Advanced Workflow Scenarios](advanced-workflow-scenarios.md#multi-team-workflow-management) document.

## Team Management Fundamentals

### Creating and Managing Teams

```bash
# Create a new team
bin/rin-cli team create frontend-team --description "Frontend Development Team"

# Add members to a team
bin/rin-cli team add-member frontend-team emma.wilson
bin/rin-cli team add-member frontend-team james.rodriguez
bin/rin-cli team add-members frontend-team alex.smith,taylor.wong

# Set team lead
bin/rin-cli team set-lead frontend-team emma.wilson
```

### Team Configuration

Configure team-specific settings:

```bash
# Set team default project
bin/rin-cli team config frontend-team --default-project PROJ-123

# Configure team notification preferences
bin/rin-cli team config frontend-team --notify-status-changes
bin/rin-cli team config frontend-team --notify-cross-team-dependencies

# Set team capacity
bin/rin-cli team config frontend-team --capacity 40 --capacity-unit points
```

### Team Work Item Assignment

```bash
# Assign an item to a team
bin/rin-cli team assign WI-123 frontend-team

# List team's current work items
bin/rin-cli team list-items frontend-team

# List team's work items by status
bin/rin-cli team list-items frontend-team --status "IN_PROGRESS,IN_TEST"
```

## Team Coordination Patterns

### Matrix Organization

For organizations using a matrix structure with functional and project teams:

```bash
# Create a functional team
bin/rin-cli team create frontend-team --type functional

# Create a project team
bin/rin-cli team create payment-team --type project

# Assign a user to both teams
bin/rin-cli team add-member frontend-team emma.wilson
bin/rin-cli team add-member payment-team emma.wilson

# Create work items with dual team assignment
bin/rin-cli create feature "Payment UI Components" \
  --functional-team frontend-team \
  --project-team payment-team
```

### Squad/Tribe Structure

For organizations using squad/tribe structures:

```bash
# Create a tribe
bin/rin-cli team create customer-experience --type tribe

# Create squads within the tribe
bin/rin-cli team create ui-squad --type squad --parent customer-experience
bin/rin-cli team create onboarding-squad --type squad --parent customer-experience

# View tribe structure
bin/rin-cli team hierarchy customer-experience
```

### Component-Based Teams

For teams organized around system components:

```bash
# Create component-based teams
bin/rin-cli team create database-team --owns "database,data-model"
bin/rin-cli team create api-team --owns "api,middleware"
bin/rin-cli team create frontend-team --owns "ui,client"

# Auto-assign based on components
bin/rin-cli create bug "Database connection pooling issue" \
  --components "database" --auto-assign
```

## Cross-Team Workflows

### Work Item Handoffs

Manage the formal transfer of work between teams:

```bash
# Request handoff to another team
bin/rin-cli handoff request WI-123 backend-team \
  --message "Frontend work complete, ready for API implementation" \
  --required-documents "API.md,REQUIREMENTS.md"

# Check pending handoffs
bin/rin-cli handoff list-pending --team backend-team

# Accept handoff
bin/rin-cli handoff accept WI-123 \
  --message "Starting backend work" \
  --assignee james.rodriguez
```

Example handoff process:

1. Team A completes their portion of work and initiates handoff
2. System transitions the item to a special HANDOFF state
3. Team B is notified of the pending handoff
4. Team B reviews and formally accepts the handoff
5. System assigns to Team B member and transitions to appropriate state
6. Handoff is recorded in work item history for audit and metrics

### Cross-Team Dependencies

Visualize and manage dependencies between teams:

```bash
# Create cross-team dependency
bin/rin-cli link WI-123 DEPENDS_ON WI-456 --cross-team

# View dependencies between teams
bin/rin-cli dependencies --team-1 frontend-team --team-2 backend-team

# Generate cross-team dependency report
bin/rin-cli report cross-team-dependencies
```

Example output of cross-team dependency report:

```
Cross-Team Dependencies Report

Frontend Team → Backend Team:
- WI-123 (Login UI) → WI-456 (Authentication API) [BLOCKED_BY]
- WI-124 (Dashboard) → WI-457 (Data API) [DEPENDS_ON]

Backend Team → Database Team:
- WI-456 (Authentication API) → WI-789 (User Schema) [BLOCKED_BY]

Critical Dependencies:
- WI-123 → WI-456 (Blocks 3 additional items)
- WI-789 (Blocks 2 features on critical path)
```

## Team Coordination Tools

### Team Synchronization Meetings

Support for Scrum-style coordination meetings:

```bash
# Create a sync meeting
bin/rin-cli meeting create daily-sync \
  --teams "frontend-team,backend-team" \
  --recurrence daily

# Generate meeting agenda
bin/rin-cli meeting agenda daily-sync --date 2025-04-10

# Record meeting notes and action items
bin/rin-cli meeting notes daily-sync --date 2025-04-10 \
  --notes "Discussed API contract issues" \
  --action-items "WI-567: Update API documentation"
```

### Team Dashboards

Create and view team-specific dashboards:

```bash
# Create team dashboard
bin/rin-cli dashboard create frontend-dashboard \
  --team frontend-team \
  --widgets "burndown,blockers,upcoming-items"

# View team dashboard
bin/rin-cli dashboard show frontend-dashboard
```

### Coordination Work Items

Create special work items for team coordination:

```bash
# Create coordination item
bin/rin-cli create coordination "API Contract Agreement" \
  --teams "frontend-team,backend-team" \
  --priority HIGH
```

## Team Metrics and Reporting

### Team Performance Metrics

Track and compare team metrics:

```bash
# View team metrics
bin/rin-cli metrics team frontend-team

# Compare team metrics
bin/rin-cli metrics team-comparison frontend-team backend-team

# Track team velocity over time
bin/rin-cli metrics velocity frontend-team --last 5

# View team workload balance
bin/rin-cli metrics workload frontend-team
```

Example team metrics:

```
Team Metrics: Frontend Team (Last 30 days)

Completed:    15 work items (35 story points)
Velocity:     11.7 points/week (trending +5%)
Cycle Time:   4.2 days average
Lead Time:    6.8 days average

Status Breakdown:
- IN_PROGRESS: 6 items
- IN_TEST:     4 items
- DONE:        15 items

Blockers: 3 items (2 external dependencies)
```

### Cross-Team Reports

Generate reports focusing on team collaboration:

```bash
# Generate handoff efficiency report
bin/rin-cli report handoffs --last 90days

# Generate inter-team dependency report
bin/rin-cli report team-dependencies

# Create weekly coordination report
bin/rin-cli report coordination --teams "frontend-team,backend-team" \
  --period weekly --output coordination.pdf
```

## Scaled Workflows for Large Organizations

### Program Management

Coordinate multiple teams working on related initiatives:

```bash
# Create a program
bin/rin-cli program create "Digital Transformation" \
  --teams "frontend-team,backend-team,database-team"

# Add objectives to program
bin/rin-cli program add-objective "Digital Transformation" \
  --title "Improve User Experience" \
  --key-results "Reduce page load time by 50%,Increase user satisfaction to 4.5/5"

# Link work items to program
bin/rin-cli program link "Digital Transformation" WI-123,WI-456,WI-789

# View program dashboard
bin/rin-cli program dashboard "Digital Transformation"
```

### Release Trains

Implement SAFe-style release trains for coordinated delivery:

```bash
# Create a release train
bin/rin-cli train create Q2-2025 \
  --teams "frontend-team,backend-team,database-team" \
  --iterations 6 \
  --iteration-length 2weeks

# Plan release train
bin/rin-cli train plan Q2-2025

# Track release train progress
bin/rin-cli train status Q2-2025

# View program increment dashboard
bin/rin-cli train dashboard Q2-2025
```

## Conflict Resolution

### Identifying Conflicts

Detect and resolve conflicts between teams:

```bash
# Detect conflicting changes
bin/rin-cli detect-conflicts --release RELEASE-456

# Identify resource contention
bin/rin-cli detect-resource-contention
```

### Resolution Process

Formalized conflict resolution workflow:

```bash
# Create a resolution item
bin/rin-cli create resolution "API Contract Dispute" \
  --teams "frontend-team,backend-team" \
  --related-items WI-123,WI-456

# Document resolution decision
bin/rin-cli resolution document RES-123 \
  --decision "Teams will use OpenAPI specification" \
  --responsible "architecture-team"

# Track resolution implementation
bin/rin-cli resolution track RES-123
```

## Team-Based Customizations

While maintaining Rinna's core workflow model, certain aspects can be customized per team:

### Custom Fields

Configure team-specific metadata fields:

```bash
# Create team-specific field
bin/rin-cli team field create frontend-team \
  --name "designReviewRequired" \
  --type boolean \
  --default true

# Set field values
bin/rin-cli metadata WI-123 set designReviewRequired false
```

### Validation Rules

Create team-specific validation rules:

```bash
# Create validation rule for frontend team
bin/rin-cli rule create "frontend-design-review" \
  --team frontend-team \
  --condition "type=FEATURE AND transition=IN_TEST->DONE" \
  --validation "metadata.designReviewComplete = true OR metadata.designReviewRequired = false"
```

### Notification Rules

Configure team-specific notifications:

```bash
# Set up notification rules
bin/rin-cli team notification frontend-team \
  --event ITEM_BLOCKED \
  --notify-lead \
  --channels "slack,email"
```

## Integration with External Team Tools

Connect team workflows with their preferred tools:

```bash
# Configure team tool integration
bin/rin-cli team tools frontend-team \
  --design-tool "figma" \
  --api-url "https://figma.com/api" \
  --api-token "$FIGMA_TOKEN"

# Link work item to design
bin/rin-cli link-external WI-123 figma:design:FIG12345
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

## Conclusion

Rinna provides powerful tools for coordinating work across multiple teams while maintaining a consistent workflow model. By implementing the patterns and practices in this guide, organizations can scale their workflows to handle complex multi-team scenarios without adding unnecessary process overhead.

For a quick overview of multi-team workflows, see the [Advanced Workflow Scenarios](advanced-workflow-scenarios.md#multi-team-workflow-management) document. For dependency management across teams, see the [Complex Dependency Management](complex-dependency-management.md) guide.
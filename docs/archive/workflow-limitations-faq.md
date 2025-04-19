# Workflow Limitations FAQ

This document addresses common questions about Rinna's workflow limitations and provides guidance on how to work effectively within the system's constraints.

## General Workflow Questions

### Why doesn't Rinna allow customizable workflow states?

**Answer:** Rinna intentionally limits workflow customization for several reasons:

1. **Decision Fatigue Reduction** - Every customization decision requires time, team discussions, and ongoing maintenance. By providing a fixed, proven workflow, Rinna eliminates this overhead.

2. **Native Developer Workflow** - Rinna's states (FOUND → TRIAGED → TO_DO → IN_PROGRESS → IN_TEST → DONE → RELEASED) represent the natural progression of software development work.

3. **Consistency Across Teams** - A fixed workflow ensures consistency and reduces onboarding complexity when developers move between teams.

4. **Enterprise Integration** - The fixed internal model can still be mapped to various enterprise tools without complicating the developer experience.

### Can I add a "Blocked" state to the workflow?

**Answer:** No. Rinna deliberately doesn't include a "Blocked" state because:

1. Blocking is considered an annotation on a work item, not a workflow state
2. Items can be blocked for various reasons while still being in specific workflow states
3. Blocked items can be tracked using:
   - The `blocked` flag on work items
   - Dependencies that mark items as blocked by others
   - Metadata that tracks blocking reasons
   - Tagging and filtering by blocked status

Instead of a "Blocked" state, use:
```bash
# Mark an item as blocked
rin-cli block WI-123 --reason "Waiting for API documentation"

# View all blocked items
rin-cli list --blocked
```

### Why is there only one loop in the workflow (between IN_PROGRESS and IN_TEST)?

**Answer:** The single loop represents the natural feedback cycle of test-driven development:

1. Implement a feature or fix (IN_PROGRESS)
2. Test the implementation (IN_TEST)
3. If issues are found, return to implementation (IN_PROGRESS)
4. When tests pass, move forward (DONE)

This intentionally discourages backward transitions that restart work unnecessarily. It encourages forward progress while still accommodating the natural implementation-test cycle.

Other backward transitions require special handling or justification to prevent workflow chaos.

## Transitions and State Changes

### Why can't I move an item from DONE back to IN_PROGRESS?

**Answer:** Rinna enforces forward workflow progression to:

1. Maintain clean workflow history
2. Prevent "ping-ponging" of items between states
3. Encourage proper workflow practices

For items needing additional work after being marked DONE:
- For minor changes: Create a new linked work item
- For major issues: Use the `rin-cli reopen` command, which creates an audit trail
- For emergency fixes: Use the special hotfix workflow

```bash
# Proper way to handle items needing more work after being marked DONE
rin-cli reopen WI-123 --reason "Feature incomplete - missing validation"
```

### How do I handle emergency fixes that need to bypass normal workflow?

**Answer:** Use Rinna's emergency fix workflow, which allows controlled exceptions to normal workflow rules:

```bash
# Create and manage an emergency fix
rin-cli create bug "Fix broken login in production" --priority CRITICAL --emergency
rin-cli hotfix start WI-123
```

The emergency workflow:
- Allows certain workflow constraints to be bypassed
- Requires higher approval levels
- Creates comprehensive audit trails
- Enforces backporting to development branches

See [Advanced Workflow Scenarios: Emergency Fix Workflows](advanced-workflow-scenarios.md#emergency-fix-workflows) for details.

### Can I customize transition validation rules?

**Answer:** Yes! While workflow states are fixed, transition validation rules can be customized:

1. For specific projects or teams
2. Based on work item metadata
3. With temporary exceptions when needed

```bash
# Create custom validation rule
rin-cli rule create --name "require-tests" \
  --condition "type=BUG AND transition=IN_PROGRESS->IN_TEST" \
  --validator "metadata.testCoverage >= 80"
```

## Dependencies and Relationships

### Why can't a work item be marked DONE when it has incomplete dependencies?

**Answer:** This constraint ensures that:

1. All prerequisites are met before claiming work is complete
2. Delivery plans remain realistic and dependencies are respected
3. Integration issues are discovered before considering work done

If you must complete an item with incomplete dependencies:
```bash
# Document the exception and override the constraint
rin-cli override-dependency-constraint WI-123 \
  --reason "Dependency will be resolved in parallel branch" \
  --approved-by "jane.doe"
```

### How do I handle circular dependencies since they're prohibited?

**Answer:** Circular dependencies are prohibited because they create logical impossibilities in planning and execution. To resolve circular dependencies:

1. Use the dependency detection tool: `rin-cli check-circular`
2. Change at least one BLOCKED_BY/DEPENDS_ON relationship to RELATED_TO
3. Restructure work to eliminate the circular dependency
4. If circular dependency is unavoidable, create a coordination item that encapsulates the cycle

```bash
# Detect circular dependencies
rin-cli check-circular --release RELEASE-456

# Change relationship type to break the circle
rin-cli link change WI-789 DEPENDS_ON WI-123 RELATED_TO
```

### What's the difference between BLOCKED_BY and DEPENDS_ON relationships?

**Answer:** While functionally similar for workflow constraints, they serve different purposes:

- **BLOCKED_BY**: Technical impediment where an item cannot technically proceed until the blocker is resolved
- **DEPENDS_ON**: Logical dependency that might affect implementation but doesn't technically prevent work

Both create workflow constraints, but they're filtered and reported differently in dependency analysis tools.

## Multi-Team Workflows

### How do I manage workflows across multiple teams with different processes?

**Answer:** Rinna's approach for multi-team workflows:

1. Maintain the same core workflow states across all teams
2. Use team-specific customizations for validation rules, fields, and notifications
3. Implement formal handoffs between teams
4. Use cross-team dependency tracking
5. Create coordination items for team alignment

```bash
# Request handoff to another team
rin-cli handoff request WI-123 backend-team \
  --message "Frontend work complete, ready for API implementation"
```

See [Multi-Team Workflows](multi-team-workflows.md) for comprehensive guidance.

### Can different teams have different workflow states?

**Answer:** No, all teams use the same core workflow states. However, teams can:

1. Add custom fields and metadata
2. Implement team-specific validation rules
3. Create custom views and filters
4. Map to different states in external tools
5. Use custom terminology in reporting

## Enterprise Integration

### How do I reconcile Rinna's fixed workflow with our enterprise tool (Jira, Azure DevOps, etc.)?

**Answer:** Rinna integrates with enterprise tools through mapping:

1. Each Rinna state maps to one or more states in external systems
2. Bi-directional synchronization translates between models
3. Developers work in Rinna's clean workflow while management uses enterprise reporting
4. Custom terminology can be displayed without changing the underlying model

```bash
# Configure enterprise tool mapping
rin-cli external map-workflow --system jira \
  --mapping "IN_PROGRESS:In Development,IN_TEST:In Review"
```

Example mapping table:

| Rinna State | Jira Example | Azure DevOps Example | GitLab Example |
|-------------|--------------|----------------------|----------------|
| FOUND | Backlog | New | Open |
| TRIAGED | Selected for Development | Approved | Triage |
| TO_DO | To Do | Committed | Todo |
| IN_PROGRESS | In Progress | Active | Doing |
| IN_TEST | In Review | Testing | Testing |
| DONE | Done | Closed | Closed |
| RELEASED | Released | Released | Production |

### How do I implement specialized workflows (compliance, regulated environments)?

**Answer:** Specialized workflows are handled through:

1. Additional validation requirements (not additional states)
2. Compliance check attachments to specific transitions
3. Audit trail enhancements
4. Required approvals at transition points

```bash
# Add a compliance check requirement
rin-cli compliance add WI-123 --check "security-review"

# Mark compliance check as completed
rin-cli compliance complete WI-123 --check "security-review" \
  --reviewer "security-team" \
  --comment "Passed security review"
```

## Working with Limitations

### How can I track multiple "categories" of work if I can't add custom states?

**Answer:** Instead of custom states, use these Rinna features:

1. **Work Item Types** - Use built-in types (FEATURE, BUG, TASK, etc.)
2. **Custom Fields** - Add your own fields to capture additional information
3. **Tags and Labels** - Apply tags for easy filtering
4. **Custom Views** - Create saved views with complex filters
5. **Component Fields** - Assign items to specific components

```bash
# Create with specific type and component
rin-cli create FEATURE "Login page redesign" --component UI

# Add custom fields
rin-cli metadata WI-123 set "marketingCampaign" "Q2Launch"

# Apply tags
rin-cli tag WI-123 UX Performance
```

### How do I implement a "Ready for Review" state since it's not part of the standard workflow?

**Answer:** Although there's no explicit "Ready for Review" state, you can use these approaches:

1. Use IN_TEST state - it serves a similar purpose
2. Add a "readyForReview" flag as metadata
3. Use tags to mark items as ready for review
4. Use comments with specific formatting to indicate review readiness

```bash
# Mark as ready for review
rin-cli transition WI-123 IN_TEST --comment "Ready for review"

# Alternative approach with metadata
rin-cli metadata WI-123 set "readyForReview" "true"
```

### How do I handle work that's waiting for external dependencies?

**Answer:** For work blocked by external factors:

1. Mark the item as blocked with a reason
2. Add a comment with details about the external dependency
3. Create a placeholder dependency work item if needed
4. Use due dates and reminders for follow-up

```bash
# Mark as blocked by external dependency
rin-cli block WI-123 --reason "Waiting for third-party API update" \
  --external --follow-up-date "2025-04-15"
```

## Conclusion

Rinna's workflow limitations are intentional design choices meant to reduce complexity while supporting real-world software development. By working within these constraints and using the provided tools for special cases, teams can maintain workflow discipline while still handling complex development scenarios.

For additional information on specific workflow scenarios, see:
- [Advanced Workflow Scenarios](advanced-workflow-scenarios.md)
- [Multi-Team Workflows](multi-team-workflows.md)
- [Complex Dependency Management](complex-dependency-management.md)
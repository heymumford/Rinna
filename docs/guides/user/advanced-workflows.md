# Advanced Workflow Scenarios

This guide covers complex workflow scenarios that go beyond the basic workflow states, addressing real-world development situations that teams commonly encounter.

## Emergency Fix Workflows

Emergency fixes (sometimes called "hotfixes") require immediate attention and often need to bypass the normal workflow process. Rinna provides specific commands and processes for handling these scenarios.

### Emergency Fix Process

When a critical issue needs immediate resolution:

1. **Create an emergency fix work item**
   ```bash
   rin create bug "Fix broken login in production" --priority CRITICAL --emergency
   ```

2. **Specify the affected release**
   ```bash
   rin release link WI-123 RELEASE-456
   ```

3. **Bypass normal workflow and assign directly to IN_PROGRESS**
   ```bash
   rin hotfix start WI-123
   ```

4. **Fix implementation and testing**
   ```bash
   # When ready for verification
   rin hotfix verify WI-123
   
   # When verified and ready to deploy
   rin hotfix deploy WI-123
   ```

5. **Backport to development branches after deployment**
   ```bash
   rin hotfix backport WI-123 --all-branches
   ```

### Emergency Fix Validation Rules

Emergency fixes have special validation rules:

- Must have CRITICAL priority
- Require admin approval
- Must be linked to an affected release
- Generate automatic notifications to all stakeholders
- Require extra documentation of the fix approach

## Feature Flag Development

Using feature flags for progressive delivery while maintaining a single workflow.

### Creating Feature Flagged Work

```bash
rin create feature "New UI Component" --feature-flag "new-ui-enabled"
```

### Feature Flag States

Track feature flag states in your work items:

```bash
# Set feature flag state
rin feature-flag WI-123 set new-ui-enabled --environments "dev,staging"

# View feature flag states
rin feature-flag list
```

### Feature Flag Transitions

Special transitions for feature-flagged work:

```bash
# Mark as done but not exposed to users
rin done WI-123 --inactive-flag

# Activate a feature flag globally
rin feature-flag activate new-ui-enabled --all-environments
```

## Multi-Team Workflow Management

When multiple teams work on the same project, additional coordination is needed.

### Team Assignments

Assign work items to specific teams:

```bash
rin team assign WI-123 frontend-team
rin team list-items frontend-team
```

Create team-specific views:

```bash
rin view create frontend-view --team frontend-team --status "IN_PROGRESS,IN_TEST"
```

### Cross-Team Dependencies

Manage dependencies between items owned by different teams:

```bash
# Link items from different teams
rin link WI-123 DEPENDS_ON WI-456 --cross-team

# View all cross-team dependencies
rin list-dependencies --cross-team
```

### Handoffs Between Teams

Implement formal handoffs when work moves between teams:

```bash
# Request handoff to another team
rin handoff WI-123 backend-team --message "Frontend work complete, ready for API implementation"

# Accept handoff
rin handoff accept WI-123 --message "Starting backend work"
```

## Long-Running Feature Development

Manage large features that span multiple releases.

### Feature Breakdown

Break down large features into manageable pieces:

```bash
# Create a parent feature
rin create epic "New Authentication System" --tracking-id AUTH-EPIC

# Create child features
rin create feature "Login Page UI" --parent AUTH-EPIC
rin create feature "Password Reset Flow" --parent AUTH-EPIC
rin create feature "Two-Factor Authentication" --parent AUTH-EPIC
```

### Progress Tracking

Track progress on large initiatives:

```bash
rin progress AUTH-EPIC
```

### Partial Release Planning

Plan to release parts of a feature across multiple releases:

```bash
rin release-plan AUTH-EPIC
```

## Compliance and Audit Workflows

For regulated environments requiring additional compliance steps.

### Compliance Checks

Add compliance verification steps:

```bash
# Add a compliance check requirement
rin compliance add WI-123 --check "security-review"

# Mark compliance check as completed
rin compliance complete WI-123 --check "security-review" \
  --reviewer "security-team" \
  --comment "Passed security review"
```

### Audit Trail

Generate audit trails for regulatory requirements:

```bash
rin audit-log WI-123
rin audit-report --release RELEASE-456 --format pdf
```

## Parallel Development Streams

Managing work across multiple development streams or branches.

### Branch-Based Workflows

Associate work items with specific branches:

```bash
rin branch associate WI-123 feature/new-login
rin branch list-items feature/new-login
```

### Managing Merge and Backport Work

Track merge and backport tasks:

```bash
# Create a merge task
rin create merge --source feature/new-login --target main --work-item WI-123

# Create a backport task
rin create backport --from-release RELEASE-456 --to-release RELEASE-457 --work-item WI-123
```

## Customizing Workflow Validation Rules

While Rinna maintains a fixed set of workflow states, the validation rules can be customized for advanced scenarios.

### Custom Validation Rules

Apply custom validation for specific projects or teams:

```bash
# Create a custom validation rule
rin rule create --name "require-tests" \
  --condition "type=BUG AND transition=IN_PROGRESS->IN_TEST" \
  --validator "metadata.testCoverage >= 80"

# Apply a rule to a project
rin rule apply --name "require-tests" --project PROJECT-123

# Apply a rule to a team
rin rule apply --name "require-tests" --team backend-team
```

### Temporary Rule Exceptions

Create temporary exceptions for special cases:

```bash
rin rule exception --name "require-tests" --work-item WI-123 \
  --reason "Legacy code without test framework" \
  --expires "2025-05-01"
```

## Integrating with External Workflows

Connect Rinna's workflow with external systems.

### Synchronization with External Systems

```bash
# Link to external system
rin external link WI-123 --system jira --id JIRA-456

# Manually trigger sync
rin external sync WI-123

# Create from external item
rin external import --system jira --id JIRA-456
```

### Custom Workflow Mapping

Map Rinna workflows to external system states:

```bash
rin external map-workflow --system jira \
  --mapping "IN_PROGRESS:In Development,IN_TEST:In Review"
```

## Best Practices for Advanced Workflows

1. **Use Emergency Workflows Sparingly**: Reserve emergency fixes for true critical issues
2. **Document Special Cases**: Maintain clear documentation of exceptions and special workflows
3. **Balance Team Autonomy with Coordination**: Give teams freedom within their areas while enforcing coordination points
4. **Maintain Audit Trails**: Enable comprehensive audit tracking for regulated environments
5. **Establish Clear Escalation Paths**: Define processes for resolving cross-team conflicts
6. **Review and Clean Up Exceptions**: Periodically review temporary exceptions and rule overrides

For more information on multi-team workflows, see [Multi-Team Usage](multi-team.md).

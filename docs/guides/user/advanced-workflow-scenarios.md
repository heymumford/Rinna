# Advanced Workflow Scenarios

This guide covers complex workflow scenarios that go beyond the basic workflow states, addressing real-world development situations that teams commonly encounter.

## Emergency Fix Workflows

Emergency fixes (sometimes called "hotfixes") require immediate attention and often need to bypass the normal workflow process. Rinna provides specific commands and processes for handling these scenarios.

### Emergency Fix Process

When a critical issue needs immediate resolution:

1. **Create an emergency fix work item**
   ```bash
   bin/rin-cli create bug "Fix broken login in production" --priority CRITICAL --emergency
   ```

2. **Specify the affected release**
   ```bash
   bin/rin-cli release link WI-123 RELEASE-456
   ```

3. **Bypass normal workflow and assign directly to IN_PROGRESS**
   ```bash
   bin/rin-cli hotfix start WI-123
   ```

4. **Fix implementation and testing**
   ```bash
   # When ready for verification
   bin/rin-cli hotfix verify WI-123
   
   # When verified and ready to deploy
   bin/rin-cli hotfix deploy WI-123
   ```

5. **Backport to development branches after deployment**
   ```bash
   bin/rin-cli hotfix backport WI-123 --all-branches
   ```

### Emergency Fix Validation Rules

Emergency fixes have special validation rules:

- Must have CRITICAL priority
- Require admin approval (`--admin-approval` flag or separate `approve` command)
- Must be linked to an affected release
- Generate automatic notifications to all stakeholders
- Require extra documentation of the fix approach

### Tracking Emergency Fixes

View all emergency fixes in the system:

```bash
bin/rin-cli list --emergency
```

View emergency fixes for a specific release:

```bash
bin/rin-cli list --emergency --release RELEASE-456
```

Generate an emergency fix report:

```bash
bin/rin-cli report emergency --release RELEASE-456
```

## Complex Dependency Management

Managing dependencies between work items is crucial for planning and execution, especially in larger projects.

### Visualizing the Dependency Graph

```bash
# Show dependencies for a specific work item
bin/rin-cli dependencies WI-123

# Show the entire dependency graph for a release
bin/rin-cli dependencies --release RELEASE-456 --graph

# Show the critical path based on dependencies
bin/rin-cli path --release RELEASE-456
```

### Dependency Types

Rinna supports multiple dependency types:

1. **BLOCKED_BY** - Cannot proceed until the blocking item is complete
   ```bash
   bin/rin-cli link WI-123 BLOCKED_BY WI-456
   ```

2. **DEPENDS_ON** - Similar to BLOCKED_BY but with different semantics for filtering
   ```bash
   bin/rin-cli link WI-123 DEPENDS_ON WI-457
   ```

3. **SUBTASK_OF** - Hierarchical relationship indicating the item is part of a larger task
   ```bash
   bin/rin-cli link WI-123 SUBTASK_OF WI-458
   ```

4. **RELATED_TO** - Non-blocking relationship indicating items are connected
   ```bash
   bin/rin-cli link WI-123 RELATED_TO WI-459
   ```

5. **DUPLICATES** - Indicates the item is a duplicate of another
   ```bash
   bin/rin-cli link WI-123 DUPLICATES WI-460
   ```

### Automated Dependency Updates

Rinna provides automatic status updates based on dependencies:

```bash
# Validate workflow status based on dependencies
bin/rin-cli validate-dependencies WI-123

# Auto-update status based on dependency completion
bin/rin-cli auto-update-dependencies --release RELEASE-456
```

### Circular Dependency Detection

Detect and resolve circular dependencies:

```bash
bin/rin-cli check-circular --release RELEASE-456
```

Example output:
```
CIRCULAR DEPENDENCY DETECTED:
WI-123 → WI-456 → WI-789 → WI-123

Options to resolve:
1. Change WI-789 DEPENDS_ON WI-123 to RELATED_TO
2. Remove WI-789 DEPENDS_ON WI-123
```

### Exporting Dependency Information

Export dependencies for project management tools:

```bash
bin/rin-cli export-dependencies --release RELEASE-456 --format json
bin/rin-cli export-dependencies --release RELEASE-456 --format mermaid
```

## Multi-Team Workflow Management

When multiple teams work on the same project, additional coordination is needed.

### Team Assignments

Assign work items to specific teams:

```bash
bin/rin-cli team assign WI-123 frontend-team
bin/rin-cli team list-items frontend-team
```

Create team-specific views:

```bash
bin/rin-cli view create frontend-view --team frontend-team --status "IN_PROGRESS,IN_TEST"
```

### Cross-Team Dependencies

Manage dependencies between items owned by different teams:

```bash
# Link items from different teams
bin/rin-cli link WI-123 DEPENDS_ON WI-456 --cross-team

# View all cross-team dependencies
bin/rin-cli list-dependencies --cross-team
```

### Handoffs Between Teams

Implement formal handoffs when work moves between teams:

```bash
# Request handoff to another team
bin/rin-cli handoff WI-123 backend-team --message "Frontend work complete, ready for API implementation"

# Accept handoff
bin/rin-cli handoff accept WI-123 --message "Starting backend work"
```

### Team Metrics

Track performance metrics for individual teams:

```bash
bin/rin-cli metrics team frontend-team
bin/rin-cli metrics team-comparison frontend-team backend-team
```

### Conflict Resolution

Identify and resolve conflicts between teams:

```bash
# Detect conflicting changes
bin/rin-cli detect-conflicts --release RELEASE-456

# Create a coordination item for resolution
bin/rin-cli create coordination --teams "frontend-team,backend-team" --title "Resolve API contract changes"
```

## Feature Flag Development

Using feature flags for progressive delivery while maintaining a single workflow.

### Creating Feature Flagged Work

```bash
bin/rin-cli create feature "New UI Component" --feature-flag "new-ui-enabled"
```

### Feature Flag States

Track feature flag states in your work items:

```bash
# Set feature flag state
bin/rin-cli feature-flag WI-123 set new-ui-enabled --environments "dev,staging"

# View feature flag states
bin/rin-cli feature-flag list
bin/rin-cli feature-flag list-environments
```

### Feature Flag Transitions

Special transitions for feature-flagged work:

```bash
# Mark as done but not exposed to users
bin/rin-cli done WI-123 --inactive-flag

# Activate a feature flag globally
bin/rin-cli feature-flag activate new-ui-enabled --all-environments
```

## Parallel Development Streams

Managing work across multiple development streams or branches.

### Branch-Based Workflows

Associate work items with specific branches:

```bash
bin/rin-cli branch associate WI-123 feature/new-login
bin/rin-cli branch list-items feature/new-login
```

### Managing Merge and Backport Work

Track merge and backport tasks:

```bash
# Create a merge task
bin/rin-cli create merge --source feature/new-login --target main --work-item WI-123

# Create a backport task
bin/rin-cli create backport --from-release RELEASE-456 --to-release RELEASE-457 --work-item WI-123
```

## Customizing Workflow Validation Rules

While Rinna maintains a fixed set of workflow states, the validation rules can be customized for advanced scenarios.

### Custom Validation Rules

Apply custom validation for specific projects or teams:

```bash
# Create a custom validation rule
bin/rin-cli rule create --name "require-tests" \
  --condition "type=BUG AND transition=IN_PROGRESS->IN_TEST" \
  --validator "metadata.testCoverage >= 80"

# Apply a rule to a project
bin/rin-cli rule apply --name "require-tests" --project PROJECT-123

# Apply a rule to a team
bin/rin-cli rule apply --name "require-tests" --team backend-team
```

### Temporary Rule Exceptions

Create temporary exceptions for special cases:

```bash
bin/rin-cli rule exception --name "require-tests" --work-item WI-123 \
  --reason "Legacy code without test framework" \
  --expires "2025-05-01"
```

## Long-Running Feature Development

Manage large features that span multiple releases.

### Feature Breakdown

Break down large features into manageable pieces:

```bash
# Create a parent feature
bin/rin-cli create epic "New Authentication System" --tracking-id AUTH-EPIC

# Create child features
bin/rin-cli create feature "Login Page UI" --parent AUTH-EPIC
bin/rin-cli create feature "Password Reset Flow" --parent AUTH-EPIC
bin/rin-cli create feature "Two-Factor Authentication" --parent AUTH-EPIC
```

### Progress Tracking

Track progress on large initiatives:

```bash
bin/rin-cli progress AUTH-EPIC
```

Example output:
```
EPIC: New Authentication System (AUTH-EPIC)
Overall Progress: 65% (10/16 story points completed)

Components:
✅ Login Page UI:             100% (5/5 story points)
⏱️ Password Reset Flow:       80% (4/5 story points)
🟦 Two-Factor Authentication: 20% (1/5 story points)

Timeline:
Started: 2025-03-15
Projected Completion: 2025-04-20 (based on velocity)
```

### Partial Release Planning

Plan to release parts of a feature across multiple releases:

```bash
bin/rin-cli release-plan AUTH-EPIC
```

Example output:
```
Release Plan for: New Authentication System (AUTH-EPIC)

RELEASE-456 (2025-04-15):
- Login Page UI
- Password Reset Flow

RELEASE-457 (2025-05-15):
- Two-Factor Authentication
```

## Compliance and Audit Workflows

For regulated environments requiring additional compliance steps.

### Compliance Checks

Add compliance verification steps:

```bash
# Add a compliance check requirement
bin/rin-cli compliance add WI-123 --check "security-review"

# Mark compliance check as completed
bin/rin-cli compliance complete WI-123 --check "security-review" \
  --reviewer "security-team" \
  --comment "Passed security review"
```

### Audit Trail

Generate audit trails for regulatory requirements:

```bash
bin/rin-cli audit-log WI-123
bin/rin-cli audit-report --release RELEASE-456 --format pdf
```

## Integrating with External Workflows

Connect Rinna's workflow with external systems.

### Synchronization with External Systems

```bash
# Link to external system
bin/rin-cli external link WI-123 --system jira --id JIRA-456

# Manually trigger sync
bin/rin-cli external sync WI-123

# Create from external item
bin/rin-cli external import --system jira --id JIRA-456
```

### Custom Workflow Mapping

Map Rinna workflows to external system states:

```bash
bin/rin-cli external map-workflow --system jira \
  --mapping "IN_PROGRESS:In Development,IN_TEST:In Review"
```

## Conclusion

These advanced workflow scenarios demonstrate how Rinna maintains a simple core workflow model while providing powerful features to handle complex real-world development situations. The consistent command-line interface ensures that even advanced scenarios remain developer-friendly and efficient.

For specific use cases not covered here, contact support or refer to the extensive [CLI Reference](rin-cli.md).
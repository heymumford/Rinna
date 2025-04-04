# Rinna Workflow Philosophy

## Why We Don't Allow Workflow Customization

Most project management tools pride themselves on flexibility and customization. Rinna deliberately goes the opposite direction. Here's why:

### 1. Decisions Are Expensive

Every customization decision consumes time and energy from your team:
- Initial decisions about workflow states
- Ongoing debates about whether to change them
- Learning curve for new team members
- Maintenance of documentation about your custom process

Rinna saves you from this entire category of decisions by providing a proven workflow that just works.

### 2. Standard Workflows Are More Efficient

The workflow in Rinna:
```
Found → Triaged → To Do → In Progress → In Test → Done
```

This isn't just a random sequence. It represents the natural, efficient flow of work in software development, battle-tested across countless teams and projects.

### 3. Enterprise Integration Without Enterprise Overhead

If your organization requires you to use Jira, Rally, Azure DevOps, or any other enterprise tool, Rinna doesn't try to replace it. Instead, Rinna provides:

- A clean, developer-friendly interface for day-to-day work
- Simple mapping capabilities to synchronize with mandatory enterprise tools
- The ability to satisfy management reporting needs without imposing their workflow on developers

## The Mapping Approach

Rinna's states can be mapped to any enterprise tool:

| Rinna State | Jira Example | Azure DevOps Example | GitLab Example |
|-------------|--------------|----------------------|----------------|
| Found | Backlog | New | Open |
| Triaged | Selected for Development | Approved | Triage |
| To Do | To Do | Committed | Todo |
| In Progress | In Progress | Active | Doing |
| In Test | In Review | Testing | Testing |
| Done | Done | Closed | Closed |

This means you can:
1. Work in Rinna's clean, efficient workflow
2. Automatically map to enterprise tools for reporting
3. Satisfy management requirements without complicating developer workflows

## But What If Our Process Is Different?

That's the point. Most "different" processes contain unnecessary complexity that accumulates over time from internal politics, legacy decisions, and attempts to solve non-engineering problems with engineering workflows.

Rinna forces a reset to basics. If you truly need custom states for legitimate engineering reasons, you can:

1. Use tags and attributes in Rinna to track special cases
2. Implement custom mapping logic to enterprise tools
3. Create views and filters to see work organized your way

But you'll thank us for not letting you make your workflow more complex than it needs to be.

## Conclusion

The goal isn't to constrain your team - it's to liberate developers from unnecessary process overhead while still providing the benefits of structured workflow management. By removing pointless flexibility where it doesn't add value, we deliver more valuable flexibility where it matters: in how developers interact with their work.
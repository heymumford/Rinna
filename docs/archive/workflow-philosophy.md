<!-- Copyright (c) 2025 [Eric C. Mumford](https://github.com/heymumford) [@heymumford] -->

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

### 2. The Native Developer Workflow

The workflow in Rinna is **native** to software development:
```
Found → Triaged → To Do → In Progress → In Test → Done
```

This isn't just a random sequence. It represents the smallest set of states and actions that supports what developers actually do:

**FOUND**: Work originates somewhere. A feature is conceived, a bug is discovered, a change is requested. It's entered into a system somewhere and eventually makes its way to Rinna.

**TRIAGED**: At this stage, the incoming work needs its complications fleshed out. Questions are asked by people who can define requirements and clarify the intended end state. This is where acceptance criteria are written down or production hotfix verification steps are noted. Written down done-ness is the quality gate to leave TRIAGED.

**TO DO**: Someone in a position of product or technical authority has permitted the change to enter your team's intake process. To Rinna, your entire backlog is simply "TO DO" because Rinna only cares about the next most important thing for the developer to work on. That's all the developer cares about too.

> Need to talk more about priorities and shuffle the backlog? Go have meetings about that and don't disturb the developer.

**IN PROGRESS**: Work has been picked up and someone's actively working on it. Let them focus and ask questions on their own terms.

**IN TEST**: When a developer believes they've finished the work, they move it to IN TEST and look for the next TO DO item. The IN TEST queue is for verification - whether through automation, manual testing, product owner review, or any combination of these. Your testing processes can do whatever you want - Rinna only cares about when testing completes.

**DONE**: Work is completed and ready for deployment or has been deployed.

### 3. The Only Loop Is Intentional

The only permitted loop in Rinna's workflow is from IN TEST back to IN PROGRESS. This represents the natural feedback cycle of development work.

There is no "BLOCKED" state because the Rinna philosophy is that blocking is not a workflow state - it's an annotation on complications unrelated to the actual work. If discoveries during development are so significant that the work can't proceed, either reset it or mark it as won't-do and set it to DONE.

### 4. Enterprise Integration Without Enterprise Overhead

If your organization requires you to use Jira, Rally, Azure DevOps, or any other enterprise tool, Rinna doesn't try to replace it. Instead, Rinna provides:

- A clean, developer-friendly interface for day-to-day work
- Simple mapping capabilities to synchronize with mandatory enterprise tools
- The ability to satisfy management reporting needs without imposing their workflow on developers

## The Mapping Approach

Rinna's states can be mapped to any enterprise tool. Do whatever you want in your fancy enterprise tool - Rinna only cares about the states that matter to developers:

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

Project planning is inherently messy, and the intention is always to produce predictable results. Rinna acts like a clean, moist sponge being run across a chaos of flour and sugar and dough after a weekend of making cookies with children. You can't leave the kitchen a wreck.

Stop forcing your team into new SDLCs and paying for a space shuttle of a software management solution when all you need is a sport compact car to get around.

The goal isn't to constrain your team - it's to liberate developers from unnecessary process overhead while still providing the benefits of structured workflow management. By removing pointless flexibility where it doesn't add value, we deliver more valuable flexibility where it matters: in how developers interact with their work.

## Related Resources

For a comprehensive framework on measuring and demonstrating IT value in the enterprise, see our [IT Workflow Metrics](metrics/IT-workflow-metrics.md) guide.

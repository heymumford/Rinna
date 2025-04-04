<!-- Copyright (c) 2025 [Eric C. Mumford](https://github.com/heymumford) [@heymumford] -->

# Lota (Development Cycle) Management

## What is a Lota?

A "Lota" represents the specific cycle duration chosen by the software engineering team. It's the time boundary between which you measure your team's throughput and effectiveness.

## Development Methodology Flexibility

Rinna intentionally doesn't dictate which software development methodology you should use. Whether you prefer:

- Kanban
- Agile/Scrum
- SAFe
- Waterfall
- XP (Extreme Programming)
- Your own custom approach

The choice is entirely yours. Lota simply provides a consistent way to measure progress over a time period that makes sense for your team.

## Setting Your Lota Duration

Your Lota can be any duration that works for your team:

- **Solo developer**: Even daily Lotas can work to measure and improve your daily throughput
- **Small teams**: 1-2 week Lotas often provide good cadence without excessive overhead
- **Larger teams**: 2-4 week Lotas may provide needed coordination time
- **Enterprise**: Match your Lota to existing organizational cadences if needed

The key is to find what cadence provides the right balance of:
1. Enough time to accomplish meaningful work
2. Short enough to provide regular feedback and course correction
3. A consistent measure for team velocity and throughput

## Creating a Lota

```bash
# Create a new Lota
rinna lota create "Sprint 1" --start 2023-06-01 --end 2023-06-14
```

## Lota Ceremonies (Optional)

Rinna doesn't require any specific ceremonies, but here are common practices that teams might find helpful:

1. **Flow Planning**: Start of Lota, establish objectives
2. **Daily Flow Check-in**: Brief progress updates
3. **Flow Review**: End-of-Lota review of completed items
4. **Flow Retrospective**: Process improvement discussions

Feel free to adapt these ceremonies to your team's needs, add your own, or skip them entirely. The only requirement is that you define a clear start and end date for your Lota to measure progress.

## Measuring Improvement

Consistent Lota durations help you measure and improve your development process over time:

- Track throughput (completed items per Lota)
- Monitor quality (defects per Lota)
- Measure predictability (estimated vs. actual completion)
- Identify bottlenecks in your workflow

Unlike prescriptive methodologies, Rinna doesn't dictate how you should interpret or act on these metrics. The data is provided for your team to make its own decisions about process improvements.

## Managing Lota Content

Managing work within your Lota is straightforward:

```bash
# Add items to a Lota
rinna lota add "Sprint 1" ITEM-1 ITEM-2

# Remove items from a Lota
rinna lota remove "Sprint 1" ITEM-1

# Show Lota progress
rinna lota progress "Sprint 1"

# Get detailed Lota metrics
rinna lota metrics "Sprint 1"

# List active and completed Lotas
rinna lota list
```

## BingBongDingDang or Whatever Works

Whether you're using a recognized methodology or something you made up last Tuesday called "BingBongDingDang Development" - it doesn't matter. Rinna's Lota concept is methodology-agnostic and focuses solely on:

1. What work items exist
2. What state they're in
3. How they progress through your workflow
4. How long that progression takes

The key is finding the rhythm that works for your team and sticking with it long enough to gather meaningful metrics that can drive improvement. Experiment, measure, adjust, and find what works best for your unique team and project needs.

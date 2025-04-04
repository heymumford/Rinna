# Lota (Development Cycle) Management

## What is a Lota?

A "Lota" represents the specific cycle duration chosen by the software engineering team, typically ranging from one to four weeks based on team needs.

## Creating a Lota

```bash
# Create a new Lota
rinna lota create "Sprint 1" --start 2023-06-01 --end 2023-06-14
```

## Lota Ceremonies

1. **Flow Planning**: Start of Lota, establish objectives
2. **Daily Flow Check-in**: Brief progress updates
3. **Flow Review**: End-of-Lota review of completed items
4. **Flow Retrospective**: Process improvement discussions

## Managing Lota Content

```bash
# Add items to a Lota
rinna lota add "Sprint 1" ITEM-1 ITEM-2

# Remove items from a Lota
rinna lota remove "Sprint 1" ITEM-1

# Show Lota progress
rinna lota progress "Sprint 1"
```
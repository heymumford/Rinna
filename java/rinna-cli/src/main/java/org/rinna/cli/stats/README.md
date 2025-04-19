# Rinna CLI Statistics System

This package contains the statistics and metrics system for the Rinna CLI, providing comprehensive insights into project progress, work item distribution, and performance metrics.

## Components

### StatisticType

The `StatisticType` enum defines different categories of statistics:

- **Count metrics**: `TOTAL_ITEMS`, `ITEMS_COMPLETED`, `ITEMS_CREATED`, `OVERDUE_ITEMS`, `WORK_IN_PROGRESS`
- **Distribution metrics**: `ITEMS_BY_TYPE`, `ITEMS_BY_STATE`, `ITEMS_BY_PRIORITY`, `ITEMS_BY_ASSIGNEE`
- **Rate metrics**: `COMPLETION_RATE`, `BURNDOWN_RATE`, `THROUGHPUT`
- **Time metrics**: `AVG_COMPLETION_TIME`, `LEAD_TIME`, `CYCLE_TIME`

### StatisticValue

The `StatisticValue` class represents individual statistics with the following properties:

- Type (from StatisticType)
- Numeric value
- Optional breakdown for distribution statistics
- Unit of measurement
- Description
- Timestamp when calculated

### StatisticsService

The `StatisticsService` computes and manages statistics:

- Calculates statistics based on work item data
- Caches statistics for performance
- Provides filtered views (summary, by category, etc.)
- Supports refreshing statistics on demand

### StatisticsVisualizer

The `StatisticsVisualizer` provides visualization capabilities:

- Bar charts for distributions
- Tables for numeric data
- Progress meters for completion rates
- Dashboards combining multiple visualizations
- Sparklines for trend data

## CLI Command

The statistics system is accessed through the `stats` command:

```bash
rin stats                # Show summary statistics
rin stats dashboard      # Show statistics dashboard
rin stats all            # Show all available statistics
rin stats distribution   # Show item distributions
rin stats detail <type>  # Show detailed statistics
rin stats <metric>       # Show a specific metric
rin stats --format=<fmt> # Specify output format
rin stats --limit=<n>    # Limit output to top N items
```

## Integration with Rinna CLI

The statistics system is integrated with the main CLI through:

1. The `handleStatsCommand` method in `RinnaCli`
2. The addition of "stats" to the help output
3. Integration with the `ItemService` for data access

## Usage Examples

### Getting Summary Statistics

```bash
rin stats
```

This shows a summary of key metrics including:
- Total items
- Completion rate
- Work in progress
- Overdue items
- Throughput

### Visualizing Distributions

```bash
rin stats distribution
```

This creates bar charts for:
- Items by type (task, bug, feature, etc.)
- Items by state (open, in progress, done, etc.)
- Items by priority
- Items by assignee

### Viewing the Dashboard

```bash
rin stats dashboard
```

This displays a comprehensive dashboard with:
- Key metrics table
- Distribution charts
- Progress meters
- Performance indicators

### Detailed Analysis

```bash
rin stats detail workflow
```

This provides a detailed look at workflow metrics:
- State distribution
- Work in progress
- Throughput
- Burndown rate

## Future Enhancements

1. Trend analysis over time
2. Export capabilities (CSV, JSON)
3. Custom metric definitions
4. Team/user specific metrics
5. Automated reporting
6. Predictive analytics
7. Comparative benchmarks
8. Integration with external reporting tools
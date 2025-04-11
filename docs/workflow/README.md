# Rinna Workflow Documentation

This directory contains comprehensive documentation about Rinna's workflow system, which is based on the Ryorin-Do philosophy.

## Workflow Overview

Rinna implements a deliberate, opinionated workflow model that represents the smallest set of states needed for effective software development:

```
                             +-----------+
                             |           |
                   +-------->| BACKLOG   +-----+
                   |         |           |     |
                   |         +-----------+     |
                   |                           | Prioritized
                   |                           v
                   |         +-----------+     |
                   |         |           |     |
                   +-------->|  TRIAGE   +-----+
                   |         |           |     |
                   |         +-----------+     |
                   |                           | Accepted
                   |                           v
                   |         +-----------+     |
                   |         |           |     |
                   +-------->|  TO_DO    +-----+
                   |         |           |     |
                   |         +-----------+     |
                   |                           | Started
                   |                           v
  Blocked          |         +-----------+     |
  +----------------+         |           |     |
  |                +-------->| IN_PROGRESS +---+
  |                |         |           |     |
  |                |         +-----------+     |
  |                |                           | Completed
  |                |                           v
  |  Blocked       |         +-----------+     |
  +----------------+         |           |     |
  |                +-------->|  IN_TEST  +-----+
  |                |         |           |     |
  |                |         +-----------+     |
  |                |                           | Verified
  |                |                           v
  v                |         +-----------+     |
+-----------+      |         |           |     |
|           |      +-------->|  DONE     +-----+
| BLOCKED   |                |           |     |
|           |                +-----------+     |
+-----------+                                  | Deployed
     ^                                         v
     |                       +-----------+     |
     |                       |           |     |
     +-----------------------+ RELEASED  +<----+
                             |           |
                             +-----------+
```

## Workflow Philosophy

Can you customize the workflow? No, you can't. That's the point.

Rinna's workflow is deliberately opinionated and minimalist for several key reasons:

1. **Cognitive Simplicity**: A fixed, clear workflow reduces cognitive overhead
2. **Team Alignment**: Everyone uses the same language for work states
3. **Optimization Focus**: Focus on optimizing work, not workflow
4. **Universal Applicability**: Works for all work types and teams

[Read more about the workflow philosophy](../user-guide/workflow-philosophy.md)

## Workflow States

| State | Description | Transition Criteria |
|-------|-------------|---------------------|
| BACKLOG | Work items that have been identified but not yet evaluated | Created via CLI, API, or integration |
| TRIAGE | Items under evaluation for acceptance and prioritization | Moved from BACKLOG when ready for review |
| TO_DO | Accepted work items ready to be worked on | Prioritized and fully specified |
| IN_PROGRESS | Work being actively performed | Assigned and started |
| BLOCKED | Work that cannot proceed due to dependencies | Blocked by external factors |
| IN_TEST | Work completed and undergoing verification | Implementation complete |
| DONE | Work verified as complete | Tested and accepted |
| RELEASED | Work deployed to production | Deployed to users |

## Ryorin-Do Integration

Rinna's workflow incorporates the Four Aspects of Work from Ryorin-Do:

1. **Intention (Ishi)**: BACKLOG, TRIAGE states
2. **Execution (Jikko)**: TO_DO, IN_PROGRESS states
3. **Verification (Kakunin)**: IN_TEST state
4. **Refinement (Kairyo)**: DONE, RELEASED states

[Learn more about Ryorin-Do](../ryorindo/RYORINDO.md)

## Critical Path Analysis

Rinna provides tools for analyzing critical paths in your workflow:

```bash
rin critical-path analysis
```

This command identifies:
- Bottlenecks in the workflow
- Dependencies that may cause delays
- Optimal resource allocation

[Read more about critical path analysis](../user-guide/README.md#critical-path-analysis)

## Work Item Lifecycle

The complete lifecycle of a work item in Rinna is documented in detail:

1. [Creation and Intake](../user-guide/workflow.md#work-item-creation)
2. [Prioritization and Triage](../user-guide/workflow.md#triage-process)
3. [Implementation](../user-guide/workflow.md#implementation-process)
4. [Testing and Verification](../user-guide/workflow.md#testing-process)
5. [Release and Deployment](../user-guide/workflow.md#release-process)

## Workflow Metrics

Rinna provides various metrics to help teams understand and optimize their workflow:

- Cycle Time: Time from TO_DO to DONE
- Lead Time: Time from creation to DONE
- Throughput: Work items completed per time period
- Work in Progress (WIP): Items currently being worked on
- Flow Efficiency: Ratio of active work time to total time

[Learn more about workflow metrics](../user-guide/metrics/it-workflow-metrics.md)
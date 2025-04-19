@portfolio @core
Feature: Portfolio Management with Workstreams and Projects
  As a stakeholder with cross-cutting organizational responsibilities
  I want to organize, track, and report on work across multiple dimensions
  So that I can manage resources, priorities, and visibility effectively

  Background:
    Given the Rinna system is initialized
    And the following organizational units exist:
      | Name               | Code   | Type          | Parent |
      | Acme Corporation   | ACME   | Organization  |        |
      | Engineering        | ENG    | Division      | ACME   |
      | Product            | PROD   | Division      | ACME   |
      | Sales              | SALES  | Division      | ACME   |
      | Platform Team      | PLAT   | Team          | ENG    |
      | Mobile Team        | MOB    | Team          | ENG    |
      | Web Team           | WEB    | Team          | ENG    |
      | QA Team            | QA     | Team          | ENG    |
    And the following people exist:
      | Name           | Role             | Unit   | Email                  |
      | Alice Johnson  | VP Engineering   | ENG    | alice@acme.com         |
      | Bob Smith      | Product Manager  | PROD   | bob@acme.com           |
      | Carol Davis    | Product Owner    | PROD   | carol@acme.com         |
      | Dave Wilson    | Project Manager  | ENG    | dave@acme.com          |
      | Emily Brown    | Developer        | PLAT   | emily@acme.com         |
      | Frank Garcia   | Developer        | MOB    | frank@acme.com         |
      | Grace Lee      | Developer        | WEB    | grace@acme.com         |
      | Henry Chen     | QA Engineer      | QA     | henry@acme.com         |
    And the following projects exist:
      | ID        | Name                 | Description                          | Manager      | Status    |
      | PROJ-001  | Platform Redesign    | Redesign core platform services      | Dave Wilson  | ACTIVE    |
      | PROJ-002  | Mobile App v2        | Next generation mobile experience    | Dave Wilson  | ACTIVE    |
      | PROJ-003  | Analytics Dashboard  | Customer analytics dashboard         | Bob Smith    | PLANNING  |
    And the following workstreams exist:
      | ID        | Name                | Description                         | Owner         | Status    |
      | WS-001    | User Authentication | User auth across all products       | Carol Davis   | ACTIVE    |
      | WS-002    | Payment Processing  | Payment functionality and checkout  | Carol Davis   | ACTIVE    |
      | WS-003    | Reporting           | Cross-product reporting tools       | Bob Smith     | PLANNING  |

  # VP Engineering Scenarios
  @vp-engineering
  Scenario: VP Engineering views division-wide resource allocation
    Given I am logged in as "Alice Johnson"
    When I run the command "rin allocation --division=ENG"
    Then the command should succeed
    And the output should include a resource allocation chart
    And the output should show allocation percentages for all projects
    And the output should include overallocated resources
    And the output should include the "Engineering" division summary

  @vp-engineering
  Scenario: VP Engineering generates cross-project status report
    Given I am logged in as "Alice Johnson"
    When I run the command "rin report division --format=html --output=eng-status.html"
    Then the command should succeed
    And the file "eng-status.html" should be created
    And the file should contain division-level metrics
    And the report should include:
      | Section            | Metrics                                        |
      | Project Status     | Projects by status, completion %               |
      | Resource Capacity  | Allocated vs available, by team                |
      | Risk Assessment    | Count of high/medium/low risk areas            |
      | Timeline Adherence | Projects on-time vs delayed                    |

  @vp-engineering
  Scenario: VP Engineering adjusts organization resource allocation
    Given I am logged in as "Alice Johnson"
    When I run the command "rin edit allocation --resource='Henry Chen' --project=PROJ-001 --allocation=50"
    And I run the command "rin edit allocation --resource='Henry Chen' --project=PROJ-002 --allocation=50"
    Then the command should succeed
    And the system should validate that total allocations do not exceed 100%
    When I run the command "rin allocation --resource='Henry Chen'"
    Then the output should show "PROJ-001: 50%" and "PROJ-002: 50%"

  @vp-engineering @negative
  Scenario: VP Engineering attempts to overallocate resources
    Given I am logged in as "Alice Johnson"
    And "Emily Brown" is already allocated 80% to "PROJ-001"
    When I run the command "rin edit allocation --resource='Emily Brown' --project=PROJ-002 --allocation=30"
    Then the command should fail
    And the output should contain "Error: Total allocation for Emily Brown would exceed 100% (current: 80%, requested: 30%)"

  # Product Manager Scenarios
  @product-manager
  Scenario: Product Manager creates a new workstream
    Given I am logged in as "Bob Smith"
    When I run the command "rin create workstream --name='User Onboarding' --description='Improve new user experience across all products'"
    Then the command should succeed
    And the output should contain a workstream ID
    When I run the command "rin view workstream --id={last-id}"
    Then the output should contain "User Onboarding"
    And the output should contain "Owner: Bob Smith"
    And the output should contain "Status: DRAFT"

  @product-manager
  Scenario: Product Manager associates work items with a workstream
    Given I am logged in as "Bob Smith"
    And the following work items exist:
      | ID        | Type      | Title                     | State   | Project   |
      | FEAT-101  | Feature   | User profile management   | BACKLOG | PROJ-001  |
      | FEAT-102  | Feature   | Login page redesign       | BACKLOG | PROJ-002  |
    When I run the command "rin associate --workstream=WS-001 --item=FEAT-101"
    And I run the command "rin associate --workstream=WS-001 --item=FEAT-102"
    Then the command should succeed
    When I run the command "rin list --workstream=WS-001"
    Then the output should contain "FEAT-101" and "FEAT-102"
    And the output should maintain project association information

  @product-manager
  Scenario: Product Manager generates workstream status dashboard
    Given I am logged in as "Bob Smith"
    And workstream "WS-001" has items in various states across projects
    When I run the command "rin dashboard --workstream=WS-001 --view=status"
    Then the command should succeed
    And the output should group items by status
    And the output should show progress by project
    And the output should include:
      | Metric                  | Value                  |
      | Overall completion      | XX%                    |
      | Items by status         | TODO: X, IN PROGRESS: Y, DONE: Z |
      | Project breakdown       | PROJ-001: X%, PROJ-002: Y%       |
      | Critical path impact    | High/Medium/Low        |

  @product-manager
  Scenario: Product Manager sets workstream priorities
    Given I am logged in as "Bob Smith"
    When I run the command "rin priority set --workstream=WS-001 --level=HIGH"
    And I run the command "rin priority set --workstream=WS-002 --level=MEDIUM"
    And I run the command "rin priority set --workstream=WS-003 --level=LOW"
    Then the command should succeed
    When I run the command "rin list workstreams --sort=priority"
    Then the output should list workstreams in order: "WS-001", "WS-002", "WS-003"

  @product-manager @negative
  Scenario: Product Manager attempts to modify another PM's workstream
    Given I am logged in as "Bob Smith"
    And workstream "WS-004" is owned by "Another PM"
    When I run the command "rin edit workstream --id=WS-004 --status=ACTIVE"
    Then the command should fail
    And the output should contain "Error: You don't have permission to edit this workstream"

  # Product Owner Scenarios
  @product-owner
  Scenario: Product Owner creates a feature in a workstream
    Given I am logged in as "Carol Davis"
    When I run the command "rin add feature --title='Social Login' --description='Allow login with social accounts' --workstream=WS-001 --project=PROJ-001"
    Then the command should succeed
    And the output should contain a work item ID
    When I run the command "rin view --id={last-id}"
    Then the output should contain "Social Login"
    And the output should contain "Workstream: WS-001"
    And the output should contain "Project: PROJ-001"

  @product-owner
  Scenario: Product Owner prioritizes workstream backlog
    Given I am logged in as "Carol Davis"
    And workstream "WS-001" has multiple work items
    When I run the command "rin rank --workstream=WS-001 --item=FEAT-103 --position=1"
    And I run the command "rin rank --workstream=WS-001 --item=FEAT-104 --position=2"
    Then the command should succeed
    When I run the command "rin backlog --workstream=WS-001"
    Then the output should list items in rank order
    And "FEAT-103" should appear before "FEAT-104"

  @product-owner
  Scenario: Product Owner generates workstream roadmap
    Given I am logged in as "Carol Davis"
    And workstream "WS-001" has items with target dates
    When I run the command "rin roadmap --workstream=WS-001 --timeframe=Q4"
    Then the command should succeed
    And the output should show a timeline visualization
    And the output should include items grouped by milestone/quarter
    And the output should include dependencies between items

  @product-owner
  Scenario: Product Owner reports on cross-project feature status
    Given I am logged in as "Carol Davis"
    And there are features for authentication across multiple projects
    When I run the command "rin report feature-status --workstream=WS-001"
    Then the command should succeed
    And the output should show feature completion by project
    And the output should highlight uneven progress across projects
    And the output should include:
      | Project     | Features Complete | Features In-Progress | Features Not Started |
      | PROJ-001    | 2                | 1                    | 0                    |
      | PROJ-002    | 1                | 2                    | 1                    |
      | Overall     | 3                | 3                    | 1                    |

  @product-owner @negative
  Scenario: Product Owner attempts to modify project allocation
    Given I am logged in as "Carol Davis"
    When I run the command "rin edit allocation --resource='Emily Brown' --project=PROJ-001 --allocation=90"
    Then the command should fail
    And the output should contain "Error: Insufficient permissions to modify resource allocations"

  # Project Manager Scenarios
  @project-manager
  Scenario: Project Manager creates a project plan
    Given I am logged in as "Dave Wilson"
    When I run the command "rin create plan --project=PROJ-001 --start=2025-01-01 --end=2025-06-30"
    Then the command should succeed
    And the output should contain "Project plan created"
    When I run the command "rin view plan --project=PROJ-001"
    Then the output should contain "Platform Redesign"
    And the output should contain "Start: 2025-01-01"
    And the output should contain "End: 2025-06-30"
    And the output should contain "Manager: Dave Wilson"

  @project-manager
  Scenario: Project Manager adds milestones to project plan
    Given I am logged in as "Dave Wilson"
    And a project plan exists for "PROJ-001"
    When I run the command "rin add milestone --project=PROJ-001 --name='Design Complete' --date=2025-02-15"
    And I run the command "rin add milestone --project=PROJ-001 --name='Beta Release' --date=2025-04-01"
    And I run the command "rin add milestone --project=PROJ-001 --name='GA Release' --date=2025-06-01"
    Then the command should succeed
    When I run the command "rin list milestones --project=PROJ-001"
    Then the output should contain "Design Complete" and "2025-02-15"
    And the output should contain "Beta Release" and "2025-04-01" 
    And the output should contain "GA Release" and "2025-06-01"

  @project-manager
  Scenario: Project Manager tracks project resources and allocation
    Given I am logged in as "Dave Wilson" 
    When I run the command "rin resources --project=PROJ-001"
    Then the command should succeed
    And the output should show all allocated resources
    And the output should include:
      | Resource     | Role          | Allocation | Start Date  | End Date    |
      | Emily Brown  | Developer     | 80%        | 2025-01-01  | 2025-06-30  |
      | Henry Chen   | QA Engineer   | 50%        | 2025-02-01  | 2025-06-30  |
    When I run the command "rin allocation --project=PROJ-001 --view=timeline"
    Then the output should show resource allocation over time
    And the output should identify periods of under/over allocation

  @project-manager
  Scenario: Project Manager generates project status report
    Given I am logged in as "Dave Wilson"
    And project "PROJ-001" has work items in various states
    When I run the command "rin report project-status --project=PROJ-001 --format=html --output=proj-status.html"
    Then the command should succeed
    And the file "proj-status.html" should be created
    And the report should include:
      | Section            | Metrics                                        |
      | Overall Status     | On Track/At Risk/Delayed                       |
      | Milestone Status   | Completed, upcoming, delayed milestones        |
      | Work Item Progress | By type, by state, blockers                    |
      | Resource Utilization | Current vs planned allocation                |
      | Risk Register      | Current risks with mitigation status           |

  @project-manager
  Scenario: Project Manager identifies cross-workstream dependencies
    Given I am logged in as "Dave Wilson"
    And there are work items in "PROJ-001" that depend on work items in other projects
    When I run the command "rin dependencies --project=PROJ-001 --view=cross-workstream"
    Then the command should succeed
    And the output should show items grouped by workstream
    And the output should identify dependencies across workstreams
    And the output should highlight critical dependencies
    And the output should show the impact on project timeline

  @project-manager @negative
  Scenario: Project Manager attempts to reassign workstream ownership
    Given I am logged in as "Dave Wilson"
    When I run the command "rin assign --workstream=WS-001 --owner='Dave Wilson'"
    Then the command should fail
    And the output should contain "Error: Insufficient permissions to reassign workstream ownership"

  # Cross-Role Collaboration Scenarios
  @cross-role
  Scenario: Cross-functional view of a work item in different contexts
    Given the following work item exists:
      | ID        | Type     | Title               | State     | Project   | Workstream |
      | FEAT-201  | Feature  | Unified Login UI    | ACTIVE    | PROJ-002  | WS-001     |
    When I am logged in as "Alice Johnson"
    And I run the command "rin view --id=FEAT-201 --context=division"
    Then the output should show division-level impact and cross-project visibility
    When I am logged in as "Bob Smith"
    And I run the command "rin view --id=FEAT-201 --context=workstream"
    Then the output should show workstream progress and feature alignment
    When I am logged in as "Carol Davis"
    And I run the command "rin view --id=FEAT-201 --context=product"
    Then the output should show product requirements and user stories
    When I am logged in as "Dave Wilson"
    And I run the command "rin view --id=FEAT-201 --context=project"
    Then the output should show project timeline impact and resource assignments

  @cross-role
  Scenario: Holistic impact analysis for a proposed change
    Given I am logged in as "Carol Davis"
    And a high-priority feature exists across multiple projects
    When I run the command "rin impact-analysis --item=FEAT-301 --change='Delay by 2 weeks'"
    Then the command should succeed
    And the output should show impact across dimensions:
      | Dimension     | Impact                                  |
      | Projects      | Timeline shifts for PROJ-001, PROJ-002  |
      | Workstreams   | Delays in WS-001 deliverables           |
      | Resources     | Changed allocation needs                |
      | Dependencies  | 3 dependent features affected           |
      | Milestones    | 1 milestone at risk                     |

  @cross-role
  Scenario: Real-time collaboration on cross-project prioritization
    Given the following stakeholders are in a prioritization session:
      | Role             | Person        |
      | VP Engineering   | Alice Johnson |
      | Product Manager  | Bob Smith     |
      | Product Owner    | Carol Davis   |
      | Project Manager  | Dave Wilson   |
    When they run the command "rin prioritize --collaborative --scope=Q1-2025"
    Then the command should enter interactive mode
    And each stakeholder should be able to provide input on priorities
    And the system should highlight conflicts across workstreams and projects
    And the system should recommend optimal resource allocation
    And the system should visualize trade-offs in real-time
    And the final prioritization should be saved with consensus tracking

  @cross-role
  Scenario: Executive dashboard showing workstreams and projects
    Given I am logged in as "Alice Johnson"
    When I run the command "rin executive-dashboard --view=combined --format=html"
    Then the command should succeed
    And the output should include:
      | Panel                   | Contents                                    |
      | Strategic Alignment     | Workstreams mapped to strategic objectives  |
      | Resource Utilization    | Overall allocation across teams             |
      | Project Health          | Status indicators for all active projects   |
      | Critical Path Items     | High-impact work items across workstreams   |
      | Risk Assessment         | Aggregated risk indicators                  |
      | Delivery Forecast       | Projected completion for key initiatives    |

  # Reporting and Forecasting Scenarios
  @forecasting
  Scenario: Portfolio-level delivery forecasting
    Given I am logged in as "Alice Johnson"
    When I run the command "rin forecast --timeframe=Q1-Q4-2025 --confidence=90%"
    Then the command should succeed
    And the output should show quarterly delivery projections
    And the output should include confidence intervals for key deliverables
    And the output should highlight potential capacity constraints
    And the output should identify cross-project dependencies affecting timelines

  @reporting
  Scenario: Resource allocation efficiency report
    Given I am logged in as "Alice Johnson"
    When I run the command "rin report efficiency --division=ENG --period=last-quarter"
    Then the command should succeed
    And the output should include:
      | Metric                       | Value                              |
      | Resource utilization         | 87% average, 95% peak              |
      | Context switching frequency  | 2.3 project changes per week       |
      | Work distribution            | 60% planned, 40% unplanned         |
      | Interdependency bottlenecks  | 3 identified, 2 resolved           |
      | Critical resource saturation | Web Team: 97%, Mobile Team: 85%    |

  @reporting
  Scenario: Workstream value delivery analysis
    Given I am logged in as "Bob Smith"
    When I run the command "rin report value --workstream=WS-001 --period=ytd"
    Then the command should succeed
    And the output should include business outcomes measurement
    And the output should track delivered vs planned features
    And the output should analyze deployment frequency and cycle time
    And the output should compare effectiveness across projects

  @forecasting @negative
  Scenario: Project Manager attempts unauthorized capacity planning
    Given I am logged in as "Dave Wilson"
    When I run the command "rin capacity-planning --division=ENG --quarter=Q1-2025"
    Then the command should fail
    And the output should contain "Error: Insufficient permissions for division-level capacity planning"
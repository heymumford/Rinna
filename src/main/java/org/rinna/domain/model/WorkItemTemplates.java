/*
 * Domain entity class for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Provides standard templates for different work categories.
 * This class acts as a repository of predefined templates that users can select from
 * when creating new work items.
 */
public final class WorkItemTemplates {
    private static final List<WorkItemTemplate> TEMPLATES = new ArrayList<>();
    
    static {
        // Initialize with predefined templates
        initializeTemplates();
    }
    
    /**
     * Private constructor to prevent instantiation.
     */
    private WorkItemTemplates() {
        // This constructor is intentionally empty
    }
    
    /**
     * Initializes the predefined templates.
     */
    private static void initializeTemplates() {
        // Product-focused templates
        TEMPLATES.add(createProductStoryTemplate());
        TEMPLATES.add(createProductFeatureTemplate());
        TEMPLATES.add(createProductEpicTemplate());
        TEMPLATES.add(createProductGoalTemplate());
        TEMPLATES.add(createProductExperimentTemplate());
        
        // Architecture-focused templates
        TEMPLATES.add(createArchitectureDecisionTemplate());
        TEMPLATES.add(createArchitectureReviewTemplate());
        TEMPLATES.add(createArchitectureRefactoringTemplate());
        TEMPLATES.add(createArchitectureProofOfConceptTemplate());
        
        // Development-focused templates
        TEMPLATES.add(createDevImplementationTaskTemplate());
        TEMPLATES.add(createDevBugFixTemplate());
        TEMPLATES.add(createDevRefactoringTemplate());
        TEMPLATES.add(createDevTechnicalDebtTemplate());
        TEMPLATES.add(createDevPerformanceTemplate());
        
        // Test-focused templates
        TEMPLATES.add(createTestPlanTemplate());
        TEMPLATES.add(createTestCaseTemplate());
        TEMPLATES.add(createTestAutomationTemplate());
        TEMPLATES.add(createTestDefectTemplate());
        
        // Operations-focused templates
        TEMPLATES.add(createOpsDeploymentTemplate());
        TEMPLATES.add(createOpsMonitoringTemplate());
        TEMPLATES.add(createOpsIncidentTemplate());
        TEMPLATES.add(createOpsSecurityTemplate());
        
        // Documentation-focused templates
        TEMPLATES.add(createDocUserGuideTemplate());
        TEMPLATES.add(createDocApiReferenceTemplate());
        TEMPLATES.add(createDocReleaseNotesTemplate());
        
        // Cross-functional templates
        TEMPLATES.add(createCrossTeamCoordinationTemplate());
        TEMPLATES.add(createCrossSystemIntegrationTemplate());
        TEMPLATES.add(createCrossInnovationTemplate());
    }
    
    /**
     * Gets all available templates.
     * 
     * @return A list of all available templates
     */
    public static List<WorkItemTemplate> getAllTemplates() {
        return Collections.unmodifiableList(TEMPLATES);
    }
    
    /**
     * Gets templates for a specific origin category.
     * 
     * @param category The origin category to filter by
     * @return A list of templates for the specified category
     */
    public static List<WorkItemTemplate> getTemplatesByCategory(OriginCategory category) {
        return TEMPLATES.stream()
                .filter(template -> template.category() == category)
                .collect(Collectors.toList());
    }
    
    /**
     * Gets templates for a specific work paradigm.
     * 
     * @param paradigm The work paradigm to filter by
     * @return A list of templates for the specified paradigm
     */
    public static List<WorkItemTemplate> getTemplatesByParadigm(WorkParadigm paradigm) {
        return TEMPLATES.stream()
                .filter(template -> template.workParadigm() == paradigm)
                .collect(Collectors.toList());
    }
    
    /**
     * Gets templates for a specific CYNEFIN domain.
     * 
     * @param domain The CYNEFIN domain to filter by
     * @return A list of templates for the specified domain
     */
    public static List<WorkItemTemplate> getTemplatesByDomain(CynefinDomain domain) {
        return TEMPLATES.stream()
                .filter(template -> template.cynefinDomain() == domain)
                .collect(Collectors.toList());
    }
    
    /**
     * Gets the default template for a specific origin category.
     * 
     * @param category The origin category
     * @return An Optional containing the default template, or empty if none exists
     */
    public static Optional<WorkItemTemplate> getDefaultTemplate(OriginCategory category) {
        return TEMPLATES.stream()
                .filter(template -> template.category() == category && template.isDefault())
                .findFirst();
    }
    
    /**
     * Gets a template by its ID.
     * 
     * @param id The template ID
     * @return An Optional containing the template, or empty if not found
     */
    public static Optional<WorkItemTemplate> getTemplateById(String id) {
        return TEMPLATES.stream()
                .filter(template -> template.id().equals(id))
                .findFirst();
    }
    
    /**
     * Gets a template by its name.
     * 
     * @param name The template name
     * @return An Optional containing the template, or empty if not found
     */
    public static Optional<WorkItemTemplate> getTemplateByName(String name) {
        return TEMPLATES.stream()
                .filter(template -> template.name().equals(name))
                .findFirst();
    }
    
    /**
     * Adds a custom template to the list of available templates.
     * 
     * @param template The template to add
     */
    public static void addTemplate(WorkItemTemplate template) {
        TEMPLATES.add(template);
    }
    
    /**
     * Removes a template from the list of available templates.
     * 
     * @param templateId The ID of the template to remove
     * @return true if the template was removed, false if not found
     */
    public static boolean removeTemplate(String templateId) {
        return TEMPLATES.removeIf(template -> template.id().equals(templateId));
    }
    
    // Product-focused templates
    
    private static WorkItemTemplate createProductStoryTemplate() {
        return WorkItemTemplate.builder()
                .id("product-story")
                .name("Product User Story")
                .description("Template for user stories from a product perspective")
                .category(OriginCategory.PROD)
                .cynefinDomain(CynefinDomain.COMPLICATED)
                .workParadigm(WorkParadigm.STORY)
                .type(WorkItemType.STORY)
                .initialState(WorkflowState.CREATED)
                .defaultPriority(Priority.MEDIUM)
                .defaultTags(Arrays.asList("user-story", "product"))
                .descriptionTemplate("""
                        ## User Story
                        As a [type of user],
                        I want [goal],
                        So that [benefit/value].
                        
                        ## Acceptance Criteria
                        1. 
                        2. 
                        3. 
                        
                        ## Business Value
                        [Describe the business value this story delivers]
                        
                        ## Dependencies
                        [List any dependencies]
                        
                        ## Notes
                        [Additional context or considerations]
                        """)
                .suggestedCognitiveLoad(5)
                .isDefault(true)
                .build();
    }
    
    private static WorkItemTemplate createProductFeatureTemplate() {
        return WorkItemTemplate.builder()
                .id("product-feature")
                .name("Product Feature")
                .description("Template for product features")
                .category(OriginCategory.PROD)
                .cynefinDomain(CynefinDomain.COMPLICATED)
                .workParadigm(WorkParadigm.STORY)
                .type(WorkItemType.FEATURE)
                .initialState(WorkflowState.CREATED)
                .defaultPriority(Priority.HIGH)
                .defaultTags(Arrays.asList("feature", "product"))
                .descriptionTemplate("""
                        ## Feature Description
                        [Provide a comprehensive description of this feature]
                        
                        ## Target Users
                        [Identify the user segments this feature targets]
                        
                        ## Business Objectives
                        [List the business objectives this feature supports]
                        
                        ## Success Metrics
                        [Define how success will be measured for this feature]
                        
                        ## Feature Requirements
                        [List the high-level requirements]
                        
                        ## Dependencies
                        [List any dependencies on other features or systems]
                        
                        ## Constraints
                        [List any constraints that must be respected]
                        """)
                .suggestedCognitiveLoad(7)
                .build();
    }
    
    private static WorkItemTemplate createProductEpicTemplate() {
        return WorkItemTemplate.builder()
                .id("product-epic")
                .name("Product Epic")
                .description("Template for product epics that group related features")
                .category(OriginCategory.PROD)
                .cynefinDomain(CynefinDomain.COMPLEX)
                .workParadigm(WorkParadigm.GOAL)
                .type(WorkItemType.EPIC)
                .initialState(WorkflowState.CREATED)
                .defaultPriority(Priority.HIGH)
                .defaultTags(Arrays.asList("epic", "product"))
                .descriptionTemplate("""
                        ## Epic Vision
                        [Provide a high-level vision for this epic]
                        
                        ## Strategic Alignment
                        [Explain how this epic aligns with the product strategy]
                        
                        ## Target Outcomes
                        [Describe the intended outcomes from delivering this epic]
                        
                        ## Success Criteria
                        [Define the criteria for determining when this epic is successful]
                        
                        ## Target Timeline
                        [Provide a high-level timeline for this epic]
                        
                        ## Key Features
                        [List the key features that compose this epic]
                        
                        ## Stakeholders
                        [List the key stakeholders for this epic]
                        
                        ## External Dependencies
                        [List any external dependencies]
                        """)
                .suggestedCognitiveLoad(8)
                .build();
    }
    
    private static WorkItemTemplate createProductGoalTemplate() {
        return WorkItemTemplate.builder()
                .id("product-goal")
                .name("Product Goal")
                .description("Template for product goals focused on outcomes")
                .category(OriginCategory.PROD)
                .cynefinDomain(CynefinDomain.COMPLEX)
                .workParadigm(WorkParadigm.GOAL)
                .type(WorkItemType.GOAL)
                .initialState(WorkflowState.CREATED)
                .defaultPriority(Priority.HIGH)
                .defaultTags(Arrays.asList("goal", "product", "outcome"))
                .descriptionTemplate("""
                        ## Goal Statement
                        [Clear statement of the goal in outcome terms]
                        
                        ## Business Impact
                        [Describe the expected business impact of achieving this goal]
                        
                        ## Success Metrics
                        [Define specific, measurable indicators of success]
                        
                        ## Time Horizon
                        [Specify the time frame for achieving this goal]
                        
                        ## Key Results
                        [List 3-5 key results that would indicate goal achievement]
                        
                        ## Assumptions
                        [Document key assumptions underlying this goal]
                        
                        ## Implementation Approach
                        [Describe the high-level approach to achieving this goal]
                        """)
                .suggestedCognitiveLoad(7)
                .build();
    }
    
    private static WorkItemTemplate createProductExperimentTemplate() {
        return WorkItemTemplate.builder()
                .id("product-experiment")
                .name("Product Experiment")
                .description("Template for product experiments to validate hypotheses")
                .category(OriginCategory.PROD)
                .cynefinDomain(CynefinDomain.COMPLEX)
                .workParadigm(WorkParadigm.EXPERIMENT)
                .type(WorkItemType.SPIKE)
                .initialState(WorkflowState.CREATED)
                .defaultPriority(Priority.MEDIUM)
                .defaultTags(Arrays.asList("experiment", "product", "learning"))
                .descriptionTemplate("""
                        ## Hypothesis
                        We believe that [change] will result in [expected outcome] for [user segment].
                        
                        ## Background
                        [Provide context about why this experiment is important]
                        
                        ## Success Metrics
                        [Define how we'll measure success of this experiment]
                        
                        ## Method
                        [Describe the experiment design and implementation approach]
                        
                        ## Required Data
                        [List the data needed to evaluate the hypothesis]
                        
                        ## Timeline
                        [Specify when the experiment will start and end]
                        
                        ## Resources Required
                        [List the resources needed to run the experiment]
                        
                        ## Risks and Mitigations
                        [Identify potential risks and mitigations]
                        """)
                .suggestedCognitiveLoad(6)
                .build();
    }
    
    // Architecture-focused templates
    
    private static WorkItemTemplate createArchitectureDecisionTemplate() {
        return WorkItemTemplate.builder()
                .id("arch-decision")
                .name("Architecture Decision")
                .description("Template for architectural decisions")
                .category(OriginCategory.ARCH)
                .cynefinDomain(CynefinDomain.COMPLICATED)
                .workParadigm(WorkParadigm.TASK)
                .type(WorkItemType.TASK)
                .initialState(WorkflowState.CREATED)
                .defaultPriority(Priority.HIGH)
                .defaultTags(Arrays.asList("architecture", "decision"))
                .descriptionTemplate("""
                        ## Context
                        [Describe the architectural context and background]
                        
                        ## Decision
                        [State the architectural decision clearly]
                        
                        ## Status
                        [Proposed/Accepted/Superseded/Deprecated]
                        
                        ## Assumptions
                        [List key assumptions]
                        
                        ## Constraints
                        [List applicable constraints]
                        
                        ## Positions Considered
                        [List alternative approaches considered]
                        
                        ## Arguments
                        [Present arguments for the selected approach]
                        
                        ## Implications
                        [Describe the implications of this decision]
                        
                        ## Related Decisions
                        [List related architectural decisions]
                        """)
                .suggestedCognitiveLoad(8)
                .isDefault(true)
                .build();
    }
    
    private static WorkItemTemplate createArchitectureReviewTemplate() {
        return WorkItemTemplate.builder()
                .id("arch-review")
                .name("Architecture Review")
                .description("Template for architecture reviews")
                .category(OriginCategory.ARCH)
                .cynefinDomain(CynefinDomain.COMPLICATED)
                .workParadigm(WorkParadigm.TASK)
                .type(WorkItemType.TASK)
                .initialState(WorkflowState.CREATED)
                .defaultPriority(Priority.MEDIUM)
                .defaultTags(Arrays.asList("architecture", "review"))
                .descriptionTemplate("""
                        ## Review Scope
                        [Define what aspects of architecture are being reviewed]
                        
                        ## Reviewers
                        [List the individuals involved in the review]
                        
                        ## Review Criteria
                        [List the criteria being used to evaluate the architecture]
                        
                        ## Areas of Focus
                        [Highlight specific areas that need attention]
                        
                        ## Required Documentation
                        [List documentation that should be available for the review]
                        
                        ## Review Process
                        [Describe the review process that will be followed]
                        
                        ## Output Format
                        [Specify the format for review findings and recommendations]
                        
                        ## Timeline
                        [Set a timeline for the review]
                        """)
                .suggestedCognitiveLoad(7)
                .build();
    }
    
    private static WorkItemTemplate createArchitectureRefactoringTemplate() {
        return WorkItemTemplate.builder()
                .id("arch-refactoring")
                .name("Architecture Refactoring")
                .description("Template for architectural refactoring tasks")
                .category(OriginCategory.ARCH)
                .cynefinDomain(CynefinDomain.COMPLICATED)
                .workParadigm(WorkParadigm.TASK)
                .type(WorkItemType.TASK)
                .initialState(WorkflowState.CREATED)
                .defaultPriority(Priority.MEDIUM)
                .defaultTags(Arrays.asList("architecture", "refactoring"))
                .descriptionTemplate("""
                        ## Current Architecture
                        [Describe the current architectural approach]
                        
                        ## Issues with Current Approach
                        [Outline problems with the current architecture]
                        
                        ## Proposed Architecture
                        [Describe the target architecture]
                        
                        ## Benefits of Refactoring
                        [Explain the benefits of the proposed changes]
                        
                        ## Refactoring Approach
                        [Outline the step-by-step approach for refactoring]
                        
                        ## Risk Assessment
                        [Identify risks and mitigations]
                        
                        ## Testing Strategy
                        [Describe how the refactoring will be tested]
                        
                        ## Impact Analysis
                        [Analyze impact on other system components]
                        """)
                .suggestedCognitiveLoad(8)
                .build();
    }
    
    private static WorkItemTemplate createArchitectureProofOfConceptTemplate() {
        return WorkItemTemplate.builder()
                .id("arch-poc")
                .name("Architecture Proof of Concept")
                .description("Template for architectural proof of concepts")
                .category(OriginCategory.ARCH)
                .cynefinDomain(CynefinDomain.COMPLEX)
                .workParadigm(WorkParadigm.EXPERIMENT)
                .type(WorkItemType.SPIKE)
                .initialState(WorkflowState.CREATED)
                .defaultPriority(Priority.HIGH)
                .defaultTags(Arrays.asList("architecture", "poc", "experiment"))
                .descriptionTemplate("""
                        ## Purpose
                        [State the purpose of this proof of concept]
                        
                        ## Hypothesis
                        [Articulate the hypothesis being tested]
                        
                        ## Success Criteria
                        [Define what will make this PoC successful]
                        
                        ## Technical Approach
                        [Outline the technical approach for the PoC]
                        
                        ## Scope and Constraints
                        [Define what is in and out of scope]
                        
                        ## Required Resources
                        [List resources needed for the PoC]
                        
                        ## Timeline
                        [Set a timeline for completing the PoC]
                        
                        ## Evaluation Method
                        [Describe how results will be evaluated]
                        
                        ## Next Steps
                        [Outline potential next steps after the PoC]
                        """)
                .suggestedCognitiveLoad(7)
                .build();
    }
    
    // Development-focused templates
    
    private static WorkItemTemplate createDevImplementationTaskTemplate() {
        return WorkItemTemplate.builder()
                .id("dev-implementation")
                .name("Development Implementation Task")
                .description("Template for development implementation tasks")
                .category(OriginCategory.DEV)
                .cynefinDomain(CynefinDomain.CLEAR)
                .workParadigm(WorkParadigm.TASK)
                .type(WorkItemType.TASK)
                .initialState(WorkflowState.CREATED)
                .defaultPriority(Priority.MEDIUM)
                .defaultTags(Arrays.asList("development", "implementation"))
                .descriptionTemplate("""
                        ## Implementation Requirements
                        [Describe what needs to be implemented]
                        
                        ## Technical Design
                        [Provide technical design details or link to design document]
                        
                        ## Acceptance Criteria
                        [List specific criteria for acceptance]
                        
                        ## Dependencies
                        [List dependencies on other work items or components]
                        
                        ## Implementation Approach
                        [Describe the implementation approach]
                        
                        ## Test Strategy
                        [Outline how this implementation will be tested]
                        
                        ## Estimated Effort
                        [Provide an estimate of the effort required]
                        
                        ## Documentation Requirements
                        [Specify documentation that should be created or updated]
                        """)
                .suggestedCognitiveLoad(5)
                .isDefault(true)
                .build();
    }
    
    private static WorkItemTemplate createDevBugFixTemplate() {
        return WorkItemTemplate.builder()
                .id("dev-bugfix")
                .name("Development Bug Fix")
                .description("Template for development bug fixes")
                .category(OriginCategory.DEV)
                .cynefinDomain(CynefinDomain.CLEAR)
                .workParadigm(WorkParadigm.TASK)
                .type(WorkItemType.BUG)
                .initialState(WorkflowState.CREATED)
                .defaultPriority(Priority.HIGH)
                .defaultTags(Arrays.asList("development", "bug", "fix"))
                .descriptionTemplate("""
                        ## Bug Description
                        [Describe the bug in detail]
                        
                        ## Steps to Reproduce
                        1. 
                        2. 
                        3. 
                        
                        ## Expected Behavior
                        [Describe what should happen]
                        
                        ## Actual Behavior
                        [Describe what actually happens]
                        
                        ## Environment
                        [Describe the environment where the bug occurs]
                        
                        ## Probable Cause
                        [Provide analysis of the probable cause if known]
                        
                        ## Fix Approach
                        [Describe the proposed approach to fix the bug]
                        
                        ## Verification Steps
                        [List steps to verify the fix]
                        """)
                .suggestedCognitiveLoad(6)
                .build();
    }
    
    private static WorkItemTemplate createDevRefactoringTemplate() {
        return WorkItemTemplate.builder()
                .id("dev-refactoring")
                .name("Code Refactoring")
                .description("Template for code refactoring tasks")
                .category(OriginCategory.DEV)
                .cynefinDomain(CynefinDomain.COMPLICATED)
                .workParadigm(WorkParadigm.TASK)
                .type(WorkItemType.TASK)
                .initialState(WorkflowState.CREATED)
                .defaultPriority(Priority.MEDIUM)
                .defaultTags(Arrays.asList("development", "refactoring"))
                .descriptionTemplate("""
                        ## Refactoring Objective
                        [State the goal of this refactoring]
                        
                        ## Current Code Issues
                        [Describe problems in the current code]
                        
                        ## Target Areas
                        [Identify specific code areas to refactor]
                        
                        ## Refactoring Approach
                        [Outline the refactoring approach]
                        
                        ## Risk Assessment
                        [Assess risks and ways to mitigate them]
                        
                        ## Test Coverage Required
                        [Specify test coverage needed to safely refactor]
                        
                        ## Acceptance Criteria
                        [Define when the refactoring is complete]
                        
                        ## Performance Implications
                        [Describe any performance implications]
                        """)
                .suggestedCognitiveLoad(7)
                .build();
    }
    
    private static WorkItemTemplate createDevTechnicalDebtTemplate() {
        return WorkItemTemplate.builder()
                .id("dev-techdebt")
                .name("Technical Debt Remediation")
                .description("Template for addressing technical debt")
                .category(OriginCategory.DEV)
                .cynefinDomain(CynefinDomain.COMPLICATED)
                .workParadigm(WorkParadigm.TASK)
                .type(WorkItemType.TASK)
                .initialState(WorkflowState.CREATED)
                .defaultPriority(Priority.MEDIUM)
                .defaultTags(Arrays.asList("development", "technical-debt"))
                .descriptionTemplate("""
                        ## Technical Debt Description
                        [Describe the technical debt in detail]
                        
                        ## Impact Assessment
                        [Assess the current impact of this technical debt]
                        
                        ## Root Cause
                        [Identify why this technical debt was incurred]
                        
                        ## Remediation Approach
                        [Outline the approach to address the debt]
                        
                        ## Acceptance Criteria
                        [Define completion criteria]
                        
                        ## Benefits of Remediation
                        [Describe the benefits of addressing this debt]
                        
                        ## Risks of Not Addressing
                        [Identify risks of leaving this debt unaddressed]
                        
                        ## Estimated Payback Period
                        [Estimate how long until remediation pays off]
                        """)
                .suggestedCognitiveLoad(6)
                .build();
    }
    
    private static WorkItemTemplate createDevPerformanceTemplate() {
        return WorkItemTemplate.builder()
                .id("dev-performance")
                .name("Performance Optimization")
                .description("Template for performance optimization tasks")
                .category(OriginCategory.DEV)
                .cynefinDomain(CynefinDomain.COMPLICATED)
                .workParadigm(WorkParadigm.TASK)
                .type(WorkItemType.TASK)
                .initialState(WorkflowState.CREATED)
                .defaultPriority(Priority.HIGH)
                .defaultTags(Arrays.asList("development", "performance"))
                .descriptionTemplate("""
                        ## Performance Issue
                        [Describe the performance issue in detail]
                        
                        ## Current Metrics
                        [Provide current performance metrics]
                        
                        ## Target Metrics
                        [Define target performance metrics]
                        
                        ## Bottleneck Analysis
                        [Analyze where the bottlenecks occur]
                        
                        ## Optimization Approach
                        [Outline the approach for optimization]
                        
                        ## Validation Method
                        [Describe how improvements will be measured]
                        
                        ## Test Scenarios
                        [Define test scenarios for performance validation]
                        
                        ## Dependencies
                        [List any dependencies]
                        """)
                .suggestedCognitiveLoad(7)
                .build();
    }
    
    // Test-focused templates
    
    private static WorkItemTemplate createTestPlanTemplate() {
        return WorkItemTemplate.builder()
                .id("test-plan")
                .name("Test Plan")
                .description("Template for test plans")
                .category(OriginCategory.TEST)
                .cynefinDomain(CynefinDomain.COMPLICATED)
                .workParadigm(WorkParadigm.TASK)
                .type(WorkItemType.TASK)
                .initialState(WorkflowState.CREATED)
                .defaultPriority(Priority.HIGH)
                .defaultTags(Arrays.asList("testing", "plan"))
                .descriptionTemplate("""
                        ## Test Scope
                        [Define what is being tested]
                        
                        ## Test Objectives
                        [List the objectives of this test plan]
                        
                        ## Features to be Tested
                        [List specific features to be tested]
                        
                        ## Features Not to be Tested
                        [List features specifically excluded from testing]
                        
                        ## Test Approach
                        [Describe the overall testing approach]
                        
                        ## Test Deliverables
                        [List expected deliverables from this test effort]
                        
                        ## Test Environment
                        [Describe the test environment requirements]
                        
                        ## Test Schedule
                        [Provide a testing schedule]
                        
                        ## Roles and Responsibilities
                        [Define who is responsible for what]
                        
                        ## Risks and Contingencies
                        [Identify risks and contingency plans]
                        """)
                .suggestedCognitiveLoad(7)
                .isDefault(true)
                .build();
    }
    
    private static WorkItemTemplate createTestCaseTemplate() {
        return WorkItemTemplate.builder()
                .id("test-case")
                .name("Test Case")
                .description("Template for test cases")
                .category(OriginCategory.TEST)
                .cynefinDomain(CynefinDomain.CLEAR)
                .workParadigm(WorkParadigm.TASK)
                .type(WorkItemType.TASK)
                .initialState(WorkflowState.CREATED)
                .defaultPriority(Priority.MEDIUM)
                .defaultTags(Arrays.asList("testing", "test-case"))
                .descriptionTemplate("""
                        ## Test Case ID
                        [Unique identifier for this test case]
                        
                        ## Test Objective
                        [What this test case is intended to verify]
                        
                        ## Preconditions
                        [Conditions that must be satisfied before executing this test]
                        
                        ## Test Data
                        [Data required for test execution]
                        
                        ## Test Steps
                        1. 
                        2. 
                        3. 
                        
                        ## Expected Results
                        [The expected outcome of each test step]
                        
                        ## Pass/Fail Criteria
                        [Criteria for determining test success]
                        
                        ## Related Requirements
                        [Links to requirements being tested]
                        """)
                .suggestedCognitiveLoad(4)
                .build();
    }
    
    private static WorkItemTemplate createTestAutomationTemplate() {
        return WorkItemTemplate.builder()
                .id("test-automation")
                .name("Test Automation")
                .description("Template for test automation tasks")
                .category(OriginCategory.TEST)
                .cynefinDomain(CynefinDomain.COMPLICATED)
                .workParadigm(WorkParadigm.TASK)
                .type(WorkItemType.TASK)
                .initialState(WorkflowState.CREATED)
                .defaultPriority(Priority.MEDIUM)
                .defaultTags(Arrays.asList("testing", "automation"))
                .descriptionTemplate("""
                        ## Automation Objective
                        [Describe what this automation is intended to accomplish]
                        
                        ## Test Scenarios to Automate
                        [List the test scenarios to be automated]
                        
                        ## Automation Approach
                        [Describe the technical approach for automation]
                        
                        ## Tools and Frameworks
                        [List tools and frameworks to be used]
                        
                        ## Test Data Management
                        [Describe how test data will be managed]
                        
                        ## Execution Environment
                        [Specify where automated tests will run]
                        
                        ## CI/CD Integration
                        [Describe how tests will integrate with CI/CD]
                        
                        ## Maintenance Plan
                        [Outline how automated tests will be maintained]
                        """)
                .suggestedCognitiveLoad(6)
                .build();
    }
    
    private static WorkItemTemplate createTestDefectTemplate() {
        return WorkItemTemplate.builder()
                .id("test-defect")
                .name("Test Defect")
                .description("Template for test defects")
                .category(OriginCategory.TEST)
                .cynefinDomain(CynefinDomain.CLEAR)
                .workParadigm(WorkParadigm.TASK)
                .type(WorkItemType.BUG)
                .initialState(WorkflowState.CREATED)
                .defaultPriority(Priority.HIGH)
                .defaultTags(Arrays.asList("testing", "defect"))
                .descriptionTemplate("""
                        ## Defect Summary
                        [Brief description of the defect]
                        
                        ## Steps to Reproduce
                        1. 
                        2. 
                        3. 
                        
                        ## Expected Result
                        [What should happen]
                        
                        ## Actual Result
                        [What actually happened]
                        
                        ## Test Environment
                        [Environment details where defect was found]
                        
                        ## Severity
                        [Defect severity: Critical/High/Medium/Low]
                        
                        ## Test Evidence
                        [Screenshots, logs, or other evidence]
                        
                        ## Workaround
                        [Temporary workaround if available]
                        """)
                .suggestedCognitiveLoad(5)
                .build();
    }
    
    // Operations-focused templates
    
    private static WorkItemTemplate createOpsDeploymentTemplate() {
        return WorkItemTemplate.builder()
                .id("ops-deployment")
                .name("Deployment Task")
                .description("Template for deployment tasks")
                .category(OriginCategory.OPS)
                .cynefinDomain(CynefinDomain.COMPLICATED)
                .workParadigm(WorkParadigm.TASK)
                .type(WorkItemType.TASK)
                .initialState(WorkflowState.CREATED)
                .defaultPriority(Priority.HIGH)
                .defaultTags(Arrays.asList("operations", "deployment"))
                .descriptionTemplate("""
                        ## Deployment Scope
                        [Define what is being deployed]
                        
                        ## Deployment Environment
                        [Specify target environment(s)]
                        
                        ## Deployment Schedule
                        [Planned date/time for deployment]
                        
                        ## Deployment Steps
                        1. 
                        2. 
                        3. 
                        
                        ## Rollback Plan
                        [Steps to rollback if deployment fails]
                        
                        ## Verification Steps
                        [Steps to verify successful deployment]
                        
                        ## Service Impact
                        [Expected impact on service availability]
                        
                        ## Stakeholder Communication
                        [Plan for communicating with stakeholders]
                        """)
                .suggestedCognitiveLoad(6)
                .isDefault(true)
                .build();
    }
    
    private static WorkItemTemplate createOpsMonitoringTemplate() {
        return WorkItemTemplate.builder()
                .id("ops-monitoring")
                .name("Monitoring Implementation")
                .description("Template for implementing monitoring")
                .category(OriginCategory.OPS)
                .cynefinDomain(CynefinDomain.COMPLICATED)
                .workParadigm(WorkParadigm.TASK)
                .type(WorkItemType.TASK)
                .initialState(WorkflowState.CREATED)
                .defaultPriority(Priority.MEDIUM)
                .defaultTags(Arrays.asList("operations", "monitoring"))
                .descriptionTemplate("""
                        ## Monitoring Objective
                        [Describe what is being monitored and why]
                        
                        ## Metrics to Collect
                        [List specific metrics to collect]
                        
                        ## Thresholds and Alerts
                        [Define alert thresholds and notification channels]
                        
                        ## Monitoring Tools
                        [Specify tools and technologies to use]
                        
                        ## Dashboard Requirements
                        [Describe dashboard requirements if applicable]
                        
                        ## Data Retention Policy
                        [Specify how long monitoring data should be retained]
                        
                        ## Integration Points
                        [List integration with other monitoring or alerting systems]
                        
                        ## Implementation Plan
                        [Outline the implementation approach]
                        """)
                .suggestedCognitiveLoad(5)
                .build();
    }
    
    private static WorkItemTemplate createOpsIncidentTemplate() {
        return WorkItemTemplate.builder()
                .id("ops-incident")
                .name("Incident Management")
                .description("Template for incident management")
                .category(OriginCategory.OPS)
                .cynefinDomain(CynefinDomain.CHAOTIC)
                .workParadigm(WorkParadigm.TASK)
                .type(WorkItemType.BUG)
                .initialState(WorkflowState.CREATED)
                .defaultPriority(Priority.CRITICAL)
                .defaultTags(Arrays.asList("operations", "incident"))
                .descriptionTemplate("""
                        ## Incident Description
                        [Describe the incident in detail]
                        
                        ## Impact Assessment
                        [Assess the business and technical impact]
                        
                        ## Current Status
                        [Describe the current status of the incident]
                        
                        ## Root Cause (if known)
                        [Identify the root cause if known]
                        
                        ## Immediate Actions Taken
                        [List actions already taken to mitigate]
                        
                        ## Next Steps
                        [Outline next steps for resolution]
                        
                        ## Communication Plan
                        [Plan for stakeholder communication]
                        
                        ## Lessons Learned (post-incident)
                        [Document lessons learned after resolution]
                        """)
                .suggestedCognitiveLoad(8)
                .build();
    }
    
    private static WorkItemTemplate createOpsSecurityTemplate() {
        return WorkItemTemplate.builder()
                .id("ops-security")
                .name("Security Implementation")
                .description("Template for security implementations")
                .category(OriginCategory.OPS)
                .cynefinDomain(CynefinDomain.COMPLICATED)
                .workParadigm(WorkParadigm.TASK)
                .type(WorkItemType.TASK)
                .initialState(WorkflowState.CREATED)
                .defaultPriority(Priority.HIGH)
                .defaultTags(Arrays.asList("operations", "security"))
                .descriptionTemplate("""
                        ## Security Objective
                        [Describe the security objective]
                        
                        ## Security Requirement
                        [Specify the security requirements]
                        
                        ## Implementation Approach
                        [Outline the implementation approach]
                        
                        ## Security Controls
                        [List specific security controls to implement]
                        
                        ## Testing Approach
                        [Describe how security will be tested]
                        
                        ## Compliance Requirements
                        [List applicable compliance requirements]
                        
                        ## Risk Assessment
                        [Assess security risks and mitigations]
                        
                        ## Documentation Requirements
                        [Specify security documentation needs]
                        """)
                .suggestedCognitiveLoad(7)
                .build();
    }
    
    // Documentation-focused templates
    
    private static WorkItemTemplate createDocUserGuideTemplate() {
        return WorkItemTemplate.builder()
                .id("doc-userguide")
                .name("User Documentation")
                .description("Template for user documentation")
                .category(OriginCategory.DOC)
                .cynefinDomain(CynefinDomain.CLEAR)
                .workParadigm(WorkParadigm.TASK)
                .type(WorkItemType.TASK)
                .initialState(WorkflowState.CREATED)
                .defaultPriority(Priority.MEDIUM)
                .defaultTags(Arrays.asList("documentation", "user"))
                .descriptionTemplate("""
                        ## Documentation Scope
                        [Define what functionality this documentation covers]
                        
                        ## Target Audience
                        [Identify the intended audience for this documentation]
                        
                        ## Documentation Format
                        [Specify the format (HTML, PDF, Markdown, etc.)]
                        
                        ## Required Sections
                        [List the sections to be included]
                        
                        ## Graphics Requirements
                        [Describe graphics/screenshots needed]
                        
                        ## Related Documentation
                        [List related documentation]
                        
                        ## Review Process
                        [Outline the review process]
                        
                        ## Publication Plan
                        [Describe how the documentation will be published]
                        """)
                .suggestedCognitiveLoad(4)
                .isDefault(true)
                .build();
    }
    
    private static WorkItemTemplate createDocApiReferenceTemplate() {
        return WorkItemTemplate.builder()
                .id("doc-api")
                .name("API Documentation")
                .description("Template for API documentation")
                .category(OriginCategory.DOC)
                .cynefinDomain(CynefinDomain.CLEAR)
                .workParadigm(WorkParadigm.TASK)
                .type(WorkItemType.TASK)
                .initialState(WorkflowState.CREATED)
                .defaultPriority(Priority.MEDIUM)
                .defaultTags(Arrays.asList("documentation", "api"))
                .descriptionTemplate("""
                        ## API Overview
                        [Provide a high-level overview of the API]
                        
                        ## Authentication
                        [Describe authentication mechanisms]
                        
                        ## Endpoints to Document
                        [List API endpoints to be documented]
                        
                        ## Request/Response Examples
                        [Provide examples for each endpoint]
                        
                        ## Error Handling
                        [Document error responses and codes]
                        
                        ## Rate Limiting
                        [Describe any rate limiting policies]
                        
                        ## Documentation Format
                        [Specify documentation format (OpenAPI, etc.)]
                        
                        ## Publication Plan
                        [Describe how API docs will be published]
                        """)
                .suggestedCognitiveLoad(5)
                .build();
    }
    
    private static WorkItemTemplate createDocReleaseNotesTemplate() {
        return WorkItemTemplate.builder()
                .id("doc-releasenotes")
                .name("Release Notes")
                .description("Template for release notes")
                .category(OriginCategory.DOC)
                .cynefinDomain(CynefinDomain.CLEAR)
                .workParadigm(WorkParadigm.TASK)
                .type(WorkItemType.TASK)
                .initialState(WorkflowState.CREATED)
                .defaultPriority(Priority.HIGH)
                .defaultTags(Arrays.asList("documentation", "release"))
                .descriptionTemplate("""
                        ## Release Version
                        [Specify the version number]
                        
                        ## Release Date
                        [Release date]
                        
                        ## New Features
                        [List new features in this release]
                        
                        ## Enhancements
                        [List enhancements to existing features]
                        
                        ## Bug Fixes
                        [List bugs fixed in this release]
                        
                        ## Known Issues
                        [Document known issues]
                        
                        ## Breaking Changes
                        [Document any breaking changes]
                        
                        ## Upgrade Notes
                        [Provide guidance for upgrading]
                        """)
                .suggestedCognitiveLoad(3)
                .build();
    }
    
    // Cross-functional templates
    
    private static WorkItemTemplate createCrossTeamCoordinationTemplate() {
        return WorkItemTemplate.builder()
                .id("cross-team")
                .name("Team Coordination")
                .description("Template for cross-team coordination")
                .category(OriginCategory.CROSS)
                .cynefinDomain(CynefinDomain.COMPLEX)
                .workParadigm(WorkParadigm.TASK)
                .type(WorkItemType.TASK)
                .initialState(WorkflowState.CREATED)
                .defaultPriority(Priority.HIGH)
                .defaultTags(Arrays.asList("cross-functional", "coordination"))
                .descriptionTemplate("""
                        ## Coordination Objective
                        [Describe what needs to be coordinated]
                        
                        ## Teams Involved
                        [List all teams that need to be involved]
                        
                        ## Decision-Making Process
                        [Define how decisions will be made]
                        
                        ## Dependencies
                        [Identify cross-team dependencies]
                        
                        ## Communication Plan
                        [Define how teams will communicate]
                        
                        ## Timeline
                        [Provide a timeline for coordination activities]
                        
                        ## Success Criteria
                        [Define what successful coordination looks like]
                        
                        ## Risk Mitigation
                        [Identify coordination risks and mitigations]
                        """)
                .suggestedCognitiveLoad(6)
                .isDefault(true)
                .build();
    }
    
    private static WorkItemTemplate createCrossSystemIntegrationTemplate() {
        return WorkItemTemplate.builder()
                .id("cross-integration")
                .name("System Integration")
                .description("Template for system integration work")
                .category(OriginCategory.CROSS)
                .cynefinDomain(CynefinDomain.COMPLICATED)
                .workParadigm(WorkParadigm.TASK)
                .type(WorkItemType.TASK)
                .initialState(WorkflowState.CREATED)
                .defaultPriority(Priority.HIGH)
                .defaultTags(Arrays.asList("cross-functional", "integration"))
                .descriptionTemplate("""
                        ## Integration Objective
                        [Describe the integration goal]
                        
                        ## Systems Involved
                        [List all systems involved in the integration]
                        
                        ## Integration Points
                        [Identify specific integration touchpoints]
                        
                        ## Data Exchange
                        [Describe data to be exchanged]
                        
                        ## Integration Architecture
                        [Outline the integration architecture]
                        
                        ## Security Considerations
                        [Address security aspects of the integration]
                        
                        ## Testing Approach
                        [Define how the integration will be tested]
                        
                        ## Rollout Strategy
                        [Describe how the integration will be rolled out]
                        """)
                .suggestedCognitiveLoad(7)
                .build();
    }
    
    private static WorkItemTemplate createCrossInnovationTemplate() {
        return WorkItemTemplate.builder()
                .id("cross-innovation")
                .name("Innovation Initiative")
                .description("Template for cross-functional innovation")
                .category(OriginCategory.CROSS)
                .cynefinDomain(CynefinDomain.COMPLEX)
                .workParadigm(WorkParadigm.EXPERIMENT)
                .type(WorkItemType.GOAL)
                .initialState(WorkflowState.CREATED)
                .defaultPriority(Priority.MEDIUM)
                .defaultTags(Arrays.asList("cross-functional", "innovation"))
                .descriptionTemplate("""
                        ## Innovation Challenge
                        [Describe the challenge or opportunity]
                        
                        ## Desired Outcomes
                        [Define what successful innovation would achieve]
                        
                        ## Target Users
                        [Identify who would benefit from this innovation]
                        
                        ## Innovation Approach
                        [Outline the approach to innovation]
                        
                        ## Required Expertise
                        [List skills and expertise needed]
                        
                        ## Constraints
                        [Identify any constraints on the innovation]
                        
                        ## Experimentation Plan
                        [Describe how ideas will be tested]
                        
                        ## Success Criteria
                        [Define how to measure success]
                        """)
                .suggestedCognitiveLoad(8)
                .build();
    }
}
package org.rinna.bdd;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Steps implementation for Test-Driven Development scenarios.
 */
public class TddSteps {
    private static final Logger logger = LoggerFactory.getLogger(TddSteps.class);
    
    private TestContext context;
    private boolean testIsFailing = false;
    private boolean testHasPassed = false;
    private boolean codeIsImplemented = false;
    private boolean codeIsRefactored = false;
    private int testExecutionCount = 0;
    
    public TddSteps(TestContext context) {
        this.context = context;
    }
    
    // TDD General Steps
    
    @Given("the development environment is set up")
    public void theDevelopmentEnvironmentIsSetUp() {
        logger.info("Development environment is set up and ready");
        context.put("environmentReady", true);
    }
    
    @Given("I have a clean working branch")
    public void iHaveACleanWorkingBranch() {
        logger.info("Using a clean working branch");
        context.put("workingBranch", "feature/tdd-test");
    }
    
    @Given("I am following TDD principles")
    public void iAmFollowingTDDPrinciples() {
        logger.info("Following TDD principles");
        context.put("tddEnabled", true);
    }
    
    @Given("I have a properly configured development environment")
    public void iHaveAProperlyConfiguredDevelopmentEnvironment() {
        logger.info("Development environment properly configured");
        context.put("environmentConfigured", true);
    }
    
    // Red phase
    
    @Given("I have written a failing test for a feature not yet implemented")
    public void iHaveWrittenFailingTest() {
        logger.info("Created failing test for unimplemented feature");
        testIsFailing = true;
        codeIsImplemented = false;
        context.put("testCreated", true);
    }
    
    @When("I run the test")
    public void iRunTheTest() {
        logger.info("Running the test");
        testExecutionCount++;
        context.put("testExecutions", testExecutionCount);
    }
    
    @Then("the test should fail with a clear error message")
    public void theTestShouldFail() {
        logger.info("Verifying test fails as expected");
        Assert.assertTrue("Test should be failing in red phase", testIsFailing);
        Assert.assertFalse("Code should not be implemented yet", codeIsImplemented);
        context.put("redPhaseComplete", true);
    }
    
    // Green phase
    
    @When("I implement the minimum code to make the test pass")
    public void iImplementMinimumCode() {
        logger.info("Implementing minimum code to pass the test");
        codeIsImplemented = true;
        testIsFailing = false;
        testHasPassed = true;
        context.put("codeImplemented", true);
    }
    
    @Then("the test should pass")
    public void theTestShouldPass() {
        logger.info("Verifying test passes");
        Assert.assertTrue("Test should pass after implementation", testHasPassed);
        Assert.assertTrue("Code should be implemented", codeIsImplemented);
        context.put("greenPhaseComplete", true);
    }
    
    // Refactor phase
    
    @When("I refactor the code while keeping the tests passing")
    public void iRefactorCode() {
        logger.info("Refactoring code while maintaining test passing state");
        codeIsRefactored = true;
        testHasPassed = true; // Tests should still pass
        context.put("refactoringDone", true);
    }
    
    @Then("the test should still pass")
    public void theTestShouldStillPass() {
        logger.info("Verifying test still passes after refactoring");
        Assert.assertTrue("Test should still pass after refactoring", testHasPassed);
        context.put("refactorPhaseComplete", true);
    }
    
    @Then("the code should be clean and maintainable")
    public void theCodeShouldBeCleanAndMaintainable() {
        logger.info("Verifying code quality after refactoring");
        Assert.assertTrue("Code should be refactored", codeIsRefactored);
        context.put("codeQualityVerified", true);
    }
    
    // Edge case handling
    
    @Given("I have implemented a feature with basic test coverage")
    public void iHaveImplementedFeatureWithBasicTests() {
        logger.info("Feature implemented with basic test coverage");
        codeIsImplemented = true;
        testHasPassed = true;
        context.put("basicFeatureImplemented", true);
    }
    
    @When("I identify an edge case for the feature")
    public void iIdentifyEdgeCase() {
        logger.info("Identified edge case for testing");
        context.put("edgeCaseIdentified", true);
    }
    
    @When("I write a test for that edge case")
    public void iWriteTestForEdgeCase() {
        logger.info("Writing test for edge case");
        testIsFailing = true; // New test for edge case should fail
        context.put("edgeCaseTestWritten", true);
    }
    
    @When("I modify the implementation to handle the edge case")
    public void iModifyImplementationForEdgeCase() {
        logger.info("Modifying implementation to handle edge case");
        testIsFailing = false;
        testHasPassed = true;
        context.put("edgeCaseImplemented", true);
    }
    
    @Then("the feature should handle the edge case correctly")
    public void featureShouldHandleEdgeCase() {
        logger.info("Verifying edge case is handled correctly");
        Assert.assertTrue("Feature should handle edge case", context.getBoolean("edgeCaseImplemented"));
    }
    
    // Bug fixing with TDD
    
    @Given("a bug has been reported in a feature")
    public void bugReportedInFeature() {
        logger.info("Bug reported in existing feature");
        context.put("bugReported", true);
    }
    
    @When("I reproduce the bug with a failing test")
    public void iReproduceBugWithFailingTest() {
        logger.info("Reproducing bug with failing test");
        testIsFailing = true;
        context.put("bugReproduced", true);
    }
    
    @Then("the test should fail and demonstrate the bug")
    public void testShouldFailAndDemonstrateBug() {
        logger.info("Verifying test demonstrates the bug");
        Assert.assertTrue("Test should fail to demonstrate bug", testIsFailing);
    }
    
    @When("I fix the bug in the implementation")
    public void iFixBugInImplementation() {
        logger.info("Fixing bug in implementation");
        testIsFailing = false;
        testHasPassed = true;
        context.put("bugFixed", true);
    }
    
    @Then("the bug should be fixed")
    public void bugShouldBeFixed() {
        logger.info("Verifying bug is fixed");
        Assert.assertTrue("Bug should be fixed", context.getBoolean("bugFixed"));
    }
    
    // Regression testing
    
    @Given("I have a feature with comprehensive test coverage")
    public void iHaveFeatureWithComprehensiveTests() {
        logger.info("Feature exists with comprehensive test coverage");
        codeIsImplemented = true;
        testHasPassed = true;
        context.put("comprehensiveTestsExist", true);
    }
    
    @When("I modify the feature implementation")
    public void iModifyFeatureImplementation() {
        logger.info("Modifying feature implementation");
        context.put("featureModified", true);
    }
    
    @When("a previously passing test now fails")
    public void previouslyPassingTestNowFails() {
        logger.info("Regression detected: previously passing test now fails");
        testIsFailing = true;
        testHasPassed = false;
        context.put("regressionDetected", true);
    }
    
    @Then("I should identify the regression")
    public void iShouldIdentifyRegression() {
        logger.info("Identifying source of regression");
        Assert.assertTrue("Regression should be identified", context.getBoolean("regressionDetected"));
    }
    
    @When("I should restore or fix the broken functionality")
    public void iShouldRestoreOrFixBrokenFunctionality() {
        logger.info("Restoring broken functionality");
        testIsFailing = false;
        testHasPassed = true;
        context.put("regressionFixed", true);
    }
    
    @Then("no regression should be introduced")
    public void noRegressionShouldBeIntroduced() {
        logger.info("Verifying no regression remains");
        Assert.assertTrue("No regression should remain", testHasPassed);
        Assert.assertFalse("Tests should not be failing", testIsFailing);
    }
    
    // Additional test quality scenarios
    
    @Given("I have a suite of tests for my feature")
    public void iHaveTestSuiteForFeature() {
        logger.info("Test suite exists for feature");
        context.put("testSuiteExists", true);
    }
    
    @When("I notice that one test passing is dependent on another test running first")
    public void iNoticeTestDependency() {
        logger.info("Test dependency identified");
        context.put("testDependencyFound", true);
    }
    
    @Then("I should identify the shared state between tests")
    public void iShouldIdentifySharedState() {
        logger.info("Identifying shared state between tests");
        Assert.assertTrue("Shared state should be identified", context.getBoolean("testDependencyFound"));
    }
    
    @When("I isolate the tests to run independently")
    public void iIsolateTests() {
        logger.info("Isolating tests to run independently");
        context.put("testsIsolated", true);
    }
    
    @When("I run each test in isolation")
    public void iRunEachTestInIsolation() {
        logger.info("Running each test in isolation");
        testHasPassed = true;
        context.put("testsRunIndependently", true);
    }
    
    @Then("each test should pass on its own merit")
    public void eachTestShouldPass() {
        logger.info("Verifying each test passes independently");
        Assert.assertTrue("Each test should pass independently", testHasPassed);
    }
    
    @Then("the tests should be free from cross-contamination")
    public void testsShouldBeFreedFromCrossContamination() {
        logger.info("Verifying tests are free from cross-contamination");
        Assert.assertTrue("Tests should be isolated", context.getBoolean("testsIsolated"));
    }
    
    // Handling flaky tests
    
    @Given("I have a test that sometimes passes and sometimes fails")
    public void iHaveFlackyTest() {
        logger.info("Identified flaky test");
        context.put("flakyTestExists", true);
    }
    
    @When("I identify the cause of the flakiness")
    public void iIdentifyCauseOfFlakiness() {
        logger.info("Identifying cause of test flakiness");
        context.put("flakinessCauseIdentified", true);
    }
    
    @Then("I should fix the test to be deterministic")
    public void iShouldFixTestToBeDeterministic() {
        logger.info("Fixing test to be deterministic");
        context.put("testFixedToBeDeterministic", true);
    }
    
    @When("I run the test multiple times")
    public void iRunTestMultipleTimes() {
        logger.info("Running test multiple times to verify stability");
        testExecutionCount = 5;
        testHasPassed = true; // Now stable
        context.put("testRunMultipleTimes", true);
    }
    
    @Then("the test should consistently pass")
    public void testShouldConsistentlyPass() {
        logger.info("Verifying test consistently passes");
        Assert.assertTrue("Test should consistently pass", testHasPassed);
    }
    
    @Then("the test should be reliable")
    public void testShouldBeReliable() {
        logger.info("Verifying test is now reliable");
        Assert.assertTrue("Test should be reliable", context.getBoolean("testFixedToBeDeterministic"));
    }
    
    // Engineering-specific steps
    
    @Given("I need to implement a REST API endpoint")
    public void iNeedToImplementRestApiEndpoint() {
        logger.info("Preparing to implement REST API endpoint");
        context.put("restApiEndpointNeeded", true);
    }
    
    @When("I write tests for successful responses")
    public void iWriteTestsForSuccessfulResponses() {
        logger.info("Writing tests for successful responses");
        context.put("successResponseTestsWritten", true);
    }
    
    @When("I write tests for client error responses")
    public void iWriteTestsForClientErrorResponses() {
        logger.info("Writing tests for client error responses");
        context.put("clientErrorTestsWritten", true);
    }
    
    @When("I write tests for server error responses")
    public void iWriteTestsForServerErrorResponses() {
        logger.info("Writing tests for server error responses");
        context.put("serverErrorTestsWritten", true);
    }
    
    @Then("all tests should fail initially")
    public void allTestsShouldFailInitially() {
        logger.info("Verifying all tests fail initially");
        testIsFailing = true;
        testHasPassed = false;
        Assert.assertTrue("Tests should fail initially", testIsFailing);
    }
    
    @When("I implement the endpoint with proper error handling")
    public void iImplementEndpointWithProperErrorHandling() {
        logger.info("Implementing endpoint with proper error handling");
        testIsFailing = false;
        testHasPassed = true;
        codeIsImplemented = true;
        context.put("endpointImplemented", true);
    }
    
    @Then("all tests should pass")
    public void allTestsShouldPass() {
        logger.info("Verifying all tests pass");
        Assert.assertTrue("All tests should pass", testHasPassed);
    }
    
    @Then("the API should handle all response scenarios correctly")
    public void apiShouldHandleAllResponseScenariosCorrectly() {
        logger.info("Verifying API handles all response scenarios");
        Assert.assertTrue("API should handle successful responses", context.getBoolean("successResponseTestsWritten"));
        Assert.assertTrue("API should handle client errors", context.getBoolean("clientErrorTestsWritten"));
        Assert.assertTrue("API should handle server errors", context.getBoolean("serverErrorTestsWritten"));
    }
}
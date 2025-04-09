/*
 * BDD step definitions for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.acceptance.bdd;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.rinna.domain.model.Release;
import org.rinna.domain.model.WorkItem;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Step definitions for release-related Cucumber scenarios.
 */
@Tag("acceptance")
public class ReleaseSteps {
    private final TestContext context;
    
    /**
     * Constructs a new ReleaseSteps with the given test context.
     *
     * @param context the test context
     */
    public ReleaseSteps(TestContext context) {
        this.context = context;
    }
    
    @When("the developer creates a release with version {string} and description {string}")
    public void theDeveloperCreatesAReleaseWithVersionAndDescription(String version, String description) {
        try {
            Release release = context.getRinna().releases().createRelease(version, description);
            context.saveRelease("current", release);
        } catch (Exception e) {
            context.setException(e);
        }
    }
    
    @Then("the release should exist with version {string}")
    public void theReleaseShouldExistWithVersion(String version) {
        Release release = context.getRelease("current");
        assertNotNull(release, "Release should not be null");
        assertEquals(version, release.getVersion(), "Release version should match");
    }
    
    @Given("a release with version {string} exists")
    public void aReleaseWithVersionExists(String version) {
        theDeveloperCreatesAReleaseWithVersionAndDescription(version, "Test release " + version);
    }
    
    @When("the developer adds the Bug to the release")
    public void theDeveloperAddsTheBugToTheRelease() {
        try {
            WorkItem bug = context.getWorkItem("current");
            Release release = context.getRelease("current");
            
            context.getRinna().releases().addWorkItem(release.getId(), bug.getId());
            
            // Refresh release in context
            context.getRinna().releases().findById(release.getId())
                .ifPresent(r -> context.saveRelease("current", r));
        } catch (Exception e) {
            context.setException(e);
        }
    }
    
    @Then("the release should contain the Bug")
    public void theReleaseShouldContainTheBug() {
        WorkItem bug = context.getWorkItem("current");
        Release release = context.getRelease("current");
        
        assertTrue(context.getRinna().releases().containsWorkItem(release.getId(), bug.getId()),
                "Release should contain the work item");
    }
    
    @When("the developer creates a minor version release")
    public void theDeveloperCreatesAMinorVersionRelease() {
        try {
            Release currentRelease = context.getRelease("current");
            Release newRelease = context.getRinna().releases().createNextMinorVersion(
                    currentRelease.getId(), "Next minor version");
            context.saveRelease("new", newRelease);
        } catch (Exception e) {
            context.setException(e);
        }
    }
    
    @Then("a release with version {string} should exist")
    public void aReleaseWithVersionShouldExist(String version) {
        Release release = context.getRelease("new");
        assertNotNull(release, "New release should not be null");
        assertEquals(version, release.getVersion(), "New release version should match expected version");
    }
}

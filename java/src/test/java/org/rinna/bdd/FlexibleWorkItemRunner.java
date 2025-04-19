/*
 * BDD test runner for flexible work item feature
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.bdd;

import org.junit.runner.RunWith;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;

/**
 * Cucumber test runner for flexible work item feature.
 */
@RunWith(Cucumber.class)
@CucumberOptions(
    features = "src/test/resources/features/flexible-work-items.feature",
    glue = {"org.rinna.bdd"},
    plugin = {
        "pretty",
        "html:target/cucumber-reports/flexible-work-items.html"
    },
    monochrome = true
)
public class FlexibleWorkItemRunner {
    // This class is empty as it's just a test runner
}
/*
 * BDD test runner for flexible work item feature
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.acceptance.bdd;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

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
@Tag("acceptance")
public class FlexibleWorkItemRunner {
    // This class is empty as it's just a test runner
}

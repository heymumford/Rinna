/*
 * BDD test runner for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.unit.bdd;

import static io.cucumber.junit.platform.engine.Constants.*;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

/**
 * Universal test runner for all Cucumber BDD tests.
 * This class configures the JUnit Platform to run all Cucumber feature files.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, 
        value = "pretty, json:target/cucumber-reports/cucumber-report.json, " +
                "html:target/cucumber-reports/cucumber-report.html")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "org.rinna.bdd")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "not @Disabled")
@ConfigurationParameter(key = EXECUTION_DRY_RUN_PROPERTY_NAME, value = "false")
@ConfigurationParameter(key = PARALLEL_EXECUTION_ENABLED_PROPERTY_NAME, value = "false")
@ConfigurationParameter(key = FEATURES_PROPERTY_NAME, value = "classpath:features")
@Tag("unit")
public class CucumberRunner {
    // This class is intentionally empty.
    // Its purpose is to be a holder for JUnit Platform Cucumber Suite annotations.
}

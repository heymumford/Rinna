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
 * Test runner for input interface-related Cucumber BDD tests.
 * This class configures the JUnit Platform to run Cucumber tests.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/input-interface.feature")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, 
        value = "pretty, json:target/cucumber-reports/input-interface-report.json, " +
                "html:target/cucumber-reports/input-interface-report.html")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "org.rinna.bdd")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "not @Disabled")
@Tag("unit")
public class InputInterfaceRunner {
    // This class is intentionally empty.
    // Its purpose is to be a holder for JUnit Platform Cucumber Suite annotations.
}

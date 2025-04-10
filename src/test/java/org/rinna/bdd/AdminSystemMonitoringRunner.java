/*
 * Admin System Monitoring BDD test runner
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.bdd;

import static io.cucumber.junit.platform.engine.Constants.*;

import org.junit.platform.suite.api.*;

/**
 * JUnit Platform test suite for running Admin System Monitoring BDD tests.
 * This runner specifically targets the admin system monitoring and diagnostics features.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, 
        value = "pretty, json:target/cucumber-reports/admin-system-monitoring-report.json, " +
                "html:target/cucumber-reports/admin-system-monitoring-report.html")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "org.rinna.bdd")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "(@admin and @monitoring) or (@admin and @diagnostics) and not @Disabled")
@ConfigurationParameter(key = EXECUTION_DRY_RUN_PROPERTY_NAME, value = "false")
@ConfigurationParameter(key = FEATURES_PROPERTY_NAME, 
        value = "classpath:features/admin-system-monitoring.feature")
public class AdminSystemMonitoringRunner {
    // This class is intentionally empty.
    // Its purpose is to be a holder for JUnit Platform Cucumber Suite annotations.
}
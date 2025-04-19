/*
 * BDD test runner for all Admin-related features
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.bdd;

import static io.cucumber.junit.platform.engine.Constants.*;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

/**
 * Test runner for all Admin-related feature tests.
 * This runner targets all three admin feature files.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, 
        value = "pretty, json:target/cucumber-reports/admin-features-report.json, " +
                "html:target/cucumber-reports/admin-features-report.html")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "org.rinna.bdd")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "not @Disabled")
@ConfigurationParameter(key = EXECUTION_DRY_RUN_PROPERTY_NAME, value = "false")
@ConfigurationParameter(key = FEATURES_PROPERTY_NAME, 
        value = "classpath:features/admin-user-management.feature, " +
                "classpath:features/admin-maven-integration.feature, " +
                "classpath:features/admin-server-autolaunch.feature, " +
                "classpath:features/admin-workitem-configuration.feature, " +
                "classpath:features/admin-workflow-configuration.feature, " +
                "classpath:features/admin-project-management.feature, " +
                "classpath:features/admin-audit-compliance.feature, " +
                "classpath:features/admin-system-monitoring.feature, " +
                "classpath:features/admin-backup-recovery.feature")
public class AdminFeaturesRunner {
    // This class is intentionally empty.
    // Its purpose is to be a holder for JUnit Platform Cucumber Suite annotations.
}
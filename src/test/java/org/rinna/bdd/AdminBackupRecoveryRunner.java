/*
 * Admin Backup and Recovery BDD test runner
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.bdd;

import org.junit.platform.suite.api.*;

import static io.cucumber.junit.platform.engine.Constants.*;

/**
 * JUnit Platform test suite for running Admin Backup and Recovery BDD tests.
 * This runner specifically targets the admin backup and recovery features.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, 
        value = "pretty, json:target/cucumber-reports/admin-backup-recovery-report.json, " +
                "html:target/cucumber-reports/admin-backup-recovery-report.html")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "org.rinna.bdd")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "(@admin and @backup) or (@admin and @recovery) and not @Disabled")
@ConfigurationParameter(key = EXECUTION_DRY_RUN_PROPERTY_NAME, value = "false")
@ConfigurationParameter(key = FEATURES_PROPERTY_NAME, 
        value = "classpath:features/admin-backup-recovery.feature")
public class AdminBackupRecoveryRunner {
    // This class is intentionally empty.
    // Its purpose is to be a holder for JUnit Platform Cucumber Suite annotations.
}
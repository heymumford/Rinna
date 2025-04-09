package org.rinna.cli.bdd;

import io.cucumber.junit.platform.engine.Cucumber;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.ConfigurationParameter;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;

/**
 * Test runner for the admin backup command BDD feature.
 */
@Cucumber
@IncludeEngines("cucumber")
@SelectClasspathResource("features/admin-backup-commands.feature")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "org.rinna.cli.bdd")
public class AdminBackupCommandRunner {
    // The runner doesn't need any implementation - configuration is done via annotations
}
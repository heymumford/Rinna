package org.rinna.cli.bdd;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;

import io.cucumber.junit.platform.engine.Cucumber;

/**
 * Test runner for the admin audit command BDD feature.
 */
@Cucumber
@IncludeEngines("cucumber")
@SelectClasspathResource("features/admin-audit-commands.feature")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "org.rinna.cli.bdd")
public class AdminAuditCommandRunner {
    // The runner doesn't need any implementation - configuration is done via annotations
}
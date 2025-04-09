package org.rinna.cli.bdd;

import io.cucumber.junit.platform.engine.Cucumber;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeTags;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.FILTER_TAGS_PROPERTY_NAME;

/**
 * Tagged test runner for admin command features.
 * This allows running admin command features by tags.
 */
@Cucumber
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "org.rinna.cli.bdd")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "@admin")
public class TaggedAdminCommandRunner {
    // The runner doesn't need any implementation - configuration is done via annotations
}
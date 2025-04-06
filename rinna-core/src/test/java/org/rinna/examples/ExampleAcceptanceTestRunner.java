package org.rinna.examples;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;
import org.junit.jupiter.api.Tag;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.FILTER_TAGS_PROPERTY_NAME;

/**
 * Runner for example acceptance tests using Cucumber.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/examples")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "org.rinna.examples")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "@acceptance")
@Tag("acceptance")
@Tag("bdd")
public class ExampleAcceptanceTestRunner {
    // No implementation needed - this class just serves as a runner
}
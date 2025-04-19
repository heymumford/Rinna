/*
 * Runner for CLI abbreviation BDD tests
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.acceptance.bdd;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import io.cucumber.junit.platform.engine.Constants;

/**
 * JUnit Platform test runner for CLI abbreviation tests.
 * Uses Cucumber to run feature files for CLI abbreviation functionality.
 */
@Suite
@IncludeEngines("cucumber")
@ConfigurationParameter(key = Constants.GLUE_PROPERTY_NAME, value = "org.rinna.bdd")
@SelectClasspathResource("features/cli-abbreviations.feature")
@Tag("acceptance")
public class CLIAbbreviationsRunner {
    // This class serves as a test runner and doesn't need implementation
}

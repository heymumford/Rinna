/*
 * Runner for tagged CLI BDD tests
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.bdd;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import io.cucumber.junit.platform.engine.Constants;

/**
 * JUnit Platform test runner for CLI tests matching specific tags.
 * This allows running specific subsets of tests by tag combination.
 * 
 * Usage: 
 * - To run all CLI tests: mvn test -Dcucumber.filter.tags="@cli"
 * - To run smoke tests: mvn test -Dcucumber.filter.tags="@smoke"
 * - To run combination: mvn test -Dcucumber.filter.tags="@cli and @smoke"
 * - To exclude negative tests: mvn test -Dcucumber.filter.tags="@cli and not @negative"
 */
@Suite
@IncludeEngines("cucumber")
@ConfigurationParameter(key = Constants.GLUE_PROPERTY_NAME, value = "org.rinna.bdd")
@SelectClasspathResource("features")
@IncludeTags("cli")
public class TaggedCLIRunner {
    // This class serves as a test runner and doesn't need implementation
    
    // Override the included tags at runtime with:
    // mvn test -Dcucumber.filter.tags="YOUR_TAG_EXPRESSION"
}
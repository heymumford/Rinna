package org.rinna.cli.polyglot;

import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Test suite for all polyglot integration tests.
 * This suite runs all tests related to cross-language integration.
 */
@Suite
@SuiteDisplayName("Polyglot Integration Test Suite")
@SelectPackages("org.rinna.cli.polyglot")
@IncludeTags("polyglot")
public class PolyglotTestSuite {
    // This class serves as a test suite marker and does not contain any test methods itself.
}
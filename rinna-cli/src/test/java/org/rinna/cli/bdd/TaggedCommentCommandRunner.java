/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.bdd;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

/**
 * Tagged runner for comment-commands.feature BDD tests,
 * allowing selective execution of tests with specific tags.
 */
@RunWith(Cucumber.class)
@CucumberOptions(
    features = "classpath:features/comment-commands.feature",
    glue = {"org.rinna.cli.bdd"},
    plugin = {"pretty", "html:target/cucumber-reports/comment-commands.html", 
              "json:target/cucumber-reports/comment-commands.json"},
    tags = "@tag:comment"
)
public class TaggedCommentCommandRunner {
    // This class is intentionally empty. It's just a runner.
}
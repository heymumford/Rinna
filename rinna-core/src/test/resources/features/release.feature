@Release
Feature: Release management
  To manage software releases clearly and transparently
  As a software engineering team
  We need explicit release versioning and management

  @Release
  Scenario: Creating a new release
    Given the Rinna system is initialized
    When the developer creates a release with version "1.0.0" and description "Initial Release"
    Then the release should exist with version "1.0.0"

  @Release
  Scenario: Adding items to a release
    Given the Rinna system is initialized
    And a Bug titled "Login fails" exists
    And a release with version "1.0.0" exists
    When the developer adds the Bug to the release
    Then the release should contain the Bug

  @Release
  Scenario: Creating next version release
    Given the Rinna system is initialized
    And a release with version "1.0.0" exists
    When the developer creates a minor version release
    Then a release with version "1.1.0" should exist
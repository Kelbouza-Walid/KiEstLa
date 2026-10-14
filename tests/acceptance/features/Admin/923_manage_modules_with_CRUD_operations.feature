# language: en
Feature: manage modules with CRUD operations
  As a secretary, I want to be able to create, view, update and delete modules
  so that I can keep the list of modules accurate

  Background: I have already logged in

  Scenario: lookup modules
    Given Modules exist in the system
    When I search for a module by its name
    Then The matching modules are displayed

  @implemented
  Scenario: add modules
    Given The module information is valid
    When I enter the module name
    And I select a reference teacher
    And I confirm the creation
    Then The module is registered in the system

  Scenario: update modules
    Given The module exists
    When I edit the module's information
    And I confirm the update
    Then The module's information is updated in the system

  @implemented
  Scenario: remove modules
    Given The module exists
    When I select the module to remove
    And I confirm the deletion
    Then The module is no longer registered in the system
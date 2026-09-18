# language: en
Feature: add/remove teachers and students to/from modules
  As a secretary, I want to be able to add or remove teachers and students from a module
  so that the module's participants stay up to date

  Background: I have already logged in

  Scenario: update module to add teachers and students
    Given The module exists
    And The teachers and students to add exist
    When I select the teachers to add to the module
    And I select the students to add to the module
    And I confirm the update
    Then The teachers and students are registered as members of the module

  Scenario: update module to remove teachers and students
    Given The module exists
    And The teachers and students to remove are currently members of the module
    When I select the teachers to remove from the module
    And I select the students to remove from the module
    And I confirm the update
    Then The teachers and students are no longer registered as members of the module
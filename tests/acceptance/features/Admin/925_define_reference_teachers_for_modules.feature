# language: en
Feature: define reference teachers for modules
  As a secretary, I want to be able to set a reference teacher for a module
  so that there is a clearly identified contact for that module

  Background: I have already logged in

  @implemented
  Scenario: set a reference teacher for modules
    Given The module exists
    When I select the teacher as the reference teacher for the module
    And I confirm the update
    Then The teacher is registered as the reference teacher of the module
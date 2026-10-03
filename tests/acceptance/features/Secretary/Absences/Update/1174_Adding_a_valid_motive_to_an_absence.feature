@implemented
# language: en
Feature: Adding a motive to an existing absence
As a secretary, I want to be able to edit an already existing absence to add a motive that was given to me by a student

Background:
  Given I have already logged in

Scenario: Adding a valid motive to an absence
  Given The student motive is valid
  When I enter the motive for the absence
  And I add a corresponding description
  And I set the absence as justified
  Then The absence is registered as justified

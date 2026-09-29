@implemented
# language: en
Feature: Viewing a module with no registered absences
As a secretary, I want to see an empty absence list when a module has no absences so that I know there is nothing to review

Background:
  Given I have already logged in

Scenario: Module without any registered absences
  Given The module has no absences registered
  When I open the absence list for that module
  Then The list is empty
@implemented
# language: en
Feature: Displaying an empty follow-up list
As a secretary, I want to see an empty list when no student meets the unjustified absence threshold so that I know there is nothing to follow up on

Background:
  Given I have already logged in

Scenario: No student has 3 or more unjustified absences
  Given No student in the module has 3 or more unjustified absences
  When I open the list of students with 3 or more unjustified absences for that module
  Then The list is empty
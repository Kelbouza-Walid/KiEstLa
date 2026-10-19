@implemented
# language: en
Feature: Listing students with recurring unjustified absences, sorted by module
As a secretary, I want to list students with 3 or more unjustified absences, sorted by module, so that I can identify students needing follow-up across modules

Background:
  Given I have already logged in

Scenario: Listing students with 3 or more unjustified absences sorted by module
  Given One or more students have 3 or more unjustified absences in a module
  When I open the list of students with 3 or more unjustified absences
  And I sort the list by module
  Then I see all students with 3 or more unjustified absences ordered by module
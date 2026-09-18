# language: en
Feature: Listing students with recurring unjustified absences, sorted by student
As a secretary, I want to list students with 3 or more unjustified absences per module, sorted by student, so that I can identify students needing follow-up

Background: I have already logged in

Scenario: Listing students with 3 or more unjustified absences sorted by student
  Given One or more students have 3 or more unjustified absences in the module
  When I open the list of students with 3 or more unjustified absences for that module
  And I sort the list by student
  Then I see all students with 3 or more unjustified absences ordered by student
# language: en
Feature: Viewing absences sorted by module
As a secretary, I want to view absences sorted by module so that I can check absences across modules easily

Background: I have already logged in

Scenario: Viewing absences sorted by module
  Given Several modules have students with registered absences
  When I open the absence list
  And I sort the list by module
  Then I see all absences ordered by module
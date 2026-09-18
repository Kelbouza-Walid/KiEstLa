# language: en
Feature: Viewing module absences sorted by student
As a secretary, I want to view the absences of a module sorted by student so that I can check absences per student easily

Background: I have already logged in

Scenario: Viewing module absences sorted by student
  Given The module has multiple students with registered absences
  When I open the absence list for that module
  And I sort the list by student
  Then I see all absences of the module ordered by student
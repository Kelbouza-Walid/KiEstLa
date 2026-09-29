# language: en
Feature: Viewing the absences of a student in a module
As a secretary, I want to view all the absences of a specific student in a module so that I can check them individually

Background:
    Given I have already logged in

Scenario: Viewing all absences of a student in a module
  Given The student has multiple absences registered in the module
  When I open the absence list for that student in that module
  Then I see all of his absences listed individually
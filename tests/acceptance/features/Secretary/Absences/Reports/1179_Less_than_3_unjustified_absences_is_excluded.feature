@implemented
# language: en
Feature: Excluding students below the unjustified absence threshold
As a secretary, I want students with fewer than 3 unjustified absences to be excluded from the follow-up list so that I only see students who actually need follow-up

Background:
  Given I have already logged in

Scenario: Student with less than 3 unjustified absences is not listed
  Given A student has less than 3 unjustified absences in the module
  When I open the list of students with 3 or more unjustified absences for that module
  Then That student does not appear in the list
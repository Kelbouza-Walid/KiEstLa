@implemented
# language: en
Feature: Counting only unjustified absences toward the threshold
As a secretary, I want only unjustified absences to count toward the 3+ threshold so that justified absences do not unfairly flag a student

Background:
  Given I have already logged in

Scenario: Student with mixed justified and unjustified absences
  Given A student has 2 unjustified absences and 2 justified absences in the module
  When I open the list of students with 3 or more unjustified absences for that module
  Then That student does not appear in the list
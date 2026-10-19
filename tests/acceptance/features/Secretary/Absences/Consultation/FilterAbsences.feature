@implemented
# language: en
Feature: Filtering the absence list
  As a secretary
  I want to filter the absence list by module or by student
  So that I only see the absences I am interested in

  Background:
    Given I have already logged in
    And Several students have absences in several modules

  Scenario: Filtering absences by module
    When I filter the absence list by module "Test fonctionnel"
    Then I only see absences from module "Test fonctionnel"

  Scenario: Filtering absences by student
    When I filter the absence list by student "noam"
    Then I only see absences of student "Noam Faivre"

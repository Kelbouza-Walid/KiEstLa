# language: en
@implemented
Feature: Student views own absences
  As a student
  I want to view my own absences
  So that I can track my attendance

  Background:
    Given I am logged in as a student with username "student1"
    And I am enrolled in module "M1" and module "M2"
    And I have absences recorded in both modules

  Scenario: View own absences sorted by module
    When I navigate to my absences page
    And I select "Sort by module" option
    Then I should see a list of my absences
    And the absences should be sorted alphabetically by module name

  Scenario: View own absences with no absences recorded
    Given I have no absences recorded
    When I navigate to my absences page
    Then I should see a message "No absences found"

  Scenario: View own absences filtered by specific module
    When I navigate to my absences page
    And I filter by module "M1"
    Then I should only see absences from module "M1"
# language: en
Feature: Student views absences status
  As a student
  I want to view the status of my absences
  So that I can see which are justified or not

  Background:
    Given I am logged in as a student with username "student1"
    And I have an absence in module "M1" with status "justified"
    And I have an absence in module "M2" with status "unjustified"

  Scenario: View absences' status sorted by module
    When I navigate to my absences status page
    And I select "Sort by module" option
    Then I should see my absences sorted by module name
    And each absence should display its status ("justified" or "unjustified")

  Scenario: View absences' status with mixed justifications
    When I navigate to my absences status page
    Then I should see the absence in "M1" marked as "justified"
    And I should see the absence in "M2" marked as "unjustified"
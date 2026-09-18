# language: en
Feature: Student views reference teachers of modules
  As a student
  I want to view the reference teachers of my modules
  So that I know who to contact

  Background:
    Given I am logged in as a student with username "student1"
    And I am enrolled in module "M1" with reference teacher "teacherA"
    And I am enrolled in module "M2" with reference teacher "teacherB"

  Scenario: View reference teachers for all my modules
    When I navigate to my modules page
    Then I should see module "M1" with reference teacher "teacherA"
    And I should see module "M2" with reference teacher "teacherB"

  Scenario: View reference teachers with no modules enrolled
    Given I am not enrolled in any modules
    When I navigate to my modules page
    Then I should see a message "You are not enrolled in any modules"
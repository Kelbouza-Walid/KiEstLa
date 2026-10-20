@implemented
# language: en
Feature: Commenting on the absences of my modules
  As a teacher
  I want to comment on the absences of the modules I am in
  So that I can communicate with the secretary about them

  Background:
    Given I am logged in as the teacher "Fabrice Bouquet"
    And "Gaspard Quentin" is absent in my module "Test fonctionnel"

  Scenario: Adding a comment from my list of absences
    When I open the comments of the absence from my list of absences
    And I send the comment "Was the student sick?"
    Then The comment "Was the student sick?" written by "Fabrice Bouquet" is displayed
    And The absence has 1 comment saved

  Scenario: Adding a comment from the details of my module
    When I open the comments of the absence from the details of my module
    And I send the comment "Was the student sick?"
    Then The comment "Was the student sick?" written by "Fabrice Bouquet" is displayed
    And The absence has 1 comment saved

  Scenario: Editing my own comment
    Given I have already commented "Was the student sick?" on the absence
    When I open the comments of the absence from my list of absences
    And I change my comment to "Was the student sick on Monday?"
    Then The comment "Was the student sick on Monday?" written by "Fabrice Bouquet" is displayed
    And The absence has 1 comment saved

  Scenario: Attempting to send an empty comment
    When I open the comments of the absence from my list of absences
    And I send the comment "   "
    Then The absence has 0 comment saved
    And I am told that the comment cannot be empty

  Scenario: Reading a comment written by the secretary
    Given The secretary "Sarah Connor" has commented "The parents called, the student was sick" on the absence
    When I open the comments of the absence from my list of absences
    Then The comment "The parents called, the student was sick" written by "Sarah Connor" is displayed
    And I cannot edit or delete that comment

  Scenario: The secretary reads my comment
    Given I have already commented "Was the student sick?" on the absence
    When The secretary "Sarah Connor" opens the comments of the absence
    Then The comment "Was the student sick?" written by "Fabrice Bouquet" is displayed
    And I cannot edit or delete that comment

  Scenario: Accessing the comments of an absence outside my modules
    Given "Noam Faivre" is absent in the module "Informatique Responsable" I am not in
    When I try to load the comments of that absence
    Then I am not allowed to read them

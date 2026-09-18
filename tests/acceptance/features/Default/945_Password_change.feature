# language: en
Feature: Password change
  As a user
  I want to change my password

Background:
  Given I was already registered in the database and I am authenticated

Scenario: A successful password change
  When I enter my new password 
  And I click a button to confirm
  Then I should get a message notifying me of the successful operation

Scenario: New password is identical to old password
  When I enter my new password 
  And I click a button to confirm
  Then I should see a message indicating that the two passwords are identical
  And I get to retry

Scenario: Password change failed
  When I enter my new password 
  And I click a button to confirm
  Then I should see a message indicating what the error was
  And I get to retry
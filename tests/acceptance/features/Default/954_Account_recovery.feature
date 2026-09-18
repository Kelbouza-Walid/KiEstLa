# language: en
Feature: Account recovery
  As a user
  I want to recover my account

Background:
  Given I was already registered in the database and I am not authenticated

Scenario: A successful account recovery
  When I enter my email address
  And I click a button to confirm
  Then I should get an email that let's me change my password
  And recover my account

Scenario: Email doesn't exist in the database
  When I enter my email address
  And I click a button to confirm
  Then I should get a message indicating that the email is wrong
  And I get to retry
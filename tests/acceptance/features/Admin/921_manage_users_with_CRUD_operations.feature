# language: en
Feature: manage users with CRUD operations
  As an administrator, I want to be able to create, view, update and delete users
  so that I can keep the list of users accurate

  Background: I have already logged in

  Scenario: lookup users
    Given Users exist in the system
    When I search for a user by name or role
    Then The matching users are displayed

  @implemented
  Scenario: add users
    Given The user information is valid
    When I enter the user's personal information
    And I set the user's role
    And I confirm the creation
    Then The user is registered in the system

  @implemented
  Scenario: new user logs in with the generated password
    Given A new user has been created by the administrator
    When The new user logs in with the generated password
    Then The new user is redirected to their home page

  @implemented
  Scenario: update users
    Given The user exists
    When I edit the user's information
    And I confirm the update
    Then The user's information is updated in the system

  Scenario: remove users
    Given The user exists
    When I select the user to remove
    And I confirm the deletion
    Then The user is no longer registered in the system
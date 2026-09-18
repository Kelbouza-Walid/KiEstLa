# language: en
Feature: Logout
  As a user
  I want to logout

Background:
  Given that I am logged in

Scenario: Logging out
  When I click a logout button
  Then I shouldn't stay identified
  And get my access revoked
  And should be redirected to the login page

Scenario: Logout failed
  When I click the logout button
  Then I should see a message indicating what the error is
  And I get to retry

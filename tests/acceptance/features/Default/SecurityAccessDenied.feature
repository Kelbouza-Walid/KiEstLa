@implemented
# language: en
Feature: Role-based access control and 403 error page
  As an authenticated user
  I want to be forbidden from accessing restricted pages of other roles
  So that application security is maintained and a 403 access denied page is shown

  Scenario: A secretary is forbidden from accessing teacher dashboard
    Given I am logged in as a secretary
    When I attempt to navigate to "/teacher"
    Then I should see the 403 access denied page

  Scenario: A teacher is forbidden from accessing secretary dashboard
    Given I am logged in as a teacher
    When I attempt to navigate to "/secretary"
    Then I should see the 403 access denied page

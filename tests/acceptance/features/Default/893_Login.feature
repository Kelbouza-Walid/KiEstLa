# language: en
Feature: Login
  As a user
  I want to enter my credentials
  So that I get authenticated and have access to the application features

Background:
  Given I was already registered in the database

@implemented
Scenario: A valid identification/login
  When I enter my details
  Then I should gain access to the features I am allowed
  And should be redirected to the right page

@implemented
Scenario: Invalid credentials
  When I enter wrong credentials
  Then I should see a message indicating what/where the error is
  And I get to retry
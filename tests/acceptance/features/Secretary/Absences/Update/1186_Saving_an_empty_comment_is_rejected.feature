# language: en
Feature: Requiring content when adding a comment
As a secretary, I want the system to reject empty comments so that only meaningful comments are recorded

Background: I have already logged in

Scenario: Attempting to save an empty comment
  Given The absence has no comment from me
  When I try to save an empty comment
  Then The comment is not saved
  And I see an error message indicating the comment cannot be empty
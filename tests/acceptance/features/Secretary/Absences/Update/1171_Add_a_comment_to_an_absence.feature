@implemented
# language: en
Feature: Adding a comment to an absence
As a secretary, I want to add a comment to an absence so that I can document additional context about it

Background:
  Given I have already logged in

Scenario: Adding a comment to an absence
  Given The absence has no comment from me
  When I write a comment
  And I save the comment
  Then The comment is displayed on the absence
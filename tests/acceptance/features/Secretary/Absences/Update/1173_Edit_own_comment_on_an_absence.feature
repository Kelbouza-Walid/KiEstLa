# language: en
Feature: Editing my own comment on an absence
As a secretary, I want to edit my own comment on an absence so that I can correct or update it

Background: I have already logged in

Scenario: Editing my own comment
  Given The absence has a comment I previously wrote
  When I edit my comment
  And I save the changes
  Then The updated comment is displayed on the absence
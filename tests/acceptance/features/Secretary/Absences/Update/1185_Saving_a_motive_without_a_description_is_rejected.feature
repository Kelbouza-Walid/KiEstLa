# language: en
Feature: Requiring a description when adding a motive
As a secretary, I want the system to require a description when I add a motive so that every justification decision is documented

Background: I have already logged in

Scenario: Attempting to save a motive without a description
  Given The student motive is valid
  When I enter the motive for the absence
  And I leave the description empty
  And I try to save the absence
  Then The absence is not saved
  And I see an error message indicating the description is required
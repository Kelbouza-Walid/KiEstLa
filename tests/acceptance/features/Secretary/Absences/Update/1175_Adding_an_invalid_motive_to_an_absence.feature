# language: en
Feature: Adding a motive to an existing absence
As a secretary, I want to be able to edit an already existing absence to add a motive that was given to me by a student

Background: I have already logged in

Scenario: Adding an invalid motive to an absence
  Given The student motive is invalid
  When I enter the motive for the absence
  And I add a corresponding description
  And I set the absence as unjustified
  Then The absence is registered as unjustified
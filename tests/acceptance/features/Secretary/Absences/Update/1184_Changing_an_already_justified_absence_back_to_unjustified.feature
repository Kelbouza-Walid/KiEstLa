# language: en
Feature: Reassessing an already justified absence
As a secretary, I want to be able to change the status of an already justified absence if a new motive is provided so that the record reflects the most accurate information

Background: I have already logged in

Scenario: Changing a justified absence to unjustified
  Given The absence is currently justified
  When I enter a new invalid motive for the absence
  And I add a corresponding description
  And I set the absence as unjustified
  Then The absence is registered as unjustified
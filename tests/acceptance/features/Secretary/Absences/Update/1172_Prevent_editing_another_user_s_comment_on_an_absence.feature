# language: en
Feature: Restricting comment edition to its author
As a secretary, I want to be prevented from editing another user's comment so that comment ownership is respected

Background: I have already logged in

Scenario: Attempting to edit another user's comment
  Given The absence has a comment written by another user
  When I try to edit that comment
  Then I am not allowed to modify it
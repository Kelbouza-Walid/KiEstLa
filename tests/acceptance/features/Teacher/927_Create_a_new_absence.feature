# language: en
Feature: Create a new absence
  As a teacher, I expect to be able to add a new absence when a student is not present in my class, and eventually add a comment to clarify the situation
  
Background: I have already logged in
  
Scenario: Adding a new absence without a comment
  Given The absent student is registered in the module I teach
  When I select the absent student
  And I define in which class he was absent
  Then A new absence is created and attributed to that student
  
Scenario: Adding a new absence with a comment
  Given The absent student is registered in the module I teach
  When I select the absent student
  And I define in which class he was absent
  And I add a comment
  Then A new absence (with a comment) is created and attributed to that student
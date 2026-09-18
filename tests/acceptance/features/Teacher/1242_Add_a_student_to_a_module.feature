# language: en
Feature: Add a student to a module
  As a reference teacher, I want to be able to add a student to a class I'm a reference for
  
Background: I am a reference teacher
  And I am logged in
  
Scenario: Adding a new student
  Given The student exists in the database
  When I select the student
  Then They are added to the list of students in my module
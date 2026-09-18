# language: en
Feature: Add a teacher to a module
  As a reference teacher, I want to be able to add another teacher to a class I'm a reference for
  
Background: I am a reference teacher
  And I am logged in
  
Scenario: Adding a new teacher
  Given The teacher exists in the database
  When I select the teacher
  Then They are added to the list of teachers in my module
  And The teacher can now see all absences of the student of my module
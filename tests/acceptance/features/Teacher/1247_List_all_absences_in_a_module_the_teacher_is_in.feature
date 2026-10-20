# language: en
Feature: List all absences in a module the teacher is in
  As a teacher I want to be able to see all the absences for modules I'm a part of
  
Background: I am a reference teacher
  And I am logged in
  
@implemented
Scenario: Sorting absences by modules
  Given I am in several different modules
  When I open the list of all the absences
  And I select "Sort by module"
  Then I should see the list of absences sorted by module
  And I should not see the absences of students who are not in my modules
  
@implemented
Scenario: Sorting absences by student
  Given I am in several different modules
  When I open the list of all the absences
  And I select "Sort by student"
  Then I should see the list of absences sorted by student
  And I should not see the absences of students who are not in my modules
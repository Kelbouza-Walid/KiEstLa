# language: en
Feature: See which students have 3 or more absences
  As a teacher I want to have access to a page that show which
  students have more than 3 unjustified absences
  
Background: I am logged in
  
@implemented
Scenario: Sorting absences by modules
  When I open the list of absenteist students
  And I select "Sort by module"
  Then I should see the list of absences sorted by module

@implemented
Scenario: Sorting absences by student
  When I open the list of absenteist students
  And I select "Sort by student"
  Then I should see the list of absenteit students
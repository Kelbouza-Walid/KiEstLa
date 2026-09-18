Feature: Notify teachers and secretaries when a student reaches 3 unjustified absences in a module
  As a member of the teaching or administrative staff
  I want to be notified when a student accumulates 3 or more unjustified absences in a module
  So that I can take the appropriate action

  Background:
    Given the notification threshold is set to 3 unjustified absences per module
    And the student "student1" is enrolled in module "M1 - Functional Testing"
    And the teachers "JMH" and "Mario" are assigned to module "M1 - Functional Testing"
    And "Sophie Bernard" is a secretary

  @happy-path
  Scenario: Notification is sent when the 3rd unjustified absence is recorded
    Given "student1" has 2 unjustified absences in module "M1 - Functional Testing"
    When a teacher records a 3rd unjustified absence for "student1" in module "M1 - Functional Testing"
    Then a notification is sent to every teacher assigned to module "M1 - Functional Testing"
    And a notification is sent to every secretary
    And the notification contains the name of "student1"
    And the notification contains the module "M1 - Functional Testing"
    And the notification contains the unjustified absence count "3"

  @happy-path
  Scenario: Only teachers of the concerned module are notified
    Given "Alan Smithee" is a teacher assigned only to module "M2 - Databases"
    And "student1" has 2 unjustified absences in module "M1 - Functional Testing"
    When a teacher records a 3rd unjustified absence for "student1" in module "M1 - Functional Testing"
    Then a notification is sent to "JMH"
    And a notification is sent to "Mario"
    And a notification is sent to the secretary "Sophie Bernard"
    But no notification is sent to "Alan Smithee"

  @negative
  Scenario: No notification below the threshold
    Given "student1" has 1 unjustified absence in module "M1 - Functional Testing"
    When a teacher records a 2nd unjustified absence for "student1" in module "M1 - Functional Testing"
    Then no notification is sent to any teacher
    And no notification is sent to any secretary

  @edge-case
  Scenario: Justified absences do not count towards the threshold
    Given "student1" has 3 justified absences and 0 unjustified absences in module "M1 - Functional Testing"
    When a teacher records a 4th justified absence for "student1" in module "M1 - Functional Testing"
    Then no notification is sent to any teacher
    And no notification is sent to any secretary

  @edge-case
  Scenario: The unjustified absence counter is computed per module
    Given "student1" has 2 unjustified absences in module "M1 - Functional Testing"
    And "student1" has 2 unjustified absences in module "M2 - Databases"
    When a teacher records a 3rd unjustified absence for "student1" in module "M1 - Functional Testing"
    Then a notification is sent to the teachers assigned to module "M1 - Functional Testing"
    And no notification is sent to the teachers assigned only to module "M2 - Databases"
    And the notification contains the unjustified absence count "3" for module "M1 - Functional Testing"

  @edge-case
  Scenario: Justifying an absence removes it from the counter
    Given "student1" has 3 unjustified absences in module "M1 - Functional Testing"
    And a notification has already been sent
    When the secretary justifies one of these absences
    Then "student1" has 2 unjustified absences in module "M1 - Functional Testing"
    And no additional notification is sent

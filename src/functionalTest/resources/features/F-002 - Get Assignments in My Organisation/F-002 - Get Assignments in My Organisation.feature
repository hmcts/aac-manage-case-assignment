@F-002
Feature: F-002: Get Assignments in My Organisation

  Background:
    Given an appropriate test context as detailed in the test data source

  # ACA-48 / AC-1
  @S-008
  Scenario: Must return case assignments in my organisation for the provided Case IDs
    Given a user [Richard – a solicitor with the required permissions to create a case],
    And a case [by Richard to create a case - C1] created as in [F-002_Prerequisite_Case_Creation_C1],
    And a case [by Richard to create another case – C2] created as in [F-002_Prerequisite_Case_Creation_C2],
    And a user [Dil – another solicitor within the same organisation as Richard],
    And a successful call [to the ‘Assign Access within Organisation’ operation of Case Assignment Microservice assigning Dil access to C1] as in [Prerequisite_Case_Assignment_C1_Dil],
    And a user [Jamal – another solicitor within the same organisation as Richard],
    And a successful call [to the ‘Assign Access within Organisation’ operation of Case Assignment Microservice assigning Jamal access to C2] as in [Prerequisite_Case_Assignment_C2_Jamal],
    When a request is prepared with appropriate values,
    And the request [is made by Richard],
    And the request [contains a correctly-formed comma separated list of the valid case ID’s of C1 and C2],
    And it is submitted to call the [Get Assignments in My Organisation] operation of [Manage Case Assignment Microservice],
    Then a positive response is received,
    And the response [includes the respective case assignments of user ID's of both Dil and Jamal and cases C1 and C2],
    And the response has all the details as expected.

  # ACA-48 / AC-2
  @S-009
  Scenario: Must return an error response when a malformed case ID is provided
    Given a user [Richard – a solicitor with the required permissions to create a case],
    And a case [by Richard to create a case - C1] created as in [F-002_Prerequisite_Case_Creation_C1],
    And a case [by Richard to create another case – C2] created as in [F-002_Prerequisite_Case_Creation_C2],
    And a user [Dil – another solicitor within the same organisation as Richard],
    And a successful call [to the ‘Assign Access within Organisation’ operation of Case Assignment Microservice assigning Dil access to C1] as in [Prerequisite_Case_Assignment_C1_Dil],
    And a user [Jamal – another solicitor within the same organisation as Richard],
    And a successful call [to the ‘Assign Access within Organisation’ operation of Case Assignment Microservice assigning Jamal access to C2] as in [Prerequisite_Case_Assignment_C2_Jamal],
    When a request is prepared with appropriate values,
    And the request [is made by Richard],
    And the request [contains a correctly-formed comma separated list of the case ID’s which includes the valid case IDs of C1 and C2 plus another case ID which is malformed],
    And it is submitted to call the [Get Assignments in My Organisation] operation of [Manage Case Assignment Microservice],
    Then a negative response is received,
    And the response has all the details as expected.

  # ACA-48 / AC-3
  @S-010
  Scenario: Must return an error response for a missing case ID
    Given a user [Richard – with a solicitor role with access to invoke the ‘Get Assignments in My Organisation’ operation of Case Assignment Microservice],
    When a request is prepared with appropriate values,
    And the request [is made by Richard],
    And the request [does not contain any case ID],
    And it is submitted to call the [Get Assignments in My Organisation] operation of [Manage Case Assignment Microservice],
    Then a negative response is received,
    And the response has all the details as expected.

  # ACA-48 / AC-4
  @S-011
  Scenario: Must return an error response for a malformed Case ID List
    Given a user [Richard – a solicitor with the required permissions to create a case],
    And a case [by Richard to create a case - C1] created as in [F-002_Prerequisite_Case_Creation_C1],
    And a case [by Richard to create another case – C2] created as in [F-002_Prerequisite_Case_Creation_C2],
    And a case [by Richard to create another case – C3] created as in [F-002_Prerequisite_Case_Creation_C3],
    And a user [Dil – another solicitor within the same organisation as Richard],
    And a successful call [to the ‘Assign Access within Organisation’ operation of Case Assignment Microservice assigning Dil access to C1] as in [Prerequisite_Case_Assignment_C1_Dil],
    And a user [Jamal – another solicitor within the same organisation as Richard],
    And a successful call [to the ‘Assign Access within Organisation’ operation of Case Assignment Microservice assigning Jamal access to C2] as in [Prerequisite_Case_Assignment_C2_Jamal],
    And a user [Mutlu – another solicitor within the same organisation as Richard],
    And a successful call [to the ‘Assign Access within Organisation’ operation of Case Assignment Microservice assigning Mutlu access to C3] as in [Prerequisite_Case_Assignment_C3_Mutlu],
    When a request is prepared with appropriate values,
    And the request [is made by Richard],
    And the request [contains the IDs of C1, C2 & C3 along with an empty element in the comma-separated list of case ID's],
    And it is submitted to call the [Get Assignments in My Organisation] operation of [Manage Case Assignment Microservice],
    Then a negative response is received,
    And the response has all the details as expected.

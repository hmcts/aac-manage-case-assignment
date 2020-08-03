@F-001
Feature: F-001: Get Assignments in My Organisation

  Background: Load test data for the scenario
    Given an appropriate test context as detailed in the test data source

  # ACA-48 / AC-1
  @S-001.48.1
  Scenario: Must return case assignments in my organisation for the provided Case IDs
    Given an appropriate test context as detailed in the test data source,
    And a user [Richard – a solicitor with the required permissions to create a case],
    And a successful call [by Richard to create a case - C1] as in [Prerequisite Case Creation Call for Case Assignment],
    And a successful call [by Richard to create another case – C2] as in [Prerequisite Case Creation Call for Case Assignment],
    And a user [Dil – another solicitor within the same organisation as Richard],
    And a successful call [to the ‘Assign Access within Organisation’ operation of Case Assignment Microservice assigning Dil access to C1],
    And a user [Jamal – another solicitor within the same organisation as Richard],
    And a successful call [to the ‘Assign Access within Organisation’ operation of Case Assignment Microservice assigning Jamal access to C2],
    When a request is prepared with appropriate values,
    And the request [is made by Richard],
    And the request [contains a correctly-formed comma separated list of the valid case ID’s of C1 and C2],
    And it is submitted to call the [Get Assignments in My Organisation] operation of [Case Assignment Microservice],
    Then a positive response is received,
    And the response [includes the respective case assignments of user ID's of both Dil and Jamal and cases C1 and C2],
    And the response has all the details as expected.

  # ACA-48 / AC-2
  @S-001.48.2
  Scenario: Must return an error response when a malformed case ID is provided
    Given an appropriate test context as detailed in the test data source,
    And a user [Richard – a solicitor with the required permissions to create a case],
    And a successful call [by Richard to create a case - C1] as in [Prerequisite Case Creation Call for Case Assignment],
    And a successful call [by Richard to create another case – C2] as in [Prerequisite Case Creation Call for Case Assignment],
    And a user [Dil – another solicitor within the same organisation as Richard],
    And a successful call [to the ‘Assign Access within Organisation’ operation of Case Assignment Microservice assigning Dil access to C1],
    And a user [Jamal – another solicitor within the same organisation as Richard],
    And a successful call [to the ‘Assign Access within Organisation’ operation of Case Assignment Microservice assigning Jamal access to C2],
    When a request is prepared with appropriate values,
    And the request [is made by Richard],
    And the request [contains a correctly-formed comma separated list of the case ID’s which includes the valid case IDs of C1 and C2 plus another case ID which is malformed],
    And it is submitted to call the [Get Assignments in My Organisation] operation of [Case Assignment Microservice],
    Then a negative response is received,
    And the response has all the details as expected.

  # ACA-48 / AC-3
  @S-001.48.3
  Scenario: Must return an error response for a missing case ID
    Given an appropriate test context as detailed in the test data source,
    And a user [Richard – with a solicitor role with access to invoke the ‘Get Assignments in My Organisation’ operation of Case Assignment Microservice],
    When a request is prepared with appropriate values,
    And the request [is made by Richard],
    And the request [does not contain any case ID],
    And it is submitted to call the [Get Assignments in My Organisation] operation of [Case Assignment Microservice],
    Then a negative response is received,
    And the response has all the details as expected.

  # ACA-48 / AC-4
  @S-001.48.4
  Scenario: Must return an error response for a malformed Case ID List
    Given an appropriate test context as detailed in the test data source,
    And a user [Richard – a solicitor with the required permissions to create a case],
    And a successful call [by Richard to create a case - C1] as in [Prerequisite Case Creation Call for Case Assignment],
    And a successful call [by Richard to create another case – C2] as in [Prerequisite Case Creation Call for Case Assignment],
    And a successful call [by Richard to create another case – C3] as in [Prerequisite Case Creation Call for Case Assignment],
    And a user [Dil – another solicitor within the same organisation as Richard],
    And a successful call [to the ‘Assign Access within Organisation’ operation of Case Assignment Microservice assigning Dil access to C1],
    And a user [Jamal – another solicitor within the same organisation as Richard],
    And a successful call [to the ‘Assign Access within Organisation’ operation of Case Assignment Microservice assigning Jamal access to C2],
    And a user [Mutlu – another solicitor within the same organisation as Richard],
    And a successful call [to the ‘Assign Access within Organisation’ operation of Case Assignment Microservice assigning Mutlu access to C3],
    When a request is prepared with appropriate values,
    And the request [is made by Richard],
    And the request [contains the IDs of C1, C2 & C3 along with an empty element in the comma-separated list of case ID's],
    And it is submitted to call the [Get Assignments in My Organisation] operation of [Case Assignment Microservice],
    Then a negative response is received,
    And the response has all the details as expected.

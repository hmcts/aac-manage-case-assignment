@F-003
Feature: F-003: Unassign Access Within Organisation

  Background:
    Given an appropriate test context as detailed in the test data source

  # ACA-51 A/C 1
  @S-012
  Scenario: Solicitor successfully removing case access for another solicitor in their org (happy path)
    Given a user [U1 - with a Solicitor role for a particular jurisdiction under an organisation to create, assign and unassign access to a case for another solicitor in their organisation],
    And a user [U2 - with a solicitor role for the same jurisdiction within the same organisation as U1],
    And a user [U3 - with a solicitor role for the same jurisdiction within the same organisation as U1],
    And a case [by U1 to create a case - C1] created as in [Prerequisite Case Creation Call for Case Assignment],
    And a successful call [by U1 to grant access to C1 for U2] as in [Prerequisite Case Creation Call for Case Assignment],
    And a successful call [by U1 to grant access to C1 for U3] as in [Prerequisite Case Creation Call for Case Assignment],
    And a successful call [by U1 to confirm the access to C1 for U2 & U3] as in [Prerequisite access confirmation call],
    When a request is prepared with appropriate values,
    And the request [is made by U2 and intends to unassign access to C1 for U1 and U3],
    And it is submitted to call the [Unassign Access within Organisation] operation of [Manage Case Assignment Microservice],
    Then a positive response is received,
    And the response has all the details as expected,
    And a call [to Get Assignments In My Organisation by U2 to verify unassignment of U1 and U3 from C1] will get the expected response as in [the respective test data file].

  # ACA-51 A/C 2
  @S-013
  Scenario: CAA successfully removing case access for another solicitor in their org (happy path)
    Given a user [U1 - with a solicitor role in a particular jurisdiction within an organisation, to create a case],
    And a user [U2 - with a pui-caa role within the same organisation to assign and unassign access to a case for a solicitor in their organisation],
    And a user [U3 - with a solicitor role for the same jurisdiction within the same organisation, to get assigned and unassigned access to a case],
    And a case [by U1 to create a case - C1] created as in [Prerequisite Case Creation Call for Case Assignment],
    And a successful call [by U2 to grant access to C1 for U1 & U3] as in [Prerequisite Case Creation Call for Case Assignment],
    And a successful call [by U2 to confirm the access to C1 for U1 & U3] as in [Prerequisite access confirmation call],
    When a request is prepared with appropriate values,
    And the request [is made by U2 and intends to unassign access to C1 for U1 and U3],
    And it is submitted to call the [Unassign Access within Organisation] operation of [Manage Case Assignment Microservice],
    Then a positive response is received,
    And the response has all the details as expected,
    And a call [to Get Assignments In My Organisation by U2 to verify unassignment of U1 and U3 from C1 ] will get the expected response as in [the respective test data file].

  # ACA-51 A/C 3
  @S-014
  Scenario: Solicitor successfully removing access to multiple cases for multiple solicitors in their org (happy path)
    Given a user [U1 - with a Solicitor role in a jurisdiction under an organisation to assign and Unassign a case role to a solicitor within the same organisation],
    And a user [U2 - with a Solicitor role in the same jurisdiction under the same organisation as U1 to be assigned and unassigned to some cases],
    And a user [U3 - with a Solicitor role in the same jurisdiction under the same organisation as U1 to be assigned and unassigned to some cases],
    And a case [by U1 to create a case - C1] created as in [Prerequisite Case Creation Call for Case Assignment],
    And a successful call [by U1 to grant access to C1 for U2 & U3] as in [Prerequisite Case Creation Call for Case Assignment],
    And a successful call [by U1 to create a case - C2] as in [Prerequisite Case Creation Call for Case Assignment],
    And a successful call [to U1 grant access to C2 for U2 & U3] as in [Prerequisite Case Creation Call for Case Assignment],
    And a successful call [by U1 to create a case - C3] as in [Prerequisite Case Creation Call for Case Assignment],
    And a successful call [by U1 to grant access to C3 for U2 & U3] as in [Prerequisite Case Creation Call for Case Assignment],
    And a successful call [by U1 to confirm the access to C1, C2 & C3 for U2 & U3] as in [Prerequisite access confirmation call],
    When a request is prepared with appropriate values,
    And the request [is made by U1 and intends to unassign access to C1, C2 and C3 for U2 & U3],
    And it is submitted to call the [Unassign Access within Organisation] operation of [Manage Case Assignment Microservice],
    Then a positive response is received,
    And the response has all the details as expected,
    And a call [to Get Assignments In My Organisation by U1 to verify unassignment of U2 and U3 from C1, C2 and C3] will get the expected response as in [the respective test data file].

  # ACA-51 A/C 4
  @S-015
  Scenario: Pui-caa successfully removing access to multiple cases for multiple solicitors in their org (happy path)
    Given a user [U1 - with a solicitor role in a particular jurisdiction within an organisation, to create a case],
    And a user [U2 - with a pui-caa role within the same organisation to assign and unassign access to a case for a solicitor in their organisation],
    And a user [U3 - with a solicitor role for the same jurisdiction within the same organisation, to get assigned and unassigned access to a case],
    And a case [by U1 to create a case - C1] created as in [Prerequisite Case Creation Call for Case Assignment],
    And a case [by U1 to create a case - C2] created as in [Prerequisite Case Creation Call for Case Assignment],
    And a case [by U1 to create a case - C3] created as in [Prerequisite Case Creation Call for Case Assignment],
    And a successful call [by U2 to grant access to C1 for U3] as in [Prerequisite Case Creation Call for Case Assignment],
    And a successful call [by U2 grant access to C2 for U3] as in [Prerequisite Case Creation Call for Case Assignment],
    And a successful call [by U2 to grant access to C3 for U3] as in [Prerequisite Case Creation Call for Case Assignment],
    And a successful call [by U2 to confirm the access to C1, C2 & C3 for U3] as in [Prerequisite access confirmation call],
    When a request is prepared with appropriate values,
    And the request [is made by U2 and intends to unassign access to C1, C2 and C3 for U1 & U3],
    And it is submitted to call the [Unassign Access within Organisation] operation of [Manage Case Assignment Microservice],
    Then a positive response is received,
    And the response has all the details as expected,
    And a call [to Get Assignments In My Organisation by U2 to verify unassignment of U1 and U3 from C1, C2 and C3] will get the expected response as in [the respective test data file].

  # ACA-51 A/C 5
  @S-016
  Scenario: Must return an error response if intended unassignee doesn't exist in invoker's organisation
    Given a user [U1 - with a Solicitor role for a particular jurisdiction under an organisation to create, assign and unassign access to a case for another solicitor in their organisation - O1],
    And a user [U2 - with a solicitor role for the same jurisdiction within organisation - O1],
    And a user [U3 - with a solicitor role for the same jurisdiction within a different organisation from U1 - O2],
    And a user [U4 - with a Pui-CAA role for the same jurisdiction and in organisation - O2],
    And a case [by U1 to create a case - C1 with Organisation policies containing R1 and R2] created as in [Prerequisite Case Creation Call for Case Assignment],
    And a successful call [by U1 to grant access to C1 for U2 with role R1] as in [Prerequisite Case Creation Call for Case Assignment],
    And a successful call [by U4 to grant access to C1 for U3 with role R2] as in [Prerequisite Case Creation Call for Case Assignment],
    And a successful call [by U1 to confirm the access to C1 for U2] as in [Prerequisite access confirmation call],
    And a successful call [by U4 to confirm the access to C1 for U3] as in [Prerequisite access confirmation call],
    When a request is prepared with appropriate values,
    And the request [is made by U1 and intends to unassign access to C1 for U2 and U3],
    And it is submitted to call the [Unassign Access within Organisation] operation of [Manage Case Assignment Microservice],
    Then a negative response is received,
    And the response has all the details as expected,
    And a call [by U1 to confirm that U2 still has access to the case] will get the expected response as in [they will still be able to access the case],
    And a call [by U4 to confirm that U3 still has access to the case] will get the expected response as in [they will still be able to access the case].

  # ACA-51 A/C 6
  @S-017
  Scenario: Must return an error response for a malformed Case ID
    Given a user [U1 - with a Solicitor role for a particular jurisdiction under an organisation to create, assign and unassign access to a case for another solicitor in their organisation],
    And a user [U2 - with a solicitor role for the same jurisdiction within the same organisation as U1],
    And a case [by U1 to create a case - C1] created as in [Prerequisite Case Creation Call for Case Assignment],
    And a successful call [by U1 to grant access to C1 for U2] as in [Prerequisite Case Creation Call for Case Assignment],
    When a request is prepared with appropriate values,
    And the request [is made by U1 and intends to unassign access for U2 to some cases],
    And the request [contains the Case ID of C1 plus an additional malformed caseID],
    And it is submitted to call the [Unassign Access within Organisation] operation of [Manage Case Assignment Microservice],
    Then a negative response is received,
    And the response has all the details as expected,
    And a call [by U1 to confirm that U2 still has access to the case - C1] will get the expected response as in [they will still be able to access the case].

  # ACA-51 A/C 7
  @S-018
  Scenario: Must return an error response for a missing Case ID
    Given a user [U1 - with a Solicitor role for a particular jurisdiction under an organisation to create, assign and unassign access to a case for another solicitor in their organisation],
    And a user [U2 - with a solicitor role for the same jurisdiction within the same organisation as U1],
    And a case [by U1 to create a case - C1] created as in [Prerequisite Case Creation Call for Case Assignment],
    And a successful call [by U1 to grant access to C1 for U2] as in [Prerequisite Case Creation Call for Case Assignment],
    When a request is prepared with appropriate values,
    And the request [is made by U1 and intends to unassign access for U2 to some cases],
    And the request [contains the Case ID of C1 and a missing case ID],
    And it is submitted to call the [Unassign Access within Organisation] operation of [Manage Case Assignment Microservice],
    Then a negative response is received,
    And the response has all the details as expected,
    And a call [by U1 to confirm that U2 still has access to the case - C1] will get the expected response as in [they will still be able to access the case].

  # ACA-51 A/C 8
  @S-019
  Scenario: Must return an error response if the invoker is a solicitor in a different jurisdiction from that of the case
    Given a user [U1 - with a Solicitor role for a particular jurisdiction under an organisation to create, assign and unassign access to a case for another solicitor in their organisation],
    And a user [U2 - with a solicitor role for a different jurisdiction within the same organisation as U1],
    And a case [by U1 to create a case - C1] created as in [Prerequisite Case Creation Call for Case Assignment],
    And a successful call [by U1 to confirm the access to C1 for U1] as in [Prerequisite access confirmation call],
    When a request is prepared with appropriate values,
    And the request [is made by U2 and intends to unassign access to C1 for U1],
    And it is submitted to call the [Unassign Access within Organisation] operation of [Manage Case Assignment Microservice],
    Then a negative response is received,
    And the response has all the details as expected,
    And a call [to Get Assignments In My Organisation by U1 to verify continued assignment of U1 for C1] will get the expected response as in [the respective test data file].

  # ACA-51 A/C 9
  @S-020
  Scenario: Must return an error response if the invoker doesn't have access to the case of the user they are trying to remove access for
    Given a user [U1 - with a Solicitor role for a particular jurisdiction under an organisation to create, assign and unassign access to a case for another solicitor in their organisation],
    And a user [U2 - with a solicitor role for the same jurisdiction within the same organisation as U1],
    And a case [by U1 to create a case - C1] created as in [Prerequisite Case Creation Call for Case Assignment],
    And a successful call [by U1 to confirm the access to C1 for U1] as in [Prerequisite access confirmation call],
    When a request is prepared with appropriate values,
    And the request [is made by U2 and intends to unassign access to C1 for U1],
    And it is submitted to call the [Unassign Access within Organisation] operation of [Manage Case Assignment Microservice],
    Then a negative response is received,
    And the response has all the details as expected,
    And a call [to Get Assignments In My Organisation by U1 to verify the continued assignment of U1 for C1] will get the expected response as in [the respective test data file].

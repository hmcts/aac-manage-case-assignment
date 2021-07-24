#================================================
@F-201
Feature: F-201: Assign Access within Organisation
#================================================

  Background:
    Given an appropriate test context as detailed in the test data source

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-201.1
  Scenario: Solicitor successfully sharing case access with another solicitor in their org (happy path)

    Given a user [S1 - a solicitor, to create a case under their organisation and share it with a fellow solicitor in the same organisation],
      And a user [S2 - another solicitor in the same organisation, with whom S1 will share a case with an assignment within organisation],
      And a case [C1, which S1 has just] created as in [F-201_Prerequisite_Case_Creation_C1],

      And a call [by S2 to access the case created by S1] will get the expected response as in [F-201_Case_Not_Found].

    When a request is prepared with appropriate values,
      And the request [is to be invoked by S1 to assign access over C1 for S2 within the same organisation],
      And it is submitted to call the [Assign Access within Organisation] operation of [Manage Case Assignment Microservice],

     Then a positive response is received,
      And the response has all other details as expected,
      And a call [by S2 to query his/her case roles granted over C1] will get the expected response as in [F-201_S2_Querying_Their_Access_Over_C1].

      And a call [by S2 to access the case created by S1] will get the expected response as in [F-201_Case_Found].

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-201.1b
  Scenario: Solicitor successfully sharing case access with another solicitor in their org (happy path) called with use_user_token to fetch case details

    Given a user [S1 - a solicitor, to create a case under their organisation and share it with a fellow solicitor in the same organisation],
      And a user [S2 - another solicitor in the same organisation, with whom S1 will share a case with an assignment within organisation],
      And a case [C1, which S1 has just] created as in [F-201_Prerequisite_Case_Creation_C1],

     When a request is prepared with appropriate values,
      And the request [is to be invoked by S1 to assign access over C1 for S2 within the same organisation],
      And it is submitted to call the [Assign Access within Organisation] operation of [Manage Case Assignment Microservice],

     Then a positive response is received,
      And the response has all other details as expected,
      And a call [by S2 to query his/her case roles granted over C1] will get the expected response as in [F-201_S2_Querying_Their_Access_Over_C1].

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-201.2
  Scenario: PUI CAA successfully sharing case access with another solicitor in their org (happy path)

    Given a user [S1 - a solicitor, to create a case under their organisation],
      And a user [S2 - another solicitor in the same organisation, with whom a CAA will share a case with an assignment within organisation],
      And a user [CAA - a PUI case access admin, to share a case with a solicitor in the same organisation],
      And a case [C1, which S1 has just] created as in [F-201_Prerequisite_Case_Creation_C1],

     When a request is prepared with appropriate values,
      And the request [is to be invoked by CAA to assign access over C1 for S2 within the same organisation],
      And it is submitted to call the [Assign Access within Organisation] operation of [Manage Case Assignment Microservice],

     Then a positive response is received,
      And the response has all other details as expected,
      And a call [by S2 to query his/her case roles granted over C1] will get the expected response as in [F-201_S2_Querying_Their_Access_Over_C1].

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-201.2b
  Scenario: Must return an error response if PUI CAA tries to share a case access with another solicitor in their org called with use_user_token, but has no case access

    Given a user [S1 - a solicitor, to create a case under their organisation],
      And a user [S2 - another solicitor in the same organisation, with whom a CAA will share a case with an assignment within organisation],
      And a user [CAA - a PUI case access admin, to share a case with a solicitor in the same organisation],
      And a case [C1, which S1 has just] created as in [F-201_Prerequisite_Case_Creation_C1],

    When a request is prepared with appropriate values,
      And the request [is to be invoked by CAA to assign access over C1 for S2 within the same organisation],
      And it is submitted to call the [Assign Access within Organisation] operation of [Manage Case Assignment Microservice],

    Then a negative response is received,
      And the response has all the details as expected,
      And a call [by S2 to query his/her case roles granted over C1] will get the expected response as in [F-201_S2_Querying_No_Access_Over_C1].

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-201.3
  Scenario: Must return an error response if assignee doesn't exist in invoker's organisation

    Given a user [S1 - a solicitor, to create a case under their organisation and share it with a fellow solicitor in the same organisation],
      And a user [S2 - a solicitor within a different organisation who doesn't have access to C1],
      And a case [C1, which S1 has just] created as in [F-201_Prerequisite_Case_Creation_C1],

     When a request is prepared with appropriate values,
      And the request [is to be invoked by S1 to assign access over C1 for S2 within a different organisation],
      And it is submitted to call the [Assign Access within Organisation] operation of [Manage Case Assignment Microservice],

     Then a negative response is received,
      And the response has all the details as expected,
      And a call [by S2 to query his/her case roles granted over C1] will get the expected response as in [F-201_S2_Querying_No_Access_Over_C1].

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-201.4
  Scenario: Must return an error response for a malformed Case ID

    Given a user [S1 - with a solicitor role under an organisation to assign a case role to another solicitor within the same organisation],
      And a user [S2 - with a solicitor role within the same organisation],

     When a request is prepared with appropriate values,
      And the request [intends to assign access within the same organisation for S2 by S1],
      And the request [contains a malformed Case ID],
      And it is submitted to call the [Assign Access within Organisation] operation of [Manage Case Assignment Microservice],

     Then a negative response is received,
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-201.5
  Scenario: Must return an error response for a missing Case ID

    Given a user [S1 - with a solicitor role under an organisation to assign a case role to another solicitor within the same organisation],
      And a user [S2 - with a solicitor role within the same organisation],

     When a request is prepared with appropriate values,
      And the request [intends to assign access within the same organisation for S2 by S1],
      And the request [does not contain a Case ID],
      And it is submitted to call the [Assign Access within Organisation] operation of [Manage Case Assignment Microservice],

     Then a negative response is received,
      And the response has all the details as expected,

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-201.6a
  Scenario: Must return an error response for an assignee user who doesn't have a solicitor role for the jurisdiction of the case

    Given a user [S1 - with a solicitor role under an organisation to assign a case role to another solicitor within the same organisation],
      And a user [S2 - who does not have a solicitor role for the jurisdiction of C1 but works within the same organisation as S1],
      And a case [C1, which S1 has just] created as in [F-201_Prerequisite_Case_Creation_C1],

     When a request is prepared with appropriate values,
      And the request [intends to assign access within the same organisation for S2 by S1],
      And it is submitted to call the [Assign Access within Organisation] operation of [Manage Case Assignment Microservice],

     Then a negative response is received,
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-201.6b
  Scenario: Must return an error response for an assignee user who doesn't have a valid solicitor role for the jurisdiction of the case

    Given a user [S1 - with a solicitor role under an organisation to assign a case role to another solicitor within the same organisation],
      And a user [S2 - who does not have a valid solicitor role],
      And a case [C1, which S1 has just] created as in [F-201_Prerequisite_Case_Creation_C1],

    When a request is prepared with appropriate values,
      And the request [intends to assign access within the same organisation for S2 by S1],
      And it is submitted to call the [Assign Access within Organisation] operation of [Manage Case Assignment Microservice],

    Then a negative response is received,
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-201.7
  Scenario: Must return a negative response when the case doesn't contain an assignment for the invoker's organisation

    Given a user [S1 - a solicitor, to create a case under their organisation and share it with a fellow solicitor in the same organisation],
      And a user [S2 - another solicitor in the same organisation, with whom S1 will share a case with an assignment within organisation],
      And a case [C1, which S1 has just] created as in [Prerequisite_Case_Creation_C0_Without_Org_Policies],

     When a request is prepared with appropriate values,
      And the request [is to be invoked by S1 to assign access over C1 for S2 within the same organisation],
      And it is submitted to call the [Assign Access within Organisation] operation of [Manage Case Assignment Microservice],

     Then a negative response is received,
      And the response has all other details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-201.8
  Scenario: Must return an error response for an invalid Luhn number used as Case ID

    Given a user [S1 - with a solicitor role under an organisation to assign a case role to another solicitor within the same organisation],
      And a user [S2 - with a solicitor role within the same organisation],

     When a request is prepared with appropriate values,
      And the request [intends to assign access within the same organisation for S2 by S1],
      And the request [contains an invalid Luhn number as Case ID],
      And it is submitted to call the [Assign Access within Organisation] operation of [Manage Case Assignment Microservice],

     Then a negative response is received,
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-201.9
  Scenario: Must return an error response for a valid Luhn number used as Case ID but no cases found

    Given a user [S1 - with a solicitor role under an organisation to assign a case role to another solicitor within the same organisation],
      And a user [S2 - with a solicitor role within the same organisation],

     When a request is prepared with appropriate values,
      And the request [intends to assign access within the same organisation for S2 by S1],
      And the request [contains a valid Luhn number as Case ID],
      And it is submitted to call the [Assign Access within Organisation] operation of [Manage Case Assignment Microservice],

     Then a negative response is received,
      And the response has all the details as expected.

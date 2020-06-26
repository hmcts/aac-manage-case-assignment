@F-001
Feature: F-001: Assign Access within Organisation

  Background:
    Given an appropriate test context as detailed in the test data source

  @S-001
  Scenario: Solicitor successfully sharing case access with another solicitor in their org (happy path)
    Given a user [S1 - a solicitor, to create a case under their organisation and share it with a fellow solicitor in the same organisation],
    And   a user [S2 - another solicitor in the same organisation, with whom S1 will share a case with an assignment within their organisation],
    And   a case [C1, which S1 has just] created as in [Prerequisite_Case_Creation_C1],
    And   logstash has finished indexing case data
    When  a request is prepared with appropriate values,
    And   the request [is to be invoked by S1 to assign access over C1 for S2 within the same organisation],
    And   it is submitted to call the [Assign Access within Organisation] operation of [Manage Case Assignment Microservice],
    Then  a positive response is received,
    And   the response has all other details as expected,
    And   a call [by S2 to query his/her case roles granted over C1] will get the expected response as in [S2_Querying_Their_Access_Over_C1].

  @S-002
  Scenario: CAA successfully sharing case access with another solicitor in their org
    Given a user [CW1 - a caseworker-caa, to create a case under their organisation and share it with a solicitor within the same organisation],
    And   a user [S2 - a solicitor in the same organisation, with whom CW1 will share a care with an assignment within their organisation],
    And   a case [C2, which CW1 has just] created as in [Prerequisite_Case_Creation_C2],
    And   logstash has finished indexing case data
    When  a request is prepared with appropriate values,
    And   the request [is to be invoked by CW1 to assign access over C2 for S2 within the same organisation],
    And   it is submitted to call the [Assign Access within Organisation] operation of [Manage Case Assignment Microservice],
    Then  a positive response is received,
    And   the response has all the details as expected,
    And   a call [by S2 to query his/her case roles granted over C2] will get the expected response as in [S2_Querying_Their_Access_Over_C2].

  @S-003
  Scenario: must return an error response if assignee doesn't exist in invoker's organisation
    Given a user [S1 - a solicitor, to create a case under their organisation and share it with a fellow solicitor in the same organisation],
    And   a user [S2 - a solicitor within a different organisation who doesn't have access to C1],
    And   a case [C1, which S1 has just] created as in [Prerequisite_Case_Creation_C1],
    And   logstash has finished indexing case data
    When  a request is prepared with appropriate values,
    And   the request [is to be invoked by S1 to assign access over C1 for S2 within a different organisation],
    And   it is submitted to call the [Assign Access within Organisation] operation of [Manage Case Assignment Microservice],
    Then  a negative response is received,
    And   the response has all the details as expected.
#    And   a call [by S2 to access C1] will get the expected response as in [they are unable to access the case].

  @S-004
  Scenario: must return an error response for a malformed Case ID
    Given a user [S1 - with a solicitor role under an organisation to assign a case role to another solicitor within the same organisation],
    And   a user [S2 - with a solicitor role within the same organisation],
    When  a request is prepared with appropriate values,
    And   the request [intends to assign access within the same organisation for S2 by S1],
    And   the request [contains a malformed Case ID],
    And   it is submitted to call the [Assign Access within Organisation] operation of [Manage Case Assignment Microservice],
    Then  a negative response is received,
    And   the response has all the details as expected.

  @S-005
  Scenario: must return an error response for a missing Case ID
    Given a user [S1 - with a solicitor role under an organisation to assign a case role to another solicitor within the same organisation],
    And   a user [S2 - with a solicitor role within the same organisation],
    When  a request is prepared with appropriate values,
    And   the request [intends to assign access within the same organisation for S2 by S1],
    And   the request [does not contain a Case ID],
    And   it is submitted to call the [Assign Access within Organisation] operation of [Manage Case Assignment Microservice],
    Then  a negative response is received,
    And   the response has all the details as expected,

  @S-006
  Scenario: must return an error response for an assignee user who doesn't have a solicitor role for the jurisdiction of the case
    Given a user [S1 - with a solicitor role under an organisation to assign a case role to another solicitor within the same organisation],
    And   a user [U2 - who does not have a solicitor role for the jurisdiction of C1 but works within the same organisation as S1],
    And   a case [C1, which S1 has just] created as in [Prerequisite_Case_Creation_C1],
    And   logstash has finished indexing case data
    When  a request is prepared with appropriate values,
    And   the request [intends to assign access within the same organisation for U2 by S1],
    And   it is submitted to call the [Assign Access within Organisation] operation of [Manage Case Assignment Microservice],
    Then  a negative response is received,
    And   the response has all the details as expected,
#    And   a call [by U2 to access C1] will get the expected response as in [they are unable to access the case].

  @S-007 @Ignore
  Scenario: Must return an error response if the invoker doesn't have a solicitor role for the jurisdiction of the case or a caseworker-caa role
    Given a successful call [to create a case - C1] as in [Prerequisite Case Creation Call for Case Assignment],
    And   a user [U1 - without a solicitor role for the jurisdiction of the case or a caseworker-caa role, under an organisation, to assign a case role to another solicitor within the same organisation],
    And   a successful call [to grant access to C1 for U1] as in [Prerequisite Case Creation Call for Case Assignment],
    And   a user [U2 - with a solicitor role within the same organisation who doesn't have access to C1],
    When  a request is prepared with appropriate values,
    And   the request [intends to assign access within the same organisation for U2 by U1],
    And   it is submitted to call the [Assign Access within Organisation] operation of [Case Assignment Microservice],
    Then  a negative response is received,
    And   the response has all the details as expected,
#    And   a call [by U2 to access C1] will get the expected response as in [they are unable to access the case].

  @S-008 @Ignore
  Scenario: Must return a negative response when the case doesn't contain an assignment for the invoker's organisation
    Given a user [U1 - with a solicitor role under an organisation to assign a case role to another solicitor within the same organisation],
    And   a successful call [to create a case - C1 - without an assignment for U1s organisation] as in [Prerequisite Case Creation Call for Case Assignment],
    And   a successful call [to grant access to C1 for U1] as in [Prerequisite Case Creation Call for Case Assignment],
    And   a user [U2 - with a solicitor role within the same organisation who doesn't have access to C1],
    When  a request is prepared with appropriate values,
    And   the request [intends to assign access within the same organisation for U2 by U1],
    And   it is submitted to call the [Assign Access within Organisation] operation of [Case Assignment Microservice],
    Then  a negative response is received,
    And   the response has all the details as expected,
#    And   a call [by U2 to access C1] will get the expected response as in [they are unable to access the case].

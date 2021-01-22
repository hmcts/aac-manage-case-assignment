#================================================
@F-204
Feature: F-204 Get Notice of Change Questions
#================================================

  Background:
    Given an appropriate test context as detailed in the test data source,

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-204.1
  Scenario: NoC questions successfully returned for a Solicitor user

    Given a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Dil - with a solicitor role for the same jurisdiction as Case C1 below and no caseworker-pui role],
      And a case [created by Richard - C1 - on behalf of Mario as the applicant which is auto-assigned to Richard's organisation] created as in [204_Case_Creation],
      And a successful call [to get the update org policy event trigger] as in [204_Update_Org_Policies_Token_Creation],
      And a successful call [to update the applicant org policy] as in [204_Update_Org_Policies],

     When a request is prepared with appropriate values,
      And the request [is made by Dil to return the questions that need answering in order to become Mario's representative on C1],
      And the request [contains a valid case ID for C1],
      And the request [contains a valid IDAM token for Dil],
      And it is submitted to call the [NoCQuestions] operation of [Case Assignment Microservice],

     Then a positive response is received,
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-204.2
  Scenario: NoC questions successfully returned for a Case Access Adminisrator

    Given a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Matt - with a caseworker-pui role and no Solicitor role],
      And a case [created by Richard - C1 - on behalf of Mario as the applicant which is auto-assigned to Richard's organisation] created as in [204_Case_Creation],
      And a successful call [to get the update org policy event trigger] as in [204_Update_Org_Policies_Token_Creation],
      And a successful call [to update the applicant org policy] as in [204_Update_Org_Policies],

     When a request is prepared with appropriate values,
      And the request [is made by Matt to return the questions that need answering in order to become Mario's representative on C1],
      And the request [contains a valid case ID for C1],
      And the request [contains a valid IDAM token for Matt],
      And it is submitted to call the [NoCQuestions] operation of [Case Assignment Microservice],

     Then a positive response is received,
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-204.3
  Scenario: NoC questions successfully returned for a Case Access Administrator and a Solicitor (but the solicitor role is not for the jurisdiction of the case)

    Given a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Alice - with a caseworker-pui role and a solicitor role for a jurisdiction that does not match Case C1 below],
      And a case [created by Richard - C1 - on behalf of Mario as the applicant which is auto-assigned to Richard's organisation] created as in [204_Case_Creation],
      And a successful call [to get the update org policy event trigger] as in [204_Update_Org_Policies_Token_Creation],
      And a successful call [to update the applicant org policy] as in [204_Update_Org_Policies],

     When a request is prepared with appropriate values,
      And the request [is made by Alice to return the questions that need answering in order to become Mario's representative on C1],
      And the request [contains a valid case ID for C1],
      And the request [contains a valid IDAM token for Alice],
      And it is submitted to call the [NoCQuestions] operation of [Case Assignment Microservice],

     Then a positive response is received,
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-204.4
  Scenario: NoC questions must return an error response for a malformed Case ID

    Given a user [Jane - with a solicitor and a pui-caseworker role],

    When a request is prepared with appropriate values,
      And the request [is made by Jane to return the questions that need answering in order to become a representative of a litigant on the case],
      And the request [contains a malformed case ID for C1],
      And the request [contains a valid IDAM token for Jane],
      And it is submitted to call the [NoCQuestions] operation of [Case Assignment Microservice],

    Then a negative response is received,
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-204.5
  Scenario: NoC questions must return an error response for a non-extant Case ID

    Given a user [Dil - with a solicitor and pui-caseworker role],

     When a request is prepared with appropriate values,
      And the request [is made by Dil to return the questions that need answering in order to become a representative on a valid case number for which the case does not exist],
      And the request [contains a valid case ID for which there is no corresponding case record],
      And the request [contains a valid IDAM token for Dil],
      And it is submitted to call the [NoCQuestions] operation of [Case Assignment Microservice],

     Then a negative response is received,
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-204.6
  Scenario: NoC questions must return an error response for a missing Case ID

    Given a user [Dil - with a solicitor and pui-caseworker role],

     When a request is prepared with appropriate values,
      And the request [is made by Dil to return the questions that need answering in order to become a representative on a case],
      And the request [does not contain a case ID],
      And the request [contains a valid IDAM token for Dil],
      And it is submitted to call the [NoCQuestions] operation of [Case Assignment Microservice],

     Then a negative response is received,
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-204.7
  Scenario: NoC questions must return an error response when a NoC request is currently pending on a case

    Given a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Dil - with a solicitor role],
      And a user [System user - with caseworker-caa role],
      And a user [Mutlu - with a solicitor role for the same jurisdiction within a different organisation from Richard],
      And a case [created by Richard - C1 - on behalf of Mario as the applicant which is auto-assigned to Richard's organisation] created as in [204_Case_Creation],
      And a successful call [to get the update org policy event trigger] as in [204_Update_Org_Policies_Token_Creation],
      And a successful call [to update the applicant org policy] as in [204_Update_Org_Policies],
      And a successful call [to get the NoC request event trigger] as in [204_Submit_NoC_Event_Token_Creation],
      And a successful call [by System user to raise a NoC request on behalf of Mutlu to become a representative for Mario on C1] as in [204_Submit_NoC_Event],
      And [the configuration of the NoC request event has no auto-approval] in the context of the scenario
      And [No user has approved or rejected the NoC request from Mutlu] in the context of the scenario

     When a request is prepared with appropriate values,
      And the request [is made by Dil to return the questions that need answering in order to become Mario's representative on C1],
      And the request [contains a valid case ID],
      And the request [contains a valid IDAM token for Dil],
      And it is submitted to call the [NoCQuestions] operation of [Case Assignment Microservice],

     Then a negative response is received,
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-204.8
  Scenario: NoC questions must return an error if there is not an Organisation Policy field containing a case role for each set of answers

    Given a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Dil - with a solicitor role for the same jurisdiction as Case C1 below],
      And a case [created by Richard - C1 - on behalf of Mario as the applicant which is auto-assigned to Richard's organisation] created as in [204_Case_Creation],
      And a successful call [to get the update org policy event trigger] as in [204_Update_Org_Policies_Token_Creation],
      And a successful call [to update the applicant org policy] as in [204_Update_Org_Policies_With_Missing_Policy],
      And [The challenge questions have answers that resolve to three distinct case roles (1,2,3)] in the context of the scenario
      And [The configuration of the case creation event only establishes organisation policies containing case roles 1 and 2] in the context of the scenario
      And a wait time of [15] seconds [to allow for the cache to refresh],

    When a request is prepared with appropriate values,
      And the request [is made by Dil to return the questions that need answering in order to become Mario's representative on C1],
      And the request [contains a valid case ID for C1],
      And the request [contains a valid IDAM token for Dil],
      And it is submitted to call the [NoCQuestions] operation of [Case Assignment Microservice],

     Then a negative response is received,
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-204.9
  Scenario: NoC questions must return an error when no NOC Request event is available on the case

    Given a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Dil - with a solicitor role for the same jurisdiction within a different organisation from Richard],
      And a case [created by Richard - C1 - on behalf of Mario as the applicant which is auto-assigned to Richard's organisation] created as in [204_NoC_WithoutEvents_Case_Creation_With_Org_Policies],

    When a request is prepared with appropriate values,
      And the request [is made by Dil to return the questions in order to become Mario's representative on C1],
      And the request [contains a valid case ID for C1],
      And the request [contains a structurally valid set of answers],
      And the request [contains a valid IDAM token for Dil],
      And the request [identifies that no NOC request event is available on the case]
      And it is submitted to call the [NoCQuestions] operation of [Case Assignment Microservice],

    Then a negative response is received,
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-204.10
  Scenario: NoC questions must return an error response when the solicitor does not have access to the jurisdiction of the case

    Given a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Ashley - with a solicitor role for a different jurisdiction from that of the case that Richard will create and within a different organisation from Richard],
      And a case [created by Richard - C1 - on behalf of Mario as the applicant which is auto-assigned to Richard's organisation] created as in [204_Case_Creation],
      And a successful call [to get the update org policy event trigger] as in [204_Update_Org_Policies_Token_Creation],
      And a successful call [to update the applicant org policy] as in [204_Update_Org_Policies],

     When a request is prepared with appropriate values,
      And the request [is made by Ashley to return the questions that need answering in order to become Mario's representative on C1],
      And it is submitted to call the [NoCQuestions] operation of [Case Assignment Microservice],

     Then a negative response is received,
      And the response has all the details as expected.

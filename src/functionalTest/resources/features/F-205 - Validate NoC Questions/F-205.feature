#================================================
@F-205
Feature: F-205 Validate Notice of Change Answers
#================================================

  Background:
    Given an appropriate test context as detailed in the test data source,

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-205.1
  Scenario: NoC Answers successfully verified for a Solicitor user

    Given a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Dil - with a solicitor role for the same jurisdiction within a different organisation from Richard],
      And [the configuration for the case type of Case C1 is such that there is a NoCRequest event available to users with the IDAM ID Caseworker-CAA when the case is in the state established following case creation] in the context of the scenario
      And a case [created by Richard - C1 - on behalf of Mario as the applicant which is auto-assigned to Richard's organisation] created as in [204_Case_Creation],
      And a successful call [to get the update org policy event trigger] as in [204_Update_Org_Policies_Token_Creation],
      And a successful call [to update the applicant org policy] as in [204_Update_Org_Policies],

     When a request is prepared with appropriate values,
      And the request [is made by Dil to validate the answers provided to the NoC questions in order to become the representative for Mario on C1],
      And the request [contains all correct answers]
      And it is submitted to call the [ValidateNoCQuestions] operation of [Case Assignment Microservice],

     Then a positive response is received,
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-205.2
  Scenario: NoC Answers successfully validated for a CAA

    Given a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Matt - with a pui-CAA within a different organisation from Richard],
      And [the configuration for the case type of Case C1 is such that there is a NoCRequest event available to users with the IDAM ID Caseworker-CAA when the case is in the state established following case creation] in the context of the scenario
      And a case [created by Richard - C1 - on behalf of Mario as the applicant which is auto-assigned to Richard's organisation] created as in [204_Case_Creation],
      And a successful call [to get the update org policy event trigger] as in [204_Update_Org_Policies_Token_Creation],
      And a successful call [to update the applicant org policy] as in [204_Update_Org_Policies],

     When a request is prepared with appropriate values,
      And the request [is made by Matt to validate the answers provided to the NoC questions in order to become the representative for Mario on C1],
      And the request [contains all correct answers]
      And it is submitted to call the [ValidateNoCQuestions] operation of [Case Assignment Microservice],

     Then a positive response is received,
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-205.3
  Scenario: NoC Answers successfully validated for a CAA who is a solicitor for a different jurisdiction

    Given a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Alice - with a pui-CAA role and a solicitor role for a different jurisdiction to Case C1 below and in a different organisation to Richard],
      And [the configuration for the case type of Case C1 is such that there is a NoCRequest event available to users with the IDAM ID Caseworker-CAA when the case is in the state established following case creation] in the context of the scenario
      And a case [created by Richard - C1 - on behalf of Mario as the applicant which is auto-assigned to Richard's organisation] created as in [204_Case_Creation],
      And a successful call [to get the update org policy event trigger] as in [204_Update_Org_Policies_Token_Creation],
      And a successful call [to update the applicant org policy] as in [204_Update_Org_Policies],

     When a request is prepared with appropriate values,
      And the request [is made by Alice to raise a NoC request and intends to validate the answers provided to the NoC questions in order to become the representative for Mario on C1],
      And the request [contains all correct answers]
      And it is submitted to call the [ValidateNoCQuestions] operation of [Case Assignment Microservice],

     Then a positive response is received,
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-205.4
  Scenario: NoC Answers must return an error response for a malformed Case ID

    Given a user [Dil - with a solicitor and a pui-caseworker role],

     When a request is prepared with appropriate values,
      And the request [is made by Dil to validate the answers to the challenge questions in order to become a representative of a litigant on the case],
      And the request [contains a malformed case ID for C1],
      And the request [contains a structurally valid set of answers],
      And the request [contains a valid IDAM token for Dil],
      And it is submitted to call the [ValidateNoCQuestions] operation of [Case Assignment Microservice],

     Then a negative response is received,
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-205.5
  Scenario: NoC Answers must return an error response for a non-extant Case ID

    Given a user [Dil - with a solicitor and pui-caseworker role],

     When a request is prepared with appropriate values,
      And the request [is made by Dil to validate the answers to the challenge questions in order to become a representative on a valid case number for which the case does not exist],
      And the request [contains a non-extant case ID for which there is no corresponding case record],
      And the request [contains a structurally valid set of answers],
      And the request [contains a valid IDAM token for Dil],
      And it is submitted to call the [ValidateNoCQuestions] operation of [Case Assignment Microservice],

     Then a negative response is received,
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-205.6
  Scenario: NoC Answers must return an error response for a missing Case ID

    Given a user [Dil - with a solicitor and pui-caseworker role],

     When a request is prepared with appropriate values,
      And the request [is made by Dil to validate the answers to the challenge questions in order to become a representative on a valid case number for which the case does not exist],
      And the request [does not contain any case ID],
      And the request [contains a structurally valid set of answers],
      And the request [contains a valid IDAM token for Dil],
      And it is submitted to call the [ValidateNoCQuestions] operation of [Case Assignment Microservice],

     Then a negative response is received,
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-205.7
  Scenario: NoC Answers must return an error response when a NoC request is currently pending on a case

    Given a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Dil - with a solicitor role for the jurisdiction of C1 below],
      And a user [System user - with caseworker-caa role],
      And a user [Mutlu - with a solicitor role for the same jurisdiction within a different organisation from Richard],
      And [the configuration of the NoC request event has no auto-approval] in the context of the scenario
      And a case [created by Richard - C1 - on behalf of Mario as the applicant which is auto-assigned to Richard's organisation] created as in [204_Case_Creation],
      And a successful call [to get the update org policy event trigger] as in [204_Update_Org_Policies_Token_Creation],
      And a successful call [to update the applicant org policy] as in [204_Update_Org_Policies],
      And a successful call [to get the NoC request event trigger] as in [204_Submit_NoC_Event_Token_Creation],
      And a successful call [by System user to raise a NoC request on behalf of Mutlu to become a representative for Mario on C1] as in [204_Submit_NoC_Event],
      And [No user has approved or rejected the NoC request from Mutlu] in the context of the scenario

     When a request is prepared with appropriate values,
      And the request [is made by Dil to validate the answers in order to become Mario's representative on C1],
      And the request [contains a valid case ID],
      And the request [contains a structurally valid set of answers],
      And the request [contains a valid IDAM token for Dil],
      And it is submitted to call the [ValidateNoCQuestions] operation of [Case Assignment Microservice],

     Then a negative response is received,
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-205.9
  Scenario: NoC Answers must return an error when no NOC Request event is available on the case

    Given a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Dil - with a solicitor role for the same jurisdiction within a different organisation from Richard],
      And a case [created by Richard - C1 - on behalf of Mario as the applicant which is auto-assigned to Richard's organisation] created as in [204_NoC_WithoutEvents_Case_Creation_With_Org_Policies],

     When a request is prepared with appropriate values,
      And the request [is made by Dil to validate the answers in order to become Mario's representative on C1],
      And the request [contains all correct answers]
      And the request [identifies that no NOC request event is available on the case],
      And it is submitted to call the [ValidateNoCQuestions] operation of [Case Assignment Microservice],

     Then a negative response is received,
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-205.10
  Scenario: NoC Answers must return an error response when the solicitor does not have access to the jurisdiction of the case

    Given a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Ashley - with a solicitor role for a different jurisdiction from that of the case that Richard and no pui-caa role will create and within a different organisation from Richard],
      And a case [created by Richard - C1 - on behalf of Mario as the applicant which is auto-assigned to Richard's organisation] created as in [204_Case_Creation],
      And a successful call [to get the update org policy event trigger] as in [204_Update_Org_Policies_Token_Creation],
      And a successful call [to update the applicant org policy] as in [204_Update_Org_Policies],

     When a request is prepared with appropriate values,
      And the request [is made by Ashley to validate the answers in order to become Mario's representative on C1],
      And the request [contains a valid case ID for C1],
      And the request [contains a structurally valid set of answers],
      And the request [contains a valid IDAM token for Dil],
      And it is submitted to call the [ValidateNoCQuestions] operation of [Case Assignment Microservice],

     Then a negative response is received,
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-205.12
  Scenario: NoC Answers must return an error response when the the invoking user's organisation is already representing the litigant identified by the answers

    Given a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Dil - with a solicitor role for the same jurisdiction within a different organisation from Richard],
      And a case [created by Richard - C1 - on behalf of Mario as the applicant which is auto-assigned to Richard's organisation] created as in [204_Case_Creation],
      And a successful call [to get the update org policy event trigger] as in [205.12_Update_Org_Policies_Token_Creation],
      And a successful call [to update the applicant org policy so Dil's organisation is representing] as in [205.12_Update_Org_Policies],

     When a request is prepared with appropriate values,
      And the request [is made by Dil to validate the answers provided to the NoC questions in order to become the representative for Mario on C1],
      And the request [contains all correct answers],
      And it is submitted to call the [ValidateNoCQuestions] operation of [Case Assignment Microservice],

     Then a negative response is received,
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-205.13 #Also covers AC8
  Scenario: NoC Answers must return an error when answers do not match a case role in the case to be represented

    Given a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Dil - with a solicitor role for the same jurisdiction within a different organisation from Richard],
      And a case [created by Richard - C1 - on behalf of Mario as the applicant which is auto-assigned to Richard's organisation] created as in [204_Case_Creation],
      And a successful call [to get the update org policy event trigger] as in [204_Update_Org_Policies_Token_Creation],
      And a successful call [to update the applicant org policy] as in [204_Update_Org_Policies_With_Missing_Policy],

     When a request is prepared with appropriate values,
      And the request [is made by Dil to validate the answers provided to the NoC questions in order to become the representative for Mario on C1],
      And the request [contains answers that do not match a case role in the case to be represented],
      And it is submitted to call the [ValidateNoCQuestions] operation of [Case Assignment Microservice],

     Then a negative response is received,
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-205.14
  Scenario: NoC Answers must return an error when the set of answers match more than one corresponding case roles for representation

    Given a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Dil - with a solicitor role for the same jurisdiction within a different organisation from Richard],
      And a case [created by Richard - C1 - on behalf of Mario as the applicant which is auto-assigned to Richard's organisation] created as in [205.14_Case_Creation],
      And a successful call [to get the update org policy event trigger] as in [205.14_Update_Org_Policies_Token_Creation],
      And a successful call [to update the applicant org policy] as in [205.14_update_applicant_org_policy],

     When a request is prepared with appropriate values,
      And the request [is made by Dil to validate the answers provided to the NoC questions in order to become the representative for Mario on C1],
      And the request [contains a set of answers that match more than one corresponding case roles for representation],
      And it is submitted to call the [ValidateNoCQuestions] operation of [Case Assignment Microservice],

     Then a negative response is received,
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  Scenario Outline: NoC Answers must return an error response for an invalid/incorrect answer

     Given a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Dil - with a solicitor role for the same jurisdiction within a different organisation from Richard],
      And a case [created by Richard - C1 - on behalf of Mario as the applicant which is auto-assigned to Richard's organisation] created as in [<case creation data file>],
      And a successful call [to get the update org policy event trigger] as in [<org policy update event trigger data file>],
      And a successful call [to update the applicant org policy] as in [<update org policy event trigger>],

     When a request is prepared with appropriate values,
      And the request [is made by Dil to validate the answers provided to the NoC questions in order to become the representative for Mario on C1],
      And the request [<condition>],
      And it is submitted to call the [ValidateNoCQuestions] operation of [Case Assignment Microservice],

     Then a negative response is received,
      And the response has all the details as expected.


  @S-205.11.1
  Examples:
    | condition                                             | case creation data file | org policy update event trigger data file   | update org policy event trigger |
    | has no answers                                        | 204_Case_Creation       | 204_Update_Org_Policies_Token_Creation      | 204_Update_Org_Policies         |

  @S-205.11.2
  Examples:
    | condition                                             | case creation data file | org policy update event trigger data file   | update org policy event trigger |
    | has too few answers                                   | 204_Case_Creation       | 204_Update_Org_Policies_Token_Creation      | 204_Update_Org_Policies         |

  @S-205.11.3
  Examples:
    | condition                                             | case creation data file | org policy update event trigger data file   | update org policy event trigger |
    | has too many answers                                  | 204_Case_Creation       | 204_Update_Org_Policies_Token_Creation      | 204_Update_Org_Policies         |

  @S-205.11.5
  Examples:
    | condition                                             | case creation data file | org policy update event trigger data file   | update org policy event trigger |
    | has a non-null answer where a null answer is expected | 205.11.5_Case_Creation  | 205.11.5_Update_Org_Policies_Token_Creation | 205.11.5_Update_Org_Policies    |

  @S-205.11.6
  Examples:
    | condition                                             | case creation data file | org policy update event trigger data file   | update org policy event trigger |
    | has a null answer where a non-null answer is expected | 204_Case_Creation       | 204_Update_Org_Policies_Token_Creation      | 204_Update_Org_Policies         |

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-205.15.1 @Ignore
  Scenario: NoC Answers must return a positive response for various correct answers

    Given a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Dil - with a solicitor role for the same jurisdiction within a different organisation from Richard],
      And a case [created by Richard - C1 - on behalf of Mario as the applicant which is auto-assigned to Richard's organisation] created as in [204_Case_Creation],
      And a successful call [to get the update org policy event trigger] as in [204_Update_Org_Policies_Token_Creation],
      And a successful call [to update the applicant org policy] as in [204_Update_Org_Policies],

    When a request is prepared with appropriate values,
      And the request [is made by Dil to validate the answers provided to the NoC questions in order to become the representative for Mario on C1],
      And the request [has answers with extra whitespaces, apostrophes, hyphens and casing that expected],
      And it is submitted to call the [ValidateNoCQuestions] operation of [Case Assignment Microservice],

     Then a positive response is received,
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-205.15.2
  Scenario: NoC Answers must return an positive response for various correct answers

    Given a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Dil - with a solicitor role for the same jurisdiction within a different organisation from Richard],
      And a case [created by Richard - C1 - on behalf of Mario as the applicant which is auto-assigned to Richard's organisation] created as in [205.11.5_Case_Creation],
      And a successful call [to get the update org policy event trigger] as in [205.11.5_Update_Org_Policies_Token_Creation],
      And a successful call [to update the applicant org policy] as in [205.11.5_Update_Org_Policies],

     When a request is prepared with appropriate values,
      And the request [is made by Dil to validate the answers provided to the NoC questions in order to become the representative for Mario on C1],
      And the request [has one null and one non-null answer as expected],
      And it is submitted to call the [ValidateNoCQuestions] operation of [Case Assignment Microservice],

     Then a negative response is received,
      And the response has all the details as expected.

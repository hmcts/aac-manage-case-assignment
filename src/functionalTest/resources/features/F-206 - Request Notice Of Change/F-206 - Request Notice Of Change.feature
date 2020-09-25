#=========================================
@F-206
Feature: F-206: Request Notice Of Change
#=========================================

  Background:
    Given an appropriate test context as detailed in the test data source

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-1
  @S-206.1
  Scenario: (Happy Path) : Solicitor successfully Requests NoC to Replace a litigant's current representation - No auto-approval applies

    Given an appropriate test context as detailed in the test data source,
      And a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Dil - with a solicitor role for the same jurisdiction within a different organisation from Richard],
      And a successful call [by Richard to create a case - C1 - on behalf of Mario as the applicant which is auto-assigned to Richard's organisation] as in [Prerequisite Case Creation Call for Case Assignment],
      And a successful call [is made by Dil to retrieve the NOC questions that must be answered in order to raise a NOC Request to become the representative for Mario on C1],
      And a successful call [is made by Dil to validate the answers provided to the NoC questions in order to become the representative for Mario on C1],

    When a request is prepared with appropriate values,
      And the request [is made by Dil to place an NOC Request in order to become Mario's representative on C1],
      And the request [contains all correct answers in the correct format - e.g. a Date of Birth provided in Date format],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

    Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify that the case is not listed in Dil's organisation] will get the expected response as in [XXX],
      And another call [to verify that Richard now observes a pending NOC request on the case] will get the expected response as in [YYY].

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-2
  @S-206.2
  Scenario: (Happy Path) : Solicitor successfully Requests NoC for a non-represented litigant - No auto-approval applies
    Given an appropriate test context as detailed in the test data source,
      And a user [Mario - to initiate a case on his own behalf via a citizen facing application]
      And a user [Dil - with a solicitor role for the same jurisdiction as that of a case initiated by Mario],
      And a successful call [by Mario to create a case - C1 - via a citizen facing application] as in [Prerequisite Case Creation Call for Case Assignment],
      And a successful call [is made by Dil to retrieve the NOC questions that must be answered in order to raise a NOC Request to become the representative for Mario on C1],
      And a successful call [is made by Dil to validate the answers provided to the NoC questions in order to become the representative for Mario on C1],

    When a request is prepared with appropriate values,
      And the request [is made by Dil to place an NOC Request in order to become Mario's representative on C1],
      And the request [contains all correct answers in the correct format - e.g. a Date of Birth provided in Date format],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

    Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify that the case is not listed in Dil's organisation] will get the expected response as in [XXX],
      And another call [to verify that Mario now observes a pending NOC request on the case] will get the expected response as in [YYY],
      And another call [to verify that a caseworker in C1's jurisdiction now observes a pending NOC request on the case] will get the expected response as in [YYY].

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-3
  @S-206.3
  Scenario: (Happy Path) : Case Access Administrator successfully Requests NoC to Replace a litigant's current representation - No auto-approval applies
    Given an appropriate test context as detailed in the test data source,
      And a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Dil - with a pui-caa role for the same jurisdiction within a different organisation from Richard],
      And a successful call [by Richard to create a case - C1 - on behalf of Mario as the applicant which is auto-assigned to Richard's organisation] as in [Prerequisite Case Creation Call for Case Assignment],
      And a successful call [is made by Dil to retrieve the NOC questions that must be answered in order to raise a NOC Request to become the representative for Mario on C1],
      And a successful call [is made by Dil to validate the answers provided to the NoC questions in order to become the representative for Mario on C1],

    When a request is prepared with appropriate values,
      And the request [is made by Dil to place an NOC Request in order to become Mario's representative on C1],
      And the request [contains all correct answers in the correct format - e.g. a Date of Birth provided in Date format],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

    Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify that the case is not listed in Dil's organisation] will get the expected response as in [XXX],
      And another call [to verify that Richard now observes a pending NOC request on the case] will get the expected response as in [YYY].

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-4
  @S-206.4
  Scenario: (Happy Path) : Case Access Administrator successfully Requests NoC for a non-represented litigant - No auto-approval applies
    Given an appropriate test context as detailed in the test data source,
      And a user [Mario - to initiate a case on his own behalf via a citizen facing application]
      And a user [Dil - with a pui-caa role for the same jurisdiction as that of a case initiated by Mario],
      And a successful call [by Mario to create a case - C1 - via a citizen facing application] as in [Prerequisite Case Creation Call for Case Assignment],
      And a successful call [is made by Dil to retrieve the NOC questions that must be answered in order to raise a NOC Request to become the representative for Mario on C1],
      And a successful call [is made by Dil to validate the answers provided to the NoC questions in order to become the representative for Mario on C1],

    When a request is prepared with appropriate values,
      And the request [is made by Dil to place an NOC Request in order to become Mario's representative on C1],
      And the request [contains all correct answers in the correct format - e.g. a Date of Birth provided in Date format],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

    Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify that the case is not listed in Dil's organisation] will get the expected response as in [XXX],
      And another call [to verify that Mario now observes a pending NOC request on the case] will get the expected response as in [YYY],
      And another call [to verify that a caseworker in C1's jurisdiction now observes a pending NOC request on the case] will get the expected response as in [YYY].

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-5
  @S-206.5
  Scenario: (Happy path) : Case Access Administrator (with a solicitor role for a different jurisdiction to that of the case) successfully Requests NoC to Replace a litigant's current representation - No auto-approval applies
    Given an appropriate test context as detailed in the test data source,
      And a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Dil - with a caseworker-pui role and a solicitor role for a jurisdiction that does not match Case C1 below],
      And a successful call [by Richard to create a case - C1 - on behalf of Mario as the applicant which is auto-assigned to Richard's organisation] as in [Prerequisite Case Creation Call for Case Assignment],
      And a successful call [is made by Dil to retrieve the NOC questions that must be answered in order to raise a NOC Request to become the representative for Mario on C1],
      And a successful call [is made by Dil to validate the answers provided to the NoC questions in order to become the representative for Mario on C1],

    When a request is prepared with appropriate values,
      And the request [is made by Dil to place an NOC Request in order to become Mario's representative on C1],
      And the request [contains all correct answers in the correct format - e.g. a Date of Birth provided in Date format],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

    Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify that the case is not listed in Dil's organisation] will get the expected response as in [XXX],
      And another call [to verify that Richard now observes a pending NOC request on the case] will get the expected response as in [YYY].

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-6
  @S-206.6
  Scenario: (Happy path) : Case Access Administrator (with a solicitor role for a different jurisdiction to that of the case)successfully Requests NoC for a non-represented litigant - No auto-approval applies
    Given an appropriate test context as detailed in the test data source,
      And a user [Mario - to initiate a case on his own behalf via a citizen facing application]
      And a user [Dil - with a caseworker-pui role and a solicitor role for a jurisdiction that does not match Case C1 below],
      And a successful call [by Mario to create a case - C1 - via a citizen facing application] as in [Prerequisite Case Creation Call for Case Assignment],
      And a successful call [is made by Dil to retrieve the NOC questions that must be answered in order to raise a NOC Request to become the representative for Mario on C1],
      And a successful call [is made by Dil to validate the answers provided to the NoC questions in order to become the representative for Mario on C1],

    When a request is prepared with appropriate values,
      And the request [is made by Dil to place an NOC Request in order to become Mario's representative on C1],
      And the request [contains all correct answers in the correct format - e.g. a Date of Birth provided in Date format],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

    Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify that the case is not listed in Dil's organisation] will get the expected response as in [XXX],
      And another call [to verify that Mario now observes a pending NOC request on the case] will get the expected response as in [YYY],
      And another call [to verify that a caseworker in C1's jurisdiction now observes a pending NOC request on the case] will get the expected response as in [YYY].

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-7
  @S-206.7
  Scenario: Must return an error for a missing Case ID
    Given an appropriate test context as detailed in the test data source,
      And a user [Dil - with a solicitor role],

    When a request is prepared with appropriate values,
      And the request [is made by Dil to place an NoC request in order to become a representative on a valid case number for which the case does not exist],
      And the request [does not contain any case ID],
      And the request [contains a structurally valid set of answers],
      And the request [contains a valid IDAM token for Dil],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

    Then a negative response is received,
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-8
  @S-206.8
  Scenario: Must return an error for a malformed Case ID
    Given an appropriate test context as detailed in the test data source,
      And a user [Dil - with a solicitor and a pui-caseworker role],

    When a request is prepared with appropriate values,
      And the request [is made by Dil to place an NoC request in order to become a representative on the case],
      And the request [contains a malformed case ID for C1],
      And the request [contains a structurally valid set of answers],
      And the request [contains a valid IDAM token for Dil],
      And it is submitted to call the [NoCRequest] operation of [Case Assignment Microservice],

    Then a negative response is received,
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-9
  @S-206.9
  Scenario: must return an error response for a non-extant Case ID
    Given an appropriate test context as detailed in the test data source,
      And a user [Dil - with a solicitor and pui-caseworker role],

    When a request is prepared with appropriate values,
      And the request [is made by Dil to place an NoC request in order to become a representative on a valid case number for which the case does not exist],
      And the request [contains a non-extant case ID for which there is no corresponding case record],
      And the request [contains a structurally valid set of answers],
      And the request [contains a valid IDAM token for Dil],
      And it is submitted to call the [NoCRequest] operation of [Case Assignment Microservice],

    Then a negative response is received,
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-10
  @S-206.10
  Scenario: Must return an error response when the solicitor does not have access to the jurisdiction of the case
    Given an appropriate test context as detailed in the test data source,
      And a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Dil - with a solicitor role for a different jurisdiction from that of the case that Richard will create and within a different organisation from Richard],
      And a successful call [by Richard to create a case - C1 - on behalf of Mario as the applicant which is auto-assigned to Richard's organisation] as in [Prerequisite Case Creation Call for Case Assignment],
      And a successful call [is made by Dil to retrieve the NOC questions that must be answered in order to raise a NOC Request to become the representative for Mario on C1],
      And a successful call [is made by Dil to validate the answers provided to the NoC questions in order to become the representative for Mario on C1],

    When a request is prepared with appropriate values,
      And the request [is made by Dil to place an NOC Request in order to become Mario's representative on C1],
      And the request [contains a valid case ID for C1],
      And the request [contains a structurally valid set of answers],
      And the request [contains a valid IDAM token for Dil],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

    Then a negative response is received,
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-11
  @S-206.11
  Scenario: Must return an error when no NOC Request event is available on the case
    Given an appropriate test context as detailed in the test data source,
      And a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Dil - with a solicitor role for the same jurisdiction within a different organisation from Richard],
      And a successful call [by Richard to create a case - C1 - on behalf of Mario as the applicant which is auto-assigned to Richard's organisation] as in [Prerequisite Case Creation Call for Case Assignment],

    When a request is prepared with appropriate values,
      And the request [is made by Dil to place an NOC Request in order to become Mario's representative on C1],
      And the request [contains a valid case ID for C1],
      And the request [contains a structurally valid set of answers],
      And the request [contains a valid IDAM token for Dil],
      And the request [identifies that no NOC request event is available on the case]
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

    Then a negative response is received,
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-12
  @S-206.12
  Scenario: Must return an error when a NOC Request is made but there is an ongoing NOC Request on the case
    Given an appropriate test context as detailed in the test data source,
      And a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Dil - with a solicitor role for the same jurisdiction within a different organisation from Richard],
      And a user [Nigel - with a solicitor role for the same jurisdiction within a different organisation from Richard and from Dil],
      And a successful call [by Richard to create a case - C1 - on behalf of Mario as the applicant which is auto-assigned to Richard's organisation] as in [Prerequisite Case Creation Call for Case Assignment],
      And a successful call [is made by Dil to retrieve the NOC questions that must be answered in order to raise a NOC Request to become the representative for Mario on C1],
      And a successful call [is made by Dil to validate the answers provided to the NoC questions in order to become the representative for Mario on C1],
      And a successful call [is made by Dil to place an NOC Request in order to become Mario's representative on C1],

    When a request is prepared with appropriate values,
      And the request [is made by Nigel to place an NOC Request in order to become Mario's representative on C1],
      And the request [contains a valid case ID],
      And the request [contains a structurally valid set of answers],
      And the request [contains a valid IDAM token for Nigel],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

    Then a negative response is received,
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-13
  @S-206.13
  Scenario: Must return an error if there is not an Organisation Policy field containing a case role for each set of answers
    Given an appropriate test context as detailed in the test data source,
      And a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Dil - with a solicitor role for the same jurisdiction as Case C1 below],
      And a successful call [by Richard to create a case - C1 - on behalf of Mario as the applicant which is auto-assigned to Richard's organisation] as in [Prerequisite Case Creation Call for Case Assignment],
      And [The challenge questions have answers that resolve to three distinct case roles (1,2,3)]
      And [The configuration of the case creation event only establishes organisation policies containing case roles 1 and 2]

    When a request is prepared with appropriate values,
      And the request [is made by Dil to place a NoC Request in order to become Mario's representative on C1],
      And the request [contains a valid case ID for C1],
      And the request [contains a structurally valid set of answers],
      And the request [contains a valid IDAM token for Dil],
      And it is submitted to call the [Request NoC] operation of [Case Assignment Microservice],

    Then a negative response is received,
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-14
  @S-206.14
  Scenario: Must return an error when answers do not match a case role in the case to be represented
    Given an appropriate test context as detailed in the test data source,
      And a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Dil - with a solicitor role for the same jurisdiction within a different organisation from Richard],
      And a successful call [by Richard to create a case - C1 - on behalf of Mario as the applicant which is auto-assigned to Richard's organisation] as in [Prerequisite Case Creation Call for Case Assignment],

    When a request is prepared with appropriate values,
      And the request [is made by Dil to place an NOC Request in order to become Mario's representative on C1],
      And the request [contains answers that do not match a case role in the case to be represented],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

    Then a negative response is received,
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-15
  @S-206.15
  Scenario: Must return an error when the set of answers do not match any corresponding case roles for representation
    Given an appropriate test context as detailed in the test data source,
      And a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Dil - with a solicitor role for the same jurisdiction within a different organisation from Richard],
      And a successful call [by Richard to create a case - C1 - on behalf of Mario as the applicant which is auto-assigned to Richard's organisation] as in [Prerequisite Case Creation Call for Case Assignment],

    When a request is prepared with appropriate values,
      And the request [is made by Dil to place an NOC Request in order to become Mario's representative on C1],
      And the request [contains a set of answers that do not match any case roles for representation],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

    Then a negative response is received,
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-16
  @S-206.16
  Scenario: Must return an error when the set of answers match more than one corresponding case roles for representation
    Given an appropriate test context as detailed in the test data source,
      And a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Dil - with a solicitor role for the same jurisdiction within a different organisation from Richard],
      And a successful call [by Richard to create a case - C1 - on behalf of Mario as the applicant which is auto-assigned to Richard's organisation] as in [Prerequisite Case Creation Call for Case Assignment],

    When a request is prepared with appropriate values,
      And the request [is made by Dil to place an NOC Request in order to become Mario's representative on C1],
      And the request [contains a set of answers that match more than one corresponding case roles for representation],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

    Then a negative response is received,
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-17
  @S-206.17
  Scenario: Must return an error response for an invalid/incorrect answer
    Given an appropriate test context as detailed in the test data source,
      And a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Dil - with a solicitor role for the same jurisdiction within a different organisation from Richard],
      And a successful call [by Richard to create a case - C1 - on behalf of Mario as the applicant which is auto-assigned to Richard's organisation] as in [Prerequisite Case Creation Call for Case Assignment],
      And a successful call [is made by Dil to retrieve the NOC questions that must be answered in order to raise a NOC Request to become the representative for Mario on C1],
      And a successful call [is made by Dil to place an NOC Request in order to become the representative for Mario on C1],

    When a request is prepared with appropriate values,
      And the request [is made by Dil to validate the answers provided to the NoC questions in order to become the representative for Mario on C1],
      And the request [contains a valid case ID for C1],
      And the request [contains a valid IDAM token for Dil],
      And the request [contains any invalid/incorrect answer(s) to any of the question(s) - see Notes section below for definition of an invalid/incorrect answer],
      And it is submitted to call the [Request NoC] operation of [Case Assignment Microservice],

    Then a negative response is received,
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-18
  @S-206.18
  Scenario: Must return an error response when the invoking user's organisation is already representing the litigant identified by the answers
    Given an appropriate test context as detailed in the test data source,
      And a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Dil - with a solicitor role for the same jurisdiction within the same organisation as Richard],
      And a successful call [by Richard to create a case - C1 - on behalf of Mario as the applicant which is auto-assigned to Richard's organisation] as in [Prerequisite Case Creation Call for Case Assignment],

    When a request is prepared with appropriate values,
      And the request [is made by Dil to place an NOC Request in order to become Mario's representative on C1],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

    Then a negative response is received,
      And the response has all the details as expected.

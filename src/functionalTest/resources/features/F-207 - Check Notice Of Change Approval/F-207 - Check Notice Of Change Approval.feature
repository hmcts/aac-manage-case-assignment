#=============================================
@F-207
Feature: F-207: Check for Notice of Change Approval
#=============================================

Background:
    Given an appropriate test context as detailed in the test data source

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-85 / AC-1
@S-207.1 @callbackTests
Scenario: (Happy Path) Successful verification that checks a NoCRequest has been auto-approved (Solicitor)

  Given a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
  And a user [system user - with caseworker-caa role],
  And a successful call [by Richard to create a case C1 which contains a role R1 which is assigned to Dil's organisation] as in [S-207.1-CaseCreation_NoCRequest_Approved],

  When a request is prepared with appropriate values,
  And the request [contains the case record for C1],
  And the request [is made by the system user (caseworker-caa IdAM role) to simulate a submitted callback from a NOC Request case event that has been auto-approved],
  And it is submitted to call the [Check NoC Approval] operation of [Manage Case Assignment Microservice],

  Then a positive response is received,
  And the response has all the details as expected
  And a call [to verify that the NoCDecision event has been successfully created] will get the expected response as in [F-207_Verify_NoCDecision_Event_Created]

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-85 / AC-2
  @S-207.2 @callbackTests
  Scenario: (Happy Path) Successful verification that checks a NoCRequest has been auto-approved - Case has not been configured with auto-approval (0).

    Given a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
    And a user [system user - with caseworker-caa role],
    And a successful call [by Richard to create a case C1 which contains a role R1 which is assigned to Dil's organisation] as in [S-207.2-CaseCreation_NoCRequest_Pending],

    When a request is prepared with appropriate values,
    And the request [contains the case record for C1],
    And the request [is made by the system user (caseworker-caa IdAM role) to simulate a submitted callback from a NOC Request case event that has not been auto-approved],
    And it is submitted to call the [Check NoC Approval] operation of [Manage Case Assignment Microservice],

    Then a positive response is received,
    And the response has all the details as expected
    And a call [to verify that the NoCDecision event has not been successfully created] will get the expected response as in [F-207_Verify_NoCDecision_Event_Not_Created]

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-85 / AC-3
  @S-207.3
  Scenario:  Must return error if approval status is not configured at all (empty / null)

    Given an appropriate test context as detailed in the test data source,
    And a user [system user - with caseworker-caa role],

    When a request is prepared with appropriate values,
    And the request [contains a malformed case record],
    And the request [is made by the system user (caseworker-caa IdAM role) to simulate a submitted callback from a NOC Request case event that has null approvalStatus],
    And it is submitted to call the [Check NoC Approval] operation of [Manage Case Assignment Microservice],

    Then a negative response is received,
    And the response has all the details as expected

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-85
  @S-207.4
  Scenario: Must return an error response for a malformed Case ID

    Given an appropriate test context as detailed in the test data source,
    And a user [system user - with caseworker-caa role],

    When a request is prepared with appropriate values,
    And the request [contains a malformed case record],
    And the request [is made by the system user (caseworker-caa IdAM role) to simulate a submitted callback from a NOC Request case event that has been auto-approved],
    And it is submitted to call the [Check NoC Approval] operation of [Manage Case Assignment Microservice],

    Then a negative response is received,
    And the response has all the details as expected

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-85
  @S-207.5
  Scenario: Must return an error response for a non-extant Case ID

    Given an appropriate test context as detailed in the test data source,
    And a user [system user - with caseworker-caa role],

    When a request is prepared with appropriate values,
    And the request [contains a non extant case record],
    And the request [is made by the system user (caseworker-caa IdAM role) to simulate a submitted callback from a NOC Request case event that has been auto-approved],
    And it is submitted to call the [Check NoC Approval] operation of [Manage Case Assignment Microservice],

    Then a negative response is received,
    And the response has all the details as expected


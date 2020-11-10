#=============================================
@F-207
Feature: F-207: Set Organisation To Remove
#=============================================

  Background:
    Given an appropriate test context as detailed in the test data source

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-87 / AC-1
@S-207.1
Scenario: (Happy Path) Successfully set up the OrganisationToRemove in the ChangeOrganisationRequest and return the updated case record for a Remove event

    Given a request is prepared with appropriate values,
      And the request [intends to set up the OrganisationToRemove in the ChangeOrganisationRequest on behalf of the user],
      And the request [contains a case record containing exactly one OrganisationPolicy.OrgPolicyCaseAssignedRole matching the ChangeOrganisationRequest.CaseRole],
      And it is submitted to call the [SetOrganisationToRemove] operation of [Manage Case Assignment Microservice],

    Then a positive response is received,
      And the response has all the details as expected,
      And the response [includes ChangeOrganisationRequest.OrganisationToRemove is equal to the organisation ID in the Organisation policy that has a OrgPolicyCaseAssignedRole matching the COR.CaseRole] will get the expected response as in [YYY].


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-87 / AC-2
@S-207.2
Scenario: Must return an error for a non extant case record

    Given a successful call [by Dil - a solicitor who has triggered a NoC Request which contains answers identifying a case role on a case - C1],
      And a successful call [is made to verify that the NoCRequest has been approved],

    When a request is prepared with appropriate values,
      And the request [intends to set up the OrganisationToRemove in the ChangeOrganisationRequest on behalf of the user],
      And the request [does not contain a case record],
      And it is submitted to call the [SetOrganisationToRemove] operation of [Manage Case Assignment Microservice],

    Then a negative response is received,
      And the response has all the details as expected.


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-87 / AC-3
@S-207.3
Scenario: Must return error if the ChangeOrganisationRequest.CaseRole is missing

    Given a request is prepared with appropriate values,
      And the request [intends to set up the OrganisationToRemove in the ChangeOrganisationRequest on behalf of the user],
      And the request [contains a case record for which the ChangeOrganisationRequest.CaseRole is missing],
      And it is submitted to call the [SetOrganisationToRemove] operation of [Manage Case Assignment Microservice],

    Then a negative response is received,
      And the response has all the details as expected.


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-87 / AC-4
@S-207.4
Scenario: Must return error if the ChangeOrganisationRequest.CaseRole is null

    Given an appropriate test context as detailed in the test data source,

    When a request is prepared with appropriate values,
      And the request [intends to set up the OrganisationToRemove in the ChangeOrganisationRequest on behalf of the user],
      And the request [contains a case record for which the ChangeOrganisationRequest.CaseRole is null],
      And it is submitted to call the [SetOrganisationToRemove] operation of [Manage Case Assignment Microservice],

    Then a negative response is received,
      And the response has all the details as expected.


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-87 / AC-5
@S-207.5
Scenario: Must return error if there is more then one OrganisationPolicy CaseRole on the case which matches the ChangeOrganisationRequest.CaseRole

    Given an appropriate test context as detailed in the test data source,

    When a request is prepared with appropriate values,
      And the request [intends to set up the OrganisationToRemove in the ChangeOrganisationRequest on behalf of the user],
      And the request [contains a case record containing more than one OrganisationPolicy.OrgPolicyCaseAssignedRole matching the ChangeOrganisationRequest.CaseRole],
      And it is submitted to call the [SetOrganisationToRemove] operation of [Manage Case Assignment Microservice],

    Then a negative response is received,
      And the response has all the details as expected.


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-87 / AC-6
@S-207.6
Scenario: Must return error if no OrganisationPolicy CaseRole on the case matches the ChangeOrganisationRequest.CaseRole

    Given an appropriate test context as detailed in the test data source,

    When a request is prepared with appropriate values,
      And the request [intends to set up the OrganisationToRemove in the ChangeOrganisationRequest on behalf of the user],
      And the request [does not contain a case record for the OrganisationPolicy.OrgPolicyCaseAssignedRole matching the ChangeOrganisationRequest.CaseRole],
      And it is submitted to call the [SetOrganisationToRemove] operation of [Manage Case Assignment Microservice],

    Then a negative response is received,
      And the response has all the details as expected.


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-87 / AC-7
@S-207.7
Scenario: Must return an error for a malformed Case ID

    Given a successful call [by Dil - a solicitor who has triggered a NoC Request which contains answers identifying a case role on a case - C1],
      And a successful call [is made to verify that the NoCRequest has been approved],

    When a request is prepared with appropriate values,
      And the request [intends to set up the OrganisationToRemove in the ChangeOrganisationRequest on behalf of the user],
      And the request [contains a malformed 16 digit case ID number],
      And it is submitted to call the [SetOrganisationToRemove] operation of [Manage Case Assignment Microservice],

    Then a negative response is received,
      And the response has all the details as expected.

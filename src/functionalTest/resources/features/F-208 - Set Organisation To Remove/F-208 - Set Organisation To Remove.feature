#=============================================
@F-208
Feature: F-208: Set Organisation To Remove
#=============================================

  Background:
    Given an appropriate test context as detailed in the test data source

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-87 / AC-1
@S-208.1
Scenario: (Happy Path) Successfully set up the OrganisationToRemove in the ChangeOrganisationRequest and return the updated case record for a Remove event

    Given a user [system_user - with caseworker-caa IdAM role],

    When a request is prepared with appropriate values,
      And the request [intends to set up the OrganisationToRemove in the ChangeOrganisationRequest on behalf of the user],
      And the request [contains a case record containing exactly one OrganisationPolicy.OrgPolicyCaseAssignedRole matching the ChangeOrganisationRequest.CaseRole],
      And it is submitted to call the [SetOrganisationToRemove] operation of [Manage Case Assignment Microservice],

    Then a positive response is received,
      And the response has all the details as expected,
      And the response [includes ChangeOrganisationRequest.OrganisationToRemove is equal to the organisation ID in the Organisation policy that has a OrgPolicyCaseAssignedRole matching the COR.CaseRole].


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-87 / AC-2
@S-208.2
Scenario: Must return an error for a non extant case record

    Given a user [system_user - with caseworker-caa IdAM role],

    When a request is prepared with appropriate values,
      And the request [intends to set up the OrganisationToRemove in the ChangeOrganisationRequest on behalf of the user],
      And the request [does not contain a case record],
      And it is submitted to call the [SetOrganisationToRemove] operation of [Manage Case Assignment Microservice],

    Then a negative response is received,
      And the response has all the details as expected.


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-87 / AC-3
@S-208.3
Scenario: Must return error if the ChangeOrganisationRequest.CaseRole is missing

    Given a user [system_user - with caseworker-caa IdAM role],

    When a request is prepared with appropriate values,
      And the request [intends to set up the OrganisationToRemove in the ChangeOrganisationRequest on behalf of the user],
      And the request [contains a case record for which the ChangeOrganisationRequest.CaseRole is missing],
      And it is submitted to call the [SetOrganisationToRemove] operation of [Manage Case Assignment Microservice],

    Then a negative response is received,
      And the response has all the details as expected.


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-87 / AC-4
@S-208.4
Scenario: Must return error if the ChangeOrganisationRequest.CaseRole is null

    Given a user [system_user - with caseworker-caa IdAM role],

    When a request is prepared with appropriate values,
      And the request [intends to set up the OrganisationToRemove in the ChangeOrganisationRequest on behalf of the user],
      And the request [contains a case record for which the ChangeOrganisationRequest.CaseRole is null],
      And it is submitted to call the [SetOrganisationToRemove] operation of [Manage Case Assignment Microservice],

    Then a negative response is received,
      And the response has all the details as expected.


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-87 / AC-5
@S-208.5
Scenario: Must return error if there is more than one OrganisationPolicy CaseRole on the case which matches the ChangeOrganisationRequest.CaseRole

    Given a user [system_user - with caseworker-caa IdAM role],

    When a request is prepared with appropriate values,
      And the request [intends to set up the OrganisationToRemove in the ChangeOrganisationRequest on behalf of the user],
      And the request [contains a case record containing more than one OrganisationPolicy.OrgPolicyCaseAssignedRole matching the ChangeOrganisationRequest.CaseRole],
      And it is submitted to call the [SetOrganisationToRemove] operation of [Manage Case Assignment Microservice],

    Then a negative response is received,
      And the response has all the details as expected.


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-87 / AC-6
@S-208.6
Scenario: Must return error if no OrganisationPolicy CaseRole on the case matches the ChangeOrganisationRequest.CaseRole

    Given a user [system_user - with caseworker-caa IdAM role],

    When a request is prepared with appropriate values,
      And the request [intends to set up the OrganisationToRemove in the ChangeOrganisationRequest on behalf of the user],
      And the request [does not contain a case record for the OrganisationPolicy.OrgPolicyCaseAssignedRole matching the ChangeOrganisationRequest.CaseRole],
      And it is submitted to call the [SetOrganisationToRemove] operation of [Manage Case Assignment Microservice],

    Then a negative response is received,
      And the response has all the details as expected.


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-87 / AC-7
@S-208.7
Scenario: Must return an error for a malformed Case ID

    Given a user [system_user - with caseworker-caa IdAM role],

    When a request is prepared with appropriate values,
      And the request [intends to set up the OrganisationToRemove in the ChangeOrganisationRequest on behalf of the user],
      And the request [contains a malformed 16 digit case ID number],
      And it is submitted to call the [SetOrganisationToRemove] operation of [Manage Case Assignment Microservice],

    Then a negative response is received,
      And the response has all the details as expected.

#=================================================
@F-210
Feature: F-210: Validate Case User Organisation Boundary
#=================================================

  Background:
    Given an appropriate test context as detailed in the test data source

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-210.1
  Scenario: Must return an error response if POST /case-users uses an organisation_id different from the caller PRD organisation

    Given a user [CAA - with a PRD organisation],

     When a request is prepared with appropriate values,
      And the request [contains a valid case-user assignment with a mismatched organisation_id],
      And it is submitted to call the [Add Case-Assigned Users and Roles] operation of [CCD Data Store],

     Then a negative response is received,
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-210.2
  Scenario: Must allow POST /case-users when organisation_id matches the caller PRD organisation

    Given a user [CAA - with a PRD organisation],

     When a request is prepared with appropriate values,
      And the request [contains a valid case-user assignment with a matching organisation_id],
      And it is submitted to call the [Add Case-Assigned Users and Roles] operation of [CCD Data Store],

     Then a positive response is received,
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-210.3
  Scenario: Must return an error response if POST /case-users omits organisation_id

    Given a user [CAA - with a PRD organisation],

     When a request is prepared with appropriate values,
      And the request [contains a valid case-user assignment without organisation_id],
      And it is submitted to call the [Add Case-Assigned Users and Roles] operation of [CCD Data Store],

     Then a negative response is received,
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-210.4
  Scenario: Must return an error response if POST /case-users uses the caller organisation_id for a case whose organisation policies do not contain the caller organisation

    Given a user [CAA - with a PRD organisation],

     When a request is prepared with appropriate values,
      And the request [contains a valid case-user assignment with a matching organisation_id for a case outside the caller organisation policies],
      And it is submitted to call the [Add Case-Assigned Users and Roles] operation of [CCD Data Store],

     Then a negative response is received,
      And the response has all the details as expected.

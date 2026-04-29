#=================================================
@F-211
Feature: F-211: Validate CCD Data Store Case User Contract
#=================================================

  Background:
    Given an appropriate test context as detailed in the test data source

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-211.1
  Scenario: Must allow direct POST /case-users when organisation_id differs from the caller PRD organisation

    Given a user [CAA - with a PRD organisation],

     When a request is prepared with appropriate values,
      And the request [contains a valid case-user assignment with a mismatched organisation_id],
      And it is submitted to call the [Add Case-Assigned Users and Roles] operation of [CCD Data Store],

     Then a positive response is received,
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-211.2
  Scenario: Must allow direct POST /case-users when organisation_id matches the caller PRD organisation

    Given a user [CAA - with a PRD organisation],

     When a request is prepared with appropriate values,
      And the request [contains a valid case-user assignment with a matching organisation_id],
      And it is submitted to call the [Add Case-Assigned Users and Roles] operation of [CCD Data Store],

     Then a positive response is received,
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-211.3
  Scenario: Must allow direct POST /case-users when organisation_id is omitted

    Given a user [CAA - with a PRD organisation],

     When a request is prepared with appropriate values,
      And the request [contains a valid case-user assignment without organisation_id],
      And it is submitted to call the [Add Case-Assigned Users and Roles] operation of [CCD Data Store],

     Then a positive response is received,
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  @S-211.4
  Scenario: Must allow direct POST /case-users when organisation_id matches the caller organisation for a case whose organisation policies do not contain the caller organisation

    Given a user [CAA - with a PRD organisation],

     When a request is prepared with appropriate values,
      And the request [contains a valid case-user assignment with a matching organisation_id for a case outside the caller organisation policies],
      And it is submitted to call the [Add Case-Assigned Users and Roles] operation of [CCD Data Store],

     Then a positive response is received,
      And the response has all the details as expected.

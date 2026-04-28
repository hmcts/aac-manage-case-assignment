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

#================================================
@F-210 @CCD-8333
Feature: F-210: Gateway response integrity
#================================================

  Background:
    Given an appropriate test context as detailed in the test data source

  @S-210.1 @Smoke
  Scenario: A compressible Data Store response is returned completely through the MCA gateway

    Given a user [S1 - a solicitor creating a case used to verify gateway response integrity],
      And a case [C1 producing a complete search response] created as in [F-210_Prerequisite_Case_Creation],
      And a wait time of [5] seconds [to allow for Logstash to index the case just created],

    When a request is prepared with appropriate values,
      And the request [searches for C1 through the MCA Data Store gateway],
      And it is submitted to call the [Search cases through gateway] operation of [Manage Case Assignment Microservice],

    Then a positive response is received,
      And the response has all other details as expected.

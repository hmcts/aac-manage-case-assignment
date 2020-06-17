@F-001 @Ignore
Feature: F-001: Assign Access within Organisation

  Background:
    Given an appropriate test context as detailed in the test data source

  @S-001 @Ignore
  Scenario: [SAMPLE] must return a successful response from the health endpoint
    When  a request is prepared with appropriate values,
    And   it is submitted to call the [Health Endpoint] operation of [Manage Case Assignment API],
    Then  a positive response is received,
    And   the response [has the 200 OK code],
    And   the response has all other details as expected.

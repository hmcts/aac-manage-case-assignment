#=================================================
@F-202
Feature: F-202: Get Assignments in My Organisation
#=================================================

Background:
    Given an appropriate test context as detailed in the test data source

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-48 / AC-1
@S-202.1
Scenario: Must return case assignments in my organisation for the provided Case IDs

    Given a user [Becky – a solicitor with the required permissions to create a case],
      And a user [Benjamin – another solicitor within the same organisation as Becky],
      And a user [Bill – another solicitor within the same organisation as Becky],
      And a case [C1, Becky has just] created as in [F-202_Prerequisite_Case_Creation_C1],
      And a case [C2, Becky has just] created as in [F-202_Prerequisite_Case_Creation_C2],
      And a wait time of [10] seconds [to allow for the cases just created to appear in search results],
      And a successful call [to the ‘Assign Access within Organisation’ operation of Case Assignment Microservice assigning Benjamin access to C1] as in [F-202_Prerequisite_Case_Assignment_C1_Benjamin],
      And a successful call [to the ‘Assign Access within Organisation’ operation of Case Assignment Microservice assigning Bill access to C2] as in [F-202_Prerequisite_Case_Assignment_C2_Bill],

     When a request is prepared with appropriate values,
      And the request [is made by Becky],
      And the request [contains a correctly-formed comma separated list of the valid case ID’s of C1 and C2],
      And it is submitted to call the [Get Assignments in My Organisation] operation of [Manage Case Assignment Microservice],

     Then a positive response is received,
      And the response [includes the respective case assignments of user ID's of both Benjamin and Bill and cases C1 and C2],
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-48 / AC-2
@S-202.2
Scenario: Must return an error response when a malformed case ID is provided

    Given a user [Becky – a solicitor with the required permissions to create a case],
      And a user [Benjamin – another solicitor within the same organisation as Becky],
      And a user [Bill – another solicitor within the same organisation as Becky],
      And a case [C1, Becky has just] created as in [F-202_Prerequisite_Case_Creation_C1],
      And a case [C2, Becky has just] created as in [F-202_Prerequisite_Case_Creation_C2],
      And a wait time of [10] seconds [to allow for the case just created to appear in search results],
      And a successful call [to the ‘Assign Access within Organisation’ operation of Case Assignment Microservice assigning Benjamin access to C1] as in [F-202_Prerequisite_Case_Assignment_C1_Benjamin],
      And a successful call [to the ‘Assign Access within Organisation’ operation of Case Assignment Microservice assigning Bill access to C2] as in [F-202_Prerequisite_Case_Assignment_C2_Bill],

     When a request is prepared with appropriate values,
      And the request [is made by Becky],
      And the request [contains a correctly-formed comma separated list of the case ID’s which includes the valid case IDs of C1 and C2 plus another case ID which is malformed],
      And it is submitted to call the [Get Assignments in My Organisation] operation of [Manage Case Assignment Microservice],

     Then a negative response is received,
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-48 / AC-3
@S-202.3
Scenario: Must return an error response for a missing case ID

    Given a user [Becky – with a solicitor role with access to invoke the ‘Get Assignments in My Organisation’ operation of Case Assignment Microservice],

     When a request is prepared with appropriate values,
      And the request [is made by Becky],
      And the request [does not contain any case ID],
      And it is submitted to call the [Get Assignments in My Organisation] operation of [Manage Case Assignment Microservice],

     Then a negative response is received,
      And the response has all the details as expected.

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-48 / AC-4
@S-202.4
Scenario: Must return an error response for a malformed Case ID List

    Given a user [Becky – a solicitor with the required permissions to create a case],
      And a user [Benjamin – another solicitor within the same organisation as Becky],
      And a user [Bill – another solicitor within the same organisation as Becky],
      And a user [Emma – another solicitor within the same organisation as Becky],
      And a case [C1, Becky has just] created as in [F-202_Prerequisite_Case_Creation_C1],
      And a case [C2, Becky has just] created as in [F-202_Prerequisite_Case_Creation_C2],
      And a case [C3, Becky has just] created as in [F-202_Prerequisite_Case_Creation_C3],
      And a wait time of [10] seconds [to allow for the case just created to appear in search results],
      And a successful call [to the ‘Assign Access within Organisation’ operation of Case Assignment Microservice assigning Benjamin access to C1] as in [F-202_Prerequisite_Case_Assignment_C1_Benjamin],
      And a successful call [to the ‘Assign Access within Organisation’ operation of Case Assignment Microservice assigning Bill access to C2] as in [F-202_Prerequisite_Case_Assignment_C2_Bill],
      And a successful call [to the ‘Assign Access within Organisation’ operation of Case Assignment Microservice assigning Emma access to C3] as in [F-202_Prerequisite_Case_Assignment_C3_Emma],

     When a request is prepared with appropriate values,
      And the request [is made by Becky],
      And the request [contains the IDs of C1, C2 & C3 along with an empty element in the comma-separated list of case ID's],
      And it is submitted to call the [Get Assignments in My Organisation] operation of [Manage Case Assignment Microservice],

     Then a negative response is received,
      And the response has all the details as expected.
#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

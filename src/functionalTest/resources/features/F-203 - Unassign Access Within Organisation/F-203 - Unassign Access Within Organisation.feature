#=================================================
@F-203
Feature: F-203: Unassign Access Within Organisation
#=================================================

Background:
    Given an appropriate test context as detailed in the test data source

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-51 A/C 1
@S-203.1
Scenario: Solicitor successfully removing case access for another solicitor in their org (happy path)

    Given a user [Becky - with a Solicitor role for a particular jurisdiction under an organisation to create, assign and unassign access to a case for another solicitor in their organisation],
      And a user [Benjamin - with a solicitor role for the same jurisdiction within the same organisation as Becky],
      And a user [Bill - with a solicitor role for the same jurisdiction within the same organisation as Becky],
      And a case [by Becky to create a case - C1] created as in [F-203_Prerequisite_Case_Creation_C1],
      And a wait time of [5] seconds [to allow for the case just created to appear in search results],
      And a successful call [by Becky to grant access to C1 for Benjamin] as in [F-203_Prerequisite_Case_Assignment_C1_Benjamin],
      And a successful call [by Becky to grant access to C1 for Bill] as in [F-203_Prerequisite_Case_Assignment_C1_Bill],
      And a successful call [by Becky to confirm the access to C1 for Benjamin & Bill] as in [F-203_Prerequisite_Case_Access_Confirmation_C1_Benjamin_Bill_By_Becky],

    When a request is prepared with appropriate values,
      And the request [is made by Benjamin and intends to unassign access to C1 for Becky and Bill],
      And it is submitted to call the [Unassign Access within Organisation] operation of [Manage Case Assignment Microservice],

    Then a positive response is received,
      And the response has all the details as expected,
      And a call [to Get Assignments In My Organisation by Benjamin to verify unassignment of Becky and Bill from C1] will get the expected response as in [S-203.1_Verify_Assignments_In_My_Org].

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-51 A/C 2
@S-203.2
Scenario: CAA successfully removing case access for another solicitor in their org (happy path)

    Given a user [Becky - with a solicitor role in a particular jurisdiction within an organisation, to create a case],
      And a user [CAA - with a pui-caa role within the same organisation to assign and unassign access to a case for a solicitor in their organisation],
      And a user [Bill - with a solicitor role for the same jurisdiction within the same organisation, to get assigned and unassigned access to a case],
      And a case [by Becky to create a case - C1] created as in [F-203_Prerequisite_Case_Creation_C1],
      And a wait time of [5] seconds [to allow for the case just created to appear in search results],
      And a successful call [by CAA to grant access to C1 for Becky] as in [F-203_Prerequisite_Case_Assignment_C1_Becky_BY_CAA],
      And a successful call [by CAA to grant access to C1 for Bill] as in [F-203_Prerequisite_Case_Assignment_C1_Bill_By_CAA],
      And a successful call [by CAA to confirm the access to C1 for Becky & Bill] as in [F-203_Prerequisite_Case_Access_Confirmation_C1_Becky_Bill_By_CAA],

    When a request is prepared with appropriate values,
      And the request [is made by CAA and intends to unassign access to C1 for Becky and Bill],
      And it is submitted to call the [Unassign Access within Organisation] operation of [Manage Case Assignment Microservice],

    Then a positive response is received,
      And the response has all the details as expected,
      And a call [to Get Assignments In My Organisation by CAA to verify unassignment of Becky and Bill from C1] will get the expected response as in [S-203.2_Verify_Assignments_In_My_Org].

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-51 A/C 3
@S-203.3
Scenario: Solicitor successfully removing access to multiple cases for multiple solicitors in their org (happy path)

    Given a user [Becky - with a Solicitor role in a jurisdiction under an organisation to assign and Unassign a case role to a solicitor within the same organisation],
      And a user [Benjamin - with a Solicitor role in the same jurisdiction under the same organisation as Becky to be assigned and unassigned to some cases],
      And a user [Bill - with a Solicitor role in the same jurisdiction under the same organisation as Becky to be assigned and unassigned to some cases],
      And a case [by Becky to create a case - C1] created as in [F-203_Prerequisite_Case_Creation_C1],
      And a case [by Becky to create a case - C2] created as in [F-203_Prerequisite_Case_Creation_C2],
      And a case [by Becky to create a case - C3] created as in [F-203_Prerequisite_Case_Creation_C3],
      And a wait time of [5] seconds [to allow for the case just created to appear in search results],
      And a successful call [by Becky to grant access to C1 for Benjamin] as in [F-203_Prerequisite_Case_Assignment_C1_Benjamin],
      And a successful call [by Becky to grant access to C1 for Bill] as in [F-203_Prerequisite_Case_Assignment_C1_Bill],
      And a successful call [by Becky to grant access to C2 for Benjamin] as in [F-203_Prerequisite_Case_Assignment_C2_Benjamin],
      And a successful call [by Becky to grant access to C2 for Bill] as in [F-203_Prerequisite_Case_Assignment_C2_Bill],
      And a successful call [by Becky to grant access to C3 for Benjamin] as in [F-203_Prerequisite_Case_Assignment_C3_Benjamin],
      And a successful call [by Becky to grant access to C3 for Bill] as in [F-203_Prerequisite_Case_Assignment_C3_Bill],
      And a successful call [by Becky to confirm the access to C1, C2 & C3 for Benjamin & Bill] as in [F-203_Prerequisite_Case_Access_Confirmation_C1_C2_C3_Benjamin_Bill_By_Becky],

    When a request is prepared with appropriate values,
      And the request [is made by Becky and intends to unassign access to C1, C2 and C3 for Benjamin & Bill],
      And it is submitted to call the [Unassign Access within Organisation] operation of [Manage Case Assignment Microservice],

    Then a positive response is received,
      And the response has all the details as expected,
      And a call [to Get Assignments In My Organisation by Becky to verify unassignment of Benjamin and Bill from C1, C2 and C3] will get the expected response as in [S-203.3_Verify_Assignments_In_My_Org].

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-51 A/C 4
@S-203.4
Scenario: Pui-caa successfully removing access to multiple cases for multiple solicitors in their org (happy path)

    Given a user [Becky - with a solicitor role in a particular jurisdiction within an organisation, to create a case],
      And a user [CAA - with a pui-caa role within the same organisation to assign and unassign access to a case for a solicitor in their organisation],
      And a user [Bill - with a solicitor role for the same jurisdiction within the same organisation, to get assigned and unassigned access to a case],
      And a case [by Becky to create a case - C1] created as in [F-203_Prerequisite_Case_Creation_C1],
      And a case [by Becky to create a case - C2] created as in [F-203_Prerequisite_Case_Creation_C2],
      And a case [by Becky to create a case - C3] created as in [F-203_Prerequisite_Case_Creation_C3],
      And a wait time of [5] seconds [to allow for the case just created to appear in search results],
      And a successful call [by CAA to grant access to C1 for Bill] as in [F-203_Prerequisite_Case_Assignment_C1_Bill_By_CAA],
      And a successful call [by CAA to grant access to C2 for Bill] as in [F-203_Prerequisite_Case_Assignment_C2_Bill_By_CAA],
      And a successful call [by CAA to grant access to C3 for Bill] as in [F-203_Prerequisite_Case_Assignment_C3_Bill_By_CAA],
      And a successful call [by CAA to confirm the access to C1, C2 & C3 for Bill] as in [F-203_Prerequisite_Case_Access_Confirmation_C1_C2_C3_Bill_By_CAA],

    When a request is prepared with appropriate values,
      And the request [is made by CAA and intends to unassign access to C1, C2 and C3 for Becky & Bill],
      And it is submitted to call the [Unassign Access within Organisation] operation of [Manage Case Assignment Microservice],

    Then a positive response is received,
      And the response has all the details as expected,
      And a call [to Get Assignments In My Organisation by CAA to verify unassignment of Becky and Bill from C1, C2 and C3] will get the expected response as in [S-203.4_Verify_Assignments_In_My_Org].

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-51 A/C 5
@S-203.5
Scenario: Must return an error response if intended unassignee doesn't exist in invoker's organisation

    Given a user [Becky - with a Solicitor role for a particular jurisdiction under an organisation to create, assign and unassign access to a case for another solicitor in their organisation - O1],
      And a user [Benjamin - with a solicitor role for the same jurisdiction within organisation - O1],
      And a user [Bill - with a solicitor role for the same jurisdiction within a different organisation from Becky - O2],
      And a user [CAA - with a Pui-CAA role for the same jurisdiction and in organisation - O2],
      And a case [by Becky to create a case - C1 with Organisation policies containing R1 and R2] created as in [F-203_Prerequisite_Case_Creation_Seperate_Org_Policies_C1],
      And a wait time of [5] seconds [to allow for the case just created to appear in search results],
      And a successful call [by Becky to grant access to C1 for Benjamin with role R1] as in [F-203_Prerequisite_Case_Assignment_Seperate_Org_Policies_C1_Benjamin_By_Becky],
    #  And a successful call [by CAA to grant access to C1 for Bill with role R2] as in [F-203_Prerequisite_Case_Assignment_Seperate_Org_Policies_C1_Bill_By_CAA_Org2],
      And a successful call [by Becky to confirm the access to C1 for Benjamin] as in [F-203_Prerequisite_Case_Access_Confirmation_Seperate_Org_Policies_C1_Benjamin_By_Becky],
    #  And a successful call [by CAA to confirm the access to C1 for Bill] as in [F-203_Prerequisite_Case_Access_Confirmation_Seperate_Org_Policies_C1_Bill_By_CAA_Org2],

    When a request is prepared with appropriate values,
      And the request [is made by Becky and intends to unassign access to C1 for Benjamin and Bill],
      And it is submitted to call the [Unassign Access within Organisation] operation of [Manage Case Assignment Microservice],

    Then a negative response is received,
      And the response has all the details as expected,
      And a call [by Becky to confirm that Benjamin still has access to the case] will get the expected response as in [S-203.5_Verify_Assignments_In_My_Org_Becky],
    #  And a call [by CAA to confirm that Bill still has access to the case] will get the expected response as in [S-203.5_Verify_Assignments_In_My_Org_CAA_Org2].

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-51 A/C 6
@S-203.6
Scenario: Must return an error response for a malformed Case ID

    Given a user [Becky - with a Solicitor role for a particular jurisdiction under an organisation to create, assign and unassign access to a case for another solicitor in their organisation],
      And a user [Benjamin - with a solicitor role for the same jurisdiction within the same organisation as Becky],
      And a case [by Becky to create a case - C1] created as in [F-203_Prerequisite_Case_Creation_C1],
      And a wait time of [5] seconds [to allow for the case just created to appear in search results],
      And a successful call [by Becky to grant access to C1 for Benjamin] as in [F-203_Prerequisite_Case_Assignment_C1_Benjamin],

    When a request is prepared with appropriate values,
      And the request [is made by Becky and intends to unassign access for Benjamin to some cases],
      And the request [contains the Case ID of C1 plus an additional malformed caseID],
      And it is submitted to call the [Unassign Access within Organisation] operation of [Manage Case Assignment Microservice],

    Then a negative response is received,
      And the response has all the details as expected,
      And a call [by Becky to confirm that Benjamin still has access to the case - C1] will get the expected response as in [S-203.6_Verify_Assignments_In_My_Org].

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-51 A/C 7
@S-203.7
Scenario: Must return an error response for a missing Case ID

    Given a user [Becky - with a Solicitor role for a particular jurisdiction under an organisation to create, assign and unassign access to a case for another solicitor in their organisation],
      And a user [Benjamin - with a solicitor role for the same jurisdiction within the same organisation as Becky],
      And a case [by Becky to create a case - C1] created as in [F-203_Prerequisite_Case_Creation_C1],
      And a wait time of [5] seconds [to allow for the case just created to appear in search results],
      And a successful call [by Becky to grant access to C1 for Benjamin] as in [F-203_Prerequisite_Case_Assignment_C1_Benjamin],

    When a request is prepared with appropriate values,
      And the request [is made by Becky and intends to unassign access for Benjamin to some cases],
      And the request [contains the Case ID of C1 and a missing case ID],
      And it is submitted to call the [Unassign Access within Organisation] operation of [Manage Case Assignment Microservice],

    Then a negative response is received,
      And the response has all the details as expected,
      And a call [by Becky to confirm that Benjamin still has access to the case - C1] will get the expected response as in [S-203.7_Verify_Assignments_In_My_Org].

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-51 A/C 8
@S-203.8
Scenario: Must return an error response if the invoker is a solicitor in a different jurisdiction from that of the case

    Given a user [Becky - with a Solicitor role for a particular jurisdiction under an organisation to create, assign and unassign access to a case for another solicitor in their organisation],
      And a user [Benjamin - with a solicitor role for a different jurisdiction within the same organisation as Becky],
      And a case [by Becky to create a case - C1] created as in [F-203_Prerequisite_Case_Creation_C1],
      And a wait time of [5] seconds [to allow for the case just created to appear in search results],
      And a successful call [by Becky to confirm the access to C1 for Becky] as in [F-203_Prerequisite_Case_Access_Confirmation_C1_Becky_By_Becky],

    When a request is prepared with appropriate values,
      And the request [is made by Benjamin and intends to unassign access to C1 for Becky],
      And it is submitted to call the [Unassign Access within Organisation] operation of [Manage Case Assignment Microservice],

    Then a negative response is received,
      And the response has all the details as expected,
      And a call [to Get Assignments In My Organisation by Becky to verify continued assignment of Becky for C1] will get the expected response as in [S-203.8_Verify_Assignments_In_My_Org].

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-51 A/C 9
@S-203.9
Scenario: Must return an error response if the invoker doesn't have access to the case of the user they are trying to remove access for

    Given a user [Becky - with a Solicitor role for a particular jurisdiction under an organisation to create, assign and unassign access to a case for another solicitor in their organisation],
      And a user [Benjamin - with a solicitor role for the same jurisdiction within the same organisation as Becky],
      And a case [by Becky to create a case - C1] created as in [F-203_Prerequisite_Case_Creation_C1],
      And a wait time of [5] seconds [to allow for the case just created to appear in search results],
      And a successful call [by Becky to confirm the access to C1 for Becky] as in [F-203_Prerequisite_Case_Access_Confirmation_C1_Becky_By_Becky],

    When a request is prepared with appropriate values,
      And the request [is made by Benjamin and intends to unassign access to C1 for Becky],
      And it is submitted to call the [Unassign Access within Organisation] operation of [Manage Case Assignment Microservice],

    Then a negative response is received,
      And the response has all the details as expected,
      And a call [to Get Assignments In My Organisation by Becky to verify the continued assignment of Becky for C1] will get the expected response as in [S-203.9_Verify_Assignments_In_My_Org].

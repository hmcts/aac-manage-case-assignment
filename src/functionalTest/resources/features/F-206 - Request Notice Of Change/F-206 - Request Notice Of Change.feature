#=============================================
@F-206
Feature: F-206: Request Notice of Change (NoC)
#=============================================

Background:
    Given an appropriate test context as detailed in the test data source

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-1
@S-206.1
Scenario: (Happy Path) Solicitor requests NoC to replace representation - no auto-approval applies

    Given a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Dil - with a solicitor role for the same jurisdiction, within a different organisation from Richard's],
      And [a citizen Mario, on behalf of whom Richard will create a case] in the context,
      And a successful call [by Richard to create a case C1 on behalf of Mario, which contains 3 Org Policies for 3 case roles: R1 which is assigned to Dil's organisation, R2 & R3 which are both assigned to Richard's organisation] as in [F-206_NoC_Case_Creation_By_Richard_With_Assigned_Org_Policies],

     When a request is prepared with appropriate values,
      And the request [is made by Dil to place an NOC Request for C1],
      And the request [contains all correct answers in the correct format],
      And the request [contains answers identifying case role R2],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

     Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify that Dil has NOT been granted any case roles for the case] will get the expected response as in [F-206_Verify_Not_Granted_Case_Roles_Dil],
      And another call [to verify there is a pending NOC request on the case and the OrganisationPolicy for R2 has NOT been updated] will get the expected response as in [F-206_Verify_Case_Data_COR_Pending],
      And another call [to get Case Events API returns a NoCRequest event in which the user ID is set to invoking users email address AND the proxied_by field set to the ID of the system user] will get the expected response as in [F-206_Verify_NoC_Request_Event_Data].


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-2
@S-206.2
Scenario: (Happy Path) Solicitor requests NoC for a non-represented litigant - no auto-approval applies

    Given a user [Mario - to initiate a case on his own behalf via a citizen facing application],
      And a user [Dil - with a solicitor role for the same jurisdiction as that of a case initiated by Mario],
      And a successful call [by Mario to create a case C1, which contains 3 Org Policies for 3 case roles: R1 which is assigned to Dil's organisation, R2 & R3 which are both not assigned to any organisation] as in [F-206_NoC_Case_Creation_By_Mario_With_Assigned_R1_Org_Policy],

     When a request is prepared with appropriate values,
      And the request [is made by Dil to place an NOC Request for C1],
      And the request [contains all correct answers in the correct format],
      And the request [contains answers identifying case role R2],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

     Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify that Dil has NOT been granted any case roles for the case] will get the expected response as in [F-206_Verify_Not_Granted_Case_Roles_Dil],
      And another call [to verify there is a pending NOC request on the case and the OrganisationPolicy for R2 has NOT been updated] will get the expected response as in [F-206_Verify_Case_Data_COR_Pending],
      And another call [to get Case Events API returns a NoCRequest event in which the user ID is set to invoking users email address AND the proxied_by field set to the ID of the system user] will get the expected response as in [F-206_Verify_NoC_Request_Event_Data].


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-3
@S-206.3
Scenario: (Happy Path) CAA requests NoC to replace representation - no auto-approval applies

    Given a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Matt - with a pui-caa role, within a different organisation from Richard's],
      And [a citizen Mario, on behalf of whom Richard will create a case] in the context,
      And a successful call [by Richard to create a case C1 on behalf of Mario, which contains 3 Org Policies for 3 case roles: R1 which is assigned to Matt's organisation, R2 & R3 which are both assigned to Richard's organisation] as in [F-206_NoC_Case_Creation_By_Richard_With_Assigned_Org_Policies],

     When a request is prepared with appropriate values,
      And the request [is made by Matt to place an NOC Request for C1],
      And the request [contains all correct answers in the correct format],
      And the request [contains answers identifying case role R2],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

     Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify that Matt has NOT been granted any case roles for the case] will get the expected response as in [F-206_Verify_Not_Granted_Case_Roles_Matt],
      And another call [to verify there is a pending NOC request on the case and the OrganisationPolicy for R2 has NOT been updated] will get the expected response as in [F-206_Verify_Case_Data_COR_Pending],
      And another call [to get Case Events API returns a NoCRequest event in which the user ID is set to invoking users email address AND the proxied_by field set to the ID of the system user] will get the expected response as in [F-206_Verify_NoC_Request_Event_Data].


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-4
@S-206.4
Scenario: (Happy Path) CAA requests NoC for a non-represented litigant - no auto-approval applies

    Given a user [Mario - to initiate a case on his own behalf via a citizen facing application],
      And a user [Matt - with a pui-caa role],
      And a successful call [by Mario to create a case C1, which contains 3 Org Policies for 3 case roles: R1 which is assigned to Matt's organisation, R2 & R3 which are both not assigned to any organisation] as in [F-206_NoC_Case_Creation_By_Mario_With_Assigned_R1_Org_Policy],

     When a request is prepared with appropriate values,
      And the request [is made by Matt to place an NOC Request for C1],
      And the request [contains all correct answers in the correct format],
      And the request [contains answers identifying case role R2],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

     Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify that Matt has NOT been granted any case roles for the case] will get the expected response as in [F-206_Verify_Not_Granted_Case_Roles_Matt],
      And another call [to verify there is a pending NOC request on the case and the OrganisationPolicy for R2 has NOT been updated] will get the expected response as in [F-206_Verify_Case_Data_COR_Pending],
      And another call [to get Case Events API returns a NoCRequest event in which the user ID is set to invoking users email address AND the proxied_by field set to the ID of the system user] will get the expected response as in [F-206_Verify_NoC_Request_Event_Data].


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-5
@S-206.5
Scenario: (Happy Path) CAA (also a solicitor for a different jurisdiction) requests NoC to replace representation - no auto-approval applies

    Given a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Alice - with a pui-caa role and a solicitor role for a different jurisdiction, within a different organisation from Richard's],
      And [a citizen Mario, on behalf of whom Richard will create a case] in the context,
      And a successful call [by Richard to create a case C1 on behalf of Mario, which contains 3 Org Policies for 3 case roles: R1 which is assigned to Alice's organisation, R2 & R3 which are both assigned to Richard's organisation] as in [F-206_NoC_Case_Creation_By_Richard_With_Assigned_Org_Policies],

     When a request is prepared with appropriate values,
      And the request [is made by Alice to place an NOC Request for C1],
      And the request [contains all correct answers in the correct format],
      And the request [contains answers identifying case role R2],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

     Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify that Alice has NOT been granted any case roles for the case] will get the expected response as in [F-206_Verify_Not_Granted_Case_Roles_Alice],
      And another call [to verify there is a pending NOC request on the case and the OrganisationPolicy for R2 has NOT been updated] will get the expected response as in [F-206_Verify_Case_Data_COR_Pending],
      And another call [to get Case Events API returns a NoCRequest event in which the user ID is set to invoking users email address AND the proxied_by field set to the ID of the system user] will get the expected response as in [F-206_Verify_NoC_Request_Event_Data].


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-6
  @S-206.6
  Scenario: (Happy Path) CAA (also a solicitor for a different jurisdiction) successfully Requests NoC for a non-represented litigant - no auto-approval applies

    Given a user [Mario - to initiate a case on his own behalf via a citizen facing application],
      And a user [Alice - with a pui-caa role and a solicitor role for a different jurisdiction as that of a case initiated by Mario],
      And a successful call [by Mario to create a case C1, which contains 3 Org Policies for 3 case roles: R1 which is assigned to Alice's organisation, R2 & R3 which are both not assigned to any organisation] as in [F-206_NoC_Case_Creation_By_Mario_With_Assigned_R1_Org_Policy],

     When a request is prepared with appropriate values,
      And the request [is made by Alice to place an NOC Request for C1],
      And the request [contains all correct answers in the correct format],
      And the request [contains answers identifying case role R2],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

     Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify that Alice has NOT been granted any case roles for the case] will get the expected response as in [F-206_Verify_Not_Granted_Case_Roles_Alice],
      And another call [to verify there is a pending NOC request on the case and the OrganisationPolicy for R2 has NOT been updated] will get the expected response as in [F-206_Verify_Case_Data_COR_Pending],
      And another call [to get Case Events API returns a NoCRequest event in which the user ID is set to invoking users email address AND the proxied_by field set to the ID of the system user] will get the expected response as in [F-206_Verify_NoC_Request_Event_Data].


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-7 CCD-5334/6255 AC1
@S-206.7 @callbackTests
Scenario: (Happy Path) Solicitor having STANDARD access to case requests NoC to replace representation - auto-approval applies

    Given a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Dil - with a solicitor role for the same jurisdiction, within a different organisation from Richard's],
      And [a citizen Mario, on behalf of whom Richard will create a case] in the context,
      And a successful call [by Richard to create a case C1 on behalf of Mario, which contains 3 Org Policies for 3 case roles: R1 which is assigned to Dil's organisation, R2 & R3 which are both assigned to Richard's organisation] as in [F-206_NoC_Auto_Approval_Case_Creation_By_Richard_With_Assigned_Org_Policies],

     When a request is prepared with appropriate values,
      And the request [is made by Dil to place a NOC Request for C1],
      And the request [contains all correct answers in the correct format],
      And the request [contains answers identifying case role R2],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

     Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify that Dil has NOT been granted any case roles for the case] will get the expected response as in [F-206_Verify_Not_Granted_Case_Roles_Dil],
      And a call [to verify there is NO pending NOC request on the case and the OrganisationPolicy for R2 HAS been updated] will get the expected response as in [F-206_Verify_Case_Data_COR_Approved_ReplaceRepresentation],
      And a call [to get Case Events API returns a NoCRequest event in which the user ID is set to invoking users email address AND the proxied_by field set to the ID of the system user] will get the expected response as in [F-206_Verify_NoC_Request_Event_Data].
      And a call [to get Grant Access Metadata API returning Standard Grant Access for case] will get the expected response as in [F-206_Verify_NoC_Request_Access_Metadata],


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-8
@S-206.8 @callbackTests
Scenario: (Happy Path) Solicitor requests NoC for a non-represented litigant - auto-approval applies

    Given a user [Mario - to initiate a case on his own behalf via a citizen facing application],
      And a user [Dil - with a solicitor role for the same jurisdiction as that of a case initiated by Mario],
      And a successful call [by Mario to create a case C1, which contains 3 Org Policies for 3 case roles: R1 which is assigned to Dil's organisation, R2 & R3 which are both not assigned to any organisation] as in [F-206_NoC_Auto_Approval_Case_Creation_By_Mario_With_Assigned_R1_Org_Policy],

     When a request is prepared with appropriate values,
      And the request [is made by Dil to place a NOC Request for C1],
      And the request [contains all correct answers in the correct format],
      And the request [contains answers identifying case role R2],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

     Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify that Dil HAS been granted case roles R1 & R2 for the case but not R3] will get the expected response as in [F-206_Verify_Granted_Case_Roles_R1_R2_Dil],
      And a call [to verify there is NO pending NOC request on the case and the OrganisationPolicy for R2 HAS been updated] will get the expected response as in [F-206_Verify_Case_Data_COR_Approved_AddRepresentation],
      And a call [to get Case Events API returns a NoCRequest event in which the user ID is set to invoking users email address AND the proxied_by field set to the ID of the system user] will get the expected response as in [F-206_Verify_NoC_Request_Event_Data].


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-9
@S-206.9 @callbackTests
Scenario: (Happy Path) CAA requests NoC to replace representation - auto-approval applies

    Given a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Matt - with a pui-caa role, within a different organisation from Richard's],
      And [a citizen Mario, on behalf of whom Richard will create a case] in the context,
      And a successful call [by Richard to create a case C1 on behalf of Mario, which contains 3 Org Policies for 3 case roles: R1 which is assigned to Matt's organisation, R2 & R3 which are both assigned to Richard's organisation] as in [F-206_NoC_Auto_Approval_Case_Creation_By_Richard_With_Assigned_Org_Policies],

     When a request is prepared with appropriate values,
      And the request [is made by Matt to place a NOC Request for C1],
      And the request [contains all correct answers in the correct format],
      And the request [contains answers identifying case role R2],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

     Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify that Matt has NOT been granted any case roles for the case] will get the expected response as in [F-206_Verify_Not_Granted_Case_Roles_Matt],
      And another call [to verify there is NO pending NOC request on the case and the OrganisationPolicy for R2 HAS been updated] will get the expected response as in [F-206_Verify_Case_Data_COR_Approved_ReplaceRepresentation],
      And another call [to get Case Events API returns a NoCRequest event in which the user ID is set to invoking users email address AND the proxied_by field set to the ID of the system user] will get the expected response as in [F-206_Verify_NoC_Request_Event_Data].


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-10
@S-206.10 @callbackTests
Scenario: (Happy Path) CAA requests NoC for a non-represented litigant - auto-approval applies

    Given a user [Mario - to initiate a case on his own behalf via a citizen facing application],
      And a user [Matt - with a pui-caa role],
      And a successful call [by Mario to create a case C1, which contains 3 Org Policies for 3 case roles: R1 which is assigned to Matt's organisation, R2 & R3 which are both not assigned to any organisation] as in [F-206_NoC_Auto_Approval_Case_Creation_By_Mario_With_Assigned_R1_Org_Policy],

     When a request is prepared with appropriate values,
      And the request [is made by Matt to place a NOC Request for C1],
      And the request [contains all correct answers in the correct format],
      And the request [contains answers identifying case role R2],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

     Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify that Matt has NOT been granted any case roles for the case] will get the expected response as in [F-206_Verify_Not_Granted_Case_Roles_Matt],
      And another call [to verify there is NO pending NOC request on the case and the OrganisationPolicy for R2 HAS been updated] will get the expected response as in [F-206_Verify_Case_Data_COR_Approved_AddRepresentation],
      And another call [to get Case Events API returns a NoCRequest event in which the user ID is set to invoking users email address AND the proxied_by field set to the ID of the system user] will get the expected response as in [F-206_Verify_NoC_Request_Event_Data].


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-11
@S-206.11 @callbackTests
Scenario: (Happy Path) CAA (also a solicitor for a different jurisdiction) requests NoC to replace representation - auto-approval applies

    Given a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Alice - with a pui-caa role and a solicitor role for a different jurisdiction, within a different organisation from Richard's],
      And [a citizen Mario, on behalf of whom Richard will create a case] in the context,
      And a successful call [by Richard to create a case C1 on behalf of Mario, which contains 3 Org Policies for 3 case roles: R1 which is assigned to Alice's organisation, R2 & R3 which are both assigned to Richard's organisation] as in [F-206_NoC_Auto_Approval_Case_Creation_By_Richard_With_Assigned_Org_Policies],

     When a request is prepared with appropriate values,
      And the request [is made by Alice to place a NOC Request for C1],
      And the request [contains all correct answers in the correct format],
      And the request [contains answers identifying case role R2],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

     Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify that Alice has NOT been granted any case roles for the case] will get the expected response as in [F-206_Verify_Not_Granted_Case_Roles_Alice],
      And another call [to verify there is NO pending NOC request on the case and the OrganisationPolicy for R2 HAS been updated] will get the expected response as in [F-206_Verify_Case_Data_COR_Approved_ReplaceRepresentation],
      And another call [to get Case Events API returns a NoCRequest event in which the user ID is set to invoking users email address AND the proxied_by field set to the ID of the system user] will get the expected response as in [F-206_Verify_NoC_Request_Event_Data].


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-12
@S-206.12 @callbackTests
Scenario: (Happy Path) CAA (also a solicitor for a different jurisdiction) requests NoC for a non-represented litigant - auto-approval applies

    Given a user [Mario - to initiate a case on his own behalf via a citizen facing application],
      And a user [Alice - with a pui-caa role and a solicitor role for a different jurisdiction as that of a case initiated by Mario],
      And a successful call [by Mario to create a case C1, which contains 3 Org Policies for 3 case roles: R1 which is assigned to Alice's organisation, R2 & R3 which are both not assigned to any organisation] as in [F-206_NoC_Auto_Approval_Case_Creation_By_Mario_With_Assigned_R1_Org_Policy],

     When a request is prepared with appropriate values,
      And the request [is made by Alice to place a NOC Request for C1],
      And the request [contains all correct answers in the correct format],
      And the request [contains answers identifying case role R2],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

     Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify that Alice has NOT been granted any case roles for the case] will get the expected response as in [F-206_Verify_Not_Granted_Case_Roles_Alice],
      And another call [to verify there is NO pending NOC request on the case and the OrganisationPolicy for R2 HAS been updated] will get the expected response as in [F-206_Verify_Case_Data_COR_Approved_AddRepresentation],
      And another call [to get Case Events API returns a NoCRequest event in which the user ID is set to invoking users email address AND the proxied_by field set to the ID of the system user] will get the expected response as in [F-206_Verify_NoC_Request_Event_Data].


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-13
@S-206.13 @callbackTests
Scenario: (Happy Path) CAA (also a solicitor for the same jurisdiction) requests NoC to replace representation - auto-approval applies

    Given a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Jane - with a pui-caa role and a solicitor role for the same jurisdiction, within a different organisation from Richard's],
      And [a citizen Mario, on behalf of whom Richard will create a case] in the context,
      And a successful call [by Richard to create a case C1 on behalf of Mario, which contains 3 Org Policies for 3 case roles: R1 which is assigned to Jane's organisation, R2 & R3 which are both assigned to Richard's organisation] as in [F-206_NoC_Auto_Approval_Case_Creation_By_Richard_With_Assigned_Org_Policies],

     When a request is prepared with appropriate values,
      And the request [is made by Jane to place a NOC Request for C1],
      And the request [contains all correct answers in the correct format],
      And the request [contains answers identifying case role R2],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

     Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify that Jane HAS been granted case roles R1 & R2 for the case but not R3] will get the expected response as in [F-206_Verify_Granted_Case_Roles_R1_R2_Jane],
      And a call [to verify there is NO pending NOC request on the case and the OrganisationPolicy for R2 HAS been updated] will get the expected response as in [F-206_Verify_Case_Data_COR_Approved_ReplaceRepresentation],
      And a call [to get Case Events API returns a NoCRequest event in which the user ID is set to invoking users email address AND the proxied_by field set to the ID of the system user] will get the expected response as in [F-206_Verify_NoC_Request_Event_Data].


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-14
@S-206.14 @callbackTests
Scenario: (Happy Path) CAA (also a solicitor for the same jurisdiction) requests NoC for a non-represented litigant - auto-approval applies

    Given a user [Mario - to initiate a case on his own behalf via a citizen facing application],
      And a user [Jane - with a pui-caa role and a solicitor role for the same jurisdiction as that of a case initiated by Mario],
      And a successful call [by Mario to create a case C1, which contains 3 Org Policies for 3 case roles: R1 which is assigned to Jane's organisation, R2 & R3 which are both not assigned to any organisation] as in [F-206_NoC_Auto_Approval_Case_Creation_By_Mario_With_Assigned_R1_Org_Policy],

     When a request is prepared with appropriate values,
      And the request [is made by Jane to place a NOC Request for C1],
      And the request [contains all correct answers in the correct format],
      And the request [contains answers identifying case role R2],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

     Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify that Jane HAS been granted case roles R1 & R2 for the case but not R3] will get the expected response as in [F-206_Verify_Granted_Case_Roles_R1_R2_Jane],
      And a call [to verify there is NO pending NOC request on the case and the OrganisationPolicy for R2 HAS been updated] will get the expected response as in [F-206_Verify_Case_Data_COR_Approved_AddRepresentation],
      And a call [to get Case Events API returns a NoCRequest event in which the user ID is set to invoking users email address AND the proxied_by field set to the ID of the system user] will get the expected response as in [F-206_Verify_NoC_Request_Event_Data].


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 (repeat of ACA-71 / AC-4  && ACA-72 / AC-4  but for [Request NoC] API)
@S-206.15
Scenario: Must return an error response for a malformed Case ID

    Given an appropriate test context as detailed in the test data source,
      And a user [Jane - with a solicitor and a pui-caa role],

     When a request is prepared with appropriate values,
      And the request [is made by Jane to place a NOC Request for a case],
      And the request [contains a malformed case ID for C1],
      And the request [contains all correct answers in the correct format],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

     Then a negative response is received,
      And the response has all the details as expected.


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 (repeat of ACA-71 / AC-5  && ACA-72 / AC-5  but for [Request NoC] API)
@S-206.16
Scenario: Must return an error response for a non-extant Case ID

    Given an appropriate test context as detailed in the test data source,
      And a user [Jane - with a solicitor and a pui-caa role],

     When a request is prepared with appropriate values,
      And the request [is made by Jane to place a NOC Request for a case],
      And the request [contains a valid case ID for which there is no corresponding case record],
      And the request [contains all correct answers in the correct format],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

     Then a negative response is received,
      And the response has all the details as expected.


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 (repeat of ACA-71 / AC-6  && ACA-72 / AC-6  but for [Request NoC] API)
@S-206.17
Scenario: Must return an error response for a missing Case ID

    Given an appropriate test context as detailed in the test data source,
      And a user [Jane - with a solicitor and a pui-caa role],

     When a request is prepared with appropriate values,
      And the request [is made by Jane to place a NOC Request for a case],
      And the request [does not contain a case ID],
      And the request [contains all correct answers in the correct format],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

     Then a negative response is received,
      And the response has all the details as expected.


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 (repeat of ACA-71 / AC-7 + AC-11 && ACA-72 / AC-7  but for [Request NoC] API)
@S-206.18
Scenario: Must return an error response when a NoC request is currently pending on a case

    Given an appropriate test context as detailed in the test data source,
      And a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Dil - with a solicitor role for the same jurisdiction, within a different organisation from Richard's],
      And a user [Emma - with a solicitor role for the same jurisdiction, within a different organisation from Richard's],
      And [a citizen Mario, on behalf of whom Richard will create a case] in the context,
      And [the configuration of the NoC request event has no auto-approval] in the context,
      And a successful call [by Richard to create a case C1 on behalf of Mario, which contains 3 Org Policies for 3 case roles: R1 which is assigned to Dil's organisation, R2 & R3 which are both assigned to Richard's organisation] as in [F-206_NoC_Case_Creation_By_Richard_With_Assigned_Org_Policies],

      And a successful call [by Emma to raise a NoC request to become a representative for Mario on C1] as in [F-206_Request_NoC_By_Emma],
      And [No user has approved or rejected the NoC request from Emma] in the context,

     When a request is prepared with appropriate values,
      And the request [is made by Dil to place a NOC Request for C1],
      And the request [contains a valid case ID for C1],
      And the request [contains all correct answers in the correct format],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

     Then a negative response is received,
      And the response has all the details as expected.


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 (repeat of ACA-71 / AC-8  && ACA-72 / AC-8  but for [Request NoC] API)
@S-206.19
Scenario: Must return an error if there is not an Organisation Policy field containing a case role for each set of answers

    Given an appropriate test context as detailed in the test data source,
      And a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Dil - with a solicitor role for the same jurisdiction, within a different organisation from Richard's],
      And [a citizen Mario, on behalf of whom Richard will create a case] in the context,
      And a successful call [by Richard to create a case C1 on behalf of Mario, which only contains 2 out 3 Org Policies for 3 case roles] as in [F-206_NoC_Case_Creation_By_Richard_With_Only_2_Out_Of_3_Org_Policies],
      And [The challenge questions have answers that resolve to three distinct case roles (1,2,3)] in the context,
      And [The configuration of the case creation event only establishes organisation policies containing case roles 1 and 2] in the context,

     When a request is prepared with appropriate values,
      And the request [is made by Dil to place a NOC Request for C1],
      And the request [contains a valid case ID for C1],
      And the request [contains all correct answers in the correct format],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

     Then a negative response is received,
      And the response has all the details as expected.


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 (repeat of ACA-71 / AC-9  && ACA-72 / AC-9  but for [Request NoC] API)
@S-206.20
Scenario: Must return an error when no NOC Request event is available on the case

    Given an appropriate test context as detailed in the test data source,
      And a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Dil - with a solicitor role for the same jurisdiction, within a different organisation from Richard's],
      And a successful call [by Richard to create a case C1 on behalf of Mario for a case type with no NoCRequest event] as in [F-206_NoC_WithoutEvents_Case_Creation_By_Richard_With_Assigned_Org_Policies],

     When a request is prepared with appropriate values,
      And the request [is made by Dil to place a NOC Request for C1],
      And the request [contains a valid case ID for C1],
      And the request [contains all correct answers in the correct format],
      And the request [identifies that no NOC request event is available on the case],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

     Then a negative response is received,
      And the response has all the details as expected.


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 (repeat of ACA-71 / AC-10  && ACA-72 / AC-10  but for [Request NoC] API)
@S-206.21
Scenario: Must return an error response when the solicitor does not have access to the jurisdiction of the case

    Given an appropriate test context as detailed in the test data source,
      And a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Ashley - with a solicitor role for a different jurisdiction, within a different organisation from Richard's],
      And [a citizen Mario, on behalf of whom Richard will create a case] in the context,
      And a successful call [by Richard to create a case C1 on behalf of Mario, which contains 3 Org Policies for 3 case roles: R1 which is assigned to Ashley's organisation, R2 & R3 which are both assigned to Richard's organisation] as in [F-206_NoC_Case_Creation_By_Richard_With_Assigned_Org_Policies],

     When a request is prepared with appropriate values,
      And the request [is made by Ashley to place a NOC Request for C1],
      And the request [contains a valid case ID for C1],
      And the request [contains all correct answers in the correct format],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

     Then a negative response is received,
      And the response has all the details as expected.


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 (repeat of ACA-72 / AC-11)
@S-206.22.1
Scenario: Must return an error response for an invalid/incorrect answer (mismatched question id)

    Given an appropriate test context as detailed in the test data source,
      And a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Dil - with a solicitor role for the same jurisdiction, within a different organisation from Richard's],
      And [a citizen Mario, on behalf of whom Richard will create a case] in the context,
      And a successful call [by Richard to create a case C1 on behalf of Mario, which contains 3 Org Policies for 3 case roles: R1 which is assigned to Ashley's organisation, R2 & R3 which are both assigned to Richard's organisation] as in [F-206_NoC_Case_Creation_By_Richard_With_Assigned_Org_Policies],

     When a request is prepared with appropriate values,
      And the request [is made by Dil to place a NOC Request for C1],
      And the request [contains a valid case ID for C1],
      And the request [contains any invalid/incorrect answer(s) to any of the question(s) - (mismatched question id)],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

     Then a negative response is received,
      And the response has all the details as expected.


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 (repeat of ACA-72 / AC-11)
@S-206.22.2
Scenario: Must return an error response for an invalid/incorrect answer (wrong number of questions)

    Given an appropriate test context as detailed in the test data source,
      And a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Dil - with a solicitor role for the same jurisdiction, within a different organisation from Richard's],
      And [a citizen Mario, on behalf of whom Richard will create a case] in the context,
      And a successful call [by Richard to create a case C1 on behalf of Mario, which contains 3 Org Policies for 3 case roles: R1 which is assigned to Ashley's organisation, R2 & R3 which are both assigned to Richard's organisation] as in [F-206_NoC_Case_Creation_By_Richard_With_Assigned_Org_Policies],

     When a request is prepared with appropriate values,
      And the request [is made by Dil to place a NOC Request for C1],
      And the request [contains a valid case ID for C1],
      And the request [contains any invalid/incorrect answer(s) to any of the question(s) - (wrong number of questions)],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

     Then a negative response is received,
      And the response has all the details as expected.


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 (repeat of ACA-72 / AC-11)
@S-206.22.3
Scenario: Must return an error response for an invalid/incorrect answer (wrong number of questions)

    Given an appropriate test context as detailed in the test data source,
      And a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Dil - with a solicitor role for the same jurisdiction, within a different organisation from Richard's],
      And [a citizen Mario, on behalf of whom Richard will create a case] in the context,
      And a successful call [by Richard to create a case C1 on behalf of Mario, which contains 3 Org Policies for 3 case roles: R1 which is assigned to Ashley's organisation, R2 & R3 which are both assigned to Richard's organisation] as in [F-206_NoC_Case_Creation_By_Richard_With_Assigned_Org_Policies],

     When a request is prepared with appropriate values,
      And the request [is made by Dil to place a NOC Request for C1],
      And the request [contains a valid case ID for C1],
      And the request [contains any invalid/incorrect answer(s) to any of the question(s) - (no answers supplied)],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

     Then a negative response is received,
      And the response has all the details as expected.


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 (repeat of ACA-72 / AC-12)
@S-206.23
Scenario: Must return an error response when the the invoking user's organisation is already representing the litigant identified by the answers

    Given an appropriate test context as detailed in the test data source,
      And a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Dil - with a solicitor role for the same jurisdiction, within a different organisation from Richard's],
      And [a citizen Mario, on behalf of whom Richard will create a case] in the context,
      And a successful call [by Richard to create a case C1 on behalf of Mario, which contains 3 Org Policies for 3 case roles: which are all assigned to Dil's organisation] as in [F-206_NoC_Case_Creation_By_Richard_With_Assigned_Org_Policies_All_To_Org1],

     When a request is prepared with appropriate values,
      And the request [is made by Dil to place a NOC Request for C1],
      And the request [contains a valid case ID for C1],
      And the request [contains all correct answers in the correct format],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

     Then a negative response is received,
      And the response has all the details as expected.


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 (repeat of ACA-72 / AC-13)
@S-206.24
Scenario: Must return an error when answers do not match a case role in the case to be represented

    Given an appropriate test context as detailed in the test data source,
      And a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Dil - with a solicitor role for the same jurisdiction, within a different organisation from Richard's],
      And [a citizen Mario, on behalf of whom Richard will create a case] in the context,
      And a successful call [by Richard to create a case C1 on behalf of Mario, which contains 3 Org Policies for 3 case roles: R1 which is assigned to Ashley's organisation, R2 & R3 which are both assigned to Richard's organisation] as in [F-206_NoC_Case_Creation_By_Richard_With_Assigned_Org_Policies],

     When a request is prepared with appropriate values,
      And the request [is made by Dil to place a NOC Request for C1],
      And the request [contains a valid case ID for C1],
      And the request [contains answers that do not match a case role in the case to be represented],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

     Then a negative response is received,
      And the response has all the details as expected.


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 (repeat of ACA-72 / AC-14)
@S-206.25
Scenario: Must return an error when the set of answers match more than one corresponding case roles for representation

    Given an appropriate test context as detailed in the test data source,
      And a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Dil - with a solicitor role for the same jurisdiction, within a different organisation from Richard's],
      And [a citizen Mario, on behalf of whom Richard will create a case] in the context,
      And a successful call [by Richard to create a case C1 on behalf of Mario, which contains 3 Org Policies for 3 case roles, with litgants that have matching names] as in [F-206_NoC_Case_Creation_By_Richard_With_Assigned_Org_Policies_And_Matching_Litigant_Names],

     When a request is prepared with appropriate values,
      And the request [is made by Dil to place a NOC Request for C1],
      And the request [contains a valid case ID for C1],
      And the request [contains a set of answers that match more than one corresponding case roles for representation],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

     Then a negative response is received,
      And the response has all the details as expected.

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
      And [a citizen Mario, on behalf of whome Richard will create a case] in the context,
      And a successful call [by Richard to create a case C1 on behalf of Mario, which contains 3 Org Policies for 3 case roles: R1 which is assigned to Dil's organisation, R2 & R3 which are both assigned to Richard's organisation] as in [F-206_NoC_Case_Creation_By_Richard_With_Assigned_Org_Policies],
      And a wait time of [5] seconds [to allow for the case just created to appear in search results],

     When a request is prepared with appropriate values,
      And the request [is made by Dil to place an NOC Request for C1],
      And the request [contains all correct answers in the correct format],
      And the request [contains answers identifying case role R2],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

     Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify that Dil has NOT been granted any case roles for the case] will get the expected response as in [F-206_Verify_Not_Granted_Case_Roles_Dil],
      And another call [to verify there is a pending NOC request on the case and the OrganisationPolicy for R2 has NOT been updated] will get the expected response as in [F-206_Verify_Case_Data_COR_Pending].


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-2
@S-206.2
Scenario: (Happy Path) Solicitor requests NoC for a non-represented litigant - no auto-approval applies

    Given a user [Mario - to initiate a case on his own behalf via a citizen facing application],
      And a user [Dil - with a solicitor role for the same jurisdiction as that of a case initiated by Mario],
      And a successful call [by Mario to create a case C1, which contains 3 Org Policies for 3 case roles: R1 which is assigned to Dil's organisation, R2 & R3 which are both not assigned to any organisation] as in [F-206_NoC_Case_Creation_By_Mario_With_Assigned_R1_Org_Policy],
      And a wait time of [5] seconds [to allow for the case just created to appear in search results],

     When a request is prepared with appropriate values,
      And the request [is made by Dil to place an NOC Request for C1],
      And the request [contains all correct answers in the correct format],
      And the request [contains answers identifying case role R2],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

     Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify that Dil has NOT been granted any case roles for the case] will get the expected response as in [F-206_Verify_Not_Granted_Case_Roles_Dil],
      And another call [to verify there is a pending NOC request on the case and the OrganisationPolicy for R2 has NOT been updated] will get the expected response as in [F-206_Verify_Case_Data_COR_Pending].


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-3
@S-206.3
Scenario: (Happy Path) CAA requests NoC to replace representation - no auto-approval applies

    Given a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Matt - with a pui-caa role, within a different organisation from Richard's],
      And [a citizen Mario, on behalf of whome Richard will create a case] in the context,
      And a successful call [by Richard to create a case C1 on behalf of Mario, which contains 3 Org Policies for 3 case roles: R1 which is assigned to Matt's organisation, R2 & R3 which are both assigned to Richard's organisation] as in [F-206_NoC_Case_Creation_By_Richard_With_Assigned_Org_Policies],
      And a wait time of [5] seconds [to allow for the case just created to appear in search results],

     When a request is prepared with appropriate values,
      And the request [is made by Matt to place an NOC Request for C1],
      And the request [contains all correct answers in the correct format],
      And the request [contains answers identifying case role R2],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

     Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify that Matt has NOT been granted any case roles for the case] will get the expected response as in [F-206_Verify_Not_Granted_Case_Roles_Matt],
      And another call [to verify there is a pending NOC request on the case and the OrganisationPolicy for R2 has NOT been updated] will get the expected response as in [F-206_Verify_Case_Data_COR_Pending].


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-4
@S-206.4
Scenario: (Happy Path) CAA requests NoC for a non-represented litigant - no auto-approval applies

    Given a user [Mario - to initiate a case on his own behalf via a citizen facing application],
      And a user [Matt - with a pui-caa role],
      And a successful call [by Mario to create a case C1, which contains 3 Org Policies for 3 case roles: R1 which is assigned to Matt's organisation, R2 & R3 which are both not assigned to any organisation] as in [F-206_NoC_Case_Creation_By_Mario_With_Assigned_R1_Org_Policy],
      And a wait time of [5] seconds [to allow for the case just created to appear in search results],

     When a request is prepared with appropriate values,
      And the request [is made by Matt to place an NOC Request for C1],
      And the request [contains all correct answers in the correct format],
      And the request [contains answers identifying case role R2],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

     Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify that Matt has NOT been granted any case roles for the case] will get the expected response as in [F-206_Verify_Not_Granted_Case_Roles_Matt],
      And another call [to verify there is a pending NOC request on the case and the OrganisationPolicy for R2 has NOT been updated] will get the expected response as in [F-206_Verify_Case_Data_COR_Pending].


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-5
@S-206.5
Scenario: (Happy Path) CAA (also a solicitor for a different jurisdiction) requests NoC to replace representation - no auto-approval applies

    Given a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Alice - with a pui-caa role and a solicitor role for a different jurisdiction, within a different organisation from Richard's],
      And [a citizen Mario, on behalf of whome Richard will create a case] in the context,
      And a successful call [by Richard to create a case C1 on behalf of Mario, which contains 3 Org Policies for 3 case roles: R1 which is assigned to Alice's organisation, R2 & R3 which are both assigned to Richard's organisation] as in [F-206_NoC_Case_Creation_By_Richard_With_Assigned_Org_Policies],
      And a wait time of [5] seconds [to allow for the case just created to appear in search results],

     When a request is prepared with appropriate values,
      And the request [is made by Alice to place an NOC Request for C1],
      And the request [contains all correct answers in the correct format],
      And the request [contains answers identifying case role R2],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

     Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify that Alice has NOT been granted any case roles for the case] will get the expected response as in [F-206_Verify_Not_Granted_Case_Roles_Alice],
      And another call [to verify there is a pending NOC request on the case and the OrganisationPolicy for R2 has NOT been updated] will get the expected response as in [F-206_Verify_Case_Data_COR_Pending].


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-6
  @S-206.6
  Scenario: (Happy Path) CAA (also a solicitor for a different jurisdiction) successfully Requests NoC for a non-represented litigant - no auto-approval applies

    Given a user [Mario - to initiate a case on his own behalf via a citizen facing application],
      And a user [Alice - with a pui-caa role and a solicitor role for a different jurisdiction as that of a case initiated by Mario],
      And a successful call [by Mario to create a case C1, which contains 3 Org Policies for 3 case roles: R1 which is assigned to Alice's organisation, R2 & R3 which are both not assigned to any organisation] as in [F-206_NoC_Case_Creation_By_Mario_With_Assigned_R1_Org_Policy],
      And a wait time of [5] seconds [to allow for the case just created to appear in search results],

     When a request is prepared with appropriate values,
      And the request [is made by Alice to place an NOC Request for C1],
      And the request [contains all correct answers in the correct format],
      And the request [contains answers identifying case role R2],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

     Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify that Alice has NOT been granted any case roles for the case] will get the expected response as in [F-206_Verify_Not_Granted_Case_Roles_Alice],
      And another call [to verify there is a pending NOC request on the case and the OrganisationPolicy for R2 has NOT been updated] will get the expected response as in [F-206_Verify_Case_Data_COR_Pending].


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-7
@S-206.7
Scenario: (Happy Path) Solicitor requests NoC to replace representation - auto-approval applies

    Given a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Dil - with a solicitor role for the same jurisdiction, within a different organisation from Richard's],
      And [a citizen Mario, on behalf of whome Richard will create a case] in the context,
      And a successful call [by Richard to create a case C1 on behalf of Mario, which contains 3 Org Policies for 3 case roles: R1 which is assigned to Dil's organisation, R2 & R3 which are both assigned to Richard's organisation] as in [F-206_NoC_Auto_Approval_Case_Creation_By_Richard_With_Assigned_Org_Policies],
      And a wait time of [5] seconds [to allow for the case just created to appear in search results],

     When a request is prepared with appropriate values,
      And the request [is made by Dil to place a NOC Request for C1],
      And the request [contains all correct answers in the correct format],
      And the request [contains answers identifying case role R2],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

     Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify that Dil HAS been granted case roles R1 & R2 for the case but not R3] will get the expected response as in [F-206_Verify_Granted_Case_Roles_R1_R2_Dil],
      And a call [to verify there is NO pending NOC request on the case and the OrganisationPolicy for R2 HAS been updated] will get the expected response as in [F-206_Verify_Case_Data_COR_Approved].


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-8
@S-206.8
Scenario: (Happy Path) Solicitor requests NoC for a non-represented litigant - auto-approval applies

    Given a user [Mario - to initiate a case on his own behalf via a citizen facing application],
      And a user [Dil - with a solicitor role for the same jurisdiction as that of a case initiated by Mario],
      And a successful call [by Mario to create a case C1, which contains 3 Org Policies for 3 case roles: R1 which is assigned to Dil's organisation, R2 & R3 which are both not assigned to any organisation] as in [F-206_NoC_Auto_Approval_Case_Creation_By_Mario_With_Assigned_R1_Org_Policy],
      And a wait time of [5] seconds [to allow for the case just created to appear in search results],

     When a request is prepared with appropriate values,
      And the request [is made by Dil to place a NOC Request for C1],
      And the request [contains all correct answers in the correct format],
      And the request [contains answers identifying case role R2],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

     Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify that Dil HAS been granted case roles R1 & R2 for the case but not R3] will get the expected response as in [F-206_Verify_Granted_Case_Roles_R1_R2_Dil],
      And a call [to verify there is NO pending NOC request on the case and the OrganisationPolicy for R2 HAS been updated] will get the expected response as in [F-206_Verify_Case_Data_COR_Approved].


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-9
@S-206.9
Scenario: (Happy Path) CAA requests NoC to replace representation - auto-approval applies

    Given a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Matt - with a pui-caa role, within a different organisation from Richard's],
      And [a citizen Mario, on behalf of whome Richard will create a case] in the context,
      And a successful call [by Richard to create a case C1 on behalf of Mario, which contains 3 Org Policies for 3 case roles: R1 which is assigned to Matt's organisation, R2 & R3 which are both assigned to Richard's organisation] as in [F-206_NoC_Auto_Approval_Case_Creation_By_Richard_With_Assigned_Org_Policies],
      And a wait time of [5] seconds [to allow for the case just created to appear in search results],

     When a request is prepared with appropriate values,
      And the request [is made by Matt to place a NOC Request for C1],
      And the request [contains all correct answers in the correct format],
      And the request [contains answers identifying case role R2],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

     Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify that Matt has NOT been granted any case roles for the case] will get the expected response as in [F-206_Verify_Not_Granted_Case_Roles_Matt],
      And another call [to verify there is NO pending NOC request on the case and the OrganisationPolicy for R2 HAS been updated] will get the expected response as in [F-206_Verify_Case_Data_COR_Approved].


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-10
@S-206.10
Scenario: (Happy Path) CAA requests NoC for a non-represented litigant - auto-approval applies

    Given a user [Mario - to initiate a case on his own behalf via a citizen facing application],
      And a user [Matt - with a pui-caa role],
      And a successful call [by Mario to create a case C1, which contains 3 Org Policies for 3 case roles: R1 which is assigned to Matt's organisation, R2 & R3 which are both not assigned to any organisation] as in [F-206_NoC_Auto_Approval_Case_Creation_By_Mario_With_Assigned_R1_Org_Policy],
      And a wait time of [5] seconds [to allow for the case just created to appear in search results],

     When a request is prepared with appropriate values,
      And the request [is made by Matt to place a NOC Request for C1],
      And the request [contains all correct answers in the correct format],
      And the request [contains answers identifying case role R2],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

     Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify that Matt has NOT been granted any case roles for the case] will get the expected response as in [F-206_Verify_Not_Granted_Case_Roles_Matt],
      And another call [to verify there is NO pending NOC request on the case and the OrganisationPolicy for R2 HAS been updated] will get the expected response as in [F-206_Verify_Case_Data_COR_Approved].


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-11
@S-206.11
Scenario: (Happy Path) CAA (also a solicitor for a different jurisdiction) requests NoC to replace representation - auto-approval applies

    Given a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Alice - with a pui-caa role and a solicitor role for a different jurisdiction, within a different organisation from Richard's],
      And [a citizen Mario, on behalf of whome Richard will create a case] in the context,
      And a successful call [by Richard to create a case C1 on behalf of Mario, which contains 3 Org Policies for 3 case roles: R1 which is assigned to Alice's organisation, R2 & R3 which are both assigned to Richard's organisation] as in [F-206_NoC_Auto_Approval_Case_Creation_By_Richard_With_Assigned_Org_Policies],
      And a wait time of [5] seconds [to allow for the case just created to appear in search results],

     When a request is prepared with appropriate values,
      And the request [is made by Alice to place a NOC Request for C1],
      And the request [contains all correct answers in the correct format],
      And the request [contains answers identifying case role R2],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

     Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify that Alice has NOT been granted any case roles for the case] will get the expected response as in [F-206_Verify_Not_Granted_Case_Roles_Alice],
      And another call [to verify there is NO pending NOC request on the case and the OrganisationPolicy for R2 HAS been updated] will get the expected response as in [F-206_Verify_Case_Data_COR_Approved].


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-12
@S-206.12
Scenario: (Happy Path) CAA (also a solicitor for a different jurisdiction) requests NoC for a non-represented litigant - auto-approval applies

    Given a user [Mario - to initiate a case on his own behalf via a citizen facing application],
      And a user [Alice - with a pui-caa role and a solicitor role for a different jurisdiction as that of a case initiated by Mario],
      And a successful call [by Mario to create a case C1, which contains 3 Org Policies for 3 case roles: R1 which is assigned to Alice's organisation, R2 & R3 which are both not assigned to any organisation] as in [F-206_NoC_Auto_Approval_Case_Creation_By_Mario_With_Assigned_R1_Org_Policy],
      And a wait time of [5] seconds [to allow for the case just created to appear in search results],

     When a request is prepared with appropriate values,
      And the request [is made by Alice to place a NOC Request for C1],
      And the request [contains all correct answers in the correct format],
      And the request [contains answers identifying case role R2],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

     Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify that Alice has NOT been granted any case roles for the case] will get the expected response as in [F-206_Verify_Not_Granted_Case_Roles_Alice],
      And another call [to verify there is NO pending NOC request on the case and the OrganisationPolicy for R2 HAS been updated] will get the expected response as in [F-206_Verify_Case_Data_COR_Approved].


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-13
@S-206.13
Scenario: (Happy Path) CAA (also a solicitor for the same jurisdiction) requests NoC to replace representation - auto-approval applies

    Given a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Jane - with a pui-caa role and a solicitor role for the same jurisdiction, within a different organisation from Richard's],
      And [a citizen Mario, on behalf of whome Richard will create a case] in the context,
      And a successful call [by Richard to create a case C1 on behalf of Mario, which contains 3 Org Policies for 3 case roles: R1 which is assigned to Jane's organisation, R2 & R3 which are both assigned to Richard's organisation] as in [F-206_NoC_Auto_Approval_Case_Creation_By_Richard_With_Assigned_Org_Policies],
      And a wait time of [5] seconds [to allow for the case just created to appear in search results],

     When a request is prepared with appropriate values,
      And the request [is made by Jane to place a NOC Request for C1],
      And the request [contains all correct answers in the correct format],
      And the request [contains answers identifying case role R2],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

     Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify that Jane HAS been granted case roles R1 & R2 for the case but not R3] will get the expected response as in [F-206_Verify_Granted_Case_Roles_R1_R2_Jane],
      And a call [to verify there is NO pending NOC request on the case and the OrganisationPolicy for R2 HAS been updated] will get the expected response as in [F-206_Verify_Case_Data_COR_Approved].


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-14
@S-206.14
Scenario: (Happy Path) CAA (also a solicitor for the same jurisdiction) requests NoC for a non-represented litigant - auto-approval applies

    Given a user [Mario - to initiate a case on his own behalf via a citizen facing application],
      And a user [Jane - with a pui-caa role and a solicitor role for the same jurisdiction as that of a case initiated by Mario],
      And a successful call [by Mario to create a case C1, which contains 3 Org Policies for 3 case roles: R1 which is assigned to Jane's organisation, R2 & R3 which are both not assigned to any organisation] as in [F-206_NoC_Auto_Approval_Case_Creation_By_Mario_With_Assigned_R1_Org_Policy],
      And a wait time of [5] seconds [to allow for the case just created to appear in search results],

     When a request is prepared with appropriate values,
      And the request [is made by Jane to place a NOC Request for C1],
      And the request [contains all correct answers in the correct format],
      And the request [contains answers identifying case role R2],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

     Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify that Jane HAS been granted case roles R1 & R2 for the case but not R3] will get the expected response as in [F-206_Verify_Granted_Case_Roles_R1_R2_Jane],
      And a call [to verify there is NO pending NOC request on the case and the OrganisationPolicy for R2 HAS been updated] will get the expected response as in [F-206_Verify_Case_Data_COR_Approved]. 

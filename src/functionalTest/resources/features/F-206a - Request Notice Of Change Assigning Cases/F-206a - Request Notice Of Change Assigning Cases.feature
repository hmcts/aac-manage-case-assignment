#=============================================
@F-206a
Feature: F-206a: Request Notice of Change (NoC) - Assigning Cases
#=============================================

Background:
    Given an appropriate test context as detailed in the test data source

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

  @S-206a.01  @callbackTests #(S-206.7 REPLACEMENT)
  Scenario: NoC is requested by a Solicitor having STANDARD access to case - auto-approval applies

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

  @S-206a.02 @callbackTests
  Scenario: NoC is requested by a Solicitor having SPECIFIC access to case - auto-approval applies

    Given a user [BeftaCaseworkerCaa - with the ability to create a case for a particular jurisdiction within an organisation],
    And a user [Dil - with a solicitor role for the same jurisdiction, within a different organisation from BeftaCaseworkerCaa's],
    And [a citizen Mario, on behalf of whom BeftaCaseworkerCaa will create a case] in the context,
    And a successful call [by BeftaCaseworkerCaa to create a case C1 on behalf of Mario, which contains 3 Org Policies for 3 case roles: R1 which is assigned to Dil's organisation, R2 & R3 which are both assigned to BeftaCaseworkerCaa's organisation] as in [F-206_NoC_Auto_Approval_Case_Creation_By_BeftaCaseworkerCaa_With_Assigned_Org_Policies],

    When a request is prepared with appropriate values,
    And the request [is made by Dil to place a NOC Request for C1],
    And the request [contains all correct answers in the correct format],
    And the request [contains answers identifying case role R2],
    And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

    Then a positive response is received,
    And the response has all the details as expected,
    And a call [to verify that specific access has been granted to BeftaCaseworkerCaa for the case] will get the expected response as in [F-206_Verify_Granted_Specific_Case_Roles_BeftaCaseworkerCaa],
    And a call [to verify there is NO pending NOC request on the case and the OrganisationPolicy for R2 HAS been updated] will get the expected response as in [F-206_Verify_Case_Data_COR_Approved_ReplaceRepresentation],
    And a call [to get Case Events API returns a NoCRequest event in which the user ID is set to invoking users email address AND the proxied_by field set to the ID of the system user] will get the expected response as in [F-206_Verify_NoC_Request_Event_Data].
    And a call [to get Grant Access Metadata API returning Standard Grant Access for case] will get the expected response as in [F-206_Verify_NoC_Request_Access_Metadata],


  @Ignore @S-206a.03 @callbackTests #SPECIFIC
  Scenario: NoC is requested by a Solicitor having SPECIFIC access to case - auto-approval applies

    Given a user [BeftaCaseworkerCaa - with the ability to create a case for a particular jurisdiction within an organisation],
    And a user [Dil - with a solicitor role for the same jurisdiction, within a different organisation from Richard's],
    And [a citizen Mario, on behalf of whom Richard will create a case] in the context,
    And a successful call [by Richard to create a case C1 on behalf of Mario, which contains 3 Org Policies for 3 case roles: R1 which is assigned to Dil's organisation, R2 & R3 which are both assigned to Richard's organisation] as in [F-206_NoC_Auto_Approval_Case_Creation_By_Richard_With_Assigned_Org_Policies],
    And a successful call [to grant SPECIFIC access to the caseworker] as in [GrantAccess_FT_NoCAutoApprovalCaseTypeRole_Caseworker_SPECIFIC]

    When a request is prepared with appropriate values,
    And the request [is made by Dil to place a NOC Request for C1],
    And the request [contains all correct answers in the correct format],
    And the request [contains answers identifying case role R2],
    And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

    Then a positive response is received,
    And the response has all the details as expected,
    And a call [to verify that specific access has been granted to BeftaCaseworkerCaa for the case] will get the expected response as in [F-206_Verify_Granted_Case_Roles_BeftaCaseworkerCaa],
    And a call [to verify there is NO pending NOC request on the case and the OrganisationPolicy for R2 HAS been updated] will get the expected response as in [F-206_Verify_Case_Data_COR_Approved_ReplaceRepresentation],
    And a call [to get Case Events API returns a NoCRequest event in which the user ID is set to invoking users email address AND the proxied_by field set to the ID of the system user] will get the expected response as in [F-206_Verify_NoC_Request_Event_Data].
    And a call [to get Grant Access Metadata API returning Standard Grant Access for case] will get the expected response as in [F-206_Verify_NoC_Request_Access_Metadata],

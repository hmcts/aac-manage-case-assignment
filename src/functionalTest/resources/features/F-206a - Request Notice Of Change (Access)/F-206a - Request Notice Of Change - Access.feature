#========================================================
@F-206a
Feature: F-206a: Request Notice of Change (NoC) (Access)
#========================================================

Background:
    Given an appropriate test context as detailed in the test data source


#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# CCD-5334 / AC02
@S-206a.01 @callbackTests
Scenario: (Happy Path) CAA requests NoC to replace representation - auto-approval applies

    Given a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Matt - with a pui-caa role, within a different organisation from Richard's],

      And [a citizen Mario, on behalf of whom Richard will create a case] in the context,
      And a successful call [by Richard to create a case C1 on behalf of Mario, which contains 3 Org Policies for 3 case roles: R1 which is assigned to Matt's organisation, R2 & R3 which are both assigned to Richard's organisation] as in [F-206_NoC_Auto_Approval_Case_Creation_By_Richard_With_Assigned_Org_Policies],
      When a request is prepared with appropriate values,

      #Matt need to be granted Specific access to the case before he can request a NoC
      And a successful call [to grant access to user with a case role BARRISTER over the case created] as in [S-206_Grant_Access],


      And the request [is made by Matt to place a NOC Request for C1],
      And the request [contains all correct answers in the correct format],
      And the request [contains answers identifying case role R2],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

     Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify that Matt has NOT been granted any case roles for the case] will get the expected response as in [F-206_Verify_Not_Granted_Case_Roles_Matt],
      And another call [to verify there is NO pending NOC request on the case and the OrganisationPolicy for R2 HAS been updated] will get the expected response as in [F-206_Verify_Case_Data_COR_Approved_ReplaceRepresentation],
      And another call [to get Case Events API returns a NoCRequest event in which the user ID is set to invoking users email address AND the proxied_by field set to the ID of the system user] will get the expected response as in [F-206_Verify_NoC_Request_Event_Data].
      And a call [to get Grant Access Metadata API returning Standard Grant Access for case] will get the expected response as in [F-206_Verify_NoC_Request_Access_Metadata]

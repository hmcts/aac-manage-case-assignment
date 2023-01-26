@F-210
Feature: F-210: Organisations Assigned Users

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
#  SINGLE CASE
#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

  @S-210.00
  Scenario: SINGLE CASE: Forbidden response when wrong service

    Given an appropriate test context as detailed in the test data source,
      And a user [INVOKER - with access to case],

     When a request is prepared with appropriate values,
      And the request [is made with a valid case id],
      And the request [is made using a service not authorised to perform operation],
      And it is submitted to call the [OrganisationsAssignedUsers-ResetForCase] operation of [Manage Case Assignment Microservice],

     Then a negative response is received,
      And the response [has the 403 return code],
      And the response has all the details as expected

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

  @S-210.01
  Scenario: SINGLE CASE: Dry Run, 2 Orgs with assigned users

    Given an appropriate test context as detailed in the test data source,
      And a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation (Org2)],
      And a user [Bill - with a solicitor role for the same jurisdiction within a different organisation (Org1) from Richard],
      And a user [Benjamin - with a solicitor role for the same jurisdiction within a different organisation (Org1) from Richard],
      And a user [INVOKER - with access to case],

      And a successful call [by Richard to create a case - C1 - is auto-assigned to both orgs] as in [F-210_Case_Creation_C1_With_Org_Policies_Both_Orgs],
      And a successful call [to set HMCTSServiceId in Supplementary Data for C1] as in [F-210_Supplementary_Data__HMCTSServiceId_C1],
      And a successful call [to remove CREATOR assigned CaseRole for C1] as in [F-210_Delete_Creator_Role_C1],

      And a successful call [to assign users to the case C1 but not update OrgCount - 2 users with same role] as in [F-210_Assign_Access_Without_OrgId_C1_Org1_QUK822N],
      And a successful call [to assign users to the case C1 but not update OrgCount - 1 user with 2 roles] as in [F-210_Assign_Access_Without_OrgId_C1_Org2_LESTKK0],

     When a request is prepared with appropriate values,
      And the request [is made after roles assigned to Org 1 - 2 users with same role],
      And the request [is made after roles assigned to Org 2 - 1 user with 2 roles],
      And the request [is made for the case record for C1],
      And it is submitted to call the [OrganisationsAssignedUsers-ResetForCase] operation of [Manage Case Assignment Microservice],

     Then a positive response is received,
      And the response has all the details as expected,
      And the response [has Org 1 count with 2 users],
      And the response [has Org 2 count with 1 user],

      And a successful call [to workaround $inc by zero issue in Supplementary Data for C1] as in [F-210_Supplementary_Data__Workaround_C1],
      And a successful call [to verify the supplimentary data's org counts are unchanged for C1] as in [F-210_Check_Orgs_Assigned_Users__Not_Assigned_C1]

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

  @S-210.01_with_save
  Scenario: SINGLE CASE: Save, 2 Orgs with assigned users

    Given an appropriate test context as detailed in the test data source,
      And a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation (Org2)],
      And a user [Bill - with a solicitor role for the same jurisdiction within a different organisation (Org1) from Richard],
      And a user [Benjamin - with a solicitor role for the same jurisdiction within a different organisation (Org1) from Richard],
      And a user [INVOKER - with access to case],

      And a successful call [by Richard to create a case - C1 - is auto-assigned to both orgs] as in [F-210_Case_Creation_C1_With_Org_Policies_Both_Orgs],
      And a successful call [to set HMCTSServiceId in Supplementary Data for C1] as in [F-210_Supplementary_Data__HMCTSServiceId_C1],
      And a successful call [to remove CREATOR assigned CaseRole for C1] as in [F-210_Delete_Creator_Role_C1],

      And a successful call [to assign users to the case C1 but not update OrgCount - 2 users with same role] as in [F-210_Assign_Access_Without_OrgId_C1_Org1_QUK822N],
      And a successful call [to assign users to the case C1 but not update OrgCount - 1 user with 2 roles] as in [F-210_Assign_Access_Without_OrgId_C1_Org2_LESTKK0],

     When a request is prepared with appropriate values,
      And the request [is made after roles assigned to Org 1 - 2 users with same role],
      And the request [is made after roles assigned to Org 2 - 1 user with 2 roles],
      And the request [is made for the case record for C1],
      And the request [is made with save, i.e. dry_run=false],
      And it is submitted to call the [OrganisationsAssignedUsers-ResetForCase] operation of [Manage Case Assignment Microservice],

     Then a positive response is received,
      And the response has all the details as expected,
      And the response [has Org 1 count with 2 users],
      And the response [has Org 2 count with 1 user],

      And a successful call [to verify the supplimentary data's org counts have been updated for C1] as in [F-210_Check_Orgs_Assigned_Users__Both_Assigned_C1],

      #--- verify impact of unassign: i.e. does unassign stil work and is count updated correctly
      And a successful call [to unassign users to the case C1 with update OrgCount - only 1 of 2 users] as in [F-210_Delete_Access_With_OrgId_C1_Org1_QUK822N],
      And a successful call [to unassign users to the case C1 with update OrgCount - 1 user but only only 1 of 2 roles] as in [F-210_Delete_Access_With_OrgId_C1_Org2_LESTKK0],
      And a successful call [to verify the supplimentary data's org counts have been updated after unassign for C1] as in [F-210_Check_Orgs_Assigned_Users__After_Unassign_C1],

      And a successful call [repeat call after unassign] as in [S-210.01_repeat_after_unassign]

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

  @S-210.02
  Scenario: SINGLE CASE: No assigned organisations

    Given an appropriate test context as detailed in the test data source,
      And a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation (Org2)],
      And a user [INVOKER - with access to case],

      And a successful call [by Richard to create a case - C2 - is not assigned to any orgs] as in [F-210_Case_Creation_C2_With_Org_Policies_Unassigned],
      And a successful call [to set HMCTSServiceId in Supplementary Data for C2] as in [F-210_Supplementary_Data__HMCTSServiceId_C2],
      And a successful call [to remove CREATOR assigned CaseRole for C2] as in [F-210_Delete_Creator_Role_C2],

     When a request is prepared with appropriate values,
      And the request [is made for the case record for C2],
      And it is submitted to call the [OrganisationsAssignedUsers-ResetForCase] operation of [Manage Case Assignment Microservice],

     Then a positive response is received,
      And the response has all the details as expected,
      And the response [has no org counts],

      And a successful call [to workaround $inc by zero issue in Supplementary Data for C2] as in [F-210_Supplementary_Data__Workaround_C2],
      And a successful call [to verify the supplimentary data's org counts are unchanged for C2] as in [F-210_Check_Orgs_Assigned_Users__Not_Assigned_C2]

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

  @S-210.03
  Scenario: SINGLE CASE: Dry Run, 1 GOOD Org with assigned users, 1 BAD org

    Given an appropriate test context as detailed in the test data source,
      And a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation (Org2)],
      And a user [INVOKER - with access to case],

      And a successful call [By Richard to create a case - C3 - is assigned to one GOOD org and 1 BAD org (i.e. unknown OrgID)] as in [F-210_Case_Creation_C3_With_Org_Policies_Assigned_Good_Org_Bad_Org],
      And a successful call [to set HMCTSServiceId in Supplementary Data for C3] as in [F-210_Supplementary_Data__HMCTSServiceId_C3],
      And a successful call [to remove CREATOR assigned CaseRole for C3] as in [F-210_Delete_Creator_Role_C3],

      And a successful call [to assign users to the case C3 but not update OrgCount - 1 user with 2 roles] as in [F-210_Assign_Access_Without_OrgId_C3_Org2_LESTKK0],

     When a request is prepared with appropriate values,
      And the request [is made after roles assigned to Org 2 - 1 user with 2 roles],
      And the request [is made for the case record for C3],
      And it is submitted to call the [OrganisationsAssignedUsers-ResetForCase] operation of [Manage Case Assignment Microservice],

     Then a positive response is received,
      And the response has all the details as expected,
      And the response [has Org 2 count with 1 user],
      And the response [has error for BAD Org],

      And a successful call [to workaround $inc by zero issue in Supplementary Data for C3] as in [F-210_Supplementary_Data__Workaround_C3],
      And a successful call [to verify the supplimentary data's org counts are unchanged for C3] as in [F-210_Check_Orgs_Assigned_Users__Not_Assigned_C3]

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

  @S-210.04
  Scenario: SINGLE CASE: Save (but no counts found), 1 Org with no assigned users

    Given an appropriate test context as detailed in the test data source,
      And a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation (Org2)],
      And a user [INVOKER - with access to case],

      And a successful call [By Richard to create a case - C4 - is assigned to one org] as in [F-210_Case_Creation_C4_With_Org_Policies_Assigned_To_1_Org],
      And a successful call [to set HMCTSServiceId in Supplementary Data for C4] as in [F-210_Supplementary_Data__HMCTSServiceId_C4],
      And a successful call [to remove CREATOR assigned CaseRole for C4] as in [F-210_Delete_Creator_Role_C4],

     When a request is prepared with appropriate values,
      And the request [is made when no users assigned to case],
      And the request [is made for the case record for C4],
      And the request [is made with save, i.e. dry_run=false],
      And it is submitted to call the [OrganisationsAssignedUsers-ResetForCase] operation of [Manage Case Assignment Microservice],

     Then a positive response is received,
      And the response has all the details as expected,
      And the response [has Org 2 count with ZERO users],

      And a successful call [to workaround $inc by zero issue in Supplementary Data for C4] as in [F-210_Supplementary_Data__Workaround_C4],
      And a successful call [to verify the supplimentary data's org counts are unchanged for C4] as in [F-210_Check_Orgs_Assigned_Users__Not_Assigned_C4]

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

  @S-210.05
  Scenario: SINGLE CASE: Dry Run, 2 Orgs with no assigned users to case

    Given an appropriate test context as detailed in the test data source,
      And a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation (Org2)],
      And a user [INVOKER - with access to case],

      And a successful call [by Richard to create a case - C1 - is auto-assigned to both orgs] as in [F-210_Case_Creation_C1_With_Org_Policies_Both_Orgs],
      And a successful call [to set HMCTSServiceId in Supplementary Data for C1] as in [F-210_Supplementary_Data__HMCTSServiceId_C1],
      And a successful call [to remove CREATOR assigned CaseRole for C1] as in [F-210_Delete_Creator_Role_C1],

     When a request is prepared with appropriate values,
      And the request [is made when no users assigned to case],
      And the request [is made for the case record for C1],
      And it is submitted to call the [OrganisationsAssignedUsers-ResetForCase] operation of [Manage Case Assignment Microservice],

     Then a positive response is received,
      And the response has all the details as expected,
      And the response [has Org 1 count with 0 users],
      And the response [has Org 2 count with 0 users],

      And a successful call [to workaround $inc by zero issue in Supplementary Data for C1] as in [F-210_Supplementary_Data__Workaround_C1],
      And a successful call [to verify the supplimentary data's org counts are unchanged for C1] as in [F-210_Check_Orgs_Assigned_Users__Not_Assigned_C1]

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

  @S-210.06
  Scenario: SINGLE CASE: Not found response when case not found

    Given an appropriate test context as detailed in the test data source,
      And a user [INVOKER - with access to case],

     When a request is prepared with appropriate values,
      And the request [is made for a case record that does not exist],
      And it is submitted to call the [OrganisationsAssignedUsers-ResetForCase] operation of [Manage Case Assignment Microservice],

     Then a negative response is received,
      And the response [has the 404 return code],
      And the response has all the details as expected

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
#  MULTIPLE CASE
#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

  @S-210.10
  Scenario: MULTIPLE CASE: Forbidden response when wrong service

    Given an appropriate test context as detailed in the test data source,
      And a user [INVOKER - with access to case],

     When a request is prepared with appropriate values,
      And the request [is made with a valid case id],
      And the request [is made using a service not authorised to perform operation],
      And it is submitted to call the [OrganisationsAssignedUsers-ResetForCases] operation of [Manage Case Assignment Microservice],

     Then a negative response is received,
      And the response [has the 403 return code],
      And the response has all the details as expected

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

  @S-210.11
  Scenario: MULTIPLE CASE: Dry Run, 4 cases + 1 not found

    Given an appropriate test context as detailed in the test data source,
      And a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation (Org2)],
      And a user [Bill - with a solicitor role for the same jurisdiction within a different organisation (Org1) from Richard],
      And a user [Benjamin - with a solicitor role for the same jurisdiction within a different organisation (Org1) from Richard],
      And a user [INVOKER - with access to case],

      And a successful call [by Richard to create a case - C1 - is auto-assigned to both orgs] as in [F-210_Case_Creation_C1_With_Org_Policies_Both_Orgs],
      And a successful call [by Richard to create a case - C2 - is not assigned to any orgs] as in [F-210_Case_Creation_C2_With_Org_Policies_Unassigned],
      And a successful call [By Richard to create a case - C3 - is assigned to one GOOD org and 1 BAD org (i.e. unknown OrgID)] as in [F-210_Case_Creation_C3_With_Org_Policies_Assigned_Good_Org_Bad_Org],
      And a successful call [By Richard to create a case - C4 - is assigned to one org] as in [F-210_Case_Creation_C4_With_Org_Policies_Assigned_To_1_Org],

      And a successful call [to set HMCTSServiceId in Supplementary Data for C1] as in [F-210_Supplementary_Data__HMCTSServiceId_C1],
      And a successful call [to set HMCTSServiceId in Supplementary Data for C2] as in [F-210_Supplementary_Data__HMCTSServiceId_C2],
      And a successful call [to set HMCTSServiceId in Supplementary Data for C3] as in [F-210_Supplementary_Data__HMCTSServiceId_C3],
      And a successful call [to set HMCTSServiceId in Supplementary Data for C4] as in [F-210_Supplementary_Data__HMCTSServiceId_C4],

      And a successful call [to assign users to the case C1 but not update OrgCount - 2 users with same role] as in [F-210_Assign_Access_Without_OrgId_C1_Org1_QUK822N],
      And a successful call [to assign users to the case C1 but not update OrgCount - 1 user with 2 roles] as in [F-210_Assign_Access_Without_OrgId_C1_Org2_LESTKK0],
      And a successful call [to assign users to the case C3 but not update OrgCount - 1 user with 2 roles] as in [F-210_Assign_Access_Without_OrgId_C3_Org2_LESTKK0],

     When a request is prepared with appropriate values,
      And the request [is made after roles assigned to C1 Org 1 - 2 users with same role],
      And the request [is made after roles assigned to C1 Org 2 - 1 user with 2 roles],
      And the request [is made after roles assigned to C3 Org 2 - 1 user with 2 roles],
      And the request [is made for the case record for C1,C2,C3,C4,NotFoundCase],
      And it is submitted to call the [OrganisationsAssignedUsers-ResetForCases] operation of [Manage Case Assignment Microservice],

     Then a positive response is received,
      And the response has all the details as expected,
      And the response [has C1 Org 1 count with 2 users],
      And the response [has C1 Org 2 count with 1 user],
      And the response [has C2 no org counts],
      And the response [has C3 Org 2 count with 1 user],
      And the response [has C3 error for BAD Org],
      And the response [has C4 Org 2 count with ZERO users],
      And the response [has error for not found case],

      And a successful call [to workaround $inc by zero issue in Supplementary Data for C1] as in [F-210_Supplementary_Data__Workaround_C1],
      And a successful call [to workaround $inc by zero issue in Supplementary Data for C2] as in [F-210_Supplementary_Data__Workaround_C2],
      And a successful call [to workaround $inc by zero issue in Supplementary Data for C3] as in [F-210_Supplementary_Data__Workaround_C3],
      And a successful call [to workaround $inc by zero issue in Supplementary Data for C4] as in [F-210_Supplementary_Data__Workaround_C4],

      And a successful call [to verify the supplimentary data's org counts are unchanged for C1] as in [F-210_Check_Orgs_Assigned_Users__Not_Assigned_C1],
      And a successful call [to verify the supplimentary data's org counts are unchanged for C2] as in [F-210_Check_Orgs_Assigned_Users__Not_Assigned_C2],
      And a successful call [to verify the supplimentary data's org counts are unchanged for C3] as in [F-210_Check_Orgs_Assigned_Users__Not_Assigned_C3],
      And a successful call [to verify the supplimentary data's org counts are unchanged for C4] as in [F-210_Check_Orgs_Assigned_Users__Not_Assigned_C4]

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

  @S-210.11_with_save
  Scenario: MULTIPLE CASE: Save, 4 cases + 1 not found

    Given an appropriate test context as detailed in the test data source,
      And a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation (Org2)],
      And a user [Bill - with a solicitor role for the same jurisdiction within a different organisation (Org1) from Richard],
      And a user [Benjamin - with a solicitor role for the same jurisdiction within a different organisation (Org1) from Richard],
      And a user [INVOKER - with access to case],

      And a successful call [by Richard to create a case - C1 - is auto-assigned to both orgs] as in [F-210_Case_Creation_C1_With_Org_Policies_Both_Orgs],
      And a successful call [by Richard to create a case - C2 - is not assigned to any orgs] as in [F-210_Case_Creation_C2_With_Org_Policies_Unassigned],
      And a successful call [By Richard to create a case - C3 - is assigned to one GOOD org and 1 BAD org (i.e. unknown OrgID)] as in [F-210_Case_Creation_C3_With_Org_Policies_Assigned_Good_Org_Bad_Org],
      And a successful call [By Richard to create a case - C4 - is assigned to one org] as in [F-210_Case_Creation_C4_With_Org_Policies_Assigned_To_1_Org],

      And a successful call [to set HMCTSServiceId in Supplementary Data for C1] as in [F-210_Supplementary_Data__HMCTSServiceId_C1],
      And a successful call [to set HMCTSServiceId in Supplementary Data for C2] as in [F-210_Supplementary_Data__HMCTSServiceId_C2],
      And a successful call [to set HMCTSServiceId in Supplementary Data for C3] as in [F-210_Supplementary_Data__HMCTSServiceId_C3],
      And a successful call [to set HMCTSServiceId in Supplementary Data for C4] as in [F-210_Supplementary_Data__HMCTSServiceId_C4],

      And a successful call [to assign users to the case C1 but not update OrgCount - 2 users with same role] as in [F-210_Assign_Access_Without_OrgId_C1_Org1_QUK822N],
      And a successful call [to assign users to the case C1 but not update OrgCount - 1 user with 2 roles] as in [F-210_Assign_Access_Without_OrgId_C1_Org2_LESTKK0],
      And a successful call [to assign users to the case C3 but not update OrgCount - 1 user with 2 roles] as in [F-210_Assign_Access_Without_OrgId_C3_Org2_LESTKK0],

     When a request is prepared with appropriate values,
      And the request [is made after roles assigned to C1 Org 1 - 2 users with same role],
      And the request [is made after roles assigned to C1 Org 2 - 1 user with 2 roles],
      And the request [is made after roles assigned to C3 Org 2 - 1 user with 2 roles],
      And the request [is made for the case record for C1,C2,C3,C4,NotFoundCase],
      And the request [is made with save, i.e. dry_run=false],
      And it is submitted to call the [OrganisationsAssignedUsers-ResetForCases] operation of [Manage Case Assignment Microservice],

     Then a positive response is received,
      And the response has all the details as expected,
      And the response [has C1 Org 1 count with 2 users],
      And the response [has C1 Org 2 count with 1 user],
      And the response [has C2 no org counts],
      And the response [has C3 Org 2 count with 1 user],
      And the response [has C3 error for BAD Org],
      And the response [has C4 Org 2 count with ZERO users],
      And the response [has error for not found case],

      And a successful call [to workaround $inc by zero issue in Supplementary Data for C1] as in [F-210_Supplementary_Data__Workaround_C1],
      And a successful call [to workaround $inc by zero issue in Supplementary Data for C2] as in [F-210_Supplementary_Data__Workaround_C2],
      And a successful call [to workaround $inc by zero issue in Supplementary Data for C3] as in [F-210_Supplementary_Data__Workaround_C3],
      And a successful call [to workaround $inc by zero issue in Supplementary Data for C4] as in [F-210_Supplementary_Data__Workaround_C4],

      And a successful call [to verify the supplimentary data's org counts have been updated for C1] as in [F-210_Check_Orgs_Assigned_Users__Both_Assigned_C1],
      And a successful call [to verify the supplimentary data's org counts are unchanged for C2] as in [F-210_Check_Orgs_Assigned_Users__Not_Assigned_C2],
      And a successful call [to verify the supplimentary data's org counts have been updated for C3] as in [F-210_Check_Orgs_Assigned_Users__One_Assigned_C3],
      And a successful call [to verify the supplimentary data's org counts are unchanged for C4] as in [F-210_Check_Orgs_Assigned_Users__Not_Assigned_C4]

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

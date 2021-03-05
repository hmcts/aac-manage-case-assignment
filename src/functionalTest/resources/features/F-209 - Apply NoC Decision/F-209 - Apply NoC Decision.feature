@F-209
Feature:
  AS AN invoker of the ApplyNoCDecision API
  I WANT to check if a NoCRequest has been manually approved
  SO THAT I can apply the necessary steps to the case in relation to the action being taken

  @S-209.1
  Scenario: AC2 Happy path  Apply NoCDecision to remove and replace representation when a NoCRequest has been approved (Happy Path)
    Given an appropriate test context as detailed in the test data source,
    Given a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
    And a user [Dil - with a solicitor role for the same jurisdiction within a different organisation from Richard],
    And a successful call [by Richard to create a case - C1 - on behalf of Mario as the applicant which is auto-assigned to Richard's organisation] as in [F-209_NoC_Case_Creation_By_Richard_With_Assigned_Org_Policies],
    And a successful call [to remove CREATOR assigned CaseRole] as in [F-209_Delete_Creator_Role],
    And a successful call [to assign users to the case] as in [F-209_assign_access_to_case],
    And a wait time of [5] seconds [to allow for Logstash to index the case just created],
    And the request [by Dil to raise a NoCRequest to become the representative for Mario on C1],
    When a request is prepared with appropriate values,
    And the request [intends to apply a NoCDecision to a case],
    And the request [contains the case record for C1],
    And the request [contains an approval status of 'Approved']
    And it is submitted to call the [ApplyNoCDecision] operation of [Manage Case Assignment Microservice],
    Then a positive response is received,
    And the response has all the details as expected,
    And a call [to verify that Richard and anyone else in Richard's organisation has had their access removed] will get the expected response as in [F_209_get_case_users_richard_not_assigned],
    And a wait time of [5] seconds [to allow for Logstash to re-index the case],
    And a call [to verify that the supplementary counter has been adjusted to reflect the reduction in the number of users who have access to the case in Richard's organisation] will get the expected response as in [F_209_Search_Case_ES_0_users],
    And the response [has all fields in the ChangeOrganisationRequest nullified],

  @S-209.2
  Scenario: AC1: Apply NocDecision when a NoCRequest has been rejected (Happy Path)
    Given an appropriate test context as detailed in the test data source,
    And a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
    And a user [Dil - with a solicitor role for the same jurisdiction within a different organisation from Richard],
    And a successful call [by Richard to create a case - C1 - on behalf of Mario as the applicant which is auto-assigned to Richard's organisation] as in [F-209_NoC_Case_Creation_By_Richard_With_Assigned_Org_Policies],
    And a successful call [to remove CREATOR assigned CaseRole] as in [F-209_Delete_Creator_Role],
    And a successful call [to assign users to the case] as in [F-209_assign_access_to_case],
    When a request is prepared with appropriate values,
    And the request [intends to apply a NoCDecision to a case],
    And the request [contains the case record for C1],
    And the request [contains an approval status of 'Rejected'],
    And it is submitted to call the [ApplyNoCDecision] operation of [Manage Case Assignment Microservice],
    Then a positive response is received,
    And the response has all the details as expected,
    And a wait time of [5] seconds [to allow for Logstash to re-index the case],
    And a call [to verify that Richard still has access to the case and Dil does not have access - C1] will get the expected response as in [F_209_get_case_users_richard_assigned].
    And a call [to verify that the supplementary counter has been adjusted to reflect the reduction in the number of users who have access to the case in Richard's organisation] will get the expected response as in [F_209_Search_Case_ES_1_users]
    And the response [has all fields in the ChangeOrganisationRequest nullified],

  @S-209.3
  Scenario: AC5: Must return an error if the Case is currently in Pending status (ChangeOrganisationRequest.ApprovalStatus of the case = 'Not considered')
    Given an appropriate test context as detailed in the test data source,
    And a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
    And a user [Dil - with a solicitor role for the same jurisdiction within a different organisation from Richard],
    And a successful call [by Richard to create a case - C1 - on behalf of Mario as the applicant which is auto-assigned to Richard's organisation] as in [F-209_NoC_Case_Creation_By_Richard_With_Assigned_Org_Policies],
    And a successful call [to remove CREATOR assigned CaseRole] as in [F-209_Delete_Creator_Role],
    And a successful call [to assign users to the case] as in [F-209_assign_access_to_case],
    When a request is prepared with appropriate values,
    And the request [intends to apply a NoCDecision to a case],
    And the request [contains an approval status of 'Not Considered'],
    And it is submitted to call the [ApplyNoCDecision] operation of [Manage Case Assignment Microservice],
    Then a positive response is received,
    And the response has all the details as expected,
    And a wait time of [5] seconds [to allow for Logstash to re-index the case],
    And a call [to verify that Richard still has access to the case and Dil does not have access - C1] will get the expected response as in [F_209_get_case_users_richard_assigned],
    And a call [to verify that the supplementary counter remains unchanged for Dil's organisation] will get the expected response as in [F_209_Search_Case_ES_1_users]
    And the response [has an error stating a decision has not yet been received on the NoC Request],

    @S-209.4
  Scenario: AC3: Apply NoCDecision to add representation when a NoCRequest has been approved (Happy Path)
    Given an appropriate test context as detailed in the test data source,
    And a user [Mario - to initiate a case on his own behalf via a citizen facing application],
    And a user [Dil - with a solicitor role for the same jurisdiction as that of a case initiated by Mario],
    And a successful call [by Mario to create a case - C1 - via a citizen facing application] as in [F-209.4_NoC_Case_Creation_By_Mario_With_Assigned_Org_Policies],
    When a request is prepared with appropriate values,
    And the request [by Dil to raise a NoCRequest to become the representative for Mario on C1],
    And the request [intends to apply a NoCDecision to a case],
    And the request [contains the case record for C1],
    And the request [contains an approval status of 'Approved'],
    And it is submitted to call the [ApplyNoCDecision] operation of [Manage Case Assignment Microservice],
    Then a positive response is received,
    And the response has all the details as expected,
    And the response [has all fields in the ChangeOrganisationRequest nullified],

      @S-209.5
  Scenario: AC4: Apply NoCDecision to remove representation when NoCRequest has been approved (If ChangeOrganisationRequest.OrganisationToAdd is NULL) (Happy Path)
    Given an appropriate test context as detailed in the test data source,
    And a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
    And a user [Matt - with a pui-caa role for the same jurisdiction within a different organisation from Richard],
    And a successful call [by Richard to create a case - C1 - on behalf of Mario as the applicant which is auto-assigned to Richard's organisation] as in [F-209_NoC_Case_Creation_By_Richard_With_Assigned_Org_Policies],
    And a successful call [to remove CREATOR assigned CaseRole] as in [F-209_Delete_Creator_Role],
    And a successful call [to assign users to the case] as in [F-209_assign_access_to_case],
    When a request is prepared with appropriate values,
    And the request [intends to apply a NoCDecision to a case],
    And the request [contains the case record for C1],
    And the request [contains an approval status of 'Approved'],
    And the request [contains a ChangeOrganisationRequest.OrganisationToAdd field which is NULL],
    And it is submitted to call the [ApplyNoCDecision] operation of [Manage Case Assignment Microservice],
    Then a positive response is received,
    And the response has all the details as expected,
    And a wait time of [5] seconds [to allow for Logstash to re-index the case],
    And a call [to verify that Richard and anyone else in Richard's organisation has had their access removed] will get the expected response as in [F_209_get_case_users_richard_not_assigned],
    And a call [to verify that the supplementary counter has been adjusted to reflect the reduction in the number of users who have access to the case in Richard's organisation] will get the expected response as in [F_209_Search_Case_ES_0_users],
    And the response [has all fields in the ChangeOrganisationRequest nullified],

      @S-209.6
  Scenario: AC8: Must return error if the case record contains more then one OrganisationPolicy with the ChangeOrganisationRequest.CaseRole
    Given an appropriate test context as detailed in the test data source,
    And a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
    And a user [Dil - with a solicitor role for the same jurisdiction within a different organisation from Richard],
    And a successful call [by Richard to create a case - C1 - on behalf of Mario as the applicant which is auto-assigned to Richard's organisation] as in [F-209_NoC_Case_Creation_By_Richard_With_Assigned_Org_Policies],
    And a successful call [to remove CREATOR assigned CaseRole] as in [F-209_Delete_Creator_Role],
    And a successful call [to assign users to the case] as in [F-209_assign_access_to_case],
    When a request is prepared with appropriate values,
    And the request [intends to apply a NoCDecision to a case],
    And the request [contains more then OrganisationPolicy with the ChangeOrganisationRequest.CaseRole in the case record],
    And it is submitted to call the [ApplyNoCDecision] operation of [Manage Case Assignment Microservice],
    Then a positive response is received,
    And the response has all the details as expected,
    And a wait time of [5] seconds [to allow for Logstash to re-index the case],
    And a call [to verify that Richard And anyone else in Richard's organisation have not had their access removed] will get the expected response as in [F_209_get_case_users_richard_assigned],
    And a call [to verify that the supplementary counter remains unchanged for Richards organisation] will get the expected response as in [F_209_Search_Case_ES_1_users],
    And the response [has an error message stating there is more that one Org Policy with the same CaseRole],


      @S-209.7
  Scenario: AC9: Must return error if the case record contains no OrganisationPolicy with the ChangeOrganisationRequest.CaseRole
    Given an appropriate test context as detailed in the test data source,
    And a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
    And a user [Dil - with a solicitor role for the same jurisdiction within a different organisation from Richard],
    And a successful call [by Richard to create a case - C1 - on behalf of Mario as the applicant which is auto-assigned to Richard's organisation] as in [F-209_NoC_Case_Creation_By_Richard_With_Assigned_Org_Policies],
    And a successful call [to remove CREATOR assigned CaseRole] as in [F-209_Delete_Creator_Role],
    And a successful call [to assign users to the case] as in [F-209_assign_access_to_case],
    When a request is prepared with appropriate values,
    And the request [intends to apply a NoCDecision to a case],
    And the request [contains no OrganisationPolicy with the ChangeOrganisationRequest.CaseRole in the case record],
    And it is submitted to call the [ApplyNoCDecision] operation of [Manage Case Assignment Microservice],
    Then a positive response is received,
    And the response has all the details as expected,
    And a wait time of [5] seconds [to allow for Logstash to re-index the case],
    And a call [to verify that Richard And anyone else in Richard's organisation have not had their access removed] will get the expected response as in [F_209_get_case_users_richard_assigned],
    And a call [to verify that the supplementary counter remains unchanged for Richards organisation] will get the expected response as in [F_209_Search_Case_ES_1_users],
    And the response [has an error message stating in no Org Policy with the requested NoC CaseRole],

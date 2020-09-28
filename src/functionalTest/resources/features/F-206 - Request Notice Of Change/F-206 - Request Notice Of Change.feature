#=========================================
@F-206
Feature: F-206: Request Notice Of Change
#=========================================

  Background:
    Given an appropriate test context as detailed in the test data source

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-1
  @S-206.1
  Scenario: (Happy Path) : Solicitor successfully Requests NoC to Replace a litigant's current representation - No auto-approval applies

    Given an appropriate test context as detailed in the test data source,
      And a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Dil - with a solicitor role for the same jurisdiction within a different organisation from Richard],
      And a successful call [by Richard to create a case - C1 - on behalf of Mario as the applicant which is auto-assigned to Richard's organisation] as in [Prerequisite Case Creation Call for Case Assignment],

    When a request is prepared with appropriate values,
      And the request [is made by Dil to place an NOC Request in order to become Mario's representative on C1],
      And the request [contains all correct answers in the correct format - e.g. a Date of Birth provided in Date format],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

    Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify that the case is not listed in Dil's organisation] will get the expected response as in [XXX],
      And a call [to verify that Dil has not been granted any case roles for the Org.] will get the expected response as in [AAA],
      And another call [to verify that Richard now observes a pending NOC request on the case] will get the expected response as in [YYY].

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-2
  @S-206.2
  Scenario: (Happy Path) : Solicitor successfully Requests NoC for a non-represented litigant - No auto-approval applies

    Given an appropriate test context as detailed in the test data source,
      And a user [Mario - to initiate a case on his own behalf via a citizen facing application]
      And a user [Dil - with a solicitor role for the same jurisdiction as that of a case initiated by Mario],
      And a successful call [by Mario to create a case - C1 - via a citizen facing application] as in [Prerequisite Case Creation Call for Case Assignment],

    When a request is prepared with appropriate values,
      And the request [is made by Dil to place an NOC Request in order to become Mario's representative on C1],
      And the request [contains all correct answers in the correct format - e.g. a Date of Birth provided in Date format],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

    Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify that the case is not listed in Dil's organisation] will get the expected response as in [XXX],
      And a call [to verify that Dil has not been granted any case roles for the Org.] will get the expected response as in [AAA],
      And another call [to verify that Mario now observes a pending NOC request on the case] will get the expected response as in [YYY],
      And another call [to verify that a caseworker in C1's jurisdiction now observes a pending NOC request on the case] will get the expected response as in [YYY].

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-3
  @S-206.3
  Scenario: (Happy Path) : Case Access Administrator successfully Requests NoC to Replace a litigant's current representation - No auto-approval applies

    Given an appropriate test context as detailed in the test data source,
      And a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Dil - with a pui-caa role for the same jurisdiction within a different organisation from Richard],
      And a successful call [by Richard to create a case - C1 - on behalf of Mario as the applicant which is auto-assigned to Richard's organisation] as in [Prerequisite Case Creation Call for Case Assignment],

    When a request is prepared with appropriate values,
      And the request [is made by Dil to place an NOC Request in order to become Mario's representative on C1],
      And the request [contains all correct answers in the correct format - e.g. a Date of Birth provided in Date format],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

    Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify that the case is not listed in Dil's organisation] will get the expected response as in [XXX],
      And a call [to verify that Dil has not been granted any case roles for the Org.] will get the expected response as in [AAA],
      And another call [to verify that Richard now observes a pending NOC request on the case] will get the expected response as in [YYY].

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-4
  @S-206.4
  Scenario: (Happy Path) : Case Access Administrator successfully Requests NoC for a non-represented litigant - No auto-approval applies

    Given an appropriate test context as detailed in the test data source,
      And a user [Mario - to initiate a case on his own behalf via a citizen facing application]
      And a user [Dil - with a pui-caa role for the same jurisdiction as that of a case initiated by Mario],
      And a successful call [by Mario to create a case - C1 - via a citizen facing application] as in [Prerequisite Case Creation Call for Case Assignment],

    When a request is prepared with appropriate values,
      And the request [is made by Dil to place an NOC Request in order to become Mario's representative on C1],
      And the request [contains all correct answers in the correct format - e.g. a Date of Birth provided in Date format],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

    Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify that the case is not listed in Dil's organisation] will get the expected response as in [XXX],
      And a call [to verify that Dil has not been granted any case roles for the Org.] will get the expected response as in [AAA],
      And another call [to verify that Mario now observes a pending NOC request on the case] will get the expected response as in [YYY],
      And another call [to verify that a caseworker in C1's jurisdiction now observes a pending NOC request on the case] will get the expected response as in [YYY].

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-5
  @S-206.5
  Scenario: (Happy Path) : Case Access Administrator (with a solicitor role for a different jurisdiction to that of the case) successfully Requests NoC to Replace a litigant's current representation - No auto-approval applies

    Given an appropriate test context as detailed in the test data source,
      And a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Dil - with a caseworker-pui role and a solicitor role for a jurisdiction that does not match Case C1 below],
      And a successful call [by Richard to create a case - C1 - on behalf of Mario as the applicant which is auto-assigned to Richard's organisation] as in [Prerequisite Case Creation Call for Case Assignment],

    When a request is prepared with appropriate values,
      And the request [is made by Dil to place an NOC Request in order to become Mario's representative on C1],
      And the request [contains all correct answers in the correct format - e.g. a Date of Birth provided in Date format],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

    Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify that the case is not listed in Dil's organisation] will get the expected response as in [XXX],
      And a call [to verify that Dil has not been granted any case roles for the Org.] will get the expected response as in [AAA],
      And another call [to verify that Richard now observes a pending NOC request on the case] will get the expected response as in [YYY].

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-6
  @S-206.6
  Scenario: (Happy Path) : Case Access Administrator (with a solicitor role for a different jurisdiction to that of the case)successfully Requests NoC for a non-represented litigant - No auto-approval applies

    Given an appropriate test context as detailed in the test data source,
      And a user [Mario - to initiate a case on his own behalf via a citizen facing application]
      And a user [Dil - with a caseworker-pui role and a solicitor role for a jurisdiction that does not match Case C1 below],
      And a successful call [by Mario to create a case - C1 - via a citizen facing application] as in [Prerequisite Case Creation Call for Case Assignment],

    When a request is prepared with appropriate values,
      And the request [is made by Dil to place an NOC Request in order to become Mario's representative on C1],
      And the request [contains all correct answers in the correct format - e.g. a Date of Birth provided in Date format],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

    Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify that the case is not listed in Dil's organisation] will get the expected response as in [XXX],
      And a call [to verify that Dil has not been granted any case roles for the Org.] will get the expected response as in [AAA],
      And another call [to verify that Mario now observes a pending NOC request on the case] will get the expected response as in [YYY],
      And another call [to verify that a caseworker in C1's jurisdiction now observes a pending NOC request on the case] will get the expected response as in [YYY].

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-7
  @S-206.7
  Scenario: (Happy Path) : Solicitor successfully Requests NoC to Replace a litigant's current representation - Auto-approval applies

    Given an appropriate test context as detailed in the test data source,
      And a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Matt - with a caseworker-pui role for a different organisation to that of Richard],
      And a user [Dil - with a solicitor role for the same jurisdiction within a different organisation from both Richard and Matt],
      And a successful call [by Richard to create a case - C1 which contains 3 Org Policies for 3 case roles: R1, R2 & R3 which is auto-assigned to Richard's organisation] as in [Prerequisite Case Creation Call for Case Assignment],
      And a successful call [by Matt to trigger a NoC Request which contains answers identifying case role R1],

    When a request is prepared with appropriate values,
      And the request [is made by Dil to place a NOC Request for C1],
      And the request [contains all correct answers in the correct format - e.g. a Date of Birth provided in Date format],
      And the request [contains answers identifying case role R2],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

    Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify that the case is listed in Dil's organisation] will get the expected response as in [XXX],
      And a call [to verify that Dil has been granted case roles R1 & R2] will get the expected response as in [AAA],
      And a call [to verify that Dil has not been granted case role R3] will get the expected response as in [BBB],
      And another call [to verify that Richard does not observe a pending NOC request on C1] will get the expected response as in [YYY].

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-8
  @S-206.8
  Scenario: (Happy Path) : Solicitor successfully Requests NoC for a non-represented litigant - Auto-approval applies

    Given an appropriate test context as detailed in the test data source,
      And a user [Mario - to initiate a case on his own behalf via a citizen facing application]
      And a user [Dil - with a solicitor role for the same jurisdiction as that of a case initiated by Mario],
      And a user [Matt - with a caseworker-pui role for a different organisation to that of Dil],
      And a successful call [by Mario to create a case - C1 which contains 3 Org Policies for 3 case roles: R1, R2 & R3  - via a citizen facing application] as in [Prerequisite Case Creation Call for Case Assignment],
      And a successful call [by Matt to trigger a NoC Request which contains answers identifying case role R1],

    When a request is prepared with appropriate values,
      And the request [is made by Dil to place a NOC Request for C1],
      And the request [contains all correct answers in the correct format - e.g. a Date of Birth provided in Date format],
      And the request [contains answers identifying case role R2],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

    Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify that the case is listed in Dil's organisation] will get the expected response as in [XXX],
      And a call [to verify that Dil has been granted case roles R1 & R2] will get the expected response as in [AAA],
      And a call [to verify that Dil has not been granted case role R3] will get the expected response as in [BBB],
      And another call [to verify that Mario does not observe a pending NOC request on the case] will get the expected response as in [YYY],
      And another call [to verify that a caseworker in C1's jurisdiction does not observe a pending NOC request on the case] will get the expected response as in [ZZZZ].

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-9
  @S-206.9
  Scenario: (Happy Path) : Case Access Administrator successfully Requests NoC to Replace a litigant's current representation - Auto-approval applies

    Given an appropriate test context as detailed in the test data source,
      And a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Matt - with a caseworker-pui role for a different organisation to that of Richard],
      And a user [Dil - with a caseworker-pui role within a different organisation from both Richard & Matt],
      And a successful call [by Richard to create a case - C1 which contains 3 Org Policies for 3 case roles: R1, R2 & R3 which is auto-assigned to Richard's organisation] as in [Prerequisite Case Creation Call for Case Assignment],
      And a successful call [by Matt to trigger a NoC Request which contains answers identifying case role R1],

    When a request is prepared with appropriate values,
      And the request [is made by Dil to place a NOC Request for C1],
      And the request [contains all correct answers in the correct format - e.g. a Date of Birth provided in Date format],
      And the request [contains answers identifying case role R2],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

    Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify that the case is listed in Dil's organisation] will get the expected response as in [XXX],
      And a call [to verify that Dil has not been granted case roles R1, R2 or R3] will get the expected response as in [AAA],
      And another call [to verify that Richard does not observe a pending NOC request on C1] will get the expected response as in [YYY].

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-10
  @S-206.10
  Scenario: (Happy Path) : Case Access Administrator successfully Requests NoC for a non-represented litigant - Auto-approval applies

    Given an appropriate test context as detailed in the test data source,
      And a user [Mario - to initiate a case on his own behalf via a citizen facing application]
      And a user [Dil - with a caseworker-pui role],
      And a user [Matt - with a caseworker-pui role for a different organisation to that of Dil],
      And a successful call [by Mario to create a case - C1 which contains 3 Org Policies for 3 case roles: R1, R2 & R3  - via a citizen facing application] as in [Prerequisite Case Creation Call for Case Assignment],
      And a successful call [by Matt to trigger a NoC Request which contains answers identifying case role R1],

    When a request is prepared with appropriate values,
      And the request [is made by Dil to place a NOC Request for C1],
      And the request [contains all correct answers in the correct format - e.g. a Date of Birth provided in Date format],
      And the request [contains answers identifying case role R2],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

    Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify that the case is listed in Dil's organisation] will get the expected response as in [XXX],
      And a call [to verify that Dil has not been granted case roles R1, R2 or R3] will get the expected response as in [AAA],
      And another call [to verify that Mario does not observe a pending NOC request on the case] will get the expected response as in [YYY],
      And another call [to verify that a caseworker in C1's jurisdiction does not observe a pending NOC request on the case] will get the expected response as in [ZZZZ].

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-11
  @S-206.11
  Scenario: (Happy Path) : Case Access Administrator (with a solicitor role for a different jurisdiction to that of the case) successfully Requests NoC to Replace a litigant's current representation - Auto-approval applies

    Given an appropriate test context as detailed in the test data source,
      And a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Matt - with a caseworker-pui role for a different organisation to that of Richard],
      And a user [Dil - with a caseworker-pui role and a solicitor role for a jurisdiction that does not match Case C1 below],
      And a successful call [by Richard to create a case - C1 which contains 3 Org Policies for 3 case roles: R1, R2 & R3 which is auto-assigned to Richard's organisation] as in [Prerequisite Case Creation Call for Case Assignment],
      And a successful call [by Matt to trigger a NoC Request which contains answers identifying case role R1],

    When a request is prepared with appropriate values,
      And the request [is made by Dil to place a NOC Request for C1],
      And the request [contains all correct answers in the correct format - e.g. a Date of Birth provided in Date format],
      And the request [contains answers identifying case role R2],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

    Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify that the case is listed in Dil's organisation] will get the expected response as in [XXX],
      And a call [to verify that Dil has not been granted case roles R1, R2 or R3] will get the expected response as in [AAA],
      And another call [to verify that Richard does not observe a pending NOC request on C1] will get the expected response as in [YYY].

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-12
  @S-206.12
  Scenario: (Happy Path) : Case Access Administrator (with a solicitor role for a different jurisdiction to that of the case) successfully Requests NoC for a non-represented litigant - Auto-approval applies

    Given an appropriate test context as detailed in the test data source,
      And a user [Mario - to initiate a case on his own behalf via a citizen facing application],
      And a user [Dil - with a caseworker-pui role and a solicitor role for a jurisdiction that does not match Case C1 below],
      And a user [Matt - with a caseworker-pui role for a different organisation to that of Dil],
      And a successful call [by Mario to create a case - C1 which contains 3 Org Policies for 3 case roles: R1, R2 & R3  - via a citizen facing application] as in [Prerequisite Case Creation Call for Case Assignment],
      And a successful call [by Matt to trigger a NoC Request which contains answers identifying case role R1],

    When a request is prepared with appropriate values,
      And the request [is made by Dil to place a NOC Request for C1],
      And the request [contains all correct answers in the correct format - e.g. a Date of Birth provided in Date format],
      And the request [contains answers identifying case role R2],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

    Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify that the case is listed in Dil's organisation] will get the expected response as in [XXX],
      And a call [to verify that Dil has not been granted case roles R1, R2 or R3] will get the expected response as in [AAA],
      And another call [to verify that Mario does not observe a pending NOC request on the case] will get the expected response as in [YYY],
      And another call [to verify that a caseworker in C1's jurisdiction does not observe a pending NOC request on the case] will get the expected response as in [ZZZZ].

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-13
  @S-206.13
  Scenario:  (Happy Path) : Case Access Administrator (with a solicitor role for the same jurisdiction to that of the case) successfully Requests NoC to Replace a litigant's current representation - Auto-approval applies

    Given an appropriate test context as detailed in the test data source,
      And a user [Richard - with the ability to create a case for a particular jurisdiction within an organisation],
      And a user [Matt - with a caseworker-pui role for a different organisation to that of Richard],
      And a user [Dil - with a caseworker-pui role and a solicitor role for the same jurisdiction as Case C1 below],
      And a successful call [by Richard to create a case - C1 which contains 3 Org Policies for 3 case roles: R1, R2 & R3 which is auto-assigned to Richard's organisation] as in [Prerequisite Case Creation Call for Case Assignment],
      And a successful call [by Matt to trigger a NoC Request which contains answers identifying case role R1],

    When a request is prepared with appropriate values,
      And the request [is made by Dil to place a NOC Request for C1],
      And the request [contains all correct answers in the correct format - e.g. a Date of Birth provided in Date format],
      And the request [contains answers identifying case role R2],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

    Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify that the case is listed in Dil's organisation] will get the expected response as in [XXX],
      And a call [to verify that Dil has been granted case roles R1 & R2] will get the expected response as in [AAA],
      And a call [to verify that Dil has not been granted case role R3] will get the expected response as in [BBB],
      And another call [to verify that Richard does not observe a pending NOC request on C1] will get the expected response as in [YYY].

#-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
# ACA-68 / AC-14
  @S-206.14
  Scenario: (Happy Path) : Case Access Administrator (with a solicitor role for the same jurisdiction to that of the case) successfully Requests NoC for a non-represented litigant - Auto-approval applies

    Given an appropriate test context as detailed in the test data source,
      And a user [Mario - to initiate a case on his own behalf via a citizen facing application]
      And a user [Dil - with a caseworker-pui role and a solicitor role for the same jurisdiction as Case C1 below],
      And a user [Matt - with a caseworker-pui role for a different organisation to that of Dil],
      And a successful call [by Mario to create a case - C1 which contains 3 Org Policies for 3 case roles: R1, R2 & R3  - via a citizen facing application] as in [Prerequisite Case Creation Call for Case Assignment],
      And a successful call [by Matt to trigger a NoC Request which contains answers identifying case role R1],

    When a request is prepared with appropriate values,
      And the request [is made by Dil to place a NOC Request for C1],
      And the request [contains all correct answers in the correct format - e.g. a Date of Birth provided in Date format],
      And the request [contains answers identifying case role R2],
      And it is submitted to call the [Request NoC] operation of [Manage Case Assignment Microservice],

    Then a positive response is received,
      And the response has all the details as expected,
      And a call [to verify that the case is listed in Dil's organisation] will get the expected response as in [XXX],
      And a call [to verify that Dil has been granted case roles R1 & R2] will get the expected response as in [AAA],
      And a call [to verify that Dil has not been granted case role R3] will get the expected response as in [BBB],
      And another call [to verify that Mario does not observe a pending NOC request on the case] will get the expected response as in [YYY],
      And another call [to verify that a caseworker in C1's jurisdiction does not observe a pending NOC request on the case] will get the expected response as in [ZZZZ].

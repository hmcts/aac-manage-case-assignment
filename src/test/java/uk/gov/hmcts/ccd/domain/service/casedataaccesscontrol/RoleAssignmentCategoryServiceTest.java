package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.RoleCategory;
import uk.gov.hmcts.reform.managecase.service.CaseAssignmentService;
import uk.gov.hmcts.reform.managecase.service.casedataaccesscontrol.RoleAssignmentCategoryService;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.RoleCategory.CITIZEN;
import static uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.RoleCategory.JUDICIAL;
import static uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.RoleCategory.LEGAL_OPERATIONS;
import static uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.RoleCategory.PROFESSIONAL;

@DisplayName("RoleAssignmentCategoryService")
@ExtendWith(MockitoExtension.class)
class RoleAssignmentCategoryServiceTest {

    private static final String USER_ID = "12345";

    @Mock
    private CaseAssignmentService caseAssignmentService;

    @InjectMocks
    private RoleAssignmentCategoryService roleAssignmentCategoryService;

    @Nested
    @DisplayName("getRoleCategory()")
    class GetRoleCategory {

        @Test
        void shouldGetRoleCategoryForSolicitorUser() {

            given(caseAssignmentService.getAssigneeRoles(USER_ID))
                .willReturn(asList("caseworker", "caseworker-autotest1-solicitor"));

            RoleCategory roleCategory = roleAssignmentCategoryService.getRoleCategory(USER_ID);

            assertThat(roleCategory, is(PROFESSIONAL));
        }

        @Test
        void shouldGetRoleCategoryForLocalAuthorityUser() {

            given(caseAssignmentService.getAssigneeRoles(USER_ID))
                .willReturn(asList("caseworker", "caseworker-autotest1-localAuthority"));

            RoleCategory roleCategory = roleAssignmentCategoryService.getRoleCategory(USER_ID);

            assertThat(roleCategory, is(PROFESSIONAL));
        }

        @Test
        void shouldGetRoleCategoryForCitizenUser() {

            given(caseAssignmentService.getAssigneeRoles(USER_ID))
                .willReturn(singletonList("citizen"));

            RoleCategory roleCategory = roleAssignmentCategoryService.getRoleCategory(USER_ID);

            assertThat(roleCategory, is(CITIZEN));
        }

        @Test
        void shouldGetRoleCategoryForLetterHolderUser() {

            given(caseAssignmentService.getAssigneeRoles(USER_ID))
                .willReturn(singletonList("letter-holder"));

            RoleCategory roleCategory = roleAssignmentCategoryService.getRoleCategory(USER_ID);

            assertThat(roleCategory, is(CITIZEN));
        }

        @Test
        void shouldGetRoleCategoryForPanelMemberUser() {

            given(caseAssignmentService.getAssigneeRoles(USER_ID))
                .willReturn(singletonList("judge1-panelmember"));

            RoleCategory roleCategory = roleAssignmentCategoryService.getRoleCategory(USER_ID);

            assertThat(roleCategory, is(JUDICIAL));
        }

        @Test
        void shouldGetRoleCategoryForLegalOperationsUser() {

            given(caseAssignmentService.getAssigneeRoles(USER_ID))
                .willReturn(singletonList("caseworker"));

            RoleCategory roleCategory = roleAssignmentCategoryService.getRoleCategory(USER_ID);

            assertThat(roleCategory, is(LEGAL_OPERATIONS));
        }

    }

}

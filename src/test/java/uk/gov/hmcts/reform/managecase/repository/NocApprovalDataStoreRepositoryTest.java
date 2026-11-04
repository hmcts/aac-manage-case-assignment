package uk.gov.hmcts.reform.managecase.repository;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.managecase.client.datastore.DataStoreApiClient;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;
import uk.gov.hmcts.reform.managecase.util.JacksonUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NocApprovalDataStoreRepositoryTest {

    @Test
    void shouldUseNoCApproverToken() {
        SecurityUtils securityUtils = mock(SecurityUtils.class);
        when(securityUtils.getNocApproverSystemUserAccessToken()).thenReturn("noc-token");
        TestRepository repository = new TestRepository(mock(DataStoreApiClient.class),
                                                        mock(JacksonUtils.class), securityUtils);

        assertThat(repository.userAuthToken()).isEqualTo("noc-token");
    }

    private static class TestRepository extends NocApprovalDataStoreRepository {
        TestRepository(DataStoreApiClient dataStoreApi, JacksonUtils jacksonUtils, SecurityUtils securityUtils) {
            super(dataStoreApi, jacksonUtils, securityUtils);
        }

        String userAuthToken() {
            return getUserAuthToken();
        }
    }
}

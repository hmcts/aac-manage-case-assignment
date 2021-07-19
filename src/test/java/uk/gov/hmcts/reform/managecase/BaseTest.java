package uk.gov.hmcts.reform.managecase;

import org.junit.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.managecase.service.ras.RoleAssignmentServiceHelper;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;
import uk.gov.hmcts.reform.managecase.service.common.UIDService;

import javax.inject.Inject;
import java.io.IOException;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:test.properties")
public abstract class BaseTest {

    @Inject
    protected UIDService uidService;

    @Inject
    protected RoleAssignmentServiceHelper roleAssignmentServiceHelper;

    @Inject
    protected SecurityUtils securityUtils;

    @Mock
    protected Authentication authentication;
    @Mock
    protected SecurityContext securityContext;

    protected static final String REFERENCE = "1504259907353529";

    @Before
    @BeforeEach
    public void initMock() throws IOException {
        MockitoAnnotations.initMocks(this);
        ReflectionTestUtils.setField(roleAssignmentServiceHelper, "securityUtils", securityUtils);
        setupUIDService();


        SecurityContextHolder.setContext(securityContext);
        SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
    }

    private void setupUIDService() {
        reset(uidService);
        when(uidService.generateUID()).thenReturn(REFERENCE);
        when(uidService.validateUID(anyString())).thenCallRealMethod();
        when(uidService.checkSum(anyString())).thenCallRealMethod();
        when(uidService.checkSum(anyString(), anyBoolean())).thenCallRealMethod();
    }
}


package uk.gov.hmcts.reform.managecase.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.managecase.TestIdamConfiguration;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignmentRequest;
import uk.gov.hmcts.reform.managecase.config.MapperConfig;
import uk.gov.hmcts.reform.managecase.config.SecurityConfiguration;
import uk.gov.hmcts.reform.managecase.domain.CaseAssignment;
import uk.gov.hmcts.reform.managecase.security.JwtGrantedAuthoritiesConverter;
import uk.gov.hmcts.reform.managecase.service.CaseAssignmentService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CaseAssignmentController.class,
    includeFilters = @ComponentScan.Filter(type = ASSIGNABLE_TYPE, classes = MapperConfig.class),
    excludeFilters = @ComponentScan.Filter(type = ASSIGNABLE_TYPE, classes =
        { SecurityConfiguration.class, JwtGrantedAuthoritiesConverter.class }))
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration(TestIdamConfiguration.class)
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.JUnitTestsShouldIncludeAssert"})
public class CaseAssignmentControllerTest {

    private static final String ASSIGNEE_ID = "0a5874a4-3f38-4bbd-ba4c";
    private static final String CASE_TYPE_ID = "TEST_CASE_TYPE";
    private static final String CASE_ID = "12345678";
    public static final String CASE_ASSIGNMENTS = "/case-assignments";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CaseAssignmentService service;

    @Autowired
    private ObjectMapper objectMapper;

    private CaseAssignmentRequest request;

    @BeforeEach
    void setUp() {
        request = new CaseAssignmentRequest(CASE_TYPE_ID, CASE_ID, ASSIGNEE_ID);
    }

    @DisplayName("should assign case successfully for a valid request")
    @Test
    void shouldAssignCaseAccess() throws Exception {
        given(service.assignCaseAccess(any(CaseAssignment.class))).willReturn("Assigned-Role");

        this.mockMvc.perform(put(CASE_ASSIGNMENTS)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(content().json("{\"status_message\":\"Assigned-Role\"}"));
    }

    @DisplayName("should delegate to service domain for a valid request")
    @Test
    void shouldDelegateToServiceDomain() throws Exception {
        this.mockMvc.perform(put(CASE_ASSIGNMENTS)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());

        ArgumentCaptor<CaseAssignment> captor = ArgumentCaptor.forClass(CaseAssignment.class);
        verify(service).assignCaseAccess(captor.capture());
        assertThat(captor.getValue().getCaseId()).isEqualTo(CASE_ID);
        assertThat(captor.getValue().getAssigneeId()).isEqualTo(ASSIGNEE_ID);
    }

    @DisplayName("should fail with 400 bad request when case type id is null")
    @Test
    void shouldFailWithBadRequestWhenCaseTypeIdIsNull() throws Exception {
        request = new CaseAssignmentRequest(null,CASE_ID, ASSIGNEE_ID);

        this.mockMvc.perform(put(CASE_ASSIGNMENTS)
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors", hasSize(1)))
            .andExpect(jsonPath("$.errors", hasItem("Case type ID can not be empty")));
    }

    @DisplayName("should fail with 400 bad request when case id is null")
    @Test
    void shouldFailWithBadRequestWhenCaseIdIsNull() throws Exception {
        request = new CaseAssignmentRequest(CASE_TYPE_ID,null, ASSIGNEE_ID);

        this.mockMvc.perform(put(CASE_ASSIGNMENTS)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors", hasSize(1)))
            .andExpect(jsonPath("$.errors", hasItem("Case ID can not be empty")));
    }

    @DisplayName("should fail with 400 bad request when assignee id is empty")
    @Test
    void shouldFailWithBadRequestWhenAssigneeIdIsNull() throws Exception {
        request = new CaseAssignmentRequest(CASE_TYPE_ID, CASE_ID, "");

        this.mockMvc.perform(put(CASE_ASSIGNMENTS)
             .contentType(MediaType.APPLICATION_JSON)
             .content(objectMapper.writeValueAsString(request)))
             .andExpect(status().isBadRequest())
             .andExpect(jsonPath("$.errors", hasSize(1)))
             .andExpect(jsonPath("$.errors", hasItem("IDAM Assignee ID can not be empty")));
    }
}

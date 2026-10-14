package uk.gov.hmcts.reform.managecase.api.controller.provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import uk.gov.hmcts.reform.managecase.config.MapperConfig;
import uk.gov.hmcts.reform.managecase.repository.DataStoreRepository;
import uk.gov.hmcts.reform.managecase.repository.IdamRepository;
import uk.gov.hmcts.reform.managecase.repository.PrdRepository;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;
import uk.gov.hmcts.reform.managecase.util.JacksonUtils;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = {ContractConfig.class, MapperConfig.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "CCD_DATA_STORE_API_BASE_URL=http://localhost:${wiremock.server.port}")
@AutoConfigureWireMock(port = 0)
class CaseAssignmentProviderRegressionTest {

    @LocalServerPort
    private int port;
    @Autowired
    private PrdRepository prdRepository;
    @Autowired
    private DataStoreRepository dataStoreRepository;
    @Autowired
    private IdamRepository idamRepository;
    @Autowired
    private JacksonUtils jacksonUtils;
    @Autowired
    private SecurityUtils securityUtils;

    private CaseAssignmentProviderTests states;

    @BeforeEach
    void setUp() {
        states = new CaseAssignmentProviderTests();
        states.prdRepository = prdRepository;
        states.dataStoreRepository = dataStoreRepository;
        states.idamRepository = idamRepository;
        states.jacksonUtils = jacksonUtils;
        states.securityUtils = securityUtils;
    }

    @Test
    void returnsAssignmentsForNumericCaseIds() throws Exception {
        states.toGetListOfCases();
        ResponseEntity<String> response = send("/case-assignments?case_ids=1588234985453946", null);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("1588234985453946", "[CR1]", "shared_with");
        assertThat(response.getBody()).doesNotContain("case_title");
    }

    @Test
    void rejectsBracketedCaseIdsInOldConsumerContract() throws Exception {
        assertThatThrownBy(() -> send("/case-assignments?case_ids=%5B123456789%5D", null))
            .isInstanceOf(HttpClientErrorException.BadRequest.class);
    }

    @Test
    void forwardsCaseTypeSearchThroughGateway() throws Exception {
        states.toHandleCaaCaseTypes();
        ResponseEntity<String> response = send("/ccd/searchCases?ctid=MoneyClaimCase,DIVORCE", "{}");
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("case 1", "case_2", "case_types_results");
        verify(postRequestedFor(urlPathEqualTo("/searchCases"))
            .withQueryParam("ctid", equalTo("MoneyClaimCase,DIVORCE"))
            .withHeader("Authorization", equalTo("TestBearToken"))
            .withHeader("ServiceAuthorization", equalTo("systemServiceAuthToken")));
    }

    @Test
    void returnsConnectionCloseForAssignmentRequest() throws Exception {
        states.toAssignUserToCase();
        ResponseEntity<String> response = send("/case-assignments", """
            {
              "case_id": "1583841721773828",
              "assignee_id": "0a5874a4-3f38-4bbd-ba4c",
              "case_type_id": "PROBATE"
            }
            """);
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getHeaders().getFirst("Connection")).isEqualTo("close");
    }

    private ResponseEntity<String> send(String path, String body) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Connection", "close");
        headers.set("ServiceAuthorization", "serviceAuthToken");
        RestTemplate client = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
        return client.exchange(java.net.URI.create("http://localhost:" + port + path),
            body == null ? HttpMethod.GET : HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
    }
}

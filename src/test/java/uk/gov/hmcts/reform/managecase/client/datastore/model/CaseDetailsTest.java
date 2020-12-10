package uk.gov.hmcts.reform.managecase.client.datastore.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.config.JacksonObjectMapperConfig;
import uk.gov.hmcts.reform.managecase.domain.ChangeOrganisationRequest;
import uk.gov.hmcts.reform.managecase.domain.DynamicList;
import uk.gov.hmcts.reform.managecase.domain.DynamicListElement;
import uk.gov.hmcts.reform.managecase.domain.Organisation;
import uk.gov.hmcts.reform.managecase.util.JacksonUtils;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.managecase.domain.ApprovalStatus.APPROVED;

@SuppressWarnings({"PMD.UseConcurrentHashMap", "PMD.AvoidDuplicateLiterals"})
public class CaseDetailsTest {

    private final ObjectMapper objectMapper = new JacksonObjectMapperConfig().defaultObjectMapper();

    @Test
    @DisplayName("Find ChangeOrganisationRequest Json nodes from data")
    void shouldFindChangeOrganisationRequestNodes() throws JsonProcessingException {
        // ARRANGE
        LocalDateTime now = LocalDateTime.of(2020, Month.MAY, 16, 11, 48, 32);

        Organisation organisationToAdd = Organisation.builder().organisationID("id1").build();
        Organisation organisationToRemove = Organisation.builder().organisationID("id2").build();

        DynamicListElement dynamicListElement = DynamicListElement.builder().code("code").label("label").build();
        DynamicList dynamicList =
            DynamicList.builder().value(dynamicListElement).listItems(List.of(dynamicListElement)).build();
        ChangeOrganisationRequest cor = ChangeOrganisationRequest.builder()
            .caseRoleId(dynamicList)
            .requestTimestamp(now)
            .organisationToAdd(organisationToAdd)
            .organisationToRemove(organisationToRemove)
            .approvalStatus(APPROVED.name())
            .build();

        final String expectedChangeOrganisationRequestJson =
            "{"
            + "\"OrganisationToAdd\":{"
            +   "\"OrganisationID\":\"" + organisationToAdd.getOrganisationID() + "\""
            + "},"
            + "\"OrganisationToRemove\":{"
            +   "\"OrganisationID\":\"" + organisationToRemove.getOrganisationID() + "\""
            + "},"
            + "\"CaseRoleId\":" + objectMapper.writeValueAsString(dynamicList) + ","
            + "\"RequestTimestamp\":\"" + now + "\","
            + "\"ApprovalStatus\":\"APPROVED\""
            + "}";


        Map<String, JsonNode> data = new HashMap<>();

        data.put(
            "CouldBeAnyChangeOrganisationRequestField",
            new JacksonUtils(objectMapper).convertValue(cor, JsonNode.class)
        );

        CaseDetails caseDetails = CaseDetails.builder().data(data).build();

        // ASSERT
        assertThat(expectedChangeOrganisationRequestJson)
            .isEqualTo(caseDetails.findChangeOrganisationRequestNode().get().toString());
    }

    @Test
    @DisplayName("Returns Optional when parsing produces no results")
    void shouldReturnEmptyOptionalWhenParsingProducesNoResults() {
        // ARRANGE
        Map<String, JsonNode> data = new HashMap<>();

        CaseDetails caseResource = CaseDetails.builder().data(data).build();

        // ASSERT
        assertThat(caseResource.findChangeOrganisationRequestNode().isPresent()).isFalse();
    }
}

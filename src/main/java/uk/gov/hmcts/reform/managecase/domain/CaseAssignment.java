package uk.gov.hmcts.reform.managecase.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
public class CaseAssignment {

    private String caseId;
    private String assigneeId;
}

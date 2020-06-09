package uk.gov.hmcts.reform.managecase.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class OrganisationPolicy {

    private Organisation organisation;
    private String orgPolicyReference;
    private String orgPolicyCaseAssignedRole;

}

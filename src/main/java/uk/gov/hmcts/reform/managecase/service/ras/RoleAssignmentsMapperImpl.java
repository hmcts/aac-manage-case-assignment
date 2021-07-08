package uk.gov.hmcts.reform.managecase.service.ras;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignment;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignment.RoleAssignmentBuilder;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentAttributes.RoleAssignmentAttributesBuilder;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentAttributes;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentAttributesResource;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentResource;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentResponse;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignments;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignments.RoleAssignmentsBuilder;

import java.util.ArrayList;
import java.util.List;

@Component
public class RoleAssignmentsMapperImpl implements RoleAssignmentsMapper {

    @Override
    public RoleAssignments toRoleAssignments(RoleAssignmentResponse roleAssignmentResponse) {
        if (roleAssignmentResponse == null) {
            return null;
        }

        RoleAssignmentsBuilder roleAssignments = RoleAssignments.builder();

        roleAssignments.roleAssignments(roleAssignmentResourceListToRoleAssignmentList(
            roleAssignmentResponse.getRoleAssignments()));

        return roleAssignments.build();
    }

    protected List<RoleAssignment> roleAssignmentResourceListToRoleAssignmentList(List<RoleAssignmentResource> list) {
        if (list == null) {
            return null;
        }

        List<RoleAssignment> list1 = new ArrayList<>(list.size());
        for (RoleAssignmentResource roleAssignmentResource : list) {
            list1.add(roleAssignmentResourceToRoleAssignment(roleAssignmentResource));
        }

        return list1;
    }

    protected RoleAssignment roleAssignmentResourceToRoleAssignment(RoleAssignmentResource roleAssignmentResource) {
        if (roleAssignmentResource == null) {
            return null;
        }

        RoleAssignmentBuilder roleAssignment = RoleAssignment.builder();

        roleAssignment.id(roleAssignmentResource.getId());
        roleAssignment.actorIdType(roleAssignmentResource.getActorIdType());
        roleAssignment.actorId(roleAssignmentResource.getActorId());
        roleAssignment.roleType(roleAssignmentResource.getRoleType());
        roleAssignment.roleName(roleAssignmentResource.getRoleName());
        roleAssignment.classification(roleAssignmentResource.getClassification());
        roleAssignment.grantType(roleAssignmentResource.getGrantType());
        roleAssignment.roleCategory(roleAssignmentResource.getRoleCategory());
        roleAssignment.readOnly(roleAssignmentResource.getReadOnly());
        roleAssignment.beginTime(roleAssignmentResource.getBeginTime());
        roleAssignment.endTime(roleAssignmentResource.getEndTime());
        roleAssignment.created(roleAssignmentResource.getCreated());
        List<String> list = roleAssignmentResource.getAuthorisations();
        if (list != null) {
            roleAssignment.authorisations(new ArrayList<String>(list));
        }
        roleAssignment.attributes(roleAssignmentAttributesResourceToRoleAssignmentAttributes(
            roleAssignmentResource.getAttributes()));

        return roleAssignment.build();
    }

    protected RoleAssignmentAttributes roleAssignmentAttributesResourceToRoleAssignmentAttributes(
        RoleAssignmentAttributesResource roleAssignmentAttributesResource) {
        if (roleAssignmentAttributesResource == null) {
            return null;
        }

        RoleAssignmentAttributesBuilder roleAssignmentAttributes = RoleAssignmentAttributes.builder();

        roleAssignmentAttributes.jurisdiction(roleAssignmentAttributesResource.getJurisdiction());
        roleAssignmentAttributes.caseType(roleAssignmentAttributesResource.getCaseType());
        roleAssignmentAttributes.caseId(roleAssignmentAttributesResource.getCaseId());
        roleAssignmentAttributes.region(roleAssignmentAttributesResource.getRegion());
        roleAssignmentAttributes.location(roleAssignmentAttributesResource.getLocation());
        roleAssignmentAttributes.contractType(roleAssignmentAttributesResource.getContractType());

        return roleAssignmentAttributes.build();
    }
}

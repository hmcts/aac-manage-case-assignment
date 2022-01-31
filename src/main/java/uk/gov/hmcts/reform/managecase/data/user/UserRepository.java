package uk.gov.hmcts.reform.managecase.data.user;

import uk.gov.hmcts.reform.managecase.api.payload.IdamUser;

import java.util.List;
import java.util.Set;

public interface UserRepository {

    IdamUser getUser();

    String getUserId();

    Set<String> getUserRoles();

    boolean anyRoleEqualsAnyOf(List<String> userRoles);
}

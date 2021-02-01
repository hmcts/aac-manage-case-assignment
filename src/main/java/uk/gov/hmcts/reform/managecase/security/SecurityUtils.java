package uk.gov.hmcts.reform.managecase.security;

import com.auth0.jwt.JWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.managecase.repository.IdamRepository;

import javax.inject.Named;
import java.util.List;
import java.util.regex.Pattern;

@Named
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class SecurityUtils {

    private static final Pattern SOLICITOR_ROLE = Pattern.compile(".+-solicitor$", Pattern.CASE_INSENSITIVE);

    public static final String BEARER = "Bearer ";
    public static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";

    private final AuthTokenGenerator authTokenGenerator;
    private final IdamRepository idamRepository;

    @Autowired
    public SecurityUtils(final AuthTokenGenerator authTokenGenerator, IdamRepository idamRepository) {
        this.authTokenGenerator = authTokenGenerator;
        this.idamRepository = idamRepository;
    }

    public String getS2SToken() {
        return authTokenGenerator.generate();
    }

    public String getCaaSystemUserToken() {
        return idamRepository.getCaaSystemUserAccessToken();
    }

    public String getNocApproverSystemUserAccessToken() {
        return idamRepository.getNocApproverSystemUserAccessToken();
    }

    public String getUserToken() {
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return jwt.getTokenValue();
    }

    public String getUserBearerToken() {
        return BEARER + getUserToken();
    }

    public UserInfo getUserInfo() {
        return idamRepository.getUserInfo(getUserBearerToken());
    }

    public String getServiceNameFromS2SToken(String serviceAuthenticationToken) {
        // NB: this grabs the service name straight from the token under the assumption
        // that the S2S token has already been verified elsewhere
        return JWT.decode(removeBearerFromToken(serviceAuthenticationToken)).getSubject();
    }

    private String removeBearerFromToken(String token) {
        return token.startsWith(BEARER) ? token.substring(BEARER.length()) : token;
    }

    public boolean hasSolicitorRole(List<String> roles) {
        return roles.stream().anyMatch(role -> SOLICITOR_ROLE.matcher(role).matches());
    }

    public boolean hasJurisdictionRole(List<String> roles, String jurisdictionId) {
        String jurisdictionRole = "caseworker-" + jurisdictionId;
        return roles.stream().anyMatch(jurisdictionRole::equalsIgnoreCase);
    }

    public boolean hasSolicitorRoleForJurisdiction(List<String> roles, String jurisdictionId) {
        return hasSolicitorRole(roles) && hasJurisdictionRole(roles, jurisdictionId);
    }
}

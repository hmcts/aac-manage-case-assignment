package uk.gov.hmcts.reform.managecase.config;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.util.Date;

final class JwtTestTokens {

    static final String KEY_ID = "test-signing-key";
    private static final Instant ISSUED_AT = Instant.parse("2024-01-01T00:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2099-01-01T00:00:00Z");

    private JwtTestTokens() {
    }

    static String signedToken(String issuer, RSAPrivateKey privateKey) throws JOSEException {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .issuer(issuer)
            .subject("test-user")
            .issueTime(Date.from(ISSUED_AT))
            .expirationTime(Date.from(EXPIRES_AT))
            .build();

        SignedJWT signedJwt = new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type(JOSEObjectType.JWT)
                .keyID(KEY_ID)
                .build(),
            claims
        );
        signedJwt.sign(new RSASSASigner(privateKey));
        return signedJwt.serialize();
    }

    static KeyPair rsaKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.generateKeyPair();
    }
}

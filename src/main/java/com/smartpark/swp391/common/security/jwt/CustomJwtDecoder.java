package com.smartpark.swp391.common.security.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.PostConstruct;
import java.text.ParseException;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomJwtDecoder implements JwtDecoder {

  NimbusJwtDecoder nimbusJwtDecoder;

  @Value("${jwt.signer-key-base64}")
  String signerKeyBase64;

  @PostConstruct
  public void init() {
    byte[] signerKeyBytes = Base64.getDecoder().decode(signerKeyBase64);
    SecretKeySpec secretKeySpec = new SecretKeySpec(signerKeyBytes, "HmacSHA512");
    nimbusJwtDecoder =
        NimbusJwtDecoder.withSecretKey(secretKeySpec).macAlgorithm(MacAlgorithm.HS512).build();
  }

  @Override
  public Jwt decode(String token) {
    try {
      validateTokenDirectly(token);
    } catch (JwtException e) {
      throw e;
    } catch (Exception e) {
      throw new JwtException("Unauthenticated", e);
    }

    return nimbusJwtDecoder.decode(token);
  }

  private void validateTokenDirectly(String token) throws JOSEException, ParseException {
    byte[] signerKeyBytes = Base64.getDecoder().decode(signerKeyBase64);
    JWSVerifier verifier = new MACVerifier(signerKeyBytes);
    SignedJWT signedJWT = SignedJWT.parse(token);
    boolean signatureValid = signedJWT.verify(verifier);

    Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
    boolean notExpired = expirationTime != null && expirationTime.after(new Date());

    if (!signatureValid) {
      throw new JwtException("Invalid JWT signature");
    }
    if (!notExpired) {
      throw new JwtValidationException(
          "JWT expired", List.of(new OAuth2Error("invalid_token", "JWT expired", null)));
    }
  }
}

package com.smartpark.swp391.common.security.jwt;

import jakarta.annotation.PostConstruct;
import java.util.Base64;
import javax.crypto.spec.SecretKeySpec;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
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
      return nimbusJwtDecoder.decode(token);
    } catch (JwtException e) {
      throw e;
    } catch (Exception e) {
      throw new JwtException("Unauthenticated", e);
    }
  }
}

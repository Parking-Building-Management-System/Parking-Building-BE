package com.smartpark.swp391.modules.identity.service.token.impl;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.infrastructure.persistence.UuidV7;
import com.smartpark.swp391.modules.identity.dto.token.request.TokenRequest;
import com.smartpark.swp391.modules.identity.dto.token.response.TokenPair;
import com.smartpark.swp391.modules.identity.service.token.TokenService;
import java.text.ParseException;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TokenServiceImpl implements TokenService {

  @Value("${jwt.signer-key-base64}")
  private String signerKeyBase64;

  @Value("${jwt.valid-duration}")
  private long accessTokenTtlSeconds;

  @Value("${jwt.refreshable-duration}")
  private long refreshTokenTtlSeconds;

  @Override
  public TokenPair generateTokenPair(TokenRequest tokenRequest) {
    String access = generateAccessToken(tokenRequest);
    String refresh = generateRefreshToken(tokenRequest);

    return TokenPair.builder()
        .accessToken(access)
        .refreshToken(refresh)
        .accessTtl(accessTokenTtlSeconds)
        .refreshTtl(refreshTokenTtlSeconds)
        .build();
  }

  @Override
  public String generateAccessToken(TokenRequest tokenRequest) {
    JWTClaimsSet claims =
        buildClaims(
            tokenRequest.getUserId(),
            tokenRequest.getTenantId(),
            tokenRequest.getSessionId(),
            tokenRequest.getSubject(),
            accessTokenTtlSeconds,
            null,
            null);
    return sign(claims);
  }

  @Override
  public String generateRefreshToken(TokenRequest tokenRequest) {
    JWTClaimsSet claims =
        buildClaims(
            tokenRequest.getUserId(),
            tokenRequest.getTenantId(),
            tokenRequest.getSessionId(),
            tokenRequest.getSubject(),
            refreshTokenTtlSeconds,
            "REFRESH",
            tokenRequest.getRefreshJti());
    return sign(claims);
  }

  @Override
  public String generateRefreshToken(TokenRequest tokenRequest, Date absoluteExpiry) {
    JWTClaimsSet claims =
        buildClaims(
            tokenRequest.getUserId(),
            tokenRequest.getTenantId(),
            tokenRequest.getSessionId(),
            tokenRequest.getSubject(),
            absoluteExpiry,
            "REFRESH",
            tokenRequest.getRefreshJti());
    return sign(claims);
  }

  @Override
  public SignedJWT verifyAccessToken(String token) {
    return verify(token, false);
  }

  @Override
  public SignedJWT verifyRefreshToken(String token) {
    return verify(token, true);
  }

  @Override
  public TokenRequest extractToken(String refreshToken) {
    SignedJWT jwt = verifyRefreshToken(refreshToken);
    try {
      var claims = jwt.getJWTClaimsSet();

      UUID userId = UUID.fromString(claims.getStringClaim("user_id"));
      UUID tenantId = UUID.fromString(claims.getStringClaim("tenant_id"));
      UUID sessionId = UUID.fromString(claims.getStringClaim("session_id"));
      UUID jti = UUID.fromString(claims.getJWTID());
      String subject = claims.getSubject();

      if (subject == null || subject.isBlank()) {
        throw new ApiException(ErrorCode.UNAUTHENTICATED);
      }

      return TokenRequest.builder()
          .userId(userId)
          .tenantId(tenantId)
          .sessionId(sessionId)
          .subject(subject)
          .refreshJti(jti)
          .build();

    } catch (ParseException | IllegalArgumentException e) {
      throw new ApiException(ErrorCode.UNAUTHENTICATED);
    }
  }

  private JWTClaimsSet buildClaims(
      UUID userId,
      UUID tenantId,
      UUID sessionId,
      String subject,
      long ttlSeconds,
      String tokenType,
      UUID refreshJti) {

    Instant now = Instant.now();

    JWTClaimsSet.Builder builder =
        new JWTClaimsSet.Builder()
            .subject(subject)
            .issueTime(Date.from(now))
            .expirationTime(Date.from(now.plusSeconds(ttlSeconds)))
            .claim("user_id", userId.toString())
            .claim("tenant_id", tenantId.toString())
            .claim("session_id", sessionId.toString());

    if (tokenType != null) {
      builder.claim("typ", tokenType);
    }
    if ("REFRESH".equals(tokenType)) {
      if (refreshJti == null) {
        throw new ApiException(ErrorCode.UNEXPECTED_ERROR);
      }
      builder.jwtID(refreshJti.toString());
    } else {
      builder.jwtID(UuidV7.random().toString());
    }

    return builder.build();
  }

  private JWTClaimsSet buildClaims(
      UUID userId,
      UUID tenantId,
      UUID sessionId,
      String subject,
      Date expirationTime,
      String tokenType,
      UUID refreshJti) {

    Instant now = Instant.now();

    JWTClaimsSet.Builder builder =
        new JWTClaimsSet.Builder()
            .subject(subject)
            .issueTime(Date.from(now))
            .expirationTime(expirationTime)
            .claim("user_id", userId.toString())
            .claim("tenant_id", tenantId.toString())
            .claim("session_id", sessionId.toString());

    if (tokenType != null) {
      builder.claim("typ", tokenType);
    }

    if ("REFRESH".equals(tokenType)) {
      if (refreshJti == null) {
        throw new ApiException(ErrorCode.UNEXPECTED_ERROR);
      }
      builder.jwtID(refreshJti.toString());
    } else {
      builder.jwtID(UuidV7.random().toString());
    }

    return builder.build();
  }

  private String sign(JWTClaimsSet claims) {
    try {
      byte[] keyBytes = Base64.getDecoder().decode(signerKeyBase64);
      SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS512), claims);
      jwt.sign(new MACSigner(keyBytes));
      return jwt.serialize();
    } catch (Exception e) {
      throw new ApiException(ErrorCode.UNEXPECTED_ERROR);
    }
  }

  private SignedJWT verify(String token, boolean refresh) {
    try {
      byte[] keyBytes = Base64.getDecoder().decode(signerKeyBase64);
      SignedJWT signedJWT = SignedJWT.parse(token);

      boolean signatureValid = signedJWT.verify(new MACVerifier(keyBytes));
      if (!signatureValid) {
        throw new ApiException(ErrorCode.UNAUTHENTICATED);
      }

      Date exp = signedJWT.getJWTClaimsSet().getExpirationTime();
      if (exp == null || exp.before(new Date())) {
        throw new ApiException(ErrorCode.UNAUTHENTICATED);
      }

      if (refresh) {
        Object typ = signedJWT.getJWTClaimsSet().getClaim("typ");
        if (!"REFRESH".equals(typ)) {
          throw new ApiException(ErrorCode.UNAUTHENTICATED);
        }
        String jti = signedJWT.getJWTClaimsSet().getJWTID();
        if (jti == null || jti.isBlank()) {
          throw new ApiException(ErrorCode.UNAUTHENTICATED);
        }
      }

      return signedJWT;

    } catch (ApiException e) {
      throw e;
    } catch (Exception e) {
      throw new ApiException(ErrorCode.UNAUTHENTICATED);
    }
  }
}

package com.smartpark.swp391.common.security.config;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.infrastructure.cached.redis.model.SessionAuthzCache;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

// Converts a raw Spring Security Jwt object into a fully populated JwtAuthenticationToken
// Extracts session identifiers from the JWT payload and bridges to the business layer
@Component
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class JwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

  SessionAuthorityResolver sessionAuthorityResolver;

  @Override
  public AbstractAuthenticationToken convert(Jwt jwt) {

    String userIdRaw = jwt.getClaimAsString("user_id");
    String sessionIdRaw = jwt.getClaimAsString("session_id");
    Instant exp = jwt.getExpiresAt();

    if (userIdRaw == null || sessionIdRaw == null || exp == null) {
      throw new JwtException("Missing required JWT claims");
    }

    UUID userId = UUID.fromString(userIdRaw);
    UUID sessionId = UUID.fromString(sessionIdRaw);

    SessionAuthzCache authz;

    try {
      // Bridge to the core caching & validation logic
      authz = sessionAuthorityResolver.resolve(sessionId, userId, exp);
    } catch (ApiException e) {
      throw new InvalidBearerTokenException(e.getMessage(), e);
    }

    List<GrantedAuthority> authorities = new ArrayList<>();

    // Map Roles with standard Spring Security prefix
    if (authz.roles() != null) {
      for (String role : authz.roles()) {
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
      }
    } else {
      // Default fallback role for unassigned users
      authorities.add(new SimpleGrantedAuthority("ROLE_UNKNOWN"));
    }

    // Map atomic Permissions (Crucial for fine-grained @PreAuthorize)
    if (authz.permissions() != null) {
      for (String perm : authz.permissions()) {
        authorities.add(new SimpleGrantedAuthority("PERM_" + perm));
      }
    }

    String principalName = jwt.getSubject() != null ? jwt.getSubject() : userId.toString();

    return new JwtAuthenticationToken(jwt, authorities, principalName);
  }
}

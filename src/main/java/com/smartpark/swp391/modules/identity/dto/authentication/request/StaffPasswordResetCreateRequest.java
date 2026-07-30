package com.smartpark.swp391.modules.identity.dto.authentication.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public record StaffPasswordResetCreateRequest(@NotBlank @Email @Size(max = 255) String email) {

  public String normalizedEmail() {
    return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
  }
}

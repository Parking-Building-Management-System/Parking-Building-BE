package com.smartpark.swp391.modules.identity.controller;

import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.common.response.ApiResponse;
import com.smartpark.swp391.common.security.annotation.RateLimit;
import com.smartpark.swp391.modules.identity.dto.authentication.request.AuthenticationRequest;
import com.smartpark.swp391.modules.identity.dto.authentication.response.AuthenticationResponse;
import com.smartpark.swp391.modules.identity.dto.authentication.response.UserProfileResponse;
import com.smartpark.swp391.modules.identity.dto.token.response.TokenPair;
import com.smartpark.swp391.modules.identity.service.auth.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Tag(
    name = "Authentication",
    description = "Đăng nhập zero-trust, cấp/refresh JWT, lấy hồ sơ và thu hồi session")
public class AuthenticationController {

  AuthenticationService authenticationService;

  @NonFinal
  @Value("${cookie.path:/}")
  String cookiePath;

  @NonFinal
  @Value("${cookie.secure:true}")
  boolean cookieSecure;

  @PostMapping("/login")
  @Operation(
      summary = "Đăng nhập bằng username/password và thiết bị tin cậy",
      description =
          "Xác thực thông tin đăng nhập, kiểm tra device fingerprint đã được duyệt,"
              + " tạo session và trả về access token kèm refresh token HttpOnly cookie.")
  @SecurityRequirements
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      required = true,
      description = "Thông tin đăng nhập và định danh thiết bị của client",
      content =
          @Content(
              schema = @Schema(implementation = AuthenticationRequest.class),
              examples =
                  @ExampleObject(
                      name = "Login",
                      value =
                          """
                                                      {
                                                        "username": "system.admin@smartpark.local",
                                                      "password": "<demo-password>",
                                                        "deviceFingerprint": "seed-system-admin-device",
                                                        "deviceLabel": "Admin laptop"
                                                      }
                                                    """)))
  @ApiResponses(
      value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Đăng nhập thành công",
            headers =
                @Header(
                    name = HttpHeaders.SET_COOKIE,
                    description = "refresh_token HttpOnly cookie"),
            content = @Content(schema = @Schema(implementation = AuthenticationResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Sai thông tin đăng nhập hoặc thiết bị chưa được tin cậy",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "429",
            description = "Vượt giới hạn số lần đăng nhập theo username",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
      })
  @RateLimit(
      limit = 5,
      duration = 60,
      type = RateLimit.Type.REQUEST_FIELD,
      fieldName = "#request.username")
  public ResponseEntity<ApiResponse<AuthenticationResponse>> login(
      @Valid @RequestBody AuthenticationRequest request) {
    TokenPair result = authenticationService.authenticate(request);

    ResponseCookie refreshCookie =
        ResponseCookie.from("refresh_token", result.getRefreshToken())
            .httpOnly(true)
            .secure(cookieSecure)
            .sameSite("None")
            .path(cookiePath)
            .maxAge(result.getRefreshTtl())
            .build();

    AuthenticationResponse body =
        AuthenticationResponse.builder()
            .authenticated(true)
            .accessToken(result.getAccessToken())
            .refreshToken(result.getRefreshToken())
            .build();

    ApiResponse<AuthenticationResponse> resp =
        ApiResponse.<AuthenticationResponse>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message("Đăng nhập thành công")
            .result(body)
            .timestamp(Instant.now())
            .path("/auth/login")
            .build();

    return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, refreshCookie.toString()).body(resp);
  }

  @PostMapping("/refresh")
  @Operation(
      summary = "Làm mới access token",
      description =
          "Nhận refresh token từ HttpOnly cookie hoặc header X-Refresh-Token,"
              + " kiểm tra session còn active và rotate refresh JTI để chống replay.")
  @SecurityRequirements
  @ApiResponses(
      value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Refresh token thành công",
            headers =
                @Header(
                    name = HttpHeaders.SET_COOKIE,
                    description = "refresh_token mới sau khi rotate JTI"),
            content = @Content(schema = @Schema(implementation = AuthenticationResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Refresh token thiếu, hết hạn, sai loại hoặc đã bị replay",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
      })
  public ResponseEntity<ApiResponse<AuthenticationResponse>> refresh(
      @Parameter(
              in = ParameterIn.COOKIE,
              name = "refresh_token",
              description = "Refresh token được lưu bằng HttpOnly cookie")
          @CookieValue(name = "refresh_token", required = false)
          String refreshCookie,
      @Parameter(
              in = ParameterIn.HEADER,
              name = "X-Refresh-Token",
              description = "Fallback cho client không gửi được cookie")
          @RequestHeader(value = "X-Refresh-Token", required = false)
          String refreshHeader) {

    String refreshToken =
        (refreshHeader != null && !refreshHeader.isEmpty()) ? refreshHeader : refreshCookie;

    TokenPair pair = authenticationService.refresh(refreshToken);

    ResponseCookie cookie =
        ResponseCookie.from("refresh_token", pair.getRefreshToken())
            .httpOnly(true)
            .secure(cookieSecure)
            .sameSite("None")
            .path(cookiePath)
            .maxAge(pair.getRefreshTtl())
            .build();

    AuthenticationResponse body =
        AuthenticationResponse.builder()
            .authenticated(true)
            .accessToken(pair.getAccessToken())
            .refreshToken(pair.getRefreshToken())
            .build();

    ApiResponse<AuthenticationResponse> resp =
        ApiResponse.<AuthenticationResponse>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message(ErrorCode.SUCCESS.getDefaultMessage())
            .result(body)
            .timestamp(Instant.now())
            .path("/auth/refresh")
            .build();

    return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body(resp);
  }

  @GetMapping("/me")
  @Operation(
      summary = "Lấy thông tin người dùng hiện tại",
      description =
          "Đọc JWT hiện tại, resolve session authorization từ Redis/DB và trả về"
              + " thông tin profile, role, permission của user.")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses(
      value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Lấy thông tin cá nhân thành công",
            content = @Content(schema = @Schema(implementation = UserProfileResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Access token thiếu, hết hạn hoặc session đã bị thu hồi",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
      })
  public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile() {

    UserProfileResponse result = authenticationService.getMyProfile();

    return ResponseEntity.ok(
        ApiResponse.<UserProfileResponse>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message("Lấy thông tin cá nhân thành công")
            .result(result)
            .timestamp(Instant.now())
            .path("/auth/me")
            .build());
  }

  @PostMapping("/logout")
  @Operation(
      summary = "Đăng xuất session hiện tại",
      description =
          "Thu hồi session trong DB, ghi revoked marker vào Redis theo TTL access token"
              + " còn lại và xóa refresh token cookie.")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses(
      value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Đăng xuất thành công",
            headers =
                @Header(name = HttpHeaders.SET_COOKIE, description = "Xóa refresh_token cookie"),
            content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Access token không hợp lệ hoặc session không còn active",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
      })
  public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal Jwt jwt) {

    UUID sessionId = UUID.fromString(jwt.getClaimAsString("session_id"));
    UUID userId = UUID.fromString(jwt.getClaimAsString("user_id"));

    authenticationService.logout(sessionId, userId);

    ResponseCookie clearCookie =
        ResponseCookie.from("refresh_token", "")
            .httpOnly(true)
            .secure(cookieSecure)
            .sameSite("None")
            .path(cookiePath)
            .maxAge(0)
            .build();

    ApiResponse<Void> response =
        ApiResponse.<Void>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message("Đăng xuất thành công")
            .result(null)
            .timestamp(Instant.now())
            .path("/auth/logout")
            .build();

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
        .body(response);
  }

  @PostMapping("/logout-all")
  @Operation(
      summary = "Đăng xuất tất cả thiết bị của user hiện tại",
      description =
          "Thu hồi toàn bộ session active của user trong DB, đánh dấu revoked trên Redis"
              + " và xóa refresh token cookie của request hiện tại.")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses(
      value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Đăng xuất tất cả thiết bị thành công",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Access token không hợp lệ",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
      })
  public ResponseEntity<ApiResponse<Void>> logoutAll(@AuthenticationPrincipal Jwt jwt) {

    UUID userId = UUID.fromString(jwt.getClaimAsString("user_id"));

    authenticationService.logoutAll(userId);

    ResponseCookie clearCookie =
        ResponseCookie.from("refresh_token", "")
            .httpOnly(true)
            .secure(cookieSecure)
            .sameSite("None")
            .path(cookiePath)
            .maxAge(0)
            .build();

    ApiResponse<Void> response =
        ApiResponse.<Void>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message("Đăng xuất tất cả thiết bị thành công")
            .result(null)
            .timestamp(Instant.now())
            .path("/auth/logout-all")
            .build();

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
        .body(response);
  }

  @PreAuthorize("hasRole('SYSTEM_ADMIN')")
  @PostMapping("/admin/users/{userId}/force-logout")
  @Operation(
      summary = "System Admin force logout một user",
      description =
          "Thu hồi toàn bộ session active của user bất kỳ. Endpoint này chỉ dành cho"
              + " role SYSTEM_ADMIN.")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses(
      value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Force logout thành công",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "User hiện tại không có role SYSTEM_ADMIN",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
      })
  public ResponseEntity<ApiResponse<Void>> forceLogout(@PathVariable UUID userId) {

    authenticationService.forceLogout(userId);

    ApiResponse<Void> response =
        ApiResponse.<Void>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message("Force logout thành công")
            .result(null)
            .timestamp(Instant.now())
            .path("/auth/admin/users/" + userId + "/force-logout")
            .build();

    return ResponseEntity.ok(response);
  }
}

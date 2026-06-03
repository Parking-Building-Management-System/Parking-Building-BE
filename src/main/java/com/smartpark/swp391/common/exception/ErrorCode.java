package com.smartpark.swp391.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
  SUCCESS(1000, "Success", HttpStatus.OK),

  // error for auth service
  INVALID_INFO(4003, "Sai thông tin đăng nhập", HttpStatus.BAD_REQUEST),
  INVALID_USER_ID(4004, "Thông tin user không hợp lệ", HttpStatus.BAD_REQUEST),

  // custom handling error for user behavior for 4xx
  VALIDATION_ERROR(4000, "Validation failed", HttpStatus.BAD_REQUEST),
  MALFORMED_JSON(4001, "Malformed JSON request", HttpStatus.BAD_REQUEST),
  INVALID_INPUT(4002, "Invalid input", HttpStatus.BAD_REQUEST),
  DEVICE_NOT_TRUST(4005, "Thiết bị chưa được tin cậy", HttpStatus.FORBIDDEN),
  UNAUTHENTICATED(4010, "Unauthenticated", HttpStatus.UNAUTHORIZED),
  FORBIDDEN_ACTION(4030, "Forbidden", HttpStatus.FORBIDDEN),
  RESOURCE_NOT_FOUND(4040, "Not found", HttpStatus.NOT_FOUND),
  DUPLICATE_RESOURCE(4090, "Conflict", HttpStatus.CONFLICT),

  UNSUPPORTED_MEDIA_TYPE(4150, "Unsupported media type", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
  TOO_MANY_REQUESTS(4290, "Too many requests", HttpStatus.TOO_MANY_REQUESTS),

  REQUEST_FAILED(4999, "Request failed", HttpStatus.BAD_REQUEST),
  STORAGE_NOT_CONFIGURED(5030, "Storage is not configured", HttpStatus.SERVICE_UNAVAILABLE),
  UNEXPECTED_ERROR(5000, "Unexpected error", HttpStatus.INTERNAL_SERVER_ERROR),
  INVALID_SORT_FIELD(4048, "Không thể sắp xếp theo trường này!", HttpStatus.BAD_REQUEST),
  INVALID_PAGINATION(4049, "Page phải >= 0, size phải từ 1-100!", HttpStatus.BAD_REQUEST);

  private final int code;
  private final String defaultMessage;
  private final HttpStatus status;
}

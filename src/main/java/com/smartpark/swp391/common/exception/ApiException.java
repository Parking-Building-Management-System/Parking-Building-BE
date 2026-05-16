package com.smartpark.swp391.common.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ApiException extends RuntimeException {
  ErrorCode errorCode;

  public ApiException(ErrorCode errorCode) {
    super(errorCode.getDefaultMessage());
    this.errorCode = errorCode;
  }

  public ApiException(ErrorCode errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }
}

package com.smartpark.swp391.common.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Custom Annotation (AOP) áp dụng đối với trước 1 method
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

  long limit() default 10;

  long duration() default 60;

  Type type() default Type.USER_ID;

  String fieldName() default "";

  enum Type {
    USER_ID,
    IP_ADDRESS,
    REQUEST_FIELD
  }
}

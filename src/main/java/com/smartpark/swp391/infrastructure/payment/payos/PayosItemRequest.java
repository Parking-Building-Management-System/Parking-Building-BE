package com.smartpark.swp391.infrastructure.payment.payos;

public record PayosItemRequest(String name, int quantity, long price) {}

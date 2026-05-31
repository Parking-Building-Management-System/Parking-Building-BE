package com.smartpark.swp391.modules.payment.service.impl;

import com.smartpark.swp391.modules.payment.service.OrderCodeGenerator;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

@Service
public class TimeOrderCodeGenerator implements OrderCodeGenerator {

  private static final DateTimeFormatter SECOND = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
  private final AtomicInteger sequence = new AtomicInteger(0);
  private volatile String sequenceSecond = LocalDateTime.now().format(SECOND);

  @Override
  public synchronized long nextOrderCode() {
    String currentSecond = LocalDateTime.now().format(SECOND);
    if (!currentSecond.equals(sequenceSecond)) {
      sequenceSecond = currentSecond;
      sequence.set(0);
    }
    int next = sequence.updateAndGet(value -> value >= 99 ? 1 : value + 1);
    return Long.parseLong(currentSecond + String.format("%02d", next));
  }
}

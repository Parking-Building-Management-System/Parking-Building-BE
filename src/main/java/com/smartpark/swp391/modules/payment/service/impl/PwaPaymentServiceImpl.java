package com.smartpark.swp391.modules.payment.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.infrastructure.payment.payos.PayosClient;
import com.smartpark.swp391.infrastructure.payment.payos.PayosCreatePaymentLinkRequest;
import com.smartpark.swp391.infrastructure.payment.payos.PayosCreatePaymentLinkResponse;
import com.smartpark.swp391.infrastructure.payment.payos.PayosItemRequest;
import com.smartpark.swp391.infrastructure.payment.payos.PayosProperties;
import com.smartpark.swp391.infrastructure.payment.payos.PayosProviderException;
import com.smartpark.swp391.modules.operation.entity.ParkingSession;
import com.smartpark.swp391.modules.operation.enumType.ParkingSessionStatus;
import com.smartpark.swp391.modules.operation.enumType.SessionPaymentStatus;
import com.smartpark.swp391.modules.operation.repository.ParkingSessionRepository;
import com.smartpark.swp391.modules.parking.entity.RfidCard;
import com.smartpark.swp391.modules.parking.enumType.RfidCardStatus;
import com.smartpark.swp391.modules.parking.repository.RfidCardRepository;
import com.smartpark.swp391.modules.payment.dto.ExistingPaymentIntentResponse;
import com.smartpark.swp391.modules.payment.dto.PaymentIntentResponse;
import com.smartpark.swp391.modules.payment.dto.PaymentIntentStatusResponse;
import com.smartpark.swp391.modules.payment.entity.PaymentIntent;
import com.smartpark.swp391.modules.payment.enumType.PaymentIntentStatus;
import com.smartpark.swp391.modules.payment.enumType.PaymentProvider;
import com.smartpark.swp391.modules.payment.repository.PaymentIntentRepository;
import com.smartpark.swp391.modules.payment.service.OrderCodeGenerator;
import com.smartpark.swp391.modules.payment.service.PwaPaymentService;
import com.smartpark.swp391.modules.pricing.dto.PricingQuoteResponse;
import com.smartpark.swp391.modules.pricing.entity.PricingRule;
import com.smartpark.swp391.modules.pricing.repository.PricingRuleRepository;
import com.smartpark.swp391.modules.pricing.service.PricingQuoteService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class PwaPaymentServiceImpl implements PwaPaymentService {

  private static final int PAYMENT_EXPIRES_MINUTES = 15;

  RfidCardRepository rfidCardRepository;
  ParkingSessionRepository parkingSessionRepository;
  PricingQuoteService pricingQuoteService;
  PricingRuleRepository pricingRuleRepository;
  PaymentIntentRepository paymentIntentRepository;
  OrderCodeGenerator orderCodeGenerator;
  PayosClient payosClient;
  PayosProperties payosProperties;
  ObjectMapper objectMapper;

  @Override
  @Transactional
  public PaymentIntentResponse createPaymentIntent(String qrToken) {
    RfidCard card = getActiveCard(qrToken);
    ParkingSession session = getActiveSessionForCard(card);
    if (session.getPaymentStatus() == SessionPaymentStatus.PAID) {
      return paidSessionResponse(session);
    }

    LocalDateTime now = LocalDateTime.now();
    PricingQuoteResponse quote =
        pricingQuoteService.quote(
            session.getTenant().getId(),
            session.getParking().getId(),
            session.getVehicleType().getId(),
            session.getCheckInAt(),
            now);

    Optional<ExistingPaymentIntentResponse> existing =
        findReusablePendingIntent(session.getId(), quote.amount());
    if (existing.isPresent()) {
      PaymentIntent intent =
          paymentIntentRepository
              .findByOrderCodeAndDeletedFalse(existing.get().orderCode())
              .orElseThrow(
                  () -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "PAYMENT_INTENT_NOT_FOUND"));
      return toResponse(intent);
    }

    PricingRule pricingRule =
        pricingRuleRepository
            .findById(quote.pricingRuleId())
            .orElseThrow(
                () ->
                    new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "PRICING_RULE_NOT_CONFIGURED"));
    if (quote.amount().compareTo(BigDecimal.ZERO) > 0) {
      try {
        payosProperties.requireReadyForPaymentCreation();
      } catch (PayosProviderException e) {
        throw new ApiException(ErrorCode.REQUEST_FAILED, e.getMessage());
      }
    }
    long orderCode = uniqueOrderCode();
    String description = "SPK" + orderCode;
    LocalDateTime expiresAt = now.plusMinutes(PAYMENT_EXPIRES_MINUTES);

    PaymentIntent intent =
        PaymentIntent.builder()
            .tenant(session.getTenant())
            .parkingSession(session)
            .rfidCard(card)
            .orderCode(orderCode)
            .amount(quote.amount())
            .currency(quote.currency())
            .status(PaymentIntentStatus.PENDING)
            .provider(PaymentProvider.PAYOS)
            .description(description)
            .pricingRule(pricingRule)
            .quoteSnapshotJson(writeJson(quote))
            .expiresAt(expiresAt)
            .build();

    if (quote.amount().compareTo(BigDecimal.ZERO) <= 0) {
      LocalDateTime paidAt = now;
      intent.setStatus(PaymentIntentStatus.PAID);
      intent.setPaidAt(paidAt);
      markSessionPaid(
          session, orderCode, quote.amount(), paidAt, pricingRule.getGraceMinutesAfterPayment());
      return toResponse(paymentIntentRepository.save(intent));
    }

    try {
      PayosCreatePaymentLinkResponse providerResponse =
          payosClient.createPaymentLink(
              new PayosCreatePaymentLinkRequest(
                  orderCode,
                  toPayosAmount(quote.amount()),
                  description,
                  List.of(
                      new PayosItemRequest(
                          "SmartPark parking fee", 1, toPayosAmount(quote.amount()))),
                  payosProperties.returnUrl(),
                  payosProperties.cancelUrl(),
                  expiresAt.atZone(ZoneId.systemDefault()).toEpochSecond()));
      intent.setCheckoutUrl(providerResponse.checkoutUrl());
      intent.setQrCode(providerResponse.qrCode());
      intent.setProviderPaymentLinkId(providerResponse.paymentLinkId());
      intent.setRawProviderResponse(providerResponse.rawResponse());
    } catch (PayosProviderException e) {
      throw new ApiException(ErrorCode.REQUEST_FAILED, e.getMessage());
    }

    return toResponse(paymentIntentRepository.save(intent));
  }

  @Override
  @Transactional(readOnly = true)
  public PaymentIntentStatusResponse getPaymentIntentStatus(Long orderCode) {
    PaymentIntent intent =
        paymentIntentRepository
            .findStatusSummaryByOrderCode(orderCode)
            .orElseThrow(
                () -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "PAYMENT_INTENT_NOT_FOUND"));
    ParkingSession session = intent.getParkingSession();
    RfidCard card = session.getRfidCard();
    return PaymentIntentStatusResponse.builder()
        .orderCode(intent.getOrderCode())
        .status(resolveLiveStatus(intent))
        .amount(intent.getAmount())
        .currency(intent.getCurrency())
        .paidAt(intent.getPaidAt())
        .exitDeadline(session.getExitDeadline())
        .sessionId(session.getId())
        .plateNumber(session.getLicensePlate())
        .cardCode(card == null ? null : card.getCode())
        .checkoutUrl(intent.getCheckoutUrl())
        .qrCode(intent.getQrCode())
        .build();
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<ExistingPaymentIntentResponse> findReusablePendingIntent(
      UUID sessionId, BigDecimal amount) {
    LocalDateTime now = LocalDateTime.now();
    return paymentIntentRepository
        .findBySessionAndStatus(sessionId, PaymentIntentStatus.PENDING)
        .stream()
        .filter(intent -> intent.getAmount().compareTo(amount) == 0)
        .filter(intent -> intent.getExpiresAt() == null || intent.getExpiresAt().isAfter(now))
        .findFirst()
        .map(
            intent ->
                ExistingPaymentIntentResponse.builder()
                    .orderCode(intent.getOrderCode())
                    .status(intent.getStatus())
                    .checkoutUrl(intent.getCheckoutUrl())
                    .expiresAt(intent.getExpiresAt())
                    .build());
  }

  @Override
  public boolean paymentProviderAvailable() {
    return payosProperties.enabled() && payosProperties.configured();
  }

  @Override
  public PaymentIntentResponse toResponse(PaymentIntent intent) {
    return PaymentIntentResponse.builder()
        .paymentIntentId(intent.getId())
        .orderCode(intent.getOrderCode())
        .amount(intent.getAmount())
        .currency(intent.getCurrency())
        .status(resolveLiveStatus(intent))
        .provider(intent.getProvider())
        .checkoutUrl(intent.getCheckoutUrl())
        .qrCode(intent.getQrCode())
        .expiresAt(intent.getExpiresAt())
        .description(intent.getDescription())
        .paidAt(intent.getPaidAt())
        .exitDeadline(intent.getParkingSession().getExitDeadline())
        .build();
  }

  private PaymentIntentResponse paidSessionResponse(ParkingSession session) {
    return PaymentIntentResponse.builder()
        .paymentIntentId(null)
        .orderCode(parseLongOrNull(session.getPaymentReference()))
        .amount(session.getTotalAmount())
        .currency("VND")
        .status(PaymentIntentStatus.PAID)
        .provider(PaymentProvider.PAYOS)
        .paidAt(session.getPaidAt())
        .exitDeadline(session.getExitDeadline())
        .description("SESSION_ALREADY_PAID")
        .build();
  }

  private PaymentIntentStatus resolveLiveStatus(PaymentIntent intent) {
    if (intent.getStatus() == PaymentIntentStatus.PENDING
        && intent.getExpiresAt() != null
        && !intent.getExpiresAt().isAfter(LocalDateTime.now())) {
      return PaymentIntentStatus.EXPIRED;
    }
    return intent.getStatus();
  }

  private void markSessionPaid(
      ParkingSession session,
      long orderCode,
      BigDecimal amount,
      LocalDateTime paidAt,
      int graceMinutes) {
    session.setPaymentStatus(SessionPaymentStatus.PAID);
    session.setPaymentMethod(PaymentProvider.PAYOS.name());
    session.setPaymentReference(String.valueOf(orderCode));
    session.setPaidAt(paidAt);
    session.setExitDeadline(paidAt.plusMinutes(graceMinutes));
    session.setTotalAmount(amount);
  }

  private RfidCard getActiveCard(String qrToken) {
    RfidCard card =
        rfidCardRepository
            .findByQrToken(normalizeToken(qrToken))
            .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "CARD_QR_NOT_FOUND"));
    if (card.getStatus() != RfidCardStatus.ACTIVE) {
      throw new ApiException(ErrorCode.FORBIDDEN_ACTION, "CARD_NOT_ACTIVE");
    }
    return card;
  }

  private ParkingSession getActiveSessionForCard(RfidCard card) {
    return parkingSessionRepository
        .findActiveByRfidCardId(card.getId(), ParkingSessionStatus.ACTIVE, PageRequest.of(0, 1))
        .stream()
        .findFirst()
        .orElseThrow(
            () -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "NO_ACTIVE_SESSION_FOR_CARD"));
  }

  private String normalizeToken(String qrToken) {
    if (qrToken == null || qrToken.isBlank()) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "qrToken must not be blank");
    }
    return qrToken.trim();
  }

  private long uniqueOrderCode() {
    for (int attempt = 0; attempt < 5; attempt++) {
      long orderCode = orderCodeGenerator.nextOrderCode();
      if (!paymentIntentRepository.existsByOrderCode(orderCode)) {
        return orderCode;
      }
    }
    throw new ApiException(ErrorCode.DUPLICATE_RESOURCE, "PAYMENT_ORDER_CODE_COLLISION");
  }

  private long toPayosAmount(BigDecimal amount) {
    try {
      return amount.toBigIntegerExact().longValueExact();
    } catch (ArithmeticException e) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "PAYMENT_AMOUNT_MUST_BE_INTEGER_VND");
    }
  }

  private String writeJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new ApiException(ErrorCode.UNEXPECTED_ERROR, "PAYMENT_QUOTE_SNAPSHOT_FAILED");
    }
  }

  private Long parseLongOrNull(String value) {
    try {
      return value == null ? null : Long.parseLong(value);
    } catch (NumberFormatException e) {
      return null;
    }
  }
}

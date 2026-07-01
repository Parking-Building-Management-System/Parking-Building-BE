package com.smartpark.swp391.modules.payment.repository;

import com.smartpark.swp391.modules.payment.entity.PaymentIntent;
import com.smartpark.swp391.modules.payment.enumType.PaymentIntentStatus;
import com.smartpark.swp391.modules.payment.enumType.PaymentProvider;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentIntentRepository extends JpaRepository<PaymentIntent, UUID> {

  Optional<PaymentIntent> findByOrderCodeAndDeletedFalse(Long orderCode);

  boolean existsByOrderCode(Long orderCode);

  @Query(
      """
          SELECT pi
          FROM PaymentIntent pi
          JOIN FETCH pi.parkingSession ps
          JOIN FETCH ps.rfidCard card
          WHERE pi.orderCode = :orderCode
            AND pi.deleted = false
          """)
  Optional<PaymentIntent> findStatusSummaryByOrderCode(@Param("orderCode") Long orderCode);

  @Query(
      """
          SELECT pi
          FROM PaymentIntent pi
          WHERE pi.parkingSession.id = :sessionId
            AND pi.status = :status
            AND pi.deleted = false
          ORDER BY pi.createdAt DESC
          """)
  List<PaymentIntent> findBySessionAndStatus(
      @Param("sessionId") UUID sessionId, @Param("status") PaymentIntentStatus status);

  @Query(
      """
          SELECT COALESCE(SUM(pi.amount), 0)
          FROM PaymentIntent pi
          JOIN pi.parkingSession ps
          WHERE pi.tenant.id = :tenantId
            AND ps.parking.id = :parkingId
            AND pi.status = :status
            AND pi.provider = :provider
            AND pi.deleted = false
            AND pi.paidAt >= :from
            AND pi.paidAt <= :to
          """)
  BigDecimal sumAmountByParkingAndPaidAtRange(
      @Param("tenantId") UUID tenantId,
      @Param("parkingId") UUID parkingId,
      @Param("status") PaymentIntentStatus status,
      @Param("provider") PaymentProvider provider,
      @Param("from") LocalDateTime from,
      @Param("to") LocalDateTime to);
}

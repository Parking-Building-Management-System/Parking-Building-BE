package com.smartpark.swp391.modules.payment.repository;

import com.smartpark.swp391.modules.payment.entity.PaymentIntent;
import com.smartpark.swp391.modules.payment.enumType.PaymentIntentStatus;
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
}

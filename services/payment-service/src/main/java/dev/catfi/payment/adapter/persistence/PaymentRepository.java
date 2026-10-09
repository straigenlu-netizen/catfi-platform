package dev.catfi.payment.adapter.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, UUID> {
  List<PaymentEntity> findByPropertyIdOrderByInitiatedAtDesc(UUID propertyId);
  java.util.Optional<PaymentEntity> findByCompensationResultId(UUID compensationResultId);
  List<PaymentEntity> findTop100ByEventPublishedFalseOrderByInitiatedAtAsc();
}

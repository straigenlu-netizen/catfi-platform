package dev.catfi.compensation.adapter.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompensationRepository extends JpaRepository<CompensationResultEntity, UUID> {
  java.util.Optional<CompensationResultEntity> findByDamageAssessmentId(UUID damageAssessmentId);
}

// Note: We need a JPA Entity for the database.
// Creating the Entity in the same package for brevity or separate.

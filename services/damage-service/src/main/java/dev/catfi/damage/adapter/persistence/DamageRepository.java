package dev.catfi.damage.adapter.persistence;

import dev.catfi.common.domain.DamageAssessment;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DamageRepository {
  private final MongoTemplate mongoTemplate;

  public java.util.List<DamageAssessment> findAll() {
    return mongoTemplate.findAll(DamageAssessment.class, "damage_assessments");
  }

  public void save(DamageAssessment assessment) {
    mongoTemplate.save(assessment, "damage_assessments");
  }
}

--liquibase formatted sql
--changeset catfi:003
CREATE UNIQUE INDEX uq_compensation_damage_assessment
  ON compensation_results(damage_assessment_id);
--rollback DROP INDEX uq_compensation_damage_assessment;

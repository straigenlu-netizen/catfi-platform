package dev.catfi.compensation.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest(
    properties = "spring.jpa.hibernate.ddl-auto=validate")
@org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase(
    replace =
        org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class DatabaseMigrationIT {
  @Container @ServiceConnection
  static final PostgreSQLContainer<?> DATABASE = new PostgreSQLContainer<>("postgres:17.6-alpine");

  @Autowired private JdbcTemplate jdbc;
  @Autowired private dev.catfi.compensation.adapter.persistence.CompensationRepository repository;

  @Test
  void appliesInitialChangelog() {
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM databasechangelog WHERE id = '001'", Integer.class))
        .isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM compensation_result", Integer.class))
        .isZero();
  }

  @Test
  void persistsCompensationUsingTheMigratedSchema() {
    var entity =
        dev.catfi.compensation.adapter.persistence.CompensationResultEntity.builder()
            .id(java.util.UUID.randomUUID())
            .propertyId(java.util.UUID.randomUUID())
            .damageAssessmentId(java.util.UUID.randomUUID())
            .amount(new java.math.BigDecimal("13000.00"))
            .damageAmount(new java.math.BigDecimal("20000.00"))
            .coverageRate(new java.math.BigDecimal("0.900000"))
            .deductible(new java.math.BigDecimal("5000.00"))
            .calculatedAt(java.time.Instant.now())
            .build();
    repository.saveAndFlush(entity);
    assertThat(
            jdbc.queryForObject(
                "SELECT amount FROM compensation_results WHERE id = ?",
                java.math.BigDecimal.class,
                entity.getId()))
        .isEqualByComparingTo("13000.00");
  }
}

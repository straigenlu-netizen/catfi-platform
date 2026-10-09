package dev.catfi.compensation.application;

import dev.catfi.common.domain.*;
import dev.catfi.compensation.adapter.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** One PostgreSQL transaction owns budget consumption, compensation and the outbox. */
@Service
public class DurableCompensationLedger {
  private final JdbcTemplate jdbc;
  private final CompensationRepository repository;
  private final CompensationApplicationService calculator;
  private final com.fasterxml.jackson.databind.ObjectMapper json;

  public DurableCompensationLedger(JdbcTemplate jdbc, CompensationRepository repository,
      CompensationApplicationService calculator, com.fasterxml.jackson.databind.ObjectMapper json) {
    this.jdbc = jdbc;
    this.repository = repository;
    this.calculator = calculator;
    this.json = json;
  }

  @Transactional
  public CompensationResult record(DamageAssessment assessment,
      dev.catfi.compensation.adapter.kafka.CompensationEventProcessor.TrustedPolicy policy) {
    if (assessment == null || assessment.getAssessmentId() == null || assessment.getPropertyId() == null
        || assessment.getDamageAmount() == null || assessment.getDamageAmount().signum() < 0
        || assessment.getDamageAmount().stripTrailingZeros().scale() > 2) {
      throw new IllegalArgumentException("Valid assessment ID, property and amount required");
    }
    UUID property = assessment.getPropertyId();
    // Serializes even the first reservation across compensation-service replicas.
    jdbc.query("SELECT pg_advisory_xact_lock(hashtext(?))",
        (org.springframework.jdbc.core.RowCallbackHandler) rs -> {}, property.toString());
    var prior = repository.findByDamageAssessmentId(assessment.getAssessmentId());
    if (prior.isPresent()) {
      var saved = prior.get();
      if (!saved.getPropertyId().equals(property)
          || saved.getDamageAmount().compareTo(assessment.getDamageAmount()) != 0) {
        throw new IllegalStateException("Conflicting replay for assessment " + assessment.getAssessmentId());
      }
      return toResult(saved);
    }
    if (policy == null || !policy.active() || !property.equals(policy.propertyId())
        || policy.policyLimit() == null || policy.coverageRate() == null
        || policy.deductible() == null || policy.alreadyPaid() == null
        || policy.alreadyPaid().signum() != 0) {
      // The in-memory policy service cannot be an authoritative paid-to-date source.
      // Explicitly reject non-zero legacy baselines until migrated/reconciled.
      throw new IllegalStateException("Active policy with zero legacy balance required; reconcile legacy balances first");
    }
    // Existing compensation rows are the migration baseline, including records created before
    // this ledger existed. The DB uniqueness constraint is the durable assessment inbox.
    BigDecimal spent = jdbc.queryForObject(
        "SELECT COALESCE(SUM(amount), 0) FROM compensation_results WHERE property_id = ?",
        BigDecimal.class, property);
    if (spent == null || spent.compareTo(policy.policyLimit()) > 0) {
      throw new IllegalStateException("Persisted compensation exceeds policy limit; manual reconciliation required");
    }
    var terms = new CompensationTerms(assessment.getDamageAmount(), policy.coverageRate(),
        policy.deductible(), policy.policyLimit(), spent);
    CompensationResult result = calculator.calculateCompensation(property, assessment.getAssessmentId(),
        assessment.getDamageAmount(), terms, PayoutScenario.HYBRID);
    var entity = CompensationResultEntity.builder().id(result.getResultId())
        .propertyId(property).damageAssessmentId(assessment.getAssessmentId())
        .amount(result.getTotalCompensation()).damageAmount(assessment.getDamageAmount())
        .coverageRate(policy.coverageRate()).deductible(policy.deductible())
        .calculatedAt(result.getCalculatedAt()).build();
    repository.saveAndFlush(entity);
    try {
      jdbc.update("INSERT INTO compensation_outbox (id, property_id, payload) VALUES (?, ?, ?::jsonb)",
          result.getResultId(), property, json.writeValueAsString(result));
    } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
      throw new IllegalStateException("Cannot serialize compensation event", ex);
    }
    return result;
  }

  private CompensationResult toResult(CompensationResultEntity saved) {
    BigDecimal pay = saved.getAmount().multiply(new BigDecimal("0.5"))
        .setScale(2, RoundingMode.HALF_EVEN);
    return CompensationResult.builder().resultId(saved.getId())
        .damageAssessmentId(saved.getDamageAssessmentId()).propertyId(saved.getPropertyId())
        .totalCompensation(saved.getAmount()).amountToPay(pay)
        .amountToInvest(saved.getAmount().subtract(pay))
        .coverageRate(saved.getCoverageRate()).deductible(saved.getDeductible())
        .scenario(PayoutScenario.HYBRID).calculatedAt(saved.getCalculatedAt()).build();
  }
}

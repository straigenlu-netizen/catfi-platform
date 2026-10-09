package dev.catfi.portfolio.adapter.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedInvestmentRepository extends JpaRepository<ProcessedInvestmentEntity, UUID> {}

package dev.catfi.reporting.application;

import dev.catfi.common.domain.*;
import java.util.*;

/** Persisted correlation and duplicate receipts, private to reporting. */
public class ReportState {
  public Map<String,DamageAssessment> damage = new LinkedHashMap<>();
  public Map<String,CompensationResult> compensation = new LinkedHashMap<>();
  public Map<String,Payment> payment = new LinkedHashMap<>();
  public Map<String,InvestmentDecision> investment = new LinkedHashMap<>();
  public Map<String,String> receipts = new LinkedHashMap<>();
  public String latestAssessment;
}

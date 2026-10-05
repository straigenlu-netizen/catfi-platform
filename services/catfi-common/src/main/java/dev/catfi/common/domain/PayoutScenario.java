package dev.catfi.common.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PayoutScenario {
  FULL_PAYOUT,
  FULL_INVESTMENT,
  HYBRID
}

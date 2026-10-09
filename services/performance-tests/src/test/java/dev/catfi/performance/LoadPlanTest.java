package dev.catfi.performance;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;

class LoadPlanTest {
  @Test void readOnlyByDefault() {
    var plan = new LoadPlan(100, 120, 0, false);
    assertThat(plan.readRps()).isEqualTo(100);
    assertThat(plan.writeRps()).isZero();
  }

  @Test void splitsRateWithoutChangingTotal() {
    var plan = new LoadPlan(1000, 30, 5, true);
    assertThat(plan.readRps()).isEqualTo(950);
    assertThat(plan.writeRps()).isEqualTo(50);
  }

  @Test void rejectsWritesWithoutExplicitOptIn() {
    assertThatThrownBy(() -> new LoadPlan(5000, 120, 5, false))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test void rejectsInvalidRates() {
    assertThatThrownBy(() -> new LoadPlan(Double.NaN, 120, 0, false)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new LoadPlan(0, 120, 0, false)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new LoadPlan(100, 0, 0, false)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new LoadPlan(100, 120, 101, true)).isInstanceOf(IllegalArgumentException.class);
  }
}

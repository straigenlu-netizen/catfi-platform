package dev.catfi.damage.application;

import static org.assertj.core.api.Assertions.*;

import java.util.*;

class DamageCalculationServiceTest {

  private final DamageCalculationService service = new DamageCalculationService();

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "ACCELEROMETER,1.5,15000",
    "WATER_LEVEL,2,30000",
    "WIND_SPEED,30,15000",
    "OTHER,1,5000",
    "ACCELEROMETER,0,0"
  })
  void shouldCalculateDamageAndPreserveCorrelation(String type, double value, String expected) {
    var property = UUID.randomUUID();
    var disaster = UUID.randomUUID();
    var result = service.calculateDamage(property, disaster, type, value);
    assertThat(result.getDamageAmount()).isEqualByComparingTo(expected).hasScaleOf(2);
    assertThat(result.getPropertyId()).isEqualTo(property);
    assertThat(result.getDisasterEventId()).isEqualTo(disaster);
    assertThat(result.getCalculationBasis()).isNotBlank();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      doubles = {-1, Double.NaN, Double.POSITIVE_INFINITY})
  void shouldRejectInvalidReadings(double value) {
    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                service.calculateDamage(
                    UUID.randomUUID(), UUID.randomUUID(), "WATER_LEVEL", value));
  }
}

package dev.catfi.weather.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class WindSpeedConverterTest {

  private final WindSpeedConverter converter = new WindSpeedConverter();

  @Test
  void shouldConvertWindSpeedFromKilometersPerHourToMetersPerSecond() {
    double speedInKilometersPerHour = 36.0;

    double result = converter.toMetersPerSecond(speedInKilometersPerHour);

    assertThat(result).isCloseTo(10.0, within(0.000001));
  }
}

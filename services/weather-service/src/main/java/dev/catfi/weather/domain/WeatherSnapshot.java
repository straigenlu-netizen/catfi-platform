package dev.catfi.weather.domain;

import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class WeatherSnapshot {
  String region;
  double temperature;
  double windSpeed;
  Double precipitation;
  Instant timestamp;
  String provider;
}

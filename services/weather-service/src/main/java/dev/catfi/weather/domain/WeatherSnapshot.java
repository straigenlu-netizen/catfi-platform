package dev.catfi.weather.domain;

import java.io.Serializable;
import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class WeatherSnapshot implements Serializable {
  private static final long serialVersionUID = 1L;
  String region;
  double temperature;
  double windSpeed;
  Double precipitation;
  Instant timestamp;
  String provider;
}

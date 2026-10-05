package dev.catfi.weather.application;

import dev.catfi.weather.adapter.OpenWeatherClient;
import dev.catfi.weather.domain.WeatherSnapshot;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class WeatherService {
  private final OpenWeatherClient openWeatherClient;

  private final String apiKey;

  public WeatherService(
      OpenWeatherClient openWeatherClient, @Value("${weather.api.key:}") String apiKey) {
    this.openWeatherClient = openWeatherClient;
    this.apiKey = apiKey;
  }

  public WeatherSnapshot getCurrentWeather(String city) {
    if (city == null || city.isBlank()) throw new IllegalArgumentException("City is required");
    if (apiKey == null || apiKey.isBlank())
      throw new WeatherIntegrationException("Weather provider is not configured", null);
    try {
      log.info("Fetching real weather data for city: {}", city);
      var response = openWeatherClient.getWeather(city, apiKey, "metric");

      return WeatherSnapshot.builder()
          .region(response.name())
          .temperature(response.main().temp())
          .windSpeed(response.wind().speed())
          .precipitation(null)
          .timestamp(Instant.now())
          .provider("OpenWeatherMap")
          .build();
    } catch (Exception e) {
      log.error("Failed to fetch weather data from OpenWeatherMap");
      throw new WeatherIntegrationException("External weather API unavailable", e);
    }
  }
}

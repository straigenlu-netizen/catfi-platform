package dev.catfi.weather.application;

public class WeatherIntegrationException extends RuntimeException {
  public WeatherIntegrationException(String message, Throwable cause) {
    super(message, cause);
  }
}

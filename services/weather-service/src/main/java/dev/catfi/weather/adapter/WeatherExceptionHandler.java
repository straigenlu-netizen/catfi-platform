package dev.catfi.weather.adapter;

import dev.catfi.weather.application.WeatherIntegrationException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class WeatherExceptionHandler {
  @ExceptionHandler(WeatherIntegrationException.class)
  public ProblemDetail providerUnavailable() {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_GATEWAY, "External weather provider unavailable");
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ProblemDetail invalidRequest() {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "City is required");
  }
}

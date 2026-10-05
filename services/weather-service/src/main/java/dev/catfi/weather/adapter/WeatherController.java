package dev.catfi.weather.adapter;

import dev.catfi.weather.application.WeatherService;
import dev.catfi.weather.domain.WeatherSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/weather")
@RequiredArgsConstructor
public class WeatherController {
  private final WeatherService weatherService;

  @GetMapping("/{region}")
  public WeatherSnapshot getWeather(@PathVariable String region) {
    return weatherService.getCurrentWeather(region);
  }
}

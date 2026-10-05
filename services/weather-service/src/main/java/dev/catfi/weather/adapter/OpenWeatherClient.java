package dev.catfi.weather.adapter;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "openweather-api", url = "https://api.openweathermap.org/data/2.5")
public interface OpenWeatherClient {

  @GetMapping("/weather")
  WeatherApiResponse getWeather(
      @RequestParam("q") String city,
      @RequestParam("appid") String apiKey,
      @RequestParam("units") String units);

  record WeatherApiResponse(MainData main, WindData wind, String name) {
    public record MainData(double temp, double humidity) {}

    public record WindData(double speed) {}
  }
}

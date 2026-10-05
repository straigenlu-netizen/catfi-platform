package dev.catfi.weather.adapter;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import dev.catfi.weather.application.WeatherService;
import dev.catfi.weather.domain.WeatherSnapshot;
import java.util.*;
import org.junit.jupiter.api.Test;

@org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest(WeatherController.class)
class WeatherControllerTest {

  @org.springframework.beans.factory.annotation.Autowired
  private org.springframework.test.web.servlet.MockMvc mvc;

  @org.springframework.test.context.bean.override.mockito.MockitoBean
  private WeatherService service;

  @Test
  void shouldExposeWeatherSnapshot() throws Exception {
    when(service.getCurrentWeather("Tokyo"))
        .thenReturn(
            WeatherSnapshot.builder()
                .region("Tokyo")
                .temperature(20)
                .windSpeed(10)
                .precipitation(null)
                .provider("OpenWeatherMap")
                .build());
    mvc.perform(get("/api/v1/weather/Tokyo"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.region").value("Tokyo"))
        .andExpect(jsonPath("$.windSpeed").value(10))
        .andExpect(jsonPath("$.provider").value("OpenWeatherMap"));
  }

  @Test
  void shouldReturnSafeProviderError() throws Exception {
    when(service.getCurrentWeather("Tokyo"))
        .thenThrow(
            new dev.catfi.weather.application.WeatherIntegrationException(
                "Secret provider URL", new IllegalStateException("secret-key")));
    mvc.perform(get("/api/v1/weather/Tokyo"))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.detail").value("External weather provider unavailable"));
  }
}

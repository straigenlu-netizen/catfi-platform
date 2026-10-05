package dev.catfi.weather.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import dev.catfi.weather.adapter.OpenWeatherClient;
import java.util.*;
import org.junit.jupiter.api.Test;

class WeatherServiceTest {

  @Test
  void shouldMapMetricResponseWithoutConvertingWindAgain() {
    var client = mock(OpenWeatherClient.class);
    var response =
        new OpenWeatherClient.WeatherApiResponse(
            new OpenWeatherClient.WeatherApiResponse.MainData(21, 70),
            new OpenWeatherClient.WeatherApiResponse.WindData(10),
            "Tokyo");
    when(client.getWeather("Tokyo", "test-key", "metric")).thenReturn(response);
    var service = new WeatherService(client, "test-key");
    var snapshot = service.getCurrentWeather("Tokyo");
    assertThat(snapshot.getRegion()).isEqualTo("Tokyo");
    assertThat(snapshot.getTemperature()).isEqualTo(21);
    assertThat(snapshot.getWindSpeed()).isEqualTo(10);
    assertThat(snapshot.getPrecipitation()).isNull();
    assertThat(snapshot.getProvider()).isEqualTo("OpenWeatherMap");
    assertThat(snapshot.getTimestamp()).isNotNull();
    verify(client).getWeather("Tokyo", "test-key", "metric");
  }

  @Test
  void shouldPreserveCauseWhenProviderFails() {
    var client = mock(OpenWeatherClient.class);
    var cause = new IllegalStateException("Provider unavailable");
    when(client.getWeather(eq("Tokyo"), any(), eq("metric"))).thenThrow(cause);
    assertThatThrownBy(() -> new WeatherService(client, "test-key").getCurrentWeather("Tokyo"))
        .isInstanceOf(WeatherIntegrationException.class)
        .hasCause(cause)
        .hasMessage("External weather API unavailable");
  }

  @Test
  void shouldRejectMalformedProviderResponse() {
    var client = mock(OpenWeatherClient.class);
    when(client.getWeather(eq("Tokyo"), any(), eq("metric")))
        .thenReturn(new OpenWeatherClient.WeatherApiResponse(null, null, "Tokyo"));
    assertThatThrownBy(() -> new WeatherService(client, "test-key").getCurrentWeather("Tokyo"))
        .isInstanceOf(WeatherIntegrationException.class);
  }
}

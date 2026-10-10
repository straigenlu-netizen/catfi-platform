package dev.catfi.sensor.configuration;

@org.springframework.context.annotation.Configuration
public class HttpConfiguration {
  @org.springframework.context.annotation.Bean
  org.springframework.web.client.RestTemplate restTemplate(
      org.springframework.boot.restclient.RestTemplateBuilder builder) {
    return builder
        .connectTimeout(java.time.Duration.ofSeconds(3))
        .readTimeout(java.time.Duration.ofSeconds(5))
        .build();
  }
}

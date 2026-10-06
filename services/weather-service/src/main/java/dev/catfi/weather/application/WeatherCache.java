package dev.catfi.weather.application;
import dev.catfi.weather.domain.WeatherSnapshot;
import java.util.Optional;
public interface WeatherCache { Optional<WeatherSnapshot> find(String region); }

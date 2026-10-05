package dev.catfi.weather.domain;

public final class WindSpeedConverter {

  private static final double KILOMETERS_PER_HOUR_PER_METER_PER_SECOND = 3.6;

  public double toMetersPerSecond(double kilometersPerHour) {
    return kilometersPerHour / KILOMETERS_PER_HOUR_PER_METER_PER_SECOND;
  }
}

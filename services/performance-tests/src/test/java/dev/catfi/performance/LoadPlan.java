package dev.catfi.performance;

/** Validated arrival-rate mix. Write load is opt-in to protect the demo event pipeline. */
public record LoadPlan(double rps, long durationSeconds, double writePercent, boolean allowWrites) {
  public LoadPlan {
    if (!Double.isFinite(rps) || rps <= 0 || durationSeconds <= 0 ||
        !Double.isFinite(writePercent) || writePercent < 0 || writePercent > 100) {
      throw new IllegalArgumentException("rps and duration must be positive; writePercent must be 0..100");
    }
    if (writePercent > 0 && !allowWrites) {
      throw new IllegalArgumentException("Set -DallowWrites=true before generating Kafka disaster events");
    }
  }

  public double readRps() { return rps * (100 - writePercent) / 100; }
  public double writeRps() { return rps * writePercent / 100; }

  public static LoadPlan fromSystemProperties() {
    return new LoadPlan(
        Double.parseDouble(System.getProperty("rps", "100")),
        Long.parseLong(System.getProperty("durationSeconds", "120")),
        Double.parseDouble(System.getProperty("writePercent", "0")),
        Boolean.parseBoolean(System.getProperty("allowWrites", "false")));
  }
}

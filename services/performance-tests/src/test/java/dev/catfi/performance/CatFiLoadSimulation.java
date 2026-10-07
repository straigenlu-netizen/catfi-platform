package dev.catfi.performance;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.PopulationBuilder;
import io.gatling.javaapi.core.Simulation;
import java.time.Duration;
import java.util.ArrayList;

public class CatFiLoadSimulation extends Simulation {
  private static final double TARGET_RPS = Double.parseDouble(System.getProperty("rps", "100"));
  private static final long DURATION_SECONDS = Long.parseLong(System.getProperty("durationSeconds", "120"));
  private static final double WRITE_PERCENT = Double.parseDouble(System.getProperty("writePercent", "5"));
  private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8087");

  {
    if (TARGET_RPS <= 0 || DURATION_SECONDS <= 0 || WRITE_PERCENT < 0 || WRITE_PERCENT > 100) {
      throw new IllegalArgumentException("Use rps > 0, durationSeconds > 0 and writePercent between 0 and 100");
    }

    double writeRps = TARGET_RPS * WRITE_PERCENT / 100.0;
    double readRps = TARGET_RPS - writeRps;
    Duration duration = Duration.ofSeconds(DURATION_SECONDS);
    var httpProtocol = http.baseUrl(BASE_URL).acceptHeader("application/json").contentTypeHeader("application/json");

    var login = exec(http("Agent login").post("/api/auth/login")
        .formParam("username", "AgentCRM1").formParam("password", "agent_391")
        .check(status().is(204)));
    var dashboard = scenario("dashboard-read")
        .exec(login)
        .exec(http("GET dashboard").get("/api/ui/dashboard").check(status().is(200)));
    var disaster = scenario("disaster-write")
        .exec(login)
        .exec(http("POST disaster simulation").post("/api/ui/trigger-disaster")
            .body(StringBody("{\"type\":\"EARTHQUAKE\",\"latitude\":35.6762,\"longitude\":139.6503,\"intensity\":6.0}"))
            .check(status().in(200, 202)));

    var population = new ArrayList<PopulationBuilder>();
    if (readRps > 0) population.add(dashboard.injectOpen(constantUsersPerSec(readRps).during(duration)));
    if (writeRps > 0) population.add(disaster.injectOpen(constantUsersPerSec(writeRps).during(duration)));

    setUp(population.toArray(PopulationBuilder[]::new))
        .protocols(httpProtocol)
        .assertions(
            global().failedRequests().percent().lt(1.0),
            global().responseTime().percentile(95.0).lt(500),
            global().responseTime().percentile(99.0).lt(1000));
  }
}

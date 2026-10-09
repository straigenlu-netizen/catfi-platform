package dev.catfi.performance;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.PopulationBuilder;
import io.gatling.javaapi.core.Simulation;
import java.time.Duration;
import java.util.ArrayList;

/** BFF end-to-end arrival rate, including one login per simulated request. */
public class CatFiLoadSimulation extends Simulation {
  private static final LoadPlan PLAN = LoadPlan.fromSystemProperties();
  private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8087");
  private static final String AGENT_USER = System.getenv().getOrDefault("CATFI_LOAD_AGENT_USER", "demo.agent");
  private static final String AGENT_PASSWORD = System.getenv("CATFI_LOAD_AGENT_PASSWORD");

  {
    if (AGENT_PASSWORD == null || AGENT_PASSWORD.isBlank()) {
      throw new IllegalArgumentException("Set CATFI_LOAD_AGENT_PASSWORD (never put passwords in the simulation source)");
    }
    Duration duration = Duration.ofSeconds(PLAN.durationSeconds());
    var httpProtocol = http.baseUrl(BASE_URL)
        .acceptHeader("application/json")
        .contentTypeHeader("application/json");

    // Spring Security protects the login itself with a session-bound CSRF token.
    var authenticatedAgent = exec(http("GET CSRF token")
            .get("/api/auth/csrf")
            .check(status().is(200), jsonPath("$.token").saveAs("csrfToken")))
        .exec(http("Agent login")
            .post("/api/auth/login")
            .header("X-XSRF-TOKEN", "#{csrfToken}")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .formParam("username", AGENT_USER)
            .formParam("password", AGENT_PASSWORD)
            .check(status().is(204)));

    var dashboard = scenario("dashboard-read")
        .exec(authenticatedAgent)
        .exec(http("GET dashboard").get("/api/ui/dashboard").check(status().is(200)));

    var disaster = scenario("disaster-write")
        .exec(authenticatedAgent)
        .exec(http("POST disaster simulation")
            .post("/api/ui/trigger-disaster")
            .header("X-XSRF-TOKEN", "#{csrfToken}")
            .body(StringBody("{\"type\":\"EARTHQUAKE\",\"latitude\":35.6762,\"longitude\":139.6503,\"intensity\":6.0}"))
            .check(status().is(200)));

    var population = new ArrayList<PopulationBuilder>();
    if (PLAN.readRps() > 0) {
      population.add(dashboard.injectOpen(constantUsersPerSec(PLAN.readRps()).during(duration)));
    }
    if (PLAN.writeRps() > 0) {
      population.add(disaster.injectOpen(constantUsersPerSec(PLAN.writeRps()).during(duration)));
    }

    setUp(population.toArray(PopulationBuilder[]::new))
        .protocols(httpProtocol)
        .assertions(
            global().failedRequests().percent().lt(1.0),
            details("GET dashboard").responseTime().percentile(95.0).lt(500),
            details("GET dashboard").responseTime().percentile(99.0).lt(1000));
  }
}

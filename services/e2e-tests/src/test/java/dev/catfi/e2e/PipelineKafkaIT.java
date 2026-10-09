package dev.catfi.e2e;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import dev.catfi.bff.adapter.client.*;
import dev.catfi.bff.adapter.web.UiController;
import dev.catfi.bff.application.*;
import dev.catfi.common.domain.*;
import dev.catfi.compensation.adapter.kafka.CompensationEventProcessor;
import dev.catfi.compensation.adapter.persistence.CompensationRepository;
import dev.catfi.compensation.application.CompensationApplicationService;
import dev.catfi.compensation.domain.CompensationCalculator;
import dev.catfi.damage.adapter.kafka.DamageEventProcessor;
import dev.catfi.damage.adapter.persistence.DamageRepository;
import dev.catfi.damage.application.DamageCalculationService;
import dev.catfi.disaster.adapter.web.SimulationController;
import dev.catfi.disaster.application.DisasterSimulationService;
import dev.catfi.investment.adapter.kafka.InvestmentEventProcessor;
import dev.catfi.investment.application.InvestmentService;
import dev.catfi.payment.adapter.kafka.PaymentEventProcessor;
import dev.catfi.payment.adapter.persistence.PaymentRepository;
import dev.catfi.payment.application.PaymentService;
import dev.catfi.portfolio.adapter.kafka.PortfolioEventProcessor;
import dev.catfi.portfolio.adapter.persistence.PortfolioRepository;
import dev.catfi.portfolio.application.PortfolioService;
import dev.catfi.reporting.adapter.kafka.ReportingEventProcessor;
import dev.catfi.reporting.application.ReportingService;
import dev.catfi.sensor.adapter.kafka.SensorEventProcessor;
import dev.catfi.sensor.application.SensorSimulationService;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(
    classes = PipelineKafkaIT.TestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
    properties = {
      "spring.kafka.consumer.auto-offset-reset=earliest",
      "spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
      "spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer",
      "spring.kafka.consumer.properties.spring.json.trusted.packages=dev.catfi.common.domain",
      "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
      "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer",
      "logging.level.root=WARN"
    })
@EmbeddedKafka(
    partitions = 1,
    topics = {
      "catfi.disasters",
      "catfi.sensors",
      "catfi.damage.assessed",
      "catfi.compensation.calculated",
      "catfi.payment.completed",
      "catfi.investment.allocated"
    },
    bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@DirtiesContext
class PipelineKafkaIT {

  private static final UUID PROPERTY = UUID.fromString("10000000-0000-0000-0000-000000000001");
  private static final UUID CLIENT = UUID.fromString("20000000-0000-0000-0000-000000000001");
  private static final int PORT = freePort();

  private static int freePort() {
    try (var socket = new java.net.ServerSocket(0)) {
      return socket.getLocalPort();
    } catch (java.io.IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("server.port", () -> PORT);
    registry.add("services.disaster.url", () -> "http://127.0.0.1:" + PORT);
  }

  @SpringBootConfiguration
  @EnableAutoConfiguration(
      exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        JpaRepositoriesAutoConfiguration.class,
        LiquibaseAutoConfiguration.class,
        MongoAutoConfiguration.class,
        MongoDataAutoConfiguration.class
      })
  @EnableFeignClients(clients = DisasterClient.class)
  @Import({
    UiController.class,
    SimulationController.class,
    SensorEventProcessor.class,
    DamageEventProcessor.class,
    CompensationEventProcessor.class,
    PaymentEventProcessor.class,
    InvestmentEventProcessor.class,
    PortfolioEventProcessor.class,
    ReportingEventProcessor.class
  })
  static class TestApplication {
    @org.springframework.context.annotation.Bean
    ReportingService reportingService() {
      return new ReportingService();
    }

    @org.springframework.context.annotation.Bean
    DisasterSimulationService disasterSimulationService() {
      return new DisasterSimulationService();
    }

    @org.springframework.context.annotation.Bean
    SensorSimulationService sensorSimulationService() {
      return new SensorSimulationService();
    }

    @org.springframework.context.annotation.Bean
    DamageCalculationService damageCalculationService() {
      return new DamageCalculationService();
    }

    @org.springframework.context.annotation.Bean
    CompensationApplicationService compensationApplicationService() {
      return new CompensationApplicationService(new CompensationCalculator());
    }

    @org.springframework.context.annotation.Bean
    PaymentService paymentService() {
      return new PaymentService();
    }

    @org.springframework.context.annotation.Bean
    InvestmentService investmentService() {
      return new InvestmentService();
    }

    @org.springframework.context.annotation.Bean
    PortfolioService portfolioService() {
      return new PortfolioService();
    }

    @org.springframework.context.annotation.Bean
    DamageRepository damageRepository() {
      return mock(DamageRepository.class);
    }

    @org.springframework.context.annotation.Bean
    CompensationRepository compensationRepository() {
      return mock(CompensationRepository.class);
    }

    @org.springframework.context.annotation.Bean
    PaymentRepository paymentRepository() {
      return mock(PaymentRepository.class);
    }

    @org.springframework.context.annotation.Bean
    PortfolioRepository portfolioRepository() {
      return mock(PortfolioRepository.class);
    }


    @org.springframework.context.annotation.Bean
    org.springframework.jdbc.core.JdbcTemplate portfolioJdbcTemplate() {
      return mock(org.springframework.jdbc.core.JdbcTemplate.class);
    }

    @org.springframework.context.annotation.Bean
    PolicyClient policyClient() {
      var client = mock(PolicyClient.class);
      var property =
          new UiModels.PropertyView(
              PROPERTY,
              CLIENT,
              "Chiyoda, Tokyo",
              "RESIDENTIAL",
              35.67,
              139.7,
              new BigDecimal("250000"));
      when(client.getProperties()).thenReturn(List.of(property));
      when(client.getProperty(PROPERTY)).thenReturn(property);
      when(client.getPolicy(PROPERTY))
          .thenReturn(
              new UiModels.PolicyView(
                  UUID.randomUUID(),
                  PROPERTY,
                  new BigDecimal("0.9"),
                  new BigDecimal("5000"),
                  new BigDecimal("100000"),
                  BigDecimal.ZERO,
                  true));
      return client;
    }

    @org.springframework.context.annotation.Bean
    UserClient userClient() {
      var client = mock(UserClient.class);
      when(client.getClient(CLIENT))
          .thenReturn(
              ClientProfile.builder()
                  .clientId(CLIENT)
                  .fullName("Demo Client")
                  .riskProfile(RiskProfile.MODERATE)
                  .build());
      return client;
    }

    @org.springframework.context.annotation.Bean
    ReportingClient reportingClient(ReportingService service) {
      var client = mock(ReportingClient.class);
      when(client.getAllReports())
          .thenAnswer(call -> new ArrayList<>(service.getAllReports().values()));
      when(client.getReportByProperty(anyString()))
          .thenAnswer(call -> service.getReport(UUID.fromString(call.getArgument(0))));
      return client;
    }

    @org.springframework.context.annotation.Bean
    UiAggregationService uiAggregationService(
        ReportingClient report, DisasterClient disaster, PolicyClient policy, UserClient user) {
      return new UiAggregationService(report, disaster, policy, user);
    }

    @org.springframework.context.annotation.Bean
    RestTemplate restTemplate() {
      var rest = mock(RestTemplate.class);
      when(rest.getForObject(anyString(), eq(SensorEventProcessor.RegisteredProperty[].class)))
          .thenReturn(
              new SensorEventProcessor.RegisteredProperty[] {
                new SensorEventProcessor.RegisteredProperty(PROPERTY)
              });
      when(rest.getForObject(anyString(), eq(CompensationEventProcessor.TrustedPolicy.class)))
          .thenReturn(
              new CompensationEventProcessor.TrustedPolicy(
                  PROPERTY,
                  new BigDecimal("0.9"),
                  new BigDecimal("5000"),
                  new BigDecimal("100000"),
                  BigDecimal.ZERO,
                  true));
      when(rest.getForObject(anyString(), eq(InvestmentEventProcessor.PropertyOwner.class)))
          .thenReturn(new InvestmentEventProcessor.PropertyOwner(CLIENT));
      when(rest.getForObject(anyString(), eq(ClientProfile.class)))
          .thenReturn(
              ClientProfile.builder().clientId(CLIENT).riskProfile(RiskProfile.MODERATE).build());
      return rest;
    }

    @org.springframework.context.annotation.Bean
    org.springframework.security.web.SecurityFilterChain security(
        org.springframework.security.config.annotation.web.builders.HttpSecurity http)
        throws Exception {
      return http.csrf(c -> c.disable())
          .authorizeHttpRequests(a -> a.anyRequest().permitAll())
          .build();
    }
  }

  @org.springframework.beans.factory.annotation.Autowired
  private TestRestTemplate http;

  @org.springframework.beans.factory.annotation.Autowired private ReportingService reporting;

  @Test
  void shouldRunFromBffHttpTriggerThroughKafkaToDashboardAndDetails() throws Exception {
    var initial = http.getForEntity("/api/ui/dashboard", UiModels.DashboardRow[].class);
    assertThat(initial.getStatusCode().value()).isEqualTo(200);
    assertThat(initial.getBody()[0].status()).isEqualTo("SAFE");
    var request =
        Map.of("type", "EARTHQUAKE", "latitude", 35.67, "longitude", 139.7, "intensity", 7);
    var response = http.postForEntity("/api/ui/trigger-disaster", request, Map.class);
    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).containsKey("eventId");
    org.awaitility.Awaitility.await()
        .atMost(java.time.Duration.ofSeconds(25))
        .untilAsserted(
            () -> {
              var report = reporting.getReport(PROPERTY);
              assertThat(report).isNotNull();
              assertThat(report.getStatus()).isEqualTo("REPORTED");
              BigDecimal invested =
                  report.getCurrentPortfolio().values().stream()
                      .reduce(BigDecimal.ZERO, BigDecimal::add);
              assertThat(report.getActualPayout().add(invested))
                  .isEqualByComparingTo(report.getTotalCompensation());
            });
    var dashboard = http.getForEntity("/api/ui/dashboard", UiModels.DashboardRow[].class);
    assertThat(dashboard.getBody()[0].status()).isEqualTo("REPORTED");
    var details =
        http.getForEntity("/api/ui/properties/" + PROPERTY, UiModels.PropertyDetails.class);
    assertThat(details.getStatusCode().value()).isEqualTo(200);
    assertThat(details.getBody().report().getPropertyId()).isEqualTo(PROPERTY);
    if (Boolean.getBoolean("catfi.browser.e2e")) {
      var ui = java.nio.file.Path.of(System.getProperty("catfi.web-ui", "../../web-ui")).toAbsolutePath().normalize();
      java.util.List<String> command =
          System.getProperty("os.name").startsWith("Windows")
              ? java.util.List.of("cmd.exe", "/c", "npm", "run", "test:e2e")
              : java.util.List.of("npm", "run", "test:e2e");
      var builder =
          new ProcessBuilder(command)
              .directory(ui.toFile())
              .redirectErrorStream(true)
              .redirectOutput(ui.resolve("test-results-live.log").toFile());
      builder.environment().put("CATFI_E2E_LIVE", "1");
      builder.environment().put("CATFI_BFF_URL", "http://127.0.0.1:" + PORT);
      var process = builder.start();
      if (!process.waitFor(60, java.util.concurrent.TimeUnit.SECONDS)) {
        process.destroyForcibly();
        throw new AssertionError("Browser verification timed out");
      }
      assertThat(process.exitValue())
          .withFailMessage("Browser test failed; see web-ui/test-results-live.log")
          .isZero();
    }
  }
}

package dev.catfi.disaster.adapter.web;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.*;
import org.junit.jupiter.api.Test;

@org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest(SimulationController.class)
class SimulationControllerTest {

  @org.springframework.beans.factory.annotation.Autowired
  private org.springframework.test.web.servlet.MockMvc mvc;

  @org.springframework.test.context.bean.override.mockito.MockitoBean
  private dev.catfi.disaster.application.DisasterSimulationService service;

  @org.springframework.test.context.bean.override.mockito.MockitoBean
  private org.springframework.kafka.core.KafkaTemplate<String, Object> kafka;

  private static final String REQUEST =
      "{\"type\":\"EARTHQUAKE\",\"latitude\":35.67,\"longitude\":139.7,\"intensity\":7}";

  @Test
  void shouldPublishValidatedManualDisaster() throws Exception {
    when(kafka.send(anyString(), anyString(), any()))
        .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));
    mvc.perform(
            post("/api/v1/simulations/disaster").contentType("application/json").content(REQUEST))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.type").value("EARTHQUAKE"));
    verify(kafka)
        .send(eq("catfi.disasters"), anyString(), any(dev.catfi.common.domain.DisasterEvent.class));
  }

  @Test
  void shouldRejectInvalidCoordinatesBeforePublishing() throws Exception {
    mvc.perform(
            post("/api/v1/simulations/disaster")
                .contentType("application/json")
                .content(REQUEST.replace("35.67", "100")))
        .andExpect(status().isBadRequest());
    verify(kafka, never()).send(anyString(), anyString(), any());
  }

  @Test
  void shouldNotReportSuccessWhenKafkaFails() throws Exception {
    when(kafka.send(anyString(), anyString(), any()))
        .thenReturn(
            java.util.concurrent.CompletableFuture.failedFuture(
                new IllegalStateException("Kafka unavailable")));
    mvc.perform(
            post("/api/v1/simulations/disaster").contentType("application/json").content(REQUEST))
        .andExpect(status().isServiceUnavailable());
  }
}

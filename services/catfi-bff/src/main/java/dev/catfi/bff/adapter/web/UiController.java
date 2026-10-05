package dev.catfi.bff.adapter.web;

import dev.catfi.bff.application.*;
import dev.catfi.bff.application.UiModels.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ui")
@RequiredArgsConstructor
public class UiController {
  private final UiAggregationService service;

  @GetMapping("/dashboard")
  public List<DashboardRow> dashboard() {
    return service.getDashboardData();
  }

  @GetMapping("/properties/{id}")
  public PropertyDetails property(@PathVariable UUID id) {
    return service.getPropertyDetails(id);
  }

  @PostMapping("/trigger-disaster")
  public Map<String, Object> trigger(@Valid @RequestBody TriggerRequest request) {
    return service.triggerDisaster(
        Map.of(
            "type",
            request.type(),
            "latitude",
            request.latitude(),
            "longitude",
            request.longitude(),
            "intensity",
            request.intensity()));
  }

  public record TriggerRequest(
      @NotBlank @Pattern(regexp = "EARTHQUAKE|FLOOD|TYPHOON|TSUNAMI") String type,
      @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
      @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,
      @NotNull @DecimalMin(value = "0", inclusive = false) @DecimalMax("10") Double intensity) {}
}

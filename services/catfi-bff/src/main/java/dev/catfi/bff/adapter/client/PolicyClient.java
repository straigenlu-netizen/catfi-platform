package dev.catfi.bff.adapter.client;

import dev.catfi.bff.application.UiModels.*;
import java.util.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "property-policy-service", url = "${services.policy.url:http://localhost:8084}")
public interface PolicyClient {
  @GetMapping("/api/v1/policies/properties")
  List<PropertyView> getProperties();

  @GetMapping("/api/v1/policies/properties/{id}")
  PropertyView getProperty(@PathVariable("id") UUID id);

  @GetMapping("/api/v1/policies/property/{id}/active")
  PolicyView getPolicy(@PathVariable("id") UUID id);
}

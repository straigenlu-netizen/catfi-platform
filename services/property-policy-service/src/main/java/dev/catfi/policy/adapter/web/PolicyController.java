package dev.catfi.policy.adapter.web;

import dev.catfi.policy.application.PolicyService;
import dev.catfi.policy.domain.InsurancePolicy;
import dev.catfi.policy.domain.Property;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/policies")
@RequiredArgsConstructor
public class PolicyController {
  private final PolicyService policyService;

  @GetMapping("/properties")
  public java.util.List<Property> listProperties() {
    return policyService.getAllProperties();
  }

  @GetMapping("/properties/{propertyId}")
  public Property getProperty(@PathVariable UUID propertyId) {
    Property property = policyService.getProperty(propertyId);
    if (property == null)
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.NOT_FOUND, "Property not found");
    return property;
  }

  @PostMapping("/properties")
  public void createProperty(@RequestBody Property property) {
    policyService.saveProperty(property);
  }

  @PostMapping
  public void createPolicy(@RequestBody InsurancePolicy policy) {
    policyService.savePolicy(policy);
  }

  @GetMapping("/property/{propertyId}/active")
  public InsurancePolicy getActivePolicy(@PathVariable UUID propertyId) {
    return policyService.getActivePolicyForProperty(propertyId);
  }
}

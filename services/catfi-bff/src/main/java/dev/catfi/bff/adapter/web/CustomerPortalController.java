package dev.catfi.bff.adapter.web;

import dev.catfi.bff.adapter.client.PaymentClient;
import dev.catfi.bff.adapter.client.UserClient;
import dev.catfi.bff.application.PortalService;
import dev.catfi.bff.application.UiModels.PropertyDetails;
import dev.catfi.bff.security.AuditService;
import dev.catfi.common.domain.ClientProfile;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerPortalController {
  private final PortalService portal;
  private final AuditService audit;

  @GetMapping("/profile")
  public ClientProfile profile(Authentication authentication) { return portal.myProfile(authentication); }

  @PutMapping("/profile")
  public ClientProfile edit(Authentication authentication, @Valid @RequestBody ProfileChange request) {
    ClientProfile updated = portal.updateProfile(
        authentication, new UserClient.ProfileUpdate(request.fullName(), request.email()));
    audit.recordAction(authentication.getName(), "PROFILE_UPDATE", "/api/customer/profile");
    return updated;
  }

  @GetMapping("/policies")
  public List<PropertyDetails> policies(Authentication authentication) { return portal.myPolicies(authentication); }

  @GetMapping("/policies/{propertyId}")
  public PropertyDetails policy(Authentication authentication, @PathVariable UUID propertyId) {
    return portal.myPolicy(authentication, propertyId);
  }

  @GetMapping("/payments")
  public List<PaymentClient.PaymentView> payments(Authentication authentication) {
    return portal.myPayments(authentication);
  }

  public record ProfileChange(@NotBlank @Size(max = 120) String fullName,
                              @NotBlank @Email @Size(max = 254) String email) {}
}

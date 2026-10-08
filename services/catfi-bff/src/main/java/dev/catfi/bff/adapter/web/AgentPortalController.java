package dev.catfi.bff.adapter.web;

import dev.catfi.bff.adapter.client.PaymentClient;
import dev.catfi.bff.application.PortalService;
import dev.catfi.bff.application.UiAggregationService;
import dev.catfi.bff.application.UiModels.*;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentPortalController {
  private final PortalService portal;
  private final UiAggregationService aggregation;

  @GetMapping("/properties") public List<DashboardRow> properties() { return aggregation.getDashboardData(); }
  @GetMapping("/properties/{id}") public PropertyDetails property(@PathVariable UUID id) {
    return aggregation.getPropertyDetails(id);
  }
  @GetMapping("/clients") public List<PortalService.AgentClientView> clients() {
    return portal.agentClients();
  }
  @GetMapping("/clients/{id}") public PortalService.AgentClientDetails client(@PathVariable UUID id) {
    return portal.agentClient(id);
  }
  @GetMapping("/payments") public List<PaymentClient.PaymentView> payments() {
    return portal.agentPayments();
  }
}

package dev.catfi.bff.observability;

import static org.assertj.core.api.Assertions.assertThat;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class HttpRequestObservabilityTest {
  @Test void aggregatesIdsIntoOneFixedRouteTag() throws Exception {
    var registry = new SimpleMeterRegistry();
    var interceptor = new HttpRequestObservability(registry);
    for (int i = 0; i < 3; i++) {
      var request = new MockHttpServletRequest("GET", "/api/ui/properties/" + i);
      var response = new MockHttpServletResponse();
      interceptor.afterCompletion(request, response, new Object(), null);
    }
    assertThat(registry.get("catfi.bff.requests").tag("route", "ui")
        .tag("outcome", "success").counter().count()).isEqualTo(3);
    assertThat(registry.getMeters().stream().filter(m -> m.getId().getName().equals("catfi.bff.requests")))
        .hasSize(1);
  }

  @Test void separatesFailuresWithoutLeakingUserIdentifiers() throws Exception {
    var registry = new SimpleMeterRegistry();
    var interceptor = new HttpRequestObservability(registry);
    var response = new MockHttpServletResponse();
    response.setStatus(403);
    interceptor.afterCompletion(new MockHttpServletRequest("GET", "/api/customer/profile"), response,
        new Object(), null);
    assertThat(registry.get("catfi.bff.requests").tag("route", "customer")
        .tag("outcome", "4xx").counter().count()).isEqualTo(1);
  }
}

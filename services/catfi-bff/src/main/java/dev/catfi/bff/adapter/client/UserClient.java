package dev.catfi.bff.adapter.client;

import dev.catfi.common.domain.ClientProfile;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "user-service", url = "${services.user.url:http://localhost:8080}")
public interface UserClient {
  @GetMapping("/api/v1/clients/{id}")
  ClientProfile getClient(@PathVariable("id") UUID id);
}

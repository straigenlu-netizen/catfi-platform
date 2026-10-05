package dev.catfi.bff.adapter.client;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "disaster-simulator", url = "${services.disaster.url:http://localhost:8086}")
public interface DisasterClient {
  @PostMapping("/api/v1/simulations/disaster")
  Map<String, Object> triggerDisaster(@RequestBody Map<String, Object> request);
}

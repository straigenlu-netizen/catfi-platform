package dev.catfi.bff.adapter.client;

import dev.catfi.common.domain.PropertyFullReport;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "reporting-service", url = "${services.reporting.url:http://localhost:8085}")
public interface ReportingClient {
  @GetMapping("/api/v1/reports/all")
  List<PropertyFullReport> getAllReports();

  @GetMapping("/api/v1/reports/property/{propertyId}")
  PropertyFullReport getReportByProperty(@PathVariable("propertyId") String propertyId);
}

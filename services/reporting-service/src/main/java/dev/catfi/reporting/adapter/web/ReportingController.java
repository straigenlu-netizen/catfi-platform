package dev.catfi.reporting.adapter.web;

import dev.catfi.common.domain.PropertyFullReport;
import dev.catfi.reporting.application.ReportingService;
import java.util.Collection;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportingController {
  private final ReportingService reportingService;

  @GetMapping("/property/{propertyId}")
  public PropertyFullReport getPropertyReport(@PathVariable UUID propertyId) {
    var report = reportingService.getReport(propertyId);
    if (report == null)
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.NOT_FOUND, "Report not found");
    return report;
  }

  @GetMapping("/all")
  public Collection<PropertyFullReport> getAllReports() {
    return reportingService.getAllReports().values();
  }
}

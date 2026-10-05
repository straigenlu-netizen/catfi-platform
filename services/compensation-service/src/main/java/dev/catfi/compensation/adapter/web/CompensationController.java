package dev.catfi.compensation.adapter.web;

import dev.catfi.compensation.application.PreviewCompensation;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/compensations")
public class CompensationController {
  private final PreviewCompensation preview;

  @PostMapping("/preview")
  public PreviewResponse preview(@Valid @RequestBody PreviewRequest request) {
    return new PreviewResponse(
        preview.execute(request.toTerms()), "EUR", "coverage-before-deductible-v1");
  }

  public record PreviewResponse(BigDecimal amount, String currency, String ruleVersion) {}
}

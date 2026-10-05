package dev.catfi.compensation.configuration;

import dev.catfi.compensation.application.PreviewCompensation;
import dev.catfi.compensation.domain.CompensationCalculator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfiguration {
  @Bean
  CompensationCalculator compensationCalculator() {
    return new CompensationCalculator();
  }

  @Bean
  PreviewCompensation previewCompensation() {
    return new PreviewCompensation(compensationCalculator());
  }
}

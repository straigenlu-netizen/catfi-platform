package dev.catfi.disaster;

import dev.catfi.disaster.configuration.SimulationConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(SimulationConfiguration.class)
public class DisasterSimulatorApplication {
  public static void main(String[] args) {
    SpringApplication.run(DisasterSimulatorApplication.class, args);
  }
}

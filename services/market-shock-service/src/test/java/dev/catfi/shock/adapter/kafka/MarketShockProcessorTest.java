package dev.catfi.shock.adapter.kafka;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import dev.catfi.common.domain.DisasterEvent;
import dev.catfi.shock.application.MarketShockService;
import dev.catfi.shock.domain.MarketShockSnapshot;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

class MarketShockProcessorTest {

  @Test
  void shouldPublishOneShockForEachConfiguredAsset() {
    KafkaTemplate<String, MarketShockSnapshot> kafka = mock(KafkaTemplate.class);
    var id = UUID.randomUUID();
    new MarketShockProcessor(new MarketShockService(), kafka)
        .handleDisasterEvent(DisasterEvent.builder().eventId(id).type("EARTHQUAKE").build());
    var events = ArgumentCaptor.forClass(MarketShockSnapshot.class);
    verify(kafka, times(4)).send(eq("catfi.market.shocked"), anyString(), events.capture());
    assertThat(events.getAllValues())
        .extracting(MarketShockSnapshot::getAssetSymbol)
        .containsExactlyInAnyOrder("BTC", "ETH", "GOLD", "BOND_US10Y");
    assertThat(events.getAllValues())
        .allSatisfy(e -> assertThat(e.getDisasterEventId()).isEqualTo(id));
  }
}

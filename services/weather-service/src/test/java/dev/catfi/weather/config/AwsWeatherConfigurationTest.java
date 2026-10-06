package dev.catfi.weather.config;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.*;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import static dev.catfi.aws.DynamoReportStore.s;

class AwsWeatherConfigurationTest {
  @Test void ignoresExpiredItemEvenBeforeDynamoTtlDeletesIt() {
    var db=mock(DynamoDbClient.class);Instant now=Instant.parse("2026-01-01T00:00:00Z");
    when(db.getItem(any(Consumer.class))).thenReturn(GetItemResponse.builder().item(Map.of("expiresAt",AttributeValue.builder().n(""+now.minusSeconds(1).getEpochSecond()).build(),"snapshot",s("not read"))).build());
    assertThat(AwsWeatherConfiguration.read(db,"table","Tokyo",Clock.fixed(now,ZoneOffset.UTC))).isEmpty();
  }
}

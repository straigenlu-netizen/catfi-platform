package dev.catfi.weather.config;

import dev.catfi.aws.*;
import dev.catfi.weather.application.WeatherCache;
import dev.catfi.weather.domain.WeatherSnapshot;
import java.time.Clock;
import java.util.Optional;
import org.springframework.context.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import static dev.catfi.aws.DynamoReportStore.s;

@Configuration
@ConditionalOnProperty(name="catfi.weather.storage",havingValue="dynamodb")
public class AwsWeatherConfiguration {
  @Bean(destroyMethod="close") DynamoDbClient weatherDb() { return AwsClients.dynamo(); }
  @Bean WeatherCache weatherCache(DynamoDbClient db,@Value("${catfi.aws.weather-table}")String table) {
    return region->read(db,table,region,Clock.systemUTC());
  }
  public static Optional<WeatherSnapshot> read(DynamoDbClient db,String table,String region,Clock clock) {
    var item=db.getItem(b->b.tableName(table).key(java.util.Map.of("region",s(region))).consistentRead(true)).item();
    if(item.isEmpty()||Long.parseLong(item.get("expiresAt").n())<=clock.instant().getEpochSecond())return Optional.empty();
    return Optional.of(Json.read(item.get("snapshot").s(),WeatherSnapshot.class));
  }
}

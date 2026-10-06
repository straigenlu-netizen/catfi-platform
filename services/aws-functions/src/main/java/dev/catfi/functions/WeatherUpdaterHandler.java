package dev.catfi.functions;

import com.amazonaws.services.lambda.runtime.*;
import dev.catfi.aws.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import static dev.catfi.aws.DynamoReportStore.s;

public class WeatherUpdaterHandler implements RequestHandler<Map<String,Object>,Map<String,Object>> {
  public interface WeatherSource { String fetch(String region) throws Exception; }
  private final DynamoDbClient db;private final String table;private final WeatherSource source;private final Clock clock;
  public WeatherUpdaterHandler() {
    this.db=AwsClients.dynamo();this.table=AwsClients.required("WEATHER_TABLE");this.clock=Clock.systemUTC();
    SecretsManagerClient secrets=AwsClients.secrets();HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    source=region->{
      String secret=secrets.getSecretValue(b->b.secretId(AwsClients.required("WEATHER_SECRET_ARN"))).secretString();
      String apiKey=Json.MAPPER.readTree(secret).path("apiKey").asText();if(apiKey.isBlank())throw new IllegalStateException("Weather secret is not configured");
      String url="https://api.openweathermap.org/data/2.5/weather?q="+URLEncoder.encode(region,StandardCharsets.UTF_8)+"&units=metric&appid="+URLEncoder.encode(apiKey,StandardCharsets.UTF_8);
      var res=http.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(8)).GET().build(),HttpResponse.BodyHandlers.ofString());
      if(res.statusCode()!=200)throw new IllegalStateException("Weather provider unavailable");
      var json=Json.MAPPER.readTree(res.body());
      var view=new LinkedHashMap<String,Object>();view.put("region",region);view.put("temperature",json.path("main").path("temp"));view.put("windSpeed",json.path("wind").path("speed"));
      view.put("precipitation",json.path("rain").get("1h"));view.put("timestamp",clock.instant().toString());view.put("provider","OpenWeatherMap");return Json.write(view);
    };
  }
  public WeatherUpdaterHandler(DynamoDbClient db,String table,WeatherSource source,Clock clock) { this.db=db;this.table=table;this.source=source;this.clock=clock; }
  public Map<String,Object> handleRequest(Map<String,Object> event,Context context) {
    String region=String.valueOf(event.getOrDefault("region","Tokyo"));
    if(!Set.of("Tokyo","Osaka","Kyoto").contains(region))throw new IllegalArgumentException("Unsupported region");
    try {
      String json=source.fetch(region);long expiry=clock.instant().plusSeconds(900).getEpochSecond();
      db.putItem(b->b.tableName(table).item(Map.of("region",s(region),"snapshot",s(json),"expiresAt",AttributeValue.builder().n(""+expiry).build())));
      return Map.of("region",region,"expiresAt",expiry);
    } catch(Exception e) {
      // Never attach provider exception/URL: it can contain API key.
      if(context!=null)context.getLogger().log("{\"event\":\"weather_update_failed\"}");
      throw new IllegalStateException("Weather update failed");
    }
  }
}

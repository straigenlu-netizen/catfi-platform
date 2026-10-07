package dev.catfi.notification;
import dev.catfi.common.domain.ClientProfile;
import java.time.Duration;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
@Component
public class RecipientResolver {
  private final RestClient http; private final String policyUrl; private final String userUrl;
  public RecipientResolver(@Value("${services.policy.url:http://localhost:8084}")String policyUrl,
      @Value("${services.user.url:http://localhost:8080}")String userUrl){
    var rf=new SimpleClientHttpRequestFactory(); rf.setConnectTimeout(Duration.ofSeconds(3)); rf.setReadTimeout(Duration.ofSeconds(5));
    this.http=RestClient.builder().requestFactory(rf).build(); this.policyUrl=policyUrl; this.userUrl=userUrl;
  }
  public ClientProfile resolve(UUID propertyId){
    PropertyOwner owner=http.get().uri(policyUrl+"/api/v1/policies/properties/"+propertyId).retrieve().body(PropertyOwner.class);
    if(owner==null||owner.clientId()==null)throw new IllegalStateException("Property owner unavailable");
    ClientProfile client=http.get().uri(userUrl+"/api/v1/clients/"+owner.clientId()).retrieve().body(ClientProfile.class);
    if(client==null||client.getEmail()==null||client.getEmail().isBlank())throw new IllegalStateException("Client email unavailable");
    return client;
  }
  @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown=true)
  public record PropertyOwner(UUID clientId){}
}

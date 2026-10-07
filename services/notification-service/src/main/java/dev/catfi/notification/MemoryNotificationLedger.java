package dev.catfi.notification;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
@Component
@ConditionalOnProperty(name="catfi.notification.mail-mode",havingValue="log",matchIfMissing=true)
public class MemoryNotificationLedger implements NotificationLedger {
  private final Set<String> ids=ConcurrentHashMap.newKeySet();
  public boolean reserve(String id,NotificationStage stage,Instant at){return ids.add(id);}
  public void delivered(String id,String providerMessageId,Instant at){}
  public void failed(String id,Instant at){ids.remove(id);}
}

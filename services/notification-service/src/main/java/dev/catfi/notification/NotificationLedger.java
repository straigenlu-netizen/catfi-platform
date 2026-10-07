package dev.catfi.notification;
import java.time.Instant;
public interface NotificationLedger {
  boolean reserve(String id, NotificationStage stage, Instant createdAt);
  void delivered(String id, String providerMessageId, Instant deliveredAt);
  void failed(String id, Instant failedAt);
}

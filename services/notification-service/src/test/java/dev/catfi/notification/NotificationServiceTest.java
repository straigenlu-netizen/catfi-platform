package dev.catfi.notification;
import static org.mockito.Mockito.*;
import dev.catfi.common.domain.*;
import java.time.Instant; import java.util.UUID;
import org.junit.jupiter.api.Test;
class NotificationServiceTest {
  @Test void sendsOnlyOnceWhenLedgerRejectsDuplicate(){
    var resolver=mock(RecipientResolver.class);var sender=mock(MailSender.class);var ledger=mock(NotificationLedger.class);
    var property=UUID.randomUUID();var client=ClientProfile.builder().clientId(UUID.randomUUID()).email("client@example.com").fullName("Client").riskProfile(RiskProfile.MODERATE).build();
    when(ledger.reserve(anyString(),any(),any())).thenReturn(true,false);
    when(resolver.resolve(property)).thenReturn(client);when(sender.send(anyString(),anyString(),anyString())).thenReturn("m-1");
    var service=new NotificationService(resolver,sender,ledger);
    service.notify(property,NotificationStage.CLAIM_IN_PROGRESS,"a-1");
    service.notify(property,NotificationStage.CLAIM_IN_PROGRESS,"a-1");
    verify(sender,times(1)).send(eq("client@example.com"),anyString(),anyString());
    verify(ledger).delivered(eq("CLAIM_IN_PROGRESS:a-1"),eq("m-1"),any(Instant.class));
  }
  @Test void releasesReservationAfterDeliveryFailure(){
    var resolver=mock(RecipientResolver.class);var sender=mock(MailSender.class);var ledger=mock(NotificationLedger.class);
    var property=UUID.randomUUID();
    when(ledger.reserve(anyString(),any(),any())).thenReturn(true);
    when(resolver.resolve(property)).thenThrow(new IllegalStateException("Client unavailable"));
    try{new NotificationService(resolver,sender,ledger).notify(property,NotificationStage.PAYMENT_COMPLETED,"p-1");}catch(IllegalStateException ignored){}
    verify(ledger).failed(eq("PAYMENT_COMPLETED:p-1"),any(Instant.class));
  }
}

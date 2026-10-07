package dev.catfi.notification;
import java.time.Instant; import java.util.UUID;
import org.springframework.stereotype.Service;
@Service
public class NotificationService {
  private final RecipientResolver recipients; private final MailSender mail; private final NotificationLedger ledger;
  public NotificationService(RecipientResolver recipients,MailSender mail,NotificationLedger ledger){this.recipients=recipients;this.mail=mail;this.ledger=ledger;}
  public void notify(UUID propertyId,NotificationStage stage,String eventId){
    String id=stage+":"+eventId; if(!ledger.reserve(id,stage,Instant.now()))return;
    try{var client=recipients.resolve(propertyId);String provider=mail.send(client.getEmail(),subject(stage),body(client.getFullName(),stage));ledger.delivered(id,provider,Instant.now());}
    catch(RuntimeException e){ledger.failed(id,Instant.now());throw e;}
  }
  static String subject(NotificationStage s){return switch(s){
    case CLAIM_SUBMITTED->"CatFi claim received"; case CLAIM_IN_PROGRESS->"CatFi claim is now in progress";
    case COMPENSATION_CALCULATED->"CatFi compensation has been calculated"; case PAYMENT_COMPLETED->"CatFi payment has been completed";
    case INVESTMENT_ALLOCATED->"CatFi investment allocation completed";};}
  static String body(String name,NotificationStage s){String hello=(name==null||name.isBlank())?"Hello":"Hello "+name; String m=switch(s){
    case CLAIM_SUBMITTED->"We have received your insurance claim."; case CLAIM_IN_PROGRESS->"Your insurance claim has entered processing.";
    case COMPENSATION_CALCULATED->"The compensation for your claim has been calculated."; case PAYMENT_COMPLETED->"The payment stage of your claim has completed.";
    case INVESTMENT_ALLOCATED->"The investment allocation stage of your claim has completed.";}; return hello+",\n\n"+m+"\n\nCatFi";}
}

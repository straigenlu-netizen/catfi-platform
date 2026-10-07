package dev.catfi.notification;
import dev.catfi.aws.AwsClients;
import java.time.Instant; import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.ses.model.*;
@Configuration
@ConditionalOnProperty(name="catfi.notification.mail-mode",havingValue="ses")
public class AwsNotificationConfiguration {
  @Bean MailSender sesMailSender(@Value("${catfi.notification.from}")String from){
    if(from==null||from.isBlank())throw new IllegalStateException("catfi.notification.from is required in SES mode");
    var ses=AwsClients.ses(); return (to,subject,body)->ses.sendEmail(b->b.source(from).destination(Destination.builder().toAddresses(to).build())
      .message(Message.builder().subject(Content.builder().data(subject).build()).body(Body.builder().text(Content.builder().data(body).build()).build()).build())).messageId();
  }
  @Bean NotificationLedger notificationLedger(@Value("${catfi.notification.table}")String table,@Value("${catfi.notification.retention-days:90}")long retentionDays){
    var db=AwsClients.dynamo(); return new NotificationLedger(){
      public boolean reserve(String id,NotificationStage stage,Instant at){try{db.putItem(b->b.tableName(table).item(Map.of(
        "notificationId",s(id),"stage",s(stage.name()),"status",s("PENDING"),"createdAt",s(at.toString()),"expiresAt",n(at.plusSeconds(retentionDays*86400).getEpochSecond())))
        .conditionExpression("attribute_not_exists(notificationId)"));return true;}catch(ConditionalCheckFailedException e){return false;}}
      public void delivered(String id,String provider,Instant at){db.updateItem(b->b.tableName(table).key(Map.of("notificationId",s(id)))
        .updateExpression("SET #s=:s, deliveredAt=:t, providerMessageId=:p").expressionAttributeNames(Map.of("#s","status"))
        .expressionAttributeValues(Map.of(":s",s("DELIVERED"),":t",s(at.toString()),":p",s(provider))));}
      public void failed(String id,Instant at){db.deleteItem(b->b.tableName(table).key(Map.of("notificationId",s(id))).conditionExpression("#s = :pending")
        .expressionAttributeNames(Map.of("#s","status")).expressionAttributeValues(Map.of(":pending",s("PENDING"))));}
    };}
  private static AttributeValue s(String v){return AttributeValue.builder().s(v).build();}
  private static AttributeValue n(long v){return AttributeValue.builder().n(Long.toString(v)).build();}
}

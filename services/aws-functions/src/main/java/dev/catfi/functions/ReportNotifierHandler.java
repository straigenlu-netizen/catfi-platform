package dev.catfi.functions;

import com.amazonaws.services.lambda.runtime.*;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import dev.catfi.aws.*;
import java.util.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;
import static dev.catfi.aws.DynamoReportStore.s;

/** One configured dev recipient. Delivery is at least once; SES has no transaction with DynamoDB. */
public class ReportNotifierHandler implements RequestHandler<DynamodbEvent,Map<String,Object>> {
  private final DynamoDbClient db;private final SesClient ses;private final String table,from,to;
  public ReportNotifierHandler() { this(AwsClients.dynamo(),AwsClients.ses(),AwsClients.required("JOBS_TABLE"),AwsClients.required("EMAIL_FROM"),AwsClients.required("EMAIL_TO")); }
  public ReportNotifierHandler(DynamoDbClient db,SesClient ses,String table,String from,String to) { this.db=db;this.ses=ses;this.table=table;this.from=from;this.to=to; }
  public Map<String,Object> handleRequest(DynamodbEvent event,Context context) {
    int sent=0;
    for(var record:event.getRecords()) {
      if("REMOVE".equals(record.getEventName()))continue;
      var next=record.getDynamodb().getNewImage();var old=record.getDynamodb().getOldImage();
      if(next==null||!next.get("pk").getS().startsWith("PROPERTY#")||!"REPORTED".equals(next.get("status").getS()))continue;
      String assessment=next.get("assessmentId").getS();
      if(old!=null&&old.containsKey("status")&&"REPORTED".equals(old.get("status").getS())&&assessment.equals(old.get("assessmentId").getS()))continue;
      String receipt="NOTICE#"+next.get("propertyId").getS()+"#"+assessment;
      var key=Map.of("pk",s(receipt),"sk",s("EMAIL"));
      if(!db.getItem(b->b.tableName(table).key(key).consistentRead(true)).item().isEmpty())continue;
      ses.sendEmail(b->b.source(from).destination(Destination.builder().toAddresses(to).build())
          .message(Message.builder().subject(Content.builder().data("CatFi report ready").build())
              .body(Body.builder().text(Content.builder().data("Report completed for property "+next.get("propertyId").getS()+"; assessment "+assessment).build()).build()).build()));
      // Only mark after successful send. An ambiguous SES response/crash can cause duplicate mail.
      try { db.putItem(b->b.tableName(table).item(key).conditionExpression("attribute_not_exists(pk)")); }
      catch(ConditionalCheckFailedException duplicate) { }
      sent++;
    }
    return Map.of("sent",sent);
  }
}

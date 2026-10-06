package dev.catfi.aws;

import java.util.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import static dev.catfi.aws.DynamoReportStore.s;

public final class DynamoJobs implements JobRepository {
  private final DynamoDbClient db;private final String table;
  public DynamoJobs(DynamoDbClient db,String table) { this.db=db;this.table=table; }
  public static Map<String,AttributeValue> key(String id) { return Map.of("pk",s("JOB#"+id),"sk",s("META")); }
  public Optional<ExportJob> find(String id) {
    var i=db.getItem(b->b.tableName(table).key(key(id)).consistentRead(true)).item();
    if(i.isEmpty())return Optional.empty();
    return Optional.of(new ExportJob(id,i.get("propertyId").s(),i.get("ownerSub").s(),i.get("reportJson").s(),
        i.get("status").s(),i.getOrDefault("objectKey",s("")).s(),i.get("createdAt").s(),i.getOrDefault("errorCode",s("")).s()));
  }
  public ExportJob create(ExportJob j) {
    var item=new HashMap<>(key(j.jobId()));
    item.put("jobId",s(j.jobId()));item.put("propertyId",s(j.propertyId()));item.put("ownerSub",s(j.ownerSub()));
    item.put("reportJson",s(j.reportJson()));item.put("status",s("PENDING"));item.put("createdAt",s(j.createdAt()));
    var outbox=Map.of("pk",s("OUTBOX#"+j.jobId()),"sk",s("EXPORT"),"jobId",s(j.jobId()),"status",s("PENDING"));
    try {
      db.transactWriteItems(b->b.transactItems(
          TransactWriteItem.builder().put(Put.builder().tableName(table).item(item).conditionExpression("attribute_not_exists(pk)").build()).build(),
          TransactWriteItem.builder().put(Put.builder().tableName(table).item(outbox).conditionExpression("attribute_not_exists(pk)").build()).build()));
      return j;
    } catch(TransactionCanceledException e) {
      // Only a competing successful create counts as idempotent. Other failures must retry.
      var existing=find(j.jobId());if(existing.isPresent())return existing.get();throw e;
    }
  }
  public void status(String id,String status,String objectKey,String errorCode) {
    String condition="attribute_exists(pk) AND #s <> :done";
    if(status.equals("SUCCEEDED"))condition="attribute_exists(pk) AND (#s = :running OR #s = :done)";
    var vals=new HashMap<String,AttributeValue>();vals.put(":s",s(status));vals.put(":key",s(objectKey));vals.put(":error",s(errorCode));vals.put(":done",s("SUCCEEDED"));
    if(status.equals("SUCCEEDED"))vals.put(":running",s("RUNNING"));
    String cond=condition;
    try { db.updateItem(b->b.tableName(table).key(key(id)).conditionExpression(cond)
        .updateExpression("SET #s = :s, objectKey = :key, errorCode = :error")
        .expressionAttributeNames(Map.of("#s","status")).expressionAttributeValues(vals)); }
    catch(ConditionalCheckFailedException e) {
      if(!find(id).map(j->j.status().equals("SUCCEEDED")).orElse(false))throw e;
    }
  }
  public void dispatched(String id) {
    db.updateItem(b->b.tableName(table).key(Map.of("pk",s("OUTBOX#"+id),"sk",s("EXPORT")))
        .updateExpression("SET #s = :s").expressionAttributeNames(Map.of("#s","status"))
        .expressionAttributeValues(Map.of(":s",s("SENT"))).conditionExpression("attribute_exists(pk)"));
  }
}

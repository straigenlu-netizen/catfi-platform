package dev.catfi.aws;

import java.nio.charset.StandardCharsets;
import java.util.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

/** A property aggregate is one atomic item. GSI contains only report items, never access grants. */
public final class DynamoReportStore implements ReportStore {
  private final DynamoDbClient db;
  private final String table;
  public DynamoReportStore(DynamoDbClient db, String table) { this.db=db; this.table=table; }
  public static AttributeValue s(String v) { return AttributeValue.builder().s(v).build(); }
  public static Map<String, AttributeValue> key(String id) { return Map.of("pk",s("PROPERTY#"+id),"sk",s("STATE")); }
  public Optional<ReportDocument> find(String id) {
    var item=db.getItem(b->b.tableName(table).key(key(id)).consistentRead(true)).item();
    return item.isEmpty() ? Optional.empty() : Optional.of(decode(item));
  }
  public boolean compareAndSet(ReportDocument next,long expected) {
    // Keep headroom below DynamoDB's 400 KB item limit; never silently drop correlation/receipts.
    if ((next.stateJson()+next.reportJson()).getBytes(StandardCharsets.UTF_8).length > 350_000)
      throw new IllegalStateException("Property aggregate retention limit reached; archive/migrate before replay");
    var item=new HashMap<>(key(next.propertyId()));
    item.put("propertyId",s(next.propertyId())); item.put("version",AttributeValue.builder().n(""+next.version()).build());
    item.put("stateJson",s(next.stateJson())); item.put("reportJson",s(next.reportJson()));
    item.put("status",s(next.status())); item.put("assessmentId",s(next.assessmentId())); item.put("updatedAt",s(next.updatedAt()));
    item.put("gsiPk",s("REPORTS")); item.put("gsiSk",s(next.propertyId()));
    try {
      db.putItem(b->b.tableName(table).item(item).conditionExpression(expected==0 ? "attribute_not_exists(pk)" : "#v = :v")
          .expressionAttributeNames(expected==0 ? null : Map.of("#v","version"))
          .expressionAttributeValues(expected==0 ? null : Map.of(":v",AttributeValue.builder().n(""+expected).build())));
      return true;
    } catch (ConditionalCheckFailedException e) { return false; }
  }
  public Page list(String cursor,int limit) {
    if(limit<1||limit>100)throw new IllegalArgumentException("limit must be 1..100");
    Map<String,AttributeValue> start=null;
    if(cursor!=null&&!cursor.isBlank()) {
      String id=new String(Base64.getUrlDecoder().decode(cursor),StandardCharsets.UTF_8);
      UUID.fromString(id); start=new HashMap<>(key(id));start.put("gsiPk",s("REPORTS"));start.put("gsiSk",s(id));
    }
    var b=QueryRequest.builder().tableName(table).indexName("reports-index").keyConditionExpression("gsiPk = :pk")
        .expressionAttributeValues(Map.of(":pk",s("REPORTS"))).limit(limit);
    if(start!=null)b.exclusiveStartKey(start);
    var response=db.query(b.build());
    String next=response.hasLastEvaluatedKey()&&!response.lastEvaluatedKey().isEmpty()
        ? Base64.getUrlEncoder().withoutPadding().encodeToString(response.lastEvaluatedKey().get("gsiSk").s().getBytes(StandardCharsets.UTF_8)):null;
    return new Page(response.items().stream().map(DynamoReportStore::decode).toList(),next);
  }
  public static ReportDocument decode(Map<String,AttributeValue> i) {
    return new ReportDocument(i.get("propertyId").s(),Long.parseLong(i.get("version").n()),i.get("stateJson").s(),
        i.get("reportJson").s(),i.get("status").s(),i.get("assessmentId").s(),i.get("updatedAt").s());
  }
}

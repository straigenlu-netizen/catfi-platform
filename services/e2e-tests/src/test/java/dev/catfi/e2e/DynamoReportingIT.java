package dev.catfi.e2e;

import static org.assertj.core.api.Assertions.*;
import dev.catfi.aws.*;
import dev.catfi.common.domain.*;
import dev.catfi.reporting.application.ReportingService;
import dev.catfi.reporting.adapter.kafka.ReportingEventProcessor;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

/** Real DynamoDB API boundary; CI provisions DynamoDB Local. No mocked repository. */
@EnabledIfEnvironmentVariable(named="CATFI_DYNAMODB_ENDPOINT",matches=".+")
class DynamoReportingIT {
  @Test void survivesRestartAndConcurrentWritersAndRetainsExactCents() throws Exception {
    try(var db=DynamoDbClient.builder().endpointOverride(URI.create(System.getenv("CATFI_DYNAMODB_ENDPOINT")))
        .region(Region.EU_CENTRAL_1).credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test","test"))).build()) {
      String table="reporting-"+UUID.randomUUID();
      db.createTable(b->b.tableName(table).billingMode(BillingMode.PAY_PER_REQUEST)
          .attributeDefinitions(AttributeDefinition.builder().attributeName("pk").attributeType(ScalarAttributeType.S).build(),
              AttributeDefinition.builder().attributeName("sk").attributeType(ScalarAttributeType.S).build(),
              AttributeDefinition.builder().attributeName("gsiPk").attributeType(ScalarAttributeType.S).build(),
              AttributeDefinition.builder().attributeName("gsiSk").attributeType(ScalarAttributeType.S).build())
          .keySchema(KeySchemaElement.builder().attributeName("pk").keyType(KeyType.HASH).build(),KeySchemaElement.builder().attributeName("sk").keyType(KeyType.RANGE).build())
          .globalSecondaryIndexes(GlobalSecondaryIndex.builder().indexName("reports-index").keySchema(KeySchemaElement.builder().attributeName("gsiPk").keyType(KeyType.HASH).build(),KeySchemaElement.builder().attributeName("gsiSk").keyType(KeyType.RANGE).build()).projection(Projection.builder().projectionType(ProjectionType.ALL).build()).build()));
      db.waiter().waitUntilTableExists(b->b.tableName(table));
      try {
        var store=new DynamoReportStore(db,table);UUID property=UUID.randomUUID(),assessment=UUID.randomUUID(),comp=UUID.randomUUID();
        var one=new ReportingEventProcessor(new ReportingService(store));
        one.handlePayment(Payment.builder().propertyId(property).compensationResultId(comp).amount(new BigDecimal("5.01")).build());
        var restarted=new ReportingEventProcessor(new ReportingService(new DynamoReportStore(db,table)));
        restarted.handleDamage(DamageAssessment.builder().propertyId(property).assessmentId(assessment).assessedAt(Instant.now()).damageAmount(new BigDecimal("20.01")).build());
        try(var pool=Executors.newFixedThreadPool(2)) {
          var a=pool.submit(()->one.handleCompensation(CompensationResult.builder().propertyId(property).resultId(comp).damageAssessmentId(assessment).totalCompensation(new BigDecimal("10.01")).amountToPay(new BigDecimal("5.01")).amountToInvest(new BigDecimal("5")).build()));
          var b=pool.submit(()->restarted.handleInvestment(InvestmentDecision.builder().propertyId(property).compensationResultId(comp).allocation(Map.of("GOLD",new BigDecimal("5"))).build()));a.get();b.get();
        }
        var report=new ReportingService(new DynamoReportStore(db,table)).getReport(property);
        assertThat(report.getStatus()).isEqualTo("REPORTED");assertThat(report.getActualPayout()).isEqualByComparingTo("5.01");
        var document=store.find(property.toString()).orElseThrow();
        assertThat(store.compareAndSet(document,document.version()-1)).isFalse();
        assertThat(store.list(null,1).items()).hasSize(1);
        var jobs=new DynamoJobs(db,table);var job=new ExportJob(UUID.randomUUID().toString(),property.toString(),"owner",document.reportJson(),"PENDING","",Instant.now().toString(),"");
        assertThat(jobs.create(job).jobId()).isEqualTo(job.jobId());assertThat(jobs.create(job)).isEqualTo(job);
        var outbox=db.getItem(b->b.tableName(table).key(Map.of("pk",DynamoReportStore.s("OUTBOX#"+job.jobId()),"sk",DynamoReportStore.s("EXPORT")))).item();
        assertThat(outbox.get("status").s()).isEqualTo("PENDING");
        var files=new HashMap<String,String>();var exports=new ExportService(jobs,(key,contents,type)->files.put(key,contents));
        String key=exports.generate(job.jobId());exports.complete(job.jobId(),key);
        assertThat(jobs.find(job.jobId()).orElseThrow().status()).isEqualTo("SUCCEEDED");
        assertThat(files.get(key)).isEqualTo(document.reportJson());
        exports.fail(job.jobId());assertThat(jobs.find(job.jobId()).orElseThrow().status()).isEqualTo("SUCCEEDED");
      } finally { db.deleteTable(b->b.tableName(table)); }
    }
  }
}

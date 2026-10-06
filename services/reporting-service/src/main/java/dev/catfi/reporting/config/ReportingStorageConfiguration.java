package dev.catfi.reporting.config;

import dev.catfi.aws.*;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Configuration
public class ReportingStorageConfiguration {
  @Bean @ConditionalOnProperty(name="catfi.reporting.storage",havingValue="memory",matchIfMissing=true)
  ReportStore memoryReportStore() { return new MemoryReportStore(); }
  @Bean(destroyMethod="close") @ConditionalOnProperty(name="catfi.reporting.storage",havingValue="dynamodb")
  DynamoDbClient dynamoClient() { return AwsClients.dynamo(); }
  @Bean @ConditionalOnProperty(name="catfi.reporting.storage",havingValue="dynamodb")
  ReportStore dynamoReportStore(DynamoDbClient db,@Value("${catfi.aws.reporting-table}")String table) {
    return new DynamoReportStore(db,table);
  }
  @Bean org.springframework.kafka.listener.DefaultErrorHandler reportingErrorHandler() {
    // Fail closed: never acknowledge a financial projection that failed to persist.
    var handler=new org.springframework.kafka.listener.DefaultErrorHandler(
        (record,error)->{throw new IllegalStateException("Reporting event requires operator recovery",error);},
        new org.springframework.util.backoff.FixedBackOff(1000L,org.springframework.util.backoff.FixedBackOff.UNLIMITED_ATTEMPTS));
    handler.setAckAfterHandle(false);return handler;
  }
}

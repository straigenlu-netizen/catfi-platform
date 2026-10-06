package dev.catfi.functions;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import dev.catfi.aws.*;
import com.amazonaws.services.lambda.runtime.events.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import java.util.function.Consumer;

class LambdaHandlersTest {
  @Test void failedSqsRecordDoesNotRetrySuccessfulBatchMembers() {
    SfnClient sfn=mock(SfnClient.class);
    var ok=new SQSEvent.SQSMessage();ok.setMessageId("ok");ok.setBody(Json.write(Map.of("jobId",UUID.randomUUID().toString())));
    var bad=new SQSEvent.SQSMessage();bad.setMessageId("bad");bad.setBody("broken json");
    var e=new SQSEvent();e.setRecords(List.of(ok,bad));
    var result=new ReportExporterHandler(sfn,"arn").handleRequest(e,null);
    assertThat(result.getBatchItemFailures()).extracting(SQSBatchResponse.BatchItemFailure::getItemIdentifier).containsExactly("bad");
    verify(sfn,times(1)).startExecution(any(Consumer.class));
  }
  @Test void requiresJwtBeforeInvokingDomainService() {
    ExportRequests requests=mock(ExportRequests.class);
    var response=new ExportApiHandler(requests,key->"signed").handleRequest(Map.of("requestContext",Map.of()),null);
    assertThat(response.get("statusCode")).isEqualTo(401);verifyNoInteractions(requests);
  }
  @Test void authenticatedUserCannotReadAnotherOwnersJob() {
    ExportRequests requests=mock(ExportRequests.class);String id=UUID.randomUUID().toString();
    when(requests.get("sub",id)).thenThrow(new SecurityException());
    var event=Map.<String,Object>of("requestContext",Map.of("authorizer",Map.of("jwt",Map.of("claims",Map.of("sub","sub")))),
        "pathParameters",Map.of("jobId",id),"routeKey","GET /exports/{jobId}");
    assertThat(new ExportApiHandler(requests,key->"signed").handleRequest(event,null).get("statusCode")).isEqualTo(403);
  }
  @Test void weatherRejectsUnknownRegionsBeforeCallingProvider() {
    DynamoDbClient db=mock(DynamoDbClient.class);
    var handler=new WeatherUpdaterHandler(db,"weather",region->{throw new AssertionError("must not call");},Clock.systemUTC());
    assertThatThrownBy(()->handler.handleRequest(Map.of("region","not-allowed"),null)).isInstanceOf(IllegalArgumentException.class);
    verifyNoInteractions(db);
  }
  @Test void weatherUsesApplicationExpiryAndSanitizesProviderErrors() {
    DynamoDbClient db=mock(DynamoDbClient.class);Instant now=Instant.parse("2026-01-01T00:00:00Z");
    var handler=new WeatherUpdaterHandler(db,"weather",region->"{\"windSpeed\":10}",Clock.fixed(now,ZoneOffset.UTC));
    assertThat(handler.handleRequest(Map.of("region","Tokyo"),null).get("expiresAt")).isEqualTo(now.plusSeconds(900).getEpochSecond());
    var failure=new WeatherUpdaterHandler(db,"weather",region->{throw new RuntimeException("secret-url-api-key");},Clock.systemUTC());
    assertThatThrownBy(()->failure.handleRequest(Map.of(),null)).hasMessage("Weather update failed").hasNoCause();
  }

  @Test void repeatedWorkflowDispatchIsAcceptedWithoutSecondExecution() {
    var sfn=mock(SfnClient.class);
    when(sfn.startExecution(any(Consumer.class))).thenThrow(ExecutionAlreadyExistsException.builder().message("duplicate").build());
    var message=new SQSEvent.SQSMessage();message.setMessageId("id");message.setBody(Json.write(Map.of("jobId",UUID.randomUUID().toString())));
    var event=new SQSEvent();event.setRecords(List.of(message));
    assertThat(new ReportExporterHandler(sfn,"arn").handleRequest(event,null).getBatchItemFailures()).isEmpty();
  }
  @Test void dispatchFailureDoesNotMarkOutboxSent() {
    var sqs=mock(software.amazon.awssdk.services.sqs.SqsClient.class);var jobs=mock(DynamoJobs.class);
    when(sqs.sendMessage(any(Consumer.class))).thenThrow(new RuntimeException("temporary"));
    var record=new DynamodbEvent.DynamodbStreamRecord();record.setEventName("INSERT");
    var stream=new com.amazonaws.services.lambda.runtime.events.models.dynamodb.StreamRecord();
    stream.setNewImage(Map.of("pk",new com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue().withS("OUTBOX#id"),
        "jobId",new com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue().withS("id")));
    record.setDynamodb(stream);var event=new DynamodbEvent();event.setRecords(List.of(record));
    assertThatThrownBy(()->new OutboxDispatcherHandler(sqs,"queue",jobs).handleRequest(event,null)).isInstanceOf(RuntimeException.class);
    verifyNoInteractions(jobs);
  }
  @Test void notifierIgnoresAlreadyReportedSameAssessment() {
    var db=mock(DynamoDbClient.class);var ses=mock(software.amazon.awssdk.services.ses.SesClient.class);
    var image=Map.of("pk",new com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue().withS("PROPERTY#id"),
        "status",new com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue().withS("REPORTED"),
        "assessmentId",new com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue().withS("assessment"));
    var stream=new com.amazonaws.services.lambda.runtime.events.models.dynamodb.StreamRecord();stream.setOldImage(image);stream.setNewImage(image);
    var record=new DynamodbEvent.DynamodbStreamRecord();record.setEventName("MODIFY");record.setDynamodb(stream);
    var event=new DynamodbEvent();event.setRecords(List.of(record));
    assertThat(new ReportNotifierHandler(db,ses,"table","from","to").handleRequest(event,null).get("sent")).isEqualTo(0);
    verifyNoInteractions(db,ses);
  }
}

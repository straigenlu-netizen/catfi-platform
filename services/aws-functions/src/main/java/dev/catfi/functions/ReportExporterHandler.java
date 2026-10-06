package dev.catfi.functions;

import com.amazonaws.services.lambda.runtime.*;
import com.amazonaws.services.lambda.runtime.events.*;
import dev.catfi.aws.*;
import java.util.*;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.ExecutionAlreadyExistsException;

/** SQS batching is an adapter; workflow execution and file generation have separate boundaries. */
public class ReportExporterHandler implements RequestHandler<SQSEvent,SQSBatchResponse> {
  private final SfnClient workflow;private final String arn;
  public ReportExporterHandler() { this(AwsClients.sfn(),AwsClients.required("WORKFLOW_ARN")); }
  public ReportExporterHandler(SfnClient workflow,String arn) { this.workflow=workflow;this.arn=arn; }
  public SQSBatchResponse handleRequest(SQSEvent event,Context context) {
    List<SQSBatchResponse.BatchItemFailure> failures=new ArrayList<>();
    for(var message:event.getRecords()) {
      try {
        var body=Json.MAPPER.readTree(message.getBody());String id=body.path("jobId").asText();UUID.fromString(id);
        try { workflow.startExecution(b->b.stateMachineArn(arn).name("export-"+id).input(Json.write(Map.of("jobId",id)))); }
        catch(ExecutionAlreadyExistsException duplicate) { /* Standard workflow owns recovery; never start a second effect. */ }
      } catch(Exception error) {
        failures.add(new SQSBatchResponse.BatchItemFailure(message.getMessageId()));
        if(context!=null)context.getLogger().log(Json.write(Map.of("event","export_dispatch_failed","messageId",message.getMessageId())));
      }
    }
    return new SQSBatchResponse(failures);
  }
}

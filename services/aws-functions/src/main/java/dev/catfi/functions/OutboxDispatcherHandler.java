package dev.catfi.functions;

import com.amazonaws.services.lambda.runtime.*;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import dev.catfi.aws.*;
import java.util.*;
import software.amazon.awssdk.services.sqs.SqsClient;

/** Streams retries may send twice: the consumer starts a stable named Step Functions execution. */
public class OutboxDispatcherHandler implements RequestHandler<DynamodbEvent,Map<String,Object>> {
  private final SqsClient sqs;private final String queue;private final DynamoJobs jobs;
  public OutboxDispatcherHandler() { this(AwsClients.sqs(),AwsClients.required("EXPORT_QUEUE_URL"),new DynamoJobs(AwsClients.dynamo(),AwsClients.required("JOBS_TABLE"))); }
  public OutboxDispatcherHandler(SqsClient sqs,String queue,DynamoJobs jobs) { this.sqs=sqs;this.queue=queue;this.jobs=jobs; }
  public Map<String,Object> handleRequest(DynamodbEvent event,Context context) {
    int sent=0;
    for(var r:event.getRecords()) {
      if(!"INSERT".equals(r.getEventName()))continue;
      var image=r.getDynamodb().getNewImage();
      if(image==null||!image.get("pk").getS().startsWith("OUTBOX#"))continue;
      String id=image.get("jobId").getS();
      sqs.sendMessage(b->b.queueUrl(queue).messageBody(Json.write(Map.of("jobId",id))));
      jobs.dispatched(id);sent++;
    }
    return Map.of("sent",sent);
  }
}

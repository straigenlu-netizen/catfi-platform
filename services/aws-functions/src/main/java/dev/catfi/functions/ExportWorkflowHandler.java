package dev.catfi.functions;

import com.amazonaws.services.lambda.runtime.*;
import dev.catfi.aws.*;
import java.util.*;
import software.amazon.awssdk.core.sync.RequestBody;

public class ExportWorkflowHandler implements RequestHandler<Map<String,String>,Map<String,String>> {
  private final ExportService exports;
  public ExportWorkflowHandler() {
    var s3=AwsClients.s3();String bucket=AwsClients.required("EXPORT_BUCKET");
    exports=new ExportService(new DynamoJobs(AwsClients.dynamo(),AwsClients.required("JOBS_TABLE")),
        (key,content,type)->s3.putObject(b->b.bucket(bucket).key(key).contentType(type),RequestBody.fromString(content)));
  }
  public ExportWorkflowHandler(ExportService exports) { this.exports=exports; }
  public Map<String,String> handleRequest(Map<String,String> input,Context context) {
    String id=input.get("jobId");UUID.fromString(id);
    return switch(input.getOrDefault("operation","generate")) {
      case "generate" -> Map.of("jobId",id,"objectKey",exports.generate(id));
      case "complete" -> { exports.complete(id,input.get("objectKey"));yield Map.of("jobId",id,"status","SUCCEEDED"); }
      case "fail" -> { exports.fail(id);yield Map.of("jobId",id,"status","FAILED"); }
      default -> throw new IllegalArgumentException("Unknown workflow operation");
    };
  }
}

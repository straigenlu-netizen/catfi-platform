package dev.catfi.functions;

import com.amazonaws.services.lambda.runtime.*;
import dev.catfi.aws.*;
import java.time.Duration;
import java.util.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import static dev.catfi.aws.DynamoReportStore.s;

/** API Gateway HTTP API v2 handler. Gateway verifies JWT; object grants remain mandatory. */
public class ExportApiHandler implements RequestHandler<Map<String,Object>,Map<String,Object>> {
  public interface Download { String url(String key); }
  private final ExportRequests requests;private final Download download;
  public ExportApiHandler() {
    var db=AwsClients.dynamo();String reports=AwsClients.required("REPORTING_TABLE");
    requests=new ExportRequests(new DynamoJobs(db,AwsClients.required("JOBS_TABLE")),new DynamoReportStore(db,reports),
        (sub,property)->!db.getItem(b->b.tableName(reports).consistentRead(true)
            .key(Map.of("pk",s("ACCESS#"+sub),"sk",s("PROPERTY#"+property)))).item().isEmpty());
    S3Presigner signer=AwsClients.presigner();String bucket=AwsClients.required("EXPORT_BUCKET");
    download=key->signer.presignGetObject(b->b.signatureDuration(Duration.ofMinutes(5))
        .getObjectRequest(r->r.bucket(bucket).key(key))).url().toString();
  }
  public ExportApiHandler(ExportRequests requests,Download download) { this.requests=requests;this.download=download; }
  @SuppressWarnings("unchecked")
  public Map<String,Object> handleRequest(Map<String,Object> event,Context context) {
    try {
      Map<String,Object> rc=(Map<String,Object>)event.get("requestContext");
      Map<String,Object> auth=rc==null?null:(Map<String,Object>)rc.get("authorizer");
      Map<String,Object> jwt=auth==null?null:(Map<String,Object>)auth.get("jwt");
      Map<String,Object> claims=jwt==null?null:(Map<String,Object>)jwt.get("claims");
      String sub=claims==null?null:(String)claims.get("sub");
      if(sub==null||sub.isBlank())return response(401,Map.of("error","UNAUTHENTICATED"));
      Map<String,String> path=(Map<String,String>)event.getOrDefault("pathParameters",Map.of());
      String route=(String)event.get("routeKey");
      if("POST /reports/{propertyId}/exports".equals(route)) {
        Map<String,String> headers=(Map<String,String>)event.getOrDefault("headers",Map.of());
        String key=headers.entrySet().stream().filter(e->e.getKey().equalsIgnoreCase("Idempotency-Key")).map(Map.Entry::getValue).findFirst().orElse(null);
        ExportJob j=requests.request(sub,path.get("propertyId"),key);
        return response(202,Map.of("jobId",j.jobId(),"status",j.status()));
      }
      ExportJob j=requests.get(sub,path.get("jobId"));
      if("GET /exports/{jobId}/download".equals(route)) {
        if(!j.status().equals("SUCCEEDED"))return response(409,Map.of("error","EXPORT_NOT_READY"));
        return response(200,Map.of("url",download.url(j.objectKey()),"expiresInSeconds",300));
      }
      if("GET /exports/{jobId}".equals(route))return response(200,Map.of("jobId",j.jobId(),"propertyId",j.propertyId(),"status",j.status(),"errorCode",j.errorCode()));
      return response(404,Map.of("error","ROUTE_NOT_FOUND"));
    } catch(SecurityException e) { return response(403,Map.of("error","ACCESS_DENIED")); }
    catch(NoSuchElementException e) { return response(404,Map.of("error","NOT_FOUND")); }
    catch(IllegalArgumentException e) { return response(400,Map.of("error","INVALID_REQUEST")); }
    catch(IllegalStateException e) { return response(409,Map.of("error","REPORT_NOT_READY")); }
    catch(Exception e) {
      if(context!=null)context.getLogger().log(Json.write(Map.of("event","export_api_failed","requestId",context.getAwsRequestId())));
      return response(503,Map.of("error","TEMPORARILY_UNAVAILABLE"));
    }
  }
  private Map<String,Object> response(int code,Object body) {
    return Map.of("statusCode",code,"headers",Map.of("content-type","application/json","cache-control","no-store"),"body",Json.write(body));
  }
}

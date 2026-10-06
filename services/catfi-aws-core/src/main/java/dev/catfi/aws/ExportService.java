package dev.catfi.aws;

/** Deterministic output makes retries safe. No live API calls or mutable latest report reads. */
public final class ExportService {
  private final JobRepository jobs;private final ObjectStorage files;
  public ExportService(JobRepository jobs,ObjectStorage files) { this.jobs=jobs;this.files=files; }
  public String generate(String id) {
    ExportJob job=jobs.find(id).orElseThrow(()->new IllegalArgumentException("Unknown export job"));
    if(job.status().equals("SUCCEEDED"))return job.objectKey();
    jobs.status(id,"RUNNING","","");
    String key="exports/"+job.ownerSub()+"/"+job.propertyId()+"/"+id+".json";
    files.put(key,job.reportJson(),"application/json");
    return key;
  }
  public void complete(String id,String key) {
    var j=jobs.find(id).orElseThrow();
    String expected="exports/"+j.ownerSub()+"/"+j.propertyId()+"/"+id+".json";
    if(!expected.equals(key))throw new IllegalArgumentException("Unexpected export key");
    jobs.status(id,"SUCCEEDED",key,"");
  }
  public void fail(String id) { jobs.status(id,"FAILED","","WORKFLOW_FAILED"); }
}

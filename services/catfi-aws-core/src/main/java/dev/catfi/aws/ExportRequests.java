package dev.catfi.aws;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

public final class ExportRequests {
  public interface Access { boolean allowed(String subject,String propertyId); }
  private final JobRepository jobs;private final ReportStore reports;private final Access access;
  public ExportRequests(JobRepository jobs,ReportStore reports,Access access) { this.jobs=jobs;this.reports=reports;this.access=access; }
  public ExportJob request(String subject,String propertyId,String idempotencyKey) {
    UUID.fromString(propertyId);
    if(idempotencyKey==null||!idempotencyKey.matches("[A-Za-z0-9_-]{8,128}"))throw new IllegalArgumentException("Idempotency-Key must be 8..128 URL-safe characters");
    if(!access.allowed(subject,propertyId))throw new SecurityException("Property access denied");
    String jobId=UUID.nameUUIDFromBytes((subject+":"+idempotencyKey).getBytes(StandardCharsets.UTF_8)).toString();
    var old=jobs.find(jobId);
    if(old.isPresent()) {
      if(!old.get().propertyId().equals(propertyId))throw new IllegalArgumentException("Idempotency key reused for another property");
      return old.get();
    }
    ReportDocument report=reports.find(propertyId).orElseThrow(()->new NoSuchElementException("Report not found"));
    if(!report.status().equals("REPORTED"))throw new IllegalStateException("Report not complete");
    ExportJob created=jobs.create(new ExportJob(jobId,propertyId,subject,report.reportJson(),"PENDING","",Instant.now().toString(),""));
    if(!created.propertyId().equals(propertyId)||!created.ownerSub().equals(subject))throw new IllegalArgumentException("Idempotency key conflict");
    return created;
  }
  public ExportJob get(String subject,String jobId) {
    UUID.fromString(jobId);
    ExportJob j=jobs.find(jobId).orElseThrow(()->new NoSuchElementException("Job not found"));
    if(!j.ownerSub().equals(subject)||!access.allowed(subject,j.propertyId()))throw new SecurityException("Export access denied");
    return j;
  }
}

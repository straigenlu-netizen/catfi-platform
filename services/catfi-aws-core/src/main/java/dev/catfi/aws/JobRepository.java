package dev.catfi.aws;

import java.util.Optional;

public interface JobRepository {
  Optional<ExportJob> find(String jobId);
  ExportJob create(ExportJob job);
  void status(String jobId,String status,String objectKey,String errorCode);
}

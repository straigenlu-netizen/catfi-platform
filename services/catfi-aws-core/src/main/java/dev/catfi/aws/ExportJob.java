package dev.catfi.aws;

public record ExportJob(String jobId,String propertyId,String ownerSub,String reportJson,String status,
                        String objectKey,String createdAt,String errorCode) {}

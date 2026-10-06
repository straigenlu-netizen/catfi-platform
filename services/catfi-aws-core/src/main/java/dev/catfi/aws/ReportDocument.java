package dev.catfi.aws;

/** Transport-neutral durable aggregate. reportJson is the existing UI DTO, stateJson is private. */
public record ReportDocument(String propertyId, long version, String stateJson, String reportJson,
                             String status, String assessmentId, String updatedAt) {}

# catfi-aws-core

Shared plain Java library for the AWS reporting/export boundary; not a deployed microservice and no Spring dependency at runtime.

`ReportStore`, `JobRepository`, `ObjectStorage` and access/download ports keep application behavior separate from AWS SDK calls. `ExportRequests` enforces property grants and request idempotency; `ExportService` generates a deterministic file from the frozen report. DynamoDB adapters implement conditional aggregate writes and transactional job/outbox creation. MemoryReportStore supports the existing local demo and unit tests.

Reporting-service and aws-functions consume this module. AWS SDK 2.x clients use the default credentials provider chain, including SSO locally and execution roles in Lambda. Tables, buckets and queue names come from configuration. No static credentials are included.

Unit tests cover request authorization, idempotency and crash recovery between file creation and job completion. Real DynamoDB API tests are in e2e-tests/DynamoReportingIT; real cloud verification is scripts/aws/cloud_e2e.py. See [AWS setup](../../docs/aws/AWS_IMPLEMENTATION_RU.md) for commands and retained-data limits.

# aws-functions

Standalone Java 21 Lambda deployment module; it packages `target/catfi-functions.jar` using Maven Shade. It does not start Spring Boot or deploy as an ECS service. Handlers are transport adapters with injectable dependencies for unit tests.

| Handler | Trigger | Responsibility and communication |
| --- | --- | --- |
| ExportApiHandler | API Gateway HTTP API with Cognito JWT | Checks DynamoDB property grants, creates job/outbox atomically, reads job status, signs private S3 download |
| OutboxDispatcherHandler | DynamoDB JobsTable Streams INSERT | Sends outbox jobId to SQS and marks SENT; duplicate sends are safe |
| ReportExporterHandler | SQS batch | Starts Standard Step Functions execution with stable name; returns only failed batch identifiers |
| ExportWorkflowHandler | Step Functions | Generate immutable JSON in S3, complete DynamoDB job, or record workflow failure |
| ReportNotifierHandler | ReportingTable Streams | Sends SES email on a matching transition to REPORTED; enabled only with configured sender/recipient |
| WeatherUpdaterHandler | EventBridge Scheduler | Retrieves OpenWeather key from Secrets Manager, fetches approved city weather, writes DynamoDB cache with expiry |

No Lambda directly queries another service's relational database. API authorization uses Cognito subject grants; it never treats CatFi clientId/risk profile as an authenticated identity. Direct Lambda invocations bypass API Gateway, so operator invocation permissions must be restricted.

SQS can redeliver. Export retries reuse the frozen snapshot and stable object key. Failed Standard workflows require Step Functions redrive; queue redelivery cannot restart a completed execution with the same name. Notification email is at least once: an ambiguous SES response can cause a duplicate. Streams failures go to a recovery DLQ; export outbox recovery script handles pending items beyond the stream retention window.

SAM defines timeouts, aliases, optional SnapStart, queues/DLQs, IAM policies, scheduler, logs and alarms. Weather schedule and SES notifications are disabled until configured. See [deployment and tests](../../docs/aws/AWS_IMPLEMENTATION_RU.md) and [infra README](../../infra/aws/README.md).

For parameter-only changes, prepare a literal version identity with scripts/aws/prepare_release.py and deploy the generated release.yaml, as the GitHub workflow does.

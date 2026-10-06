# e2e-tests

## Role in CatFi

A verification module, not a deployed service. It checks that real application components cooperate across HTTP and Kafka.

## How it works

PipelineKafkaIT starts embedded Kafka, the BFF HTTP entry point, real Feign forwarding and the insurance processors. It asserts SAFE → IMPACTED → REPORTED behavior and exact money conservation. Optional Playwright tests drive the actual UI.

## Communication and purpose

Depends on the pipeline modules for component wiring. Database repositories and selected boundary APIs are mocked; Kafka and the BFF/simulator HTTP boundary are real.

## Important details

Run with the integration Maven profile. This test does not prove PostgreSQL migrations, Docker networking or an AWS deployment. DatabaseMigrationIT belongs to compensation-service and requires Docker. See root VALIDATION.md for exact executed scopes.

## Verification and operation

Build and run commands, service ports and environment variables are maintained in the [platform README](../../README.md). Test scope and remaining limitations are maintained in [VALIDATION.md](../../VALIDATION.md) and [REVIEW.md](../../REVIEW.md). Update this page when the service behavior or dependencies change.

## AWS adapter verification

DynamoReportingIT uses a real DynamoDB Local endpoint from CATFI_DYNAMODB_ENDPOINT, tests conditional competing writers, persistence across service instances, transactional job/outbox creation and deterministic export completion. Without the endpoint this test is explicitly skipped. GitHub CI provisions DynamoDB Local. Cloud JWT/SQS/Step Functions/S3 verification is separate in scripts/aws/cloud_e2e.py and runs after AWS deployment. PostgreSQL migration integration tests still require Docker.

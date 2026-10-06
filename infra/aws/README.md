# CatFi AWS infrastructure

`template.yaml` is the deployable SAM serverless learning stack. It creates durable reporting/jobs/weather tables, authenticated export API, six Java Lambda handlers, an export Standard Step Functions workflow, SQS queues/DLQs, a private versioned S3 bucket, Cognito, secret, schedule, logs and alarms. No financial database or real payment provider is moved by this stack.

`github-oidc-bootstrap.yaml` is a one-time administrator CloudFormation template. It creates/reuses GitHub OIDC, a repository/environment-specific deploy role, separate CloudFormation execution role, artifact bucket and optional ECR publication target. The deployment environment must restrict branches to main. Use an existing provider ARN when the account already has GitHub OIDC.

Build from the repository root with Java 21: `./mvnw clean verify`; Maven Shade produces services/aws-functions/target/catfi-functions.jar. SAM deploy uploads that exact artifact via CodeUri; no second Lambda build is required. Run `sam validate --lint --template-file infra/aws/template.yaml` and `cfn-lint infra/aws/github-oidc-bootstrap.yaml` before deployment.

Resources incur AWS charges. Tables/buckets have Retain policies, so deleting the stack keeps data; cleanup is explicit, including S3 versions. SES sender/recipient and SNS subscription require setup. Weather schedule defaults to disabled. Detailed Windows commands, GitHub variables, permission boundaries, recovery and test commands are in [AWS implementation guide](../../docs/aws/AWS_IMPLEMENTATION_RU.md).

For parameter-only changes, prepare a literal version identity with scripts/aws/prepare_release.py and deploy the generated release.yaml, as the GitHub workflow does.

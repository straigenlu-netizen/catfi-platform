package dev.catfi.aws;

import java.net.URI;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

/** Composition root. Production credentials come from the SDK provider chain, never source code. */
public final class AwsClients {
  private AwsClients() {}
  public static String required(String name) {
    String value = System.getenv(name);
    if (value == null || value.isBlank()) throw new IllegalStateException("Missing setting: " + name);
    return value;
  }
  public static Region region() { return Region.of(System.getenv().getOrDefault("AWS_REGION", "eu-central-1")); }
  public static DynamoDbClient dynamo() { return DynamoDbClient.builder().region(region()).build(); }
  public static S3Client s3() { return S3Client.builder().region(region()).build(); }
  public static S3Presigner presigner() { return S3Presigner.builder().region(region()).build(); }
  public static SqsClient sqs() { return SqsClient.builder().region(region()).build(); }
  public static SfnClient sfn() { return SfnClient.builder().region(region()).build(); }
  public static SesClient ses() { return SesClient.builder().region(region()).build(); }
  public static SecretsManagerClient secrets() { return SecretsManagerClient.builder().region(region()).build(); }
}

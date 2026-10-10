# CI security remediation

The application remains on Java 21. Spring Boot 4.1.1 and Spring Cloud 2025.1.3 replace the unsupported community Spring Boot 3.5 line. This updates Spring Framework, Spring Security and Tomcat together, rather than combining incompatible framework generations. Security patch overrides pin PostgreSQL JDBC 42.7.14, Bouncy Castle 1.85, Tomcat 11.0.25, Jackson 2.22.3 / 3.1.7 lz4-java 1.11.4 and MongoDB driver 5.11.1.

Focused Spring Boot 4 starters replace the classic auto-configuration classpath. The Jackson 2 compatibility module is an explicit migration bridge. Existing Kafka JSON contracts retain their Jackson 2 serializers. Spring Boot test imports use the modular packages and Spring Test MockitoBean replaces the removed MockBean annotation. Future work should migrate the Jackson 2 contracts deliberately.

The Kafka pipeline integration fixture exercises a real PostgreSQL compensation ledger and transactional outbox publisher, alongside embedded Kafka. Some repository adapters remain test doubles; the service-specific PostgreSQL integration tests cover full JPA persistence and concurrency. It is not an externally deployed end-to-end environment.

The frontend uses an unprivileged Nginx image and listens on container port 8080. The host UI URL remains http://localhost:5173. The Java image does not use the base image's Pebble supervisor, so that unused executable is removed.

AWS export buckets, queues and alarm topics use rotating customer-managed KMS keys. Lambda roles have explicit decrypt/data-key permissions for their workloads; the alarm key policy permits CloudWatch publishing only from this account. The bootstrap deployment role can read encrypted artifacts and the CloudFormation execution role can provision stack keys. Keys and buckets are retained when a stack is deleted. Existing S3 objects are not automatically re-encrypted by changing a bucket default: migrate historical objects separately and validate download links before deployment. Customer-managed keys incur AWS charges when deployed; no cloud deployment is performed by this change.

SonarQube bootstrap generates a random administrator password with guaranteed uppercase, lowercase, digit and symbol characters. A regression test covers a random suffix with no special characters.

Security thresholds remain HIGH/CRITICAL for Trivy and CVSS 7 for Dependency-Check. Failed dependency scans are findings to investigate, not a reason to disable checks.

Dependency-Check 13.0.0 downloads the official NIST NVD 2.0 compressed feeds directly. This retains the full NVD dataset while avoiding unauthenticated API pagination and its rate limits. The versioned database cache is preserved after findings so subsequent scans can update incrementally. No NVD API key is required in feed mode.

Weather caching is enabled in a dedicated configuration class. Full application contexts retain the configured cache provider; HTTP controller slice tests do not require a Redis connection or cache manager.

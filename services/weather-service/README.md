# weather-service

## Role in CatFi

Provides optional weather enrichment for future disaster/property modelling. It is not required by the current insurance pipeline.

## How it works

GET /api/v1/weather/{region} validates the city, calls OpenWeather through Feign and maps the response to a weather snapshot.

## Communication and purpose

External OpenWeather API supplies temperature, wind and optional precipitation. Future simulator/BFF consumers may request enrichment; no main-pipeline service currently calls it.

## Important details

WEATHER_API_KEY must be configured outside source control. Metric provider wind is already metres per second. Missing precipitation remains unknown rather than zero. Timeouts and sanitized failures avoid leaking provider credentials. Unit tests mock the provider.

## Verification and operation

Build and run commands, service ports and environment variables are maintained in the [platform README](../../README.md). Test scope and remaining limitations are maintained in [VALIDATION.md](../../VALIDATION.md) and [REVIEW.md](../../REVIEW.md). Update this page when the service behavior or dependencies change.

## AWS cache mode

With profile `aws`, this service reads WeatherTable snapshots produced by scheduled WeatherUpdater Lambda instead of calling OpenWeather directly. Set CATFI_WEATHER_TABLE and AWS_REGION; the role needs only GetItem on that table. `expiresAt` is checked in code because DynamoDB TTL deletion is asynchronous. Missing/expired data fails explicitly. Original Feign mode remains the default. The Lambda retrieves the API key from Secrets Manager and sanitizes provider failures. See [AWS setup](../../docs/aws/AWS_IMPLEMENTATION_RU.md).

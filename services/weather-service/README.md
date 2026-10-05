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

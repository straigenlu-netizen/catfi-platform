# performance-tests

## Role in CatFi

A non-deployed Gatling module for repeatable HTTP load tests against a running CatFi BFF. It is intentionally not bound to `mvn verify`, because a load test needs an explicitly started target environment.

## Workload

`CatFiLoadSimulation` uses an open workload model, so `rps` controls the arrival rate rather than a fixed number of simultaneous users. By default 95% of traffic reads `/api/ui/dashboard` and 5% triggers `/api/ui/trigger-disaster`, exercising the asynchronous Kafka pipeline behind the BFF.

## Run

Start the complete local demo first, then run:

```powershell
.\mvnw.cmd -pl services/performance-tests gatling:test -Drps=100
.\mvnw.cmd -pl services/performance-tests gatling:test -Drps=1000
.\mvnw.cmd -pl services/performance-tests gatling:test -Drps=5000
```

Override `durationSeconds`, `writePercent` and `baseUrl` as needed. Set `writePercent=0` for a read-only BFF baseline. The test fails at 1% global errors, p95 >= 500 ms or p99 >= 1000 ms. Reports are written under `target/gatling`.

A 5000 requests/s run is not valid capacity evidence when the load generator and CatFi share one developer laptop. Use a separate load generator (for example EC2), increase load gradually, and monitor CPU, memory, connection pools, PostgreSQL and Kafka consumer lag.

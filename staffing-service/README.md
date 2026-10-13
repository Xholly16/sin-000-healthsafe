# StaffingServiceApp

## Overview

Provides on-call schedules for doctors based on ward and status.

Part of the [HealthSafe](../README.md) project. Independent Maven module, no
parent pom.

MQ: this service publishes to the ActiveMQ topic `staffing-events-topic` — see [`../common/`](../common). Broker URL and topic name come from the common `co.wethinkcode.healthsafe.mq.MqConfig` class alongside it in this module.

REST: calls `ward-service` (`../ward-service`) to validate the ward and
`alert-level-service` (`../alert-level-service`) to read the current Emergency
Status before computing a schedule — see [Integration contracts](../README.md#integration-contracts)
in the root README for the endpoint shapes.

## How the schedule is sized

`StaffingScheduler.doctorsRequired(level)` maps the Emergency Status to doctors on call:

| Emergency Status | Doctors on call |
|---|---|
| 0-2 | 1 |
| 3-5 | 3 |
| 6-8 | 6 |

Any level outside 0-8 is rejected with an `IllegalArgumentException`.

## Endpoints

| Method | Path | Behaviour |
|---|---|---|
| GET | `/health` | `OK` |
| GET | `/staffing/{wardId}` | `{"wardId", "alertLevel", "doctorsRequired"}` |
| POST | `/staffing/{wardId}/events` | same schedule, plus `"event": "STAFFING_UPDATED"`; also publishes it to `staffing-events-topic` and answers `202` |

Both routes ask `ward-service` whether the ward exists, then ask `alert-level-service`
for the current level. Error responses:

- `404` if the ward is unknown (nothing is published).
- `502` if a downstream service answers with an unexpected status.
- `503` if a downstream service cannot be reached, or (POST only) the broker is down.

## Project structure

~~~
staffing-service/
├── pom.xml
└── src/
    ├── main/java/co/wethinkcode/healthsafe/
    │   ├── StaffingServiceApp.java
    │   ├── StaffingScheduler.java
    │   └── mq/
    │       ├── MqConfig.java
    │       └── StaffingEventPublisher.java
    └── test/java/co/wethinkcode/healthsafe/
        └── StaffingSchedulerTest.java
~~~

## Build

~~~
mvn package
~~~

## Run

~~~
java -jar target/staffing-service.jar
~~~

Listens on port `7033`. Needs `ward-service` (7031) and `alert-level-service` (7032)
running, and the broker from `../common` for the events route.

## Test

~~~
mvn test
~~~

4 JUnit 5 tests cover the doctors-on-call table and the invalid-level cases. The HTTP
routes are checked manually with `curl`.

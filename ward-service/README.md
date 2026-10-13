# WardServiceApp

## Overview

Provides lists of wards and departments.

Part of the [HealthSafe](../README.md) project. Independent Maven module, no
parent pom.

MQ: this service subscribes to the ActiveMQ topic `staffing-events-topic` — see [`../common/`](../common) — and publishes to the ActiveMQ queue `equipment-failure-queue` when it detects an equipment failure on one of its wards, consumed by [`../equipment-alert-service`](../equipment-alert-service). Broker URL, topic name, and queue name come from the common `co.wethinkcode.healthsafe.mq.MqConfig` class alongside it in this module.

REST: called by `staffing-service` (`../staffing-service`) and `alert-level-service` (`../alert-level-service`) — see [Integration contracts](../README.md#integration-contracts) in the root README for the endpoint shapes.

## Endpoints

| Method | Path | Behaviour |
|---|---|---|
| GET | `/health` | `OK` |
| GET | `/wards` | all wards, as fetched from `ingestion-service` at startup |
| GET | `/wards/{id}` | one ward, or `404` with `{"error": "Ward not found: ..."}` |
| GET | `/wards/{id}/staffing` | the latest staffing event heard on `staffing-events-topic` for that ward; `404` if none yet, `503` if not connected to the broker |
| POST | `/wards/{id}/equipment-failure` | body `{"equipment": "...", "details": "..."}`; puts an `EQUIPMENT_FAILURE` alert on `equipment-failure-queue` and answers `202` |

`POST /wards/{id}/equipment-failure` answers `404` for an unknown ward, `400` for a body
that is not valid JSON or has no `equipment` field, and `503` if the broker is down.
Alerts are sent as persistent messages, so the broker keeps them until they are collected.

Things to know:

- The ward list is fetched once at startup, so start `ingestion-service` first.
- The latest staffing event per ward is kept in memory. The topic subscription is not
  durable, so events published while this service is down are not replayed. That is
  acceptable for staffing updates, and it is why equipment failures use a queue instead.

## Project structure

~~~
ward-service/
├── pom.xml
└── src/
    ├── main/java/co/wethinkcode/healthsafe/
    │   ├── WardServiceApp.java
    │   ├── WardLookup.java
    │   ├── WardRecord.java
    │   └── mq/
    │       ├── MqConfig.java
    │       ├── StaffingEventSubscriber.java
    │       └── EquipmentFailurePublisher.java
    └── test/java/co/wethinkcode/healthsafe/
        └── WardLookupTest.java
~~~

## Build

~~~
mvn package
~~~

## Run

~~~
java -jar target/ward-service.jar
~~~

Listens on port `7031`. Needs `ingestion-service` (7030) running first, and the broker
from `../common` for the staffing and equipment routes.

## Test

~~~
mvn test
~~~

`WardLookupTest` covers the ward lookup. The HTTP and MQ routes are checked manually
with `curl` and the ActiveMQ web console.

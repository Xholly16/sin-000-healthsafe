# EquipmentAlertServiceApp

## Overview

Uses a Queue to guarantee delivery of critical medical equipment failure alerts.

Part of the [HealthSafe](../README.md) project — its alerting service.
Independent Maven module, no parent pom.

Mechanism: ActiveMQ Queue (guaranteed delivery)

Queue: `equipment-failure-queue`

- Producer: `ward-service` (`../ward-service`) — publishes when it detects an
  equipment failure on one of its wards.
- Consumer: this service.

Broker URL and queue name are shared via a common `co.wethinkcode.healthsafe.mq.MqConfig`
class (`BROKER_URL`, `QUEUE`). It's identical in every participating service's own
source tree — each service here is an independent Maven project with no shared
parent pom, so the common package is duplicated rather than imported from one place.

A queue (not a topic) is used deliberately: each failure alert must be delivered to
exactly one consumer and processed at least once, even if this service is briefly
down — unlike the broadcast `staffing-events-topic` in [`../common/`](../common),
where every subscriber gets every message.

## How delivery is guaranteed

`EquipmentFailureConsumer` reads the queue in a transacted session. A message is only
removed from the queue when the session commits, which happens after the alert has been
handled. If handling fails, the session rolls back and the broker delivers the alert
again. Alerts sent while this service is down wait on the queue and are delivered when
it starts.

## Endpoints

| Method | Path | Behaviour |
|---|---|---|
| GET | `/health` | `OK` |
| GET | `/alerts` | JSON array of the alerts received since this service started; `503` if not connected to the broker |

The `/alerts` list is held in memory and resets when this service restarts. The queue
is the durable part.

## Project structure

~~~
equipment-alert-service/
├── pom.xml
└── src/main/java/co/wethinkcode/healthsafe/
    ├── EquipmentAlertServiceApp.java
    └── mq/
        ├── MqConfig.java
        └── EquipmentFailureConsumer.java
~~~

## Build

~~~
mvn package
~~~

## Run

~~~
java -jar target/equipment-alert-service.jar
~~~

Listens on port `7034`. Needs the broker from `../common`.

## Test

No automated tests. The guarantee is checked manually:

1. Start the broker and `ward-service`, but not this service.
2. Send an alert:
   `curl -X POST -H "Content-Type: application/json" -d '{"equipment":"Ventilator"}' http://localhost:7031/wards/W-01/equipment-failure`
3. Open http://localhost:8161/admin/queues.jsp: `equipment-failure-queue` shows 1 pending message.
4. Start this service. It prints the alert, and `curl http://localhost:7034/alerts` lists it.

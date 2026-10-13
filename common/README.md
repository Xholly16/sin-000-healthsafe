# common — Asynchronous Decoupling (MQ)

## Overview

Topic: `staffing-events-topic`

Staffing updates are broadcast as Events via the broker to decouple the frontend from the Staffing Service.

Part of the [HealthSafe](../README.md) project. Holds the ActiveMQ broker shared
by the services below — not a service itself, so it has no port of its own.

- Producer: `staffing-service` (`../staffing-service`)
- Consumer(s): ward-service

Broker URL and topic name are shared via a common `co.wethinkcode.healthsafe.mq.MqConfig` class
(`BROKER_URL`, `TOPIC`). It's identical in every participating service's own source
tree — each service here is an independent Maven project with no shared parent pom,
so the common package is duplicated rather than imported from one place.

## Project structure

```
common/
├── docker-compose.yml
└── README.md
```

This folder holds the broker config and notes only — the actual publish/subscribe
code belongs in the producer/consumer services listed above (their poms already
depend on `activemq-client`, and each already has
`src/main/java/co/wethinkcode/healthsafe/mq/MqConfig.java`).

## Build

Nothing to build here directly — this folder just brings up the broker used by the
services listed above.

## Run

```
docker compose up -d
```

- Broker URL for clients: `tcp://localhost:61616`
- Web console: http://localhost:8161 (default admin/admin)

Then start the producer/consumer services as usual (`mvn package && java -jar ...`
from their own directories at the project root).

## Test

~~~
docker compose ps          # confirm the broker container is up
~~~

End-to-end check for the topic, with `ingestion-service`, `ward-service`,
`alert-level-service` and `staffing-service` running:

1. Publish an event:
   `curl -X POST http://localhost:7033/staffing/W-01/events`
2. The `ward-service` terminal prints `Received staffing event for W-01: ...`.
3. `curl http://localhost:7031/wards/W-01/staffing` returns that event.
4. http://localhost:8161/admin/topics.jsp shows `staffing-events-topic` with 1 consumer.

## Status

- Publisher: `staffing-service` (`StaffingEventPublisher`, called from `POST /staffing/{wardId}/events`).
- Subscriber: `ward-service` (`StaffingEventSubscriber`, exposed through `GET /wards/{id}/staffing`).
- The same broker also carries `equipment-failure-queue`; see `../equipment-alert-service`.
- The topic subscription is not durable: a subscriber only receives events published
  while it is running.

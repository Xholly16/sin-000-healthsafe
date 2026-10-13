# AlertLevelServiceApp

## Overview

Tracks the hospital Emergency Status (0-8, 8 = full Code Blue).

Part of the [HealthSafe](../README.md) project. Independent Maven module, no
parent pom.

REST: called by `staffing-service` (`../staffing-service`) to read the current
status when computing on-call schedules — see [Integration contracts](../README.md#integration-contracts)
in the root README for the endpoint shapes.

## Endpoints

| Method | Path | Behaviour |
|---|---|---|
| GET | `/health` | `OK` |
| GET | `/alert-level` | `{"level": 0-8}`, the current Emergency Status |
| POST | `/alert-level` | body `{"level": 0-8}` sets the status and echoes it back |

The level starts at `0` and is held in memory, so it resets when the service restarts.

`POST /alert-level` answers `400` with `{"error": "..."}` for a body that is not valid
JSON, a missing `level`, a `level` that is not a number, a fractional number such as
`3.5`, or a number outside 0-8. A rejected request never changes the stored level.

## Project structure

~~~
alert-level-service/
├── pom.xml
└── src/
    ├── main/java/co/wethinkcode/healthsafe/
    │   ├── AlertLevelServiceApp.java
    │   └── AlertLevelValidator.java
    └── test/java/co/wethinkcode/healthsafe/
        └── AlertLevelValidatorTest.java
~~~

## Build

~~~
mvn package
~~~

## Run

~~~
java -jar target/alert-level-service.jar
~~~

Listens on port `7032`.

## Test

~~~
mvn test
~~~

7 JUnit 5 tests cover the validation rules.

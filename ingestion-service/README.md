# IngestionServiceApp

## Overview

Parses and cleans `wards-outdated.csv`, a messy legacy export of wards, wings, and specialist departments data, and is the
first stop in the HealthSafe pipeline. Independent Maven module, no parent pom.

Part of the [HealthSafe](../README.md) project.

REST: exposes the cleaned records for `ward-service` (`../ward-service`) to
consume — see [Integration contracts](../README.md#integration-contracts) in the
root README for the endpoint shape.

## Example: one row cleaned

Input (`wards-outdated.csv`, row 6):

```
w-05,east wing ,PAEDIATRICS,five
```

Expected shape after cleaning (exact field names are up to you — this illustrates
the *kind* of transform expected, not a fixed schema to match exactly):

```json
{
  "wardId": "W-05",
  "wing": "East Wing",
  "department": "Paediatrics",
  "bedsAvailable": null,
  "notes": "bedsAvailable was non-numeric ('five') — flagged for follow-up"
}
```

Note this row is also a near-duplicate of `W-05` two rows above it (same real ward,
different ID casing and field values) — deciding how to merge or flag duplicates
like this is part of the exercise.

## Known data issues

`wards-outdated.csv` is deliberately messy — cleaning it is the point of this service. Look
out for (and handle) at least:

- **Inconsistent casing** in IDs, names, and status/category values (`Active` /
  `active` / `ACTIVE`)
- **Padding** — leading/trailing spaces, and the occasional double space, inside
  fields
- **Duplicate records** for the same real-world entity, written with a different ID
  casing/format and/or slightly different field values
- **Inconsistent date formats** (`YYYY-MM-DD`, `MM/DD/YYYY`, `DD-MM-YYYY`, one- and
  two-digit months/days) and outright invalid dates
- **Missing / placeholder values** — blank fields, `N/A`, `n/a`, `TBD`, `unknown`,
  `-`, `NaN`
- **Invalid or non-numeric values** in numeric columns (negative counts, spelled-out
  numbers, unrealistic values)
- **Inconsistent boolean/flag representations** (`Y`/`N`, `yes`/`no`, `1`/`0`,
  `true`/`FALSE`)
- **Naming/spelling variants** for the same thing (e.g. regional spelling
  differences, synonyms)

## What the cleaner does

Implemented in `WardDataCleaner`. Every change that throws data away is explained in
the record's `notes` field; rows that needed no changes have `"notes": null`.

- IDs are trimmed and upper-cased (`w-05` becomes `W-05`).
- Padding and double spaces are removed, and wing and department names are Title Cased.
- Regional spellings are unified: `Paediatrics` and `Pediatrics` both become
  `Pediatrics` (so the README example above shows the idea, not the exact spelling),
  and `icu` becomes `ICU`.
- `bedsAvailable` becomes `null` with a note when the value is blank or a placeholder
  (`N/A`, `TBD`, `unknown`), non-numeric (`five`), negative, or unrealistic (over 100).
- A missing wing becomes `null` with a note.
- Rows with the same ward ID are merged: the first non-empty value of each field wins,
  the notes from every row are kept, and a `Merged duplicate rows for <id>` note is added.

This CSV has no date or flag columns, so those items from the list above do not apply to it.

## Endpoints

| Method | Path | Returns |
|---|---|---|
| GET | `/health` | `OK` |
| GET | `/wards` | the cleaned ward records as JSON |

## Project structure

~~~
ingestion-service/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/co/wethinkcode/healthsafe/
    │   │   ├── IngestionServiceApp.java
    │   │   ├── WardDataCleaner.java
    │   │   └── WardRecord.java
    │   └── resources/wards-outdated.csv
    └── test/
        ├── java/co/wethinkcode/healthsafe/
        │   ├── WardDataCleanerTest.java
        │   └── WardDataCleanerNotesTest.java
        └── resources/
            ├── test-wards.csv
            └── test-notes.csv
~~~

## Build

~~~
mvn package
~~~

## Run

~~~
java -jar target/ingestion-service.jar
~~~

Listens on port `7030`. Start this service first: `ward-service` fetches the cleaned
wards from it at startup.

## Test

~~~
mvn test
~~~

13 JUnit 5 tests cover the cleaning rules and the notes.

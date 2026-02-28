# ClueApi

A lightweight REST API that serves Jeopardy clues grouped by canonical topic, built with [Javalin](https://javalin.io/).

## Overview

ClueApi sits in front of the PostgreSQL database populated by [ClueStorer](../ClueStorer). It exposes three endpoints consumed by the Phase 4 study UI:

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/topics` | All canonical topics, sorted alphabetically |
| GET | `/api/clues?topic=<topic>` | Up to 20 random clues for the given topic |
| POST | `/api/stats` | Record a pass/fail result for a study session |

## Requirements

- Java 21
- Maven 3.x
- PostgreSQL database populated by ClueStorer (see `../ClueStorer`)

## Configuration

The API reads database credentials from environment variables:

| Variable | Example |
|----------|---------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/jeopardy` |
| `DB_USER` | `jeopardy_user` |
| `DB_PASSWORD` | `yourpassword` |

## Running

```bash
mvn package
DB_URL=jdbc:postgresql://localhost:5432/jeopardy \
DB_USER=jeopardy_user \
DB_PASSWORD=yourpassword \
java -jar target/ClueApi-1.0-SNAPSHOT.jar
```

The server starts on port **7070**.

## Running Tests

Tests use an H2 in-memory database — no PostgreSQL required.

```bash
mvn test
```

## Database Tables

See `../ClueStorer/db/migrations.sql` for the full schema. The tables used by this API are:

- `clues_java` — raw Jeopardy clues
- `category_mappings` — maps Jeopardy category names to canonical topics (populated by the LLM normalization pipeline)
- `user_stats` — pass/fail records written by `POST /api/stats`

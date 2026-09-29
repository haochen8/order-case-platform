# Spring Boot backend

See the [root README](../README.md) for local infrastructure, identity setup,
architecture, and migration requirements.

Run PostgreSQL and Keycloak with `docker compose up -d postgres identity` from
repository root. Then run locally from this directory:

```bash
SPRING_DATASOURCE_PASSWORD=local-development-only GRADLE_USER_HOME=../.gradle-home ./gradlew bootRun
```

Use `./gradlew test` for fast tests, `./gradlew integrationTest` for mandatory
PostgreSQL tests, and `./gradlew check bootJar` for release verification. Prefix
with `GRADLE_USER_HOME=../.gradle-home` to use the repository-local cache.

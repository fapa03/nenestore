# Pre-production Readiness Report — nenestore backend

> Generated: 2026-08-03
> Context: Spring Boot API being containerized and deployed to a home server behind Docker + Cloudflare Tunnel.

---

## 1. Security

### 1.1 JWT secret ✅ Done

Removed `private static final String SECRET` and `private static final long EXPIRATION_MS` from `JwtService.java`. Replaced with `@Value("${jwt.secret}")` / `@Value("${jwt.expiration-ms:86400000}")` injected fields. All former usages of the constants now reference the instance fields.

Added the `jwt:` block to `application.yml` reading from `${JWT_SECRET}` / `${JWT_EXPIRATION_MS}` with the old hardcoded value kept as the local-dev default (the string is already public, so no harm keeping it as a fallback — **set `JWT_SECRET` in your `.env` to a fresh secret before deploying**).

`JwtService` is a normal `@Service` bean used only via constructor/field injection throughout — no static callsites exist.

---

### 1.2 Other hardcoded secrets ⚠️ Findings — needs a decision

Only one hardcoded secret was found in Java source (the JWT one, now fixed). However, `application.yml` still contains:

| Property | Hardcoded value |
|---|---|
| `spring.datasource.password` | `nenestore123` |
| `spring.datasource.username` | `nenestore` |
| `google.sheets.spreadsheet-id` | `13JG71z4WHQYay9VAGNiJNrVCH7UTaTta-eAk7A_fmQU` |

Spring Boot automatically maps env vars like `SPRING_DATASOURCE_PASSWORD` → `spring.datasource.password`, so if your `.env` sets those, the hardcoded values are overridden at runtime without changing the YAML. **But they are still committed in plaintext to the repository.**

**Recommendation:** Replace them explicitly in `application.yml` with references like `${SPRING_DATASOURCE_PASSWORD:nenestore123}` so the intent is clear. No code change required — YAML only.

---

### 1.3 Google Sheets credentials ✅ Done

`GoogleSheetsConfig.java` was hardcoded to `new ClassPathResource("sheetsCredentials.json")`, ignoring the `google.sheets.credentials-file` property entirely.

Fixed to inject `@Value("${google.sheets.credentials-file}") private Resource credentialsFile` and call `credentialsFile.getInputStream()`. Spring's `Resource` abstraction handles both prefixes transparently:

| Environment | Value |
|---|---|
| Local dev | `classpath:sheetsCredentials.json` |
| Production | `file:/app/secrets/sheetsCredentials.json` |

Removed the now-unused `ClassPathResource` import.

---

### 1.4 CORS / allowed origins ✅ Done

`SecurityConfig.java` had `List.of("http://localhost:5173")` hardcoded. Replaced with `@Value("${app.cors.allowed-origins}")` injected as a `String` and split on commas. Multiple origins are supported:

```
CORS_ALLOWED_ORIGINS=https://app.nenestore.app,http://localhost:5173
```

Added to `application.yml`:

```yaml
app:
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:5173}
```

---

## 2. Storage

### 2.1 Image storage path ✅ Done (+ bug fixed)

All three consumers — `ImageService`, `CatalogService`, `WebConfig` — already use `@Value("${storage.images-path}")`. `ImageService.downloadImage()` calls `Files.createDirectories()` on first use so the directory self-creates on a fresh Docker volume.

**Bug fixed:** Both path constructions were using string concatenation (`imagesPath + "items/"`) which produces a broken path like `/app/imagesitems/` when the env var is set without a trailing slash. Changed to `Path.of(imagesPath).resolve("items")` in `ImageService.java`.

> Note: Storage is intentionally local-disk for now; object storage migration is a known future task. The path is fully configurable via `STORAGE_IMAGES_PATH` — no filesystem layout is assumed.

---

## 3. Configuration hygiene

### 3.1 Spring profiles ➖ Already correct

Single `application.yml`, `ddl-auto: validate`. Flyway owns all schema changes; Hibernate will never auto-modify the schema in any environment.

---

### 3.2 Logging ⚠️ Needs a decision

`show-sql: true` and `format_sql: true` are both active. This logs every SQL statement to stdout at INFO level — verbose in production and can expose query parameter values in logs.

**Options:**
- Turn off now: set `show-sql: false` (or `${SHOW_SQL:false}` to keep it overridable)
- Keep on temporarily during initial deployment validation, disable before go-live

No change was made — this requires an intentional call.

---

## 4. Health check ✅ Done

`spring-boot-starter-actuator` added to `pom.xml`. `/actuator/health` is exposed by default in Spring Boot 3.x with no additional config.

**Recommended Docker Compose healthcheck:**

```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
  interval: 30s
  timeout: 5s
  retries: 3
  start_period: 40s
```

> The existing custom `/api/health` endpoint in `HealthController` remains and is still usable alongside Actuator.

---

## Summary

| # | Item | Status |
|---|---|---|
| 1.1 | JWT secret externalized | ✅ Done |
| 1.2 | DB credentials & spreadsheet ID still hardcoded in `application.yml` | ⚠️ Decision needed |
| 1.3 | Google Sheets credentials use `Resource` abstraction | ✅ Done |
| 1.4 | CORS origins externalized | ✅ Done |
| 2.1 | Image path configurable + path construction bug fixed | ✅ Done |
| 3.1 | `ddl-auto: validate`, single profile | ➖ Already correct |
| 3.2 | SQL logging enabled | ⚠️ Decision needed |
| 4 | Actuator health endpoint added | ✅ Done |

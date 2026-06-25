# Nenestore API — Executive Handoff Summary

**Date:** June 2026  
**Stack:** Java 21 + Spring Boot 3.5.14 + PostgreSQL 16 + Docker  
**Repo branch:** `dev`  
**Developer level:** Junior Java developer — learning through the project

---

## 1. What We Are Building

**Nenestore** is an internal operations platform for a small clothing resale business. It is NOT a public e-commerce store — Shopify handles that. This system manages:

- **Inventory** — individual physical clothing units, each with a unique SKU
- **Sales** — cash and credit sales linked to clients
- **Client CRM** — lightweight customer tracking (name, WhatsApp, Instagram, level)
- **Google Sheets Sync** — the owner scrapes orders from supplier websites (YoungLA, Gymshark) using a Python/Playwright script that writes to Google Sheets. This Java backend reads that Sheet and imports data into PostgreSQL
- **PDF Catalog** — filterable product catalog for sharing with clients
- **Dashboard** — inventory stats, monthly sales, recent orders

### Business Data Flow
```
Gymshark/YoungLA website
    ↓ (Python + Playwright scraper — manual, monthly)
Google Sheets (staging area)
    ↓ (Java sync pipeline — manual trigger or scheduled)
PostgreSQL (source of truth)
    ↓
React Frontend (not built yet)
```

### SKU Format
```
[order_id]-[unit_index]
Example: YLA7845573-001, YLA7845573-002
```
One SKU = one physical unit. A row with quantity=3 generates 3 SKUs.

### Gender Auto-tagging Logic
```
size == 'N/A' → U (accessory)
YoungLA (order_id starts with YLA):
    product starts with 'W' → F, else → M
Gymshark (order_id starts with US):
    keyword match (leggings, bra, crop → F) (jogger, hoodie, tee → M)
    no match → U
```

---

## 2. Project Structure

```
com.nenestore.api
├── controller
│   ├── AuthController.java        POST /api/auth/login, /register
│   ├── HealthController.java      GET /api/health, /sheets-test
│   ├── OrderController.java       GET /api/orders
│   └── SyncController.java        GET /api/sync/preview, POST /api/sync/confirm, /api/sync/all
├── service
│   ├── AuthService.java           register + login logic, BCrypt
│   ├── GenderTagger.java          auto-tags gender from order_id + product + size
│   ├── GoogleSheetsService.java   reads orders and items sheets
│   ├── ImageService.java          downloads Shopify images to local filesystem
│   ├── JwtService.java            generates and validates JWT tokens
│   ├── OrderService.java          getAllOrders()
│   ├── SkuService.java            generates SKUs, queries max unit index
│   └── SyncService.java           orchestrates full pipeline (preview + confirm + syncAll)
├── repository
│   ├── ItemRepository.java        + custom query: findMaxUnitIndexByOrderId
│   ├── OrderRepository.java       + findByOrderId
│   ├── SyncLogRepository.java
│   └── UserRepository.java        + findByUsername
├── entity
│   ├── Item.java                  maps to items table
│   ├── Order.java                 maps to orders table
│   ├── SyncLog.java               maps to sync_log table
│   └── User.java                  maps to users table
├── dto
│   ├── SheetItem.java             data carrier from Google Sheets items tab
│   └── SheetOrder.java            data carrier from Google Sheets orders tab
├── security
│   ├── JwtFilter.java             intercepts requests, validates Bearer token
│   └── SecurityConfig.java        deny-by-default, permits /api/auth/**, adds JwtFilter
└── config
    ├── GoogleSheetsConfig.java    creates Sheets bean from credentials file
    └── WebConfig.java             serves /images/** from local filesystem
```

---

## 3. Database Schema

### Flyway Migrations
```
V1__init_schema.sql         → orders, items, users, sync_log + indexes
V2__clients_and_sales.sql   → clients, sales, sale_items, credit_sales, payments
V3__add_barcode_to_items    → ALTER TABLE items ADD COLUMN barcode VARCHAR(50)
```

### Key Tables

```sql
orders      → id, order_id (YLA/US prefix), order_date, total_price, total_items
items       → id, order_id(FK), sku, product, color, size, gender, status,
              purchase_price_usd, sale_price_mxn, selled_price_mxn,
              image_url, barcode, created_at, updated_at, deleted_at
users       → id, username, password(BCrypt), role(ADMIN/EMPLOYEE), created_at
sync_log    → id, triggered_by(FK users), started_at, finished_at,
              rows_processed, status(SUCCESS/PARTIAL/FAILED), error_msg
clients     → id, name, whatsapp, email, instagram, level(NEW/REGULAR/VIP/WHOLESALE), notes
sales       → id, client_id(FK), sale_date, payment_type(CASH/CREDIT),
              discount_mxn, total_mxn, notes
sale_items  → id, sale_id(FK), item_id(FK), price_mxn
credit_sales→ id, sale_id(FK UNIQUE), original_debt, paid_amount, balance,
              status(PENDING/PARTIAL/PAID)
payments    → id, credit_sale_id(FK), amount, payment_date, notes
```

### Item Status Values
```
Stock       → available for sale
Unavailable → sold or removed
```

### Item Gender Values
```
M → Male
F → Female
U → Unisex / Unknown / Accessory
```

---

## 4. Configuration Files

### application.yml
```yaml
spring:
  application:
    name: nenestore-api
  datasource:
    url: jdbc:postgresql://localhost:5433/nenesport_db?sslmode=disable
    username: nenestore
    password: nenestore123
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect
  flyway:
    enabled: true
    locations: classpath:db/migration

server:
  port: 8080

google:
  sheets:
    spreadsheet-id: 13JG71z4WHQYay9VAGNiJNrVCH7UTaTta-eAk7A_fmQU
    credentials-file: classpath:sheetsCredentials.json

storage:
  images-path: ./images/items/
```

### docker-compose.yml
```yaml
services:
  postgres:
    image: postgres:16
    container_name: nenesport_db
    restart: unless-stopped
    environment:
      POSTGRES_DB: nenesport_db
      POSTGRES_USER: nenestore
      POSTGRES_PASSWORD: nenestore123
    ports:
      - "5433:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
volumes:
  postgres_data:
```

**Note:** Port is `5433` on host (not 5432) because the work machine has a native PostgreSQL on 5432.

### Important files NOT in GitHub (gitignored)
```
src/main/resources/sheetsCredentials.json   ← Google Service Account credentials
images/                                      ← downloaded product images
```

---

## 5. pom.xml Key Dependencies

```xml
spring-boot-starter-web
spring-boot-starter-data-jpa
spring-boot-starter-security
postgresql (runtime)
flyway-core
flyway-database-postgresql
jjwt-api / jjwt-impl / jjwt-jackson  (version 0.12.6)
google-api-services-sheets (v4-rev20230815-2.0.0)
google-auth-library-oauth2-http (1.19.0)
```

---

## 6. Working Endpoints

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | /api/auth/register | None | Create user, returns JWT |
| POST | /api/auth/login | None | Login, returns JWT |
| GET | /api/health | JWT | Health check |
| GET | /api/orders | JWT | List all orders |
| GET | /api/sync/preview | JWT | Preview new Sheet orders with auto-gender |
| POST | /api/sync/confirm | JWT | Confirm and insert specific orders |
| POST | /api/sync/all | JWT | One-shot: preview + confirm all new orders |
| GET | /api/sheets-test | JWT | Debug: raw Sheet orders data |

---

## 7. Current Bug — CRITICAL 🔴

**Problem:** Spring Security is NOT loading `SecurityConfig.java` on a fresh machine clone.

**Symptom:**
```
Using generated security password: ea83640d-...
```
This line in startup logs means Spring fell back to default Basic Auth instead of using our JWT configuration. All endpoints return empty response (401 with no body) instead of proper JWT-protected responses.

**Root cause suspected:** One of two things:
1. `SecurityConfig.java` or `JwtFilter.java` is missing from the cloned repo (files may not have been committed on the previous machine)
2. A compilation error is silently preventing the security package from loading

**Files to verify exist:**
```
src/main/java/com/nenestore/api/security/SecurityConfig.java
src/main/java/com/nenestore/api/security/JwtFilter.java
```

**What SecurityConfig.java must contain:**
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    // must have @Bean SecurityFilterChain
    // must have @Bean PasswordEncoder (BCryptPasswordEncoder)
    // must permit /api/auth/**
    // must add JwtFilter before UsernamePasswordAuthenticationFilter
    // must set SessionCreationPolicy.STATELESS
    // must disable CSRF
}
```

**What to do first:**
1. Run `git status` and `git log --oneline -10` to see what was actually committed
2. Check if security package files exist on disk
3. If missing — recreate them (code below)
4. Run `./mvnw spring-boot:run` and verify the generated password line is GONE

---

## 8. What Is NOT Built Yet

### Backend (remaining)
```
[ ] GET  /api/inventory              list items with search + filters
[ ] PATCH /api/inventory/{id}/status toggle Stock/Unavailable
[ ] PATCH /api/inventory/{id}/price  set sale_price_mxn
[ ] GET  /api/dashboard/stats        total stock, sold, value
[ ] GET  /api/dashboard/orders       last N orders
[ ] POST /api/sales                  register a sale (cash or credit)
[ ] POST /api/sales/{id}/payments    register a credit payment
[ ] GET  /api/clients                list clients
[ ] POST /api/clients                create client
[ ] GET  /api/catalog/pdf            generate PDF catalog with filters
[ ] Global error handling            clean JSON errors instead of stack traces
```

### Frontend (not started)
```
[ ] React + Vite + Tailwind setup
[ ] Login screen
[ ] Sidebar layout (Dashboard, Inventory, Catalog, DB Update, Sales)
[ ] Dashboard screen
[ ] Inventory screen with search
[ ] Sale registry screen
[ ] Credit sales view
[ ] Client list + profile
[ ] PDF catalog generator screen
```

---

## 9. Environment Notes

- **Work machine:** Windows — two machines at same job, both need fresh setup
- **Personal machine:** Linux
- **Terminal:** Always use Git Bash on Windows, NOT PowerShell (curl behaves differently)
- **Java situation:** Work machines have Java 7 and 8 installed (legacy job projects). JDK 21 needs to be installed separately from https://adoptium.net — use the MSI installer and check "Set JAVA_HOME" during install
- **YAML indentation:** Always use 2 spaces, never tabs (caused multiple failures)
- **Port:** PostgreSQL runs on 5433 (not 5432) on work machines due to native PG conflict

---

## 10. Recovery Procedure (fresh machine)

```bash
# 1. Clone
git clone https://github.com/YOUR_USERNAME/nenestore-api.git
cd nenestore-api
git checkout dev

# 2. Start DB
docker compose up -d

# 3. Start app (Flyway runs all 3 migrations automatically)
./mvnw spring-boot:run

# 4. Register admin user
curl -X POST "http://localhost:8080/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"username":"antonio","password":"admin123","role":"ADMIN"}'

# 5. Sync all data from Google Sheets
curl -X POST "http://localhost:8080/api/sync/all" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Prerequisites per machine:**
- Docker Desktop installed
- JDK 21 installed (not JRE, not Java 8)
- Git installed
- `sheetsCredentials.json` copied manually to `src/main/resources/`

---

*Handoff document generated June 2026 — Nenestore API v0.1 dev*
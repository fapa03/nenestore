# Nenestore — Executive Handoff Summary

**Version:** 2.0  
**Date:** July 2026  
**Stack:** Java 21 + Spring Boot 3.5.14 + React 18 + PostgreSQL 16 + Docker  
**Repo:** https://github.com/fapa03/nenestore  
**Active branch:** `dev` (merged to `main` periodically)  
**Status:** v1.0 complete — all core features operational

---

## 1. What This System Is

**Nenestore** is an internal operations platform for a small clothing resale business. NOT a public storefront (Shopify handles that). This system manages:

- Inventory of individual physical units imported from US suppliers (YoungLA, Gymshark)
- Sales tracking (cash and credit) linked to clients
- Lightweight CRM (WhatsApp, Instagram, loyalty level)
- PDF catalog generation for clients
- Google Sheets sync pipeline (the bridge between a Python scraper and the DB)

### Business Data Flow
```
YoungLA / Gymshark website
    ↓ Python + Playwright scraper (manual, ~monthly) — EXTERNAL, not part of this repo
Google Sheets (staging area — source of truth for raw import data)
    ↓ POST /api/sync/all  (SSE real-time progress)
    ↓ POST /api/sync/refresh  (bulk update prices + status)
PostgreSQL (operational database — source of truth for app state)
    ↓
React Frontend → http://localhost:5173
```

---

## 2. Complete Feature Inventory

### Backend — ALL BUILT ✅

| Endpoint | Description |
|---|---|
| POST `/api/auth/register` | Create user + JWT |
| POST `/api/auth/login` | Login + JWT |
| GET `/api/inventory` | Search/filter items (SKU, product, order, gender, status, size) |
| PATCH `/api/inventory/{id}/status` | Toggle Stock/Unavailable |
| PATCH `/api/inventory/{id}/price` | Set sale_price_mxn |
| GET `/api/dashboard/stats` | totalStock, totalSold, totalPurchaseValueUsd |
| GET `/api/dashboard/orders` | Last N orders |
| GET `/api/sync/preview` | Preview new Sheet orders |
| POST `/api/sync/confirm` | Import specific orders |
| POST `/api/sync/all` | One-shot full import |
| GET `/api/sync/stream` | SSE real-time progress (token via query param) |
| POST `/api/sync/refresh` | Bulk update prices + status from Sheets |
| POST `/api/sync/repair-images` | Re-download missing local images |
| GET `/api/clients` | List + search |
| POST `/api/clients` | Create |
| PUT `/api/clients/{id}` | Update |
| GET `/api/sales` | List all sales with items |
| POST `/api/sales` | Register cash or credit sale |
| GET `/api/sales/credit` | Open credit sales with payment history |
| POST `/api/sales/credit/{id}/payments` | Register payment |
| GET `/api/catalog/pdf` | Generate filtered PDF with images |

### Frontend — ALL BUILT ✅

| Page | Key Features |
|---|---|
| Login | JWT auth, redirect to dashboard |
| Dashboard | Stats cards, recent orders table |
| Inventory | Search, filter, image thumbnails, inline price edit, status toggle, CSV/JSON export |
| Sales | History table, new sale modal (cart, client search, discount, cash/credit), inline client creation |
| Credit Sales | Open debts table, payment modal, 📋 WhatsApp debt summary clipboard (Spanish) |
| Clients | CRUD, search, Instagram clickable link, level badge |
| Catalog | Filter form, PDF download |
| DB Update | SSE sync pipeline with step-by-step progress, Database Refresh button |
| Sidebar | All nav links, dark/light mode toggle, logout |

---

## 3. Architecture — Critical Rules

### Layered architecture (never break this)
```
Controller → Service → Repository → Entity
```
No business logic in controllers. No DB calls in controllers. No cross-layer imports in wrong direction.

### SKU system (immutable)
```
Format: [order_id]-[unit_index]
Example: YLA7845573-001
Rules:
- unit_index always 3 digits zero-padded
- counter continuous across all products in same order
- driven by quantity field (1 row → N SKUs)
- re-import safe: queries MAX(unit_index) before generating
- NEVER modified after creation
```

### Google Sheets column mapping
```java
// ALWAYS by column name, NEVER by index position
Map<String, Integer> colMap = buildColumnMap(rows.get(0));
str(row, colMap, "column_name")
```

**Items sheet exact column names:**
```
order_id | product | image_url | quantity | size | color | purchase_price_usd | stock | sales_price_mxn
```
⚠️ `stock` = `Stock` or `Vendido` (Vendido → `Unavailable` in DB)
⚠️ `sales_price_mxn` — has an **s** (caused a bug, be careful)

### Gender auto-tagging (GenderTagger.java)
```
size == '' or 'N/A' → U (accessory, no size = not clothing)
YLA orders: product starts with 'W' → F, else → M
US orders:  keyword dict (leggings/bra/crop → F, jogger/hoodie/tee → M), else → U
Values: M / F / U only
```

### Size normalization (SizeNormalizer.java)
Used in Database Refresh matching: `XLarge → XL`, `Large → L`, etc.
Match key for refresh: `order_id + product + color + normalized_size`

### Sync vs Refresh — critical distinction
```
POST /api/sync/all      → INSERT only, skips existing orders, never touches existing records
POST /api/sync/refresh  → UPDATE only, matches existing items, never creates new records
```

### Image storage
```
Download source:  Shopify CDN URL (from Google Sheets)
Stored at:        ./images/items/SKU.jpg  (relative to project root)
DB stores:        /images/items/SKU.jpg  (URL path)
Served at:        http://localhost:8080/images/items/SKU.jpg
Spring config:    storage.images-path = ./images/
WebConfig maps:   /images/** → file:./images/
Security:         /images/** is permitAll() in SecurityConfig
```

### SSE endpoint (sync/stream)
EventSource doesn't support custom headers — JWT token passed as query param `?token=...`. JwtFilter handles both `Authorization: Bearer` header AND `?token` query param.

### Soft deletes
`deleted_at` column on `orders` and `items`. All queries must include `WHERE deleted_at IS NULL`. Never hard-delete inventory records.

---

## 4. Database Schema

### Flyway migrations applied
```
V1__init_schema.sql         → orders, items, users, sync_log
V2__clients_and_sales.sql   → clients, sales, sale_items, credit_sales, payments
V3__add_barcode_to_items    → ALTER TABLE items ADD COLUMN barcode VARCHAR(50)
```

### Enum-like field values (enforced in code, not DB constraints)
```
items.status:         Stock | Unavailable
items.gender:         M | F | U
credit_sales.status:  PENDING | PARTIAL | PAID
clients.level:        NEW | REGULAR | VIP | WHOLESALE
users.role:           ADMIN | EMPLOYEE
```

### Price fields on items (all three matter)
```
purchase_price_usd  → what owner paid supplier (from Sheets, set on import)
sale_price_mxn      → listed price (set manually or via Database Refresh)
selled_price_mxn    → actual sale price (set when item is sold via Sales)
```

---

## 5. Configuration

### application.yml (key values)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/nenesport_db?sslmode=disable
    username: nenestore
    password: nenestore123
  jpa:
    hibernate:
      ddl-auto: validate        # Flyway owns schema, Hibernate only validates
  flyway:
    locations: classpath:db/migration

server:
  port: 8080

google:
  sheets:
    spreadsheet-id: 13JG71z4WHQYay9VAGNiJNrVCH7UTaTta-eAk7A_fmQU
    credentials-file: classpath:sheetsCredentials.json

storage:
  images-path: ./images/
```

### docker-compose.yml
```yaml
services:
  postgres:
    image: postgres:16
    container_name: nenesport_db
    ports:
      - "5433:5432"    # 5433 on host — NEVER change, native PG owns 5432
    environment:
      POSTGRES_DB: nenesport_db
      POSTGRES_USER: nenestore
      POSTGRES_PASSWORD: nenestore123
    volumes:
      - postgres_data:/var/lib/postgresql/data
```

### Gitignored files (copy manually per machine)
```
src/main/resources/sheetsCredentials.json   ← Google Service Account key
images/                                      ← downloaded product images
```

---

## 6. Environment & Machine Notes

| Concern | Rule |
|---|---|
| Terminal on Windows | Git Bash ONLY — PowerShell mangles curl, env vars |
| YAML indentation | 2 spaces — tabs cause silent startup failures |
| Java version | JDK 21 — work machines also have Java 7/8, verify JAVA_HOME |
| DB port | Always 5433 on host — native PostgreSQL owns 5432 on work machines |
| Node | v20+ required, installed in `frontend/` only — never at project root |
| Commits | Work on `dev`, merge to `main` when stable |

---

## 7. Recovery Procedure (Fresh Machine)

```bash
# 1. Clone
git clone https://github.com/fapa03/nenestore.git
cd nenestore && git checkout dev

# 2. Copy credentials (manual)
# Place sheetsCredentials.json → src/main/resources/

# 3. Start database
docker compose up -d

# 4. Start backend (Flyway auto-applies V1, V2, V3)
./mvnw spring-boot:run

# 5. Register admin user
curl -X POST "http://localhost:8080/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"username":"antonio","password":"admin123","role":"ADMIN"}'

# 6. Import inventory
# POST /api/sync/all with Bearer token

# 7. Restore prices and status
# POST /api/sync/refresh with Bearer token

# 8. Start frontend
cd frontend && npm install && npm run dev
```

---

## 8. Known Issues & Resolved Bugs

| Issue | Status | Resolution |
|---|---|---|
| Spring Security falling back to Basic Auth | Resolved | Was PowerShell not sending JSON body — use Git Bash |
| color/size swapped on import | Resolved | Google Sheets columns mapped by name not index |
| total_items = 0 on all orders | Resolved | SQL UPDATE recalculated from actual items count |
| Images saving to wrong path | Resolved | `imagesPath + "items/"` prefix added |
| `sales_price_mxn` not updating | Resolved | Column name has 's' — `sales_` not `sale_` |
| 12 missing images after sync | Resolved | repair-images endpoint re-downloads from Sheets |
| SSE endpoint returning 403 | Resolved | JWT token accepted via `?token=` query param |
| Export dropdown closes immediately | In progress | `e.stopPropagation()` on Export button click |

---

## 9. Immediate Next Steps

1. **Commit current state** — export dropdown fix + CreditSales WhatsApp clipboard + Database Refresh
2. **Merge dev → main**
3. **Optional polish:**
   - User management screen (ADMIN only)
   - Monthly revenue chart on Dashboard
   - Scheduled auto-sync

---

## 10. Future Roadmap

```
[ ] User management screen
[ ] Scheduled @Scheduled auto-sync job
[ ] Barcode scanner integration
[ ] Monthly revenue chart
[ ] Full Docker containerization (Spring Boot + React + Nginx)
[ ] Cloud server deployment
[ ] Cloudflare R2 image storage (replace ./images/items/)
```

---

*Nenestore Handoff v2.0 — July 2026 — All core features complete*
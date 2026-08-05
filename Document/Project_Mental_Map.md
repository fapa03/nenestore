# Nenestore — Project Mental Map

**Generated:** July 2026  
**Purpose:** Structural and logical analysis for session handoff / onboarding

---

## 1. Core Architecture

**Type:** Full-stack monolith — one backend, one frontend, one database.

**Backend:** Java 21 + Spring Boot 3.5.14, strictly layered:

```
Controller → Service → Repository → Entity
```

REST API on port 8080. JWT stateless auth, BCrypt passwords, Flyway schema versioning, global exception handler returning clean JSON errors (404, 409, 400, 500).

**Frontend:** React 18 + Vite + Tailwind CSS 3 on port 5173. Auth via JWT stored in `localStorage`, dark/light theme persisted via `localStorage`. Axios for all API calls with a centralized interceptor that attaches the Bearer token automatically and redirects to `/login` on 401/403.

**Database:** PostgreSQL 16 in Docker, exposed on port `5433` on the host machine (5432 is taken by a native PostgreSQL installation on work machines). 3 Flyway migrations applied and verified.

**Infrastructure:** Docker Compose manages **only PostgreSQL**. Spring Boot and React run locally — not containerized yet. This is intentional for development convenience. Full containerization is a future task.

---

## 2. Current State & Data Flow

### Full pipeline (operational end-to-end)

```
YoungLA / Gymshark website
    ↓
Python + Playwright scraper
(manual, ~monthly, external script — NOT part of this repo)
    ↓
Google Sheets
(staging area — raw import data lives here)
    ↓  POST /api/sync/all
       SSE real-time progress → GET /api/sync/stream
PostgreSQL
(operational database — source of truth for app state)
    ↓  POST /api/sync/refresh
       (bulk update prices + status without re-importing)
    ↓
React Frontend → http://localhost:5173
```

### Components — fully operational ✅

**Backend:**
- JWT auth (register, login, roles: ADMIN / EMPLOYEE)
- Sync pipeline: full import, SSE progress stream, duplicate protection, image download, SKU generation, gender auto-tagging
- Database Refresh: bulk update `sale_price_mxn` + `status` from Sheets, matched by `order_id + product + color + normalized_size`
- Image repair endpoint: re-downloads missing local images using Sheets source URLs
- Inventory: GET with search/filter (SKU, product, order, gender, status, size), PATCH status, PATCH price — sorted by `created_at DESC`
- Dashboard: stats (totalStock, totalSold, totalPurchaseValueUsd), last N orders
- Clients: CRUD + search (name, WhatsApp, Instagram)
- Sales: register cash/credit, list all with items
- Credit Sales: list open with payment history, register payments, auto-recalculate balance and status
- Global error handling: clean JSON with status, error, message, timestamp
- PDF Catalog: iText 7.2.5, filtered by gender/status/size/search, images loaded from local filesystem

**Frontend (all pages built and functional):**
- Login → protected routes → sidebar layout
- Dashboard: stats cards + recent orders table
- Inventory: search, filter, image thumbnails, inline price edit, status toggle, CSV/JSON export
- Sales: history + new sale modal (cart, client search, discount, cash/credit toggle), inline client creation without losing sale context
- Credit Sales: open debts, payment modal, WhatsApp-ready debt summary clipboard (Spanish, WhatsApp markdown)
- Clients: CRUD, search, Instagram clickable link, level badge
- Catalog: filter form + PDF download
- DB Update: SSE step-by-step pipeline progress + Database Refresh secondary button
- Sidebar: all navigation, dark/light mode toggle, logout

### Pending / future ⏳

```
[ ] User management screen (ADMIN only)
[ ] Scheduled auto-sync (@Scheduled backend job)
[ ] Barcode scanner integration
[ ] Monthly revenue chart on Dashboard
[ ] Full Docker containerization (Spring Boot + React + Nginx)
[ ] Cloud deployment
[ ] Cloudflare R2 image storage (replace local filesystem)
```

---

## 3. Implicit Constraints

These are architectural rules established through the project. Breaking any of them will cause bugs or data integrity issues.

### SKU is immutable
```
Format:  [order_id]-[unit_index]
Example: YLA7845573-001

Rules:
- unit_index always 3 digits, zero-padded (001–999)
- counter is continuous across ALL products in the same order
- driven by quantity field — one row with quantity=3 → 3 SKUs
- re-import safe: queries MAX(unit_index) before generating new ones
- NEVER modified after creation under any circumstance
```

### Sync vs Refresh — never conflate these
```
POST /api/sync/all      → INSERT only
                          skips orders already in DB (findByOrderId check)
                          never touches existing records

POST /api/sync/refresh  → UPDATE only
                          matches existing items by order_id + product + color + size
                          never creates new records
```

### Google Sheets column mapping — always by name, never by index
```java
// CORRECT
Map<String, Integer> colMap = buildColumnMap(rows.get(0));
str(row, colMap, "column_name")

// WRONG — breaks if columns reorder
str(row, 4)
```

**Exact items sheet column names (case-sensitive concern):**
```
order_id | product | image_url | quantity | size | color | purchase_price_usd | stock | sales_price_mxn
```
⚠️ `stock` values in Sheet: `Stock` or `Vendido` → maps to `Stock` or `Unavailable` in DB  
⚠️ `sales_price_mxn` — has an **s** in the name (`sales_`, not `sale_`) — caused a production bug

### Gender values — only three valid values
```
M → Male
F → Female
U → Unisex / Unknown / Accessory
```

### Status values — only two valid values
```
Stock       → available for sale
Unavailable → sold or removed
```

### Credit sale status — only three valid values
```
PENDING → no payments yet
PARTIAL → some payments made
PAID    → fully settled
```

### Soft deletes — never hard delete inventory
`deleted_at` column on `orders` and `items`. All queries must filter `WHERE deleted_at IS NULL`. Records are never permanently removed.

### Image storage contract
```
Download source:  Shopify CDN URL (from Google Sheets image_url column)
Physical path:    ./images/items/SKU.jpg  (relative to project root)
DB stores:        /images/items/SKU.jpg   (URL path string)
Served at:        http://localhost:8080/images/items/SKU.jpg
Spring config:    storage.images-path = ./images/
WebConfig maps:   /images/** → file:./images/
Security rule:    /images/** is permitAll() — no JWT needed for image requests
```

### SSE endpoint token handling
`EventSource` browser API does not support custom headers. The `/api/sync/stream` endpoint receives the JWT token via query parameter `?token=...` instead of the `Authorization` header. `JwtFilter.java` handles both methods.

### Port 5433 — never change
Work machines have native PostgreSQL on port 5432. Docker container is always mapped to `5433:5432`. `application.yml` always uses `localhost:5433`.

### Three price fields on items — all have different meanings
```
purchase_price_usd  → what owner paid the supplier (set on import, from Sheets)
sale_price_mxn      → listed price (set manually or via Database Refresh)
selled_price_mxn    → actual sold price (set when sale is registered)
```

### Size normalization
`SizeNormalizer.java` normalizes size strings before matching in Database Refresh:
```
XLarge → XL
Large  → L
Medium → M
Small  → S
XSmall → XS
2XL    → XXL
```
Applied to both the Sheet value and the DB value before comparison.

### Terminal on Windows — Git Bash only
PowerShell intercepts `curl` with `Invoke-WebRequest` which does not support `-d` flag correctly. All curl commands, git operations, and Maven commands must run in Git Bash. This caused multiple hours of debugging during the project.

### YAML indentation — 2 spaces, never tabs
`application.yml` silently fails to parse with tab indentation. This caused the `google.sheets.spreadsheet-id` property to be unresolvable during development.

---

## 4. Immediate Next Steps

### What the old handoff said vs. reality
The Hand-off document (v1.0) was written mid-project. It listed the entire frontend as "not started" and all backend endpoints as missing. In reality, **everything is built and operational.**

### Active work at time of this analysis
1. **Export dropdown fix** — Inventory CSV/JSON export dropdown closes immediately on click due to missing `e.stopPropagation()` on the Export button. Fix is identified, pending application.
2. **CreditSales WhatsApp clipboard** — `CopyButton` component and `buildWhatsAppSummary` function added. Was causing white screen due to component defined outside the file scope — resolved by placing it correctly between the utility function and the default export.
3. **Database Refresh** — fully built and tested. Updated 561 items with real production prices and status in one run.

### Commit state
Recent changes not yet committed:
- Inventory export (CSV/JSON)
- CreditSales WhatsApp clipboard
- Database Refresh feature
- Updated README and Hand-off documents

**Next action:** commit all pending changes to `dev`, then merge to `main`.

---

*Nenestore Project Mental Map — July 2026*

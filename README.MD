# Nenestore — Internal Operations Platform

A full-stack internal management system for a small clothing resale business. Built with Spring Boot + React.

> **Not a public storefront** — Shopify handles that. This is the internal tool for inventory, sales, clients, and catalog generation.

---

## Features

- 📦 **Inventory Management** — track individual units with unique SKUs, search, filter, toggle status, set prices, export as CSV/JSON
- 🔄 **Google Sheets Sync** — real-time SSE pipeline with step-by-step progress tracking
- 🔁 **Database Refresh** — bulk update prices and stock status from Google Sheets without re-importing
- 🏷️ **Auto Gender Tagging** — intelligent tagging based on supplier rules and keyword dictionary
- 💰 **Sales Registry** — register cash and credit sales, attach clients, apply discounts, inline client creation
- 💳 **Credit Sales** — track payment history, remaining balance, WhatsApp-ready debt summary clipboard
- 👥 **Client CRM** — manage client profiles with WhatsApp, Instagram, and loyalty levels
- 📄 **PDF Catalog** — generate filtered product catalogs with images for sharing with clients
- 📊 **Dashboard** — inventory stats, purchase value, and recent order history
- 🌓 **Dark / Light Mode** — persistent theme preference
- 🖼️ **Image Repair** — re-download missing product images from Google Sheets source URLs

---

## Tech Stack

### Backend
| Technology | Version | Purpose |
|---|---|---|
| Java | 21 LTS | Language |
| Spring Boot | 3.5.14 | Web framework |
| Spring Security | — | JWT authentication |
| Spring Data JPA | — | ORM |
| PostgreSQL | 16 | Database |
| Flyway | — | Schema migrations |
| Google Sheets API | v4-rev20230815-2.0.0 | Data source |
| Google Auth | 1.19.0 | Service account credentials |
| iText | 7.2.5 | PDF generation |
| jjwt | 0.12.6 | JWT tokens |

### Frontend
| Technology | Purpose |
|---|---|
| React 18 + Vite | UI framework |
| Tailwind CSS 3 | Styling with brand tokens |
| React Router | Navigation + protected routes |
| Axios | HTTP client with JWT interceptor |
| Lucide React | Icons |

### Infrastructure
| Tool | Purpose |
|---|---|
| Docker + Docker Compose | PostgreSQL 16 container |
| Flyway | Database version control |

---

## Brand Colors

| Token | Hex | Usage |
|---|---|---|
| `brand-deep` | `#1a0a2e` | Background |
| `brand-accent` | `#7c3aed` | Interactive elements |
| `brand-black` | `#1e1e2e` | Card surfaces |
| `brand-smoke` | `#e2e8f0` | Primary text |
| `brand-gray` | `#94a3b8` | Secondary text |

---

## Project Structure

```
nenestore/
├── docker-compose.yml
├── pom.xml
├── src/main/java/com/nenestore/api/
│   ├── controller/
│   │   ├── AuthController.java         POST /api/auth/login, /register
│   │   ├── CatalogController.java      GET /api/catalog/pdf
│   │   ├── DashboardController.java    GET /api/dashboard/stats, /orders
│   │   ├── HealthController.java       GET /api/health, /sheets-test
│   │   ├── InventoryController.java    GET/PATCH /api/inventory
│   │   ├── OrderController.java        GET /api/orders
│   │   ├── SaleController.java         GET/POST /api/sales, /credit, /payments
│   │   ├── ClientController.java       GET/POST/PUT /api/clients
│   │   └── SyncController.java        GET/POST /api/sync/*
│   ├── service/
│   │   ├── AuthService.java
│   │   ├── CatalogService.java         iText PDF generation
│   │   ├── ClientService.java
│   │   ├── DashboardService.java
│   │   ├── GenderTagger.java           auto-tags M/F/U
│   │   ├── GoogleSheetsService.java    reads Sheets by column name
│   │   ├── ImageService.java           download + repair images
│   │   ├── InventoryService.java
│   │   ├── JwtService.java
│   │   ├── OrderService.java
│   │   ├── SaleService.java
│   │   ├── SizeNormalizer.java         XLarge→XL etc.
│   │   ├── SkuService.java
│   │   └── SyncService.java            full pipeline orchestrator
│   ├── repository/                     JPA interfaces (= DAO layer)
│   ├── entity/                         JPA-mapped DB tables
│   ├── dto/                            SheetOrder, SheetItem, InventoryResponse
│   ├── security/                       JwtFilter, SecurityConfig
│   ├── config/                         GoogleSheetsConfig, WebConfig
│   └── exception/                      GlobalExceptionHandler + custom exceptions
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/
│       ├── V1__init_schema.sql
│       ├── V2__clients_and_sales.sql
│       └── V3__add_barcode_to_items.sql
├── frontend/
│   ├── src/
│   │   ├── pages/
│   │   │   ├── Dashboard.jsx
│   │   │   ├── Inventory.jsx           search, filter, export CSV/JSON
│   │   │   ├── Sales.jsx               history + new sale modal
│   │   │   ├── CreditSales.jsx         payments + WhatsApp clipboard
│   │   │   ├── Clients.jsx             CRUD + Instagram link
│   │   │   ├── Catalog.jsx             PDF download
│   │   │   └── Sync.jsx                SSE pipeline + Database Refresh
│   │   ├── components/
│   │   │   ├── Layout.jsx
│   │   │   ├── Sidebar.jsx             nav + dark/light toggle
│   │   │   └── ProtectedRoute.jsx
│   │   ├── context/
│   │   │   ├── AuthContext.jsx
│   │   │   └── ThemeContext.jsx
│   │   └── services/
│   │       └── api.js                  axios instance + interceptors
│   └── package.json
└── images/items/                       downloaded product images (gitignored)
```

---

## Getting Started

### Prerequisites

- Java 21 JDK ([Adoptium](https://adoptium.net))
- Docker Desktop
- Node.js 20+
- Git
- Git Bash (Windows) — never use PowerShell for terminal commands

### 1. Clone the repo

```bash
git clone https://github.com/fapa03/nenestore.git
cd nenestore
git checkout dev
```

### 2. Add credentials file (manual — gitignored)

```
src/main/resources/sheetsCredentials.json
```

### 3. Start PostgreSQL

```bash
docker compose up -d
```

### 4. Start the backend

```bash
./mvnw spring-boot:run
```

Flyway applies all 3 migrations automatically on first run.

### 5. Register admin user

```bash
curl -X POST "http://localhost:8080/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"username":"antonio","password":"admin123","role":"ADMIN"}'
```

### 6. Import inventory from Google Sheets

```bash
curl -X POST "http://localhost:8080/api/sync/all" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 7. Start the frontend

```bash
cd frontend
npm install
npm run dev
```

Open [http://localhost:5173](http://localhost:5173)

---

## Startup Order (always follow this)

```
1. docker compose up -d        ← PostgreSQL must be first
2. ./mvnw spring-boot:run      ← backend connects to DB on startup
3. cd frontend && npm run dev  ← frontend connects to backend
```

---

## API Endpoints

### Auth
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | None | Create user, returns JWT |
| POST | `/api/auth/login` | None | Login, returns JWT |

### Inventory
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/inventory` | JWT | List + search + filter items |
| PATCH | `/api/inventory/{id}/status` | JWT | Toggle Stock/Unavailable |
| PATCH | `/api/inventory/{id}/price` | JWT | Set sale_price_mxn |

### Dashboard
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/dashboard/stats` | JWT | totalStock, totalSold, totalPurchaseValueUsd |
| GET | `/api/dashboard/orders` | JWT | Last N orders (default 10) |

### Sync
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/sync/preview` | JWT | Preview new orders from Sheets |
| POST | `/api/sync/confirm` | JWT | Import specific orders |
| POST | `/api/sync/all` | JWT | One-shot full import |
| GET | `/api/sync/stream` | JWT (query param) | SSE real-time progress stream |
| POST | `/api/sync/refresh` | JWT | Bulk update prices + status from Sheets |
| POST | `/api/sync/repair-images` | JWT | Re-download missing local images |

### Clients
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/clients` | JWT | List + search clients |
| POST | `/api/clients` | JWT | Create client |
| GET | `/api/clients/{id}` | JWT | Get by ID |
| PUT | `/api/clients/{id}` | JWT | Update client |

### Sales
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/sales` | JWT | List all sales with items |
| POST | `/api/sales` | JWT | Register cash or credit sale |
| GET | `/api/sales/credit` | JWT | List open credit sales with payments |
| POST | `/api/sales/credit/{id}/payments` | JWT | Register payment |

### Catalog
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/catalog/pdf` | JWT | Generate PDF (params: gender, status, size, search) |

---

## Database Schema

### Flyway Migrations
| Version | File | What it creates |
|---|---|---|
| V1 | `V1__init_schema.sql` | orders, items, users, sync_log |
| V2 | `V2__clients_and_sales.sql` | clients, sales, sale_items, credit_sales, payments |
| V3 | `V3__add_barcode_to_items.sql` | barcode column on items |

### Key Field Rules
- `items.status` — only `Stock` or `Unavailable`
- `items.gender` — only `M`, `F`, or `U`
- `items.sku` — immutable after creation, format: `[order_id]-[unit_index]`
- `credit_sales.status` — only `PENDING`, `PARTIAL`, or `PAID`
- `clients.level` — only `NEW`, `REGULAR`, `VIP`, `WHOLESALE`
- Soft deletes: `deleted_at` on orders and items — all queries filter `WHERE deleted_at IS NULL`

### Google Sheets Column Names (items tab — critical)
```
order_id | product | image_url | quantity | size | color | purchase_price_usd | stock | sales_price_mxn
```
⚠️ `stock` values: `Stock` or `Vendido` (Vendido → Unavailable in DB)
⚠️ `sales_price_mxn` has an **s** — `sales_`, not `sale_`

---

## Environment Notes

- **Docker port:** `5433:5432` — never change, work machines have native PG on 5432
- **Images:** `./images/items/` on disk, `/images/**` served by Spring, gitignored
- **Credentials:** `sheetsCredentials.json` gitignored — copy manually per machine
- **Terminal:** Git Bash on Windows only — PowerShell mangles curl and env vars
- **YAML:** 2 spaces indentation only — tabs cause silent startup failures
- **Java:** JDK 21 required — work machines also have Java 7/8 for legacy projects

---

## Recovery (Fresh Machine)

```bash
git clone https://github.com/fapa03/nenestore.git
cd nenestore && git checkout dev
# copy sheetsCredentials.json manually
docker compose up -d
./mvnw spring-boot:run
# POST /api/auth/register to create user
# POST /api/sync/all to restore inventory
# POST /api/sync/refresh to restore prices and status
cd frontend && npm install && npm run dev
```

---

## Roles

| Role | Access |
|---|---|
| `ADMIN` | Full access |
| `EMPLOYEE` | Dashboard view + item status toggle only |

---

## Future Roadmap

```
[ ] User management screen (ADMIN only)
[ ] Scheduled auto-sync (@Scheduled)
[ ] Barcode scanner integration
[ ] Monthly revenue chart on Dashboard
[ ] Full Docker containerization (Spring Boot + React + Nginx)
[ ] Cloud deployment
[ ] Cloudflare R2 image storage (replace local filesystem)
```

---

*Nenestore v1.0 — July 2026*
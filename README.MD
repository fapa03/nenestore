# Nenestore — Internal Operations Platform

A full-stack internal management system for a clothing resale business. Built with Spring Boot + React.

> **Not a public storefront** — Shopify handles that. This is the internal tool for inventory, sales, clients, and catalog generation.

---

## Features

- 📦 **Inventory Management** — track individual units with unique SKUs, search, filter, toggle status, set prices
- 🔄 **Google Sheets Sync** — import orders from supplier scraper into PostgreSQL with one click
- 🏷️ **Auto Gender Tagging** — intelligent tagging based on supplier rules and keyword dictionary
- 💰 **Sales Registry** — register cash and credit sales, attach clients, apply discounts
- 💳 **Credit Sales** — track payment history, remaining balance, and status per client
- 👥 **Client CRM** — manage client profiles with WhatsApp, Instagram, and loyalty levels
- 📄 **PDF Catalog** — generate filtered product catalogs with images for sharing with clients
- 📊 **Dashboard** — inventory stats, purchase value, and recent order history
- 🌓 **Dark / Light Mode** — persistent theme preference

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
| Google Sheets API | v4 | Data source |
| iText | 7.2.5 | PDF generation |
| jjwt | 0.12.6 | JWT tokens |

### Frontend
| Technology | Purpose |
|---|---|
| React 18 + Vite | UI framework |
| Tailwind CSS 3 | Styling |
| React Router | Navigation |
| Axios | HTTP client |
| Recharts | Charts |
| Lucide React | Icons |

### Infrastructure
| Tool | Purpose |
|---|---|
| Docker + Docker Compose | PostgreSQL container |
| Flyway | Database version control |

---

## Project Structure

```
nenestore/
├── docker-compose.yml          ← PostgreSQL container
├── pom.xml                     ← Maven dependencies
├── src/main/java/com/nenestore/api/
│   ├── controller/             ← HTTP endpoints
│   ├── service/                ← Business logic
│   ├── repository/             ← Database access (DAO)
│   ├── entity/                 ← JPA-mapped tables
│   ├── dto/                    ← Data transfer objects
│   ├── security/               ← JWT filter + Security config
│   ├── config/                 ← Google Sheets + Web config
│   └── exception/              ← Global error handling
├── src/main/resources/
│   ├── application.yml         ← App configuration
│   └── db/migration/           ← Flyway SQL migrations
│       ├── V1__init_schema.sql
│       ├── V2__clients_and_sales.sql
│       └── V3__add_barcode_to_items.sql
├── frontend/                   ← React app
│   ├── src/
│   │   ├── pages/              ← Dashboard, Inventory, Sales, etc.
│   │   ├── components/         ← Sidebar, Layout, ProtectedRoute
│   │   ├── context/            ← AuthContext, ThemeContext
│   │   └── services/           ← Axios API client
│   └── package.json
└── images/                     ← Downloaded product images (gitignored)
```

---

## Getting Started

### Prerequisites

- Java 21 JDK ([Adoptium](https://adoptium.net))
- Docker Desktop
- Node.js 20+
- Git

### 1. Clone the repo

```bash
git clone https://github.com/fapa03/nenestore.git
cd nenestore
git checkout dev
```

### 2. Add credentials file

Copy `sheetsCredentials.json` (Google Service Account) to:
```
src/main/resources/sheetsCredentials.json
```

> This file is gitignored — never commit it.

### 3. Start PostgreSQL

```bash
docker compose up -d
```

### 4. Start the backend

Flyway runs automatically and applies all migrations on startup.

```bash
./mvnw spring-boot:run
```

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

## API Endpoints

### Auth
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | None | Create user |
| POST | `/api/auth/login` | None | Login, returns JWT |

### Inventory
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/inventory` | JWT | List items with search + filters |
| PATCH | `/api/inventory/{id}/status` | JWT | Toggle Stock/Unavailable |
| PATCH | `/api/inventory/{id}/price` | JWT | Set sale price MXN |

### Dashboard
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/dashboard/stats` | JWT | Inventory stats |
| GET | `/api/dashboard/orders` | JWT | Recent orders |

### Sync
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/sync/preview` | JWT | Preview new orders from Sheets |
| POST | `/api/sync/confirm` | JWT | Confirm and import specific orders |
| POST | `/api/sync/all` | JWT | One-shot full sync |

### Clients
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/clients` | JWT | List clients with search |
| POST | `/api/clients` | JWT | Create client |
| GET | `/api/clients/{id}` | JWT | Get client by ID |
| PUT | `/api/clients/{id}` | JWT | Update client |

### Sales
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/sales` | JWT | List all sales |
| POST | `/api/sales` | JWT | Register cash or credit sale |
| GET | `/api/sales/credit` | JWT | List open credit sales |
| POST | `/api/sales/credit/{id}/payments` | JWT | Register payment |

### Catalog
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/catalog/pdf` | JWT | Generate PDF with filters |

---

## Database Schema

### Tables
- `orders` — supplier orders (YoungLA, Gymshark)
- `items` — individual physical units (one row = one item)
- `users` — app users with roles
- `sync_log` — import audit trail
- `clients` — customer CRM
- `sales` — sale transactions
- `sale_items` — items per sale
- `credit_sales` — credit sale tracking
- `payments` — payment history per credit sale

### SKU Format
```
[order_id]-[unit_index]
YLA7845573-001
YLA7845573-002
```

One SKU = one physical unit. Quantity 3 generates 3 SKUs.

### Item Status
| Value | Meaning |
|---|---|
| `Stock` | Available for sale |
| `Unavailable` | Sold or removed |

### Gender Tags
| Value | Meaning |
|---|---|
| `M` | Male |
| `F` | Female |
| `U` | Unisex / Unknown / Accessory |

### Client Levels
| Value | Meaning |
|---|---|
| `NEW` | First purchase |
| `REGULAR` | Occasional buyer |
| `VIP` | High-value frequent buyer |
| `WHOLESALE` | Bulk buyer |

---

## Environment Notes

- **Docker port:** `5433:5432` (avoids conflict with native PostgreSQL on work machines)
- **Images:** stored in `./images/items/`, served at `/images/**`, gitignored
- **Credentials:** `sheetsCredentials.json` gitignored — copy manually per machine
- **Terminal:** use Git Bash on Windows, not PowerShell
- **YAML:** always 2 spaces for indentation, never tabs

---

## Data Flow

```
Supplier website (YoungLA / Gymshark)
    ↓ Python + Playwright scraper (manual, monthly)
Google Sheets (staging area)
    ↓ Java sync pipeline (POST /api/sync/all)
PostgreSQL (source of truth)
    ↓
React Frontend (http://localhost:5173)
```

---

## Recovery (Fresh Machine)

```bash
git clone https://github.com/fapa03/nenestore.git
cd nenestore && git checkout dev
# copy sheetsCredentials.json to src/main/resources/
docker compose up -d
./mvnw spring-boot:run
# register user + run sync
cd frontend && npm install && npm run dev
```

---

## Roles

| Role | Access |
|---|---|
| `ADMIN` | Full access — sync, edit, price, user management, catalog |
| `EMPLOYEE` | View dashboard, toggle item status only |

---

*Nenestore v1.0 — June 2026*
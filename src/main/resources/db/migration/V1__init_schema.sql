-- Users
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'EMPLOYEE',
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Orders
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    order_id VARCHAR(50) NOT NULL UNIQUE,
    order_date DATE NOT NULL,
    total_price NUMERIC(10, 2) NOT NULL DEFAULT 0,
    total_items INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP NULL DEFAULT NULL
);

-- Items
CREATE TABLE items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders (id),
    sku VARCHAR(80) NOT NULL UNIQUE,
    product VARCHAR(150) NOT NULL,
    color VARCHAR(100) NOT NULL,
    size VARCHAR(10) NOT NULL,
    gender VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'Stock',
    purchase_price_usd NUMERIC(10, 2) NOT NULL,
    sale_price_mxn NUMERIC(10, 2),
    selled_price_mxn NUMERIC(10, 2),
    image_url TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP NULL DEFAULT NULL
);

-- Sync log
CREATE TABLE sync_log (
    id BIGSERIAL PRIMARY KEY,
    triggered_by BIGINT REFERENCES users (id),
    started_at TIMESTAMP NOT NULL DEFAULT NOW(),
    finished_at TIMESTAMP,
    rows_processed INT,
    status VARCHAR(20),
    error_msg TEXT
);

-- Indexes
CREATE INDEX idx_items_order ON items (order_id);

CREATE INDEX idx_items_sku ON items (sku);

CREATE INDEX idx_items_status ON items (status);
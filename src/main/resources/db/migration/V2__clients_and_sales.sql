-- Clients
CREATE TABLE clients (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    whatsapp VARCHAR(20),
    email VARCHAR(100),
    instagram VARCHAR(50),
    level VARCHAR(20) NOT NULL DEFAULT 'NEW',
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Sales
CREATE TABLE sales (
    id BIGSERIAL PRIMARY KEY,
    client_id BIGINT REFERENCES clients (id),
    sale_date DATE NOT NULL DEFAULT CURRENT_DATE,
    payment_type VARCHAR(10) NOT NULL DEFAULT 'CASH',
    discount_mxn NUMERIC(10, 2) NOT NULL DEFAULT 0,
    total_mxn NUMERIC(10, 2) NOT NULL DEFAULT 0,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Sale items
CREATE TABLE sale_items (
    id BIGSERIAL PRIMARY KEY,
    sale_id BIGINT NOT NULL REFERENCES sales (id),
    item_id BIGINT NOT NULL REFERENCES items (id),
    price_mxn NUMERIC(10, 2) NOT NULL
);

-- Credit sales
CREATE TABLE credit_sales (
    id BIGSERIAL PRIMARY KEY,
    sale_id BIGINT NOT NULL UNIQUE REFERENCES sales (id),
    original_debt NUMERIC(10, 2) NOT NULL,
    paid_amount NUMERIC(10, 2) NOT NULL DEFAULT 0,
    balance NUMERIC(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Payments
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    credit_sale_id BIGINT NOT NULL REFERENCES credit_sales (id),
    amount NUMERIC(10, 2) NOT NULL,
    payment_date DATE NOT NULL DEFAULT CURRENT_DATE,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_sales_client ON sales (client_id);

CREATE INDEX idx_sales_date ON sales (sale_date);

CREATE INDEX idx_sale_items_sale ON sale_items (sale_id);

CREATE INDEX idx_sale_items_item ON sale_items (item_id);

CREATE INDEX idx_payments_credit ON payments (credit_sale_id);

CREATE INDEX idx_credit_sale ON credit_sales (sale_id);
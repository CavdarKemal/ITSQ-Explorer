-- StandardMDIGUI Database Initialization
-- =======================================

-- Create sample tables for testing

CREATE TABLE IF NOT EXISTS customers (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(50),
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS orders (
    id SERIAL PRIMARY KEY,
    customer_id INTEGER REFERENCES customers(id),
    order_date DATE DEFAULT CURRENT_DATE,
    total_amount DECIMAL(10,2),
    status VARCHAR(20) DEFAULT 'pending',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS products (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    price DECIMAL(10,2),
    stock INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert sample data
INSERT INTO customers (name, email, phone) VALUES
    ('Max Mustermann', 'max@example.com', '+49 123 456789'),
    ('Erika Musterfrau', 'erika@example.com', '+49 987 654321'),
    ('Hans Schmidt', 'hans@example.com', '+49 555 123456'),
    ('Anna Weber', 'anna@example.com', '+49 555 654321');

INSERT INTO products (name, description, price, stock) VALUES
    ('Laptop', 'Business Laptop 15 Zoll', 999.99, 10),
    ('Monitor', '27 Zoll 4K Display', 449.99, 25),
    ('Tastatur', 'Mechanische Tastatur', 129.99, 50),
    ('Maus', 'Ergonomische Maus', 59.99, 100);

INSERT INTO orders (customer_id, total_amount, status) VALUES
    (1, 1449.98, 'completed'),
    (2, 189.98, 'shipped'),
    (3, 999.99, 'pending'),
    (1, 59.99, 'completed');

-- Create a view for order summary
CREATE OR REPLACE VIEW v_order_summary AS
SELECT
    o.id AS order_id,
    c.name AS customer_name,
    o.order_date,
    o.total_amount,
    o.status
FROM orders o
JOIN customers c ON o.customer_id = c.id;

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO postgres;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO postgres;

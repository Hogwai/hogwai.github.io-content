CREATE TABLE IF NOT EXISTS products (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2),
    stock_quantity INT DEFAULT 0
);

INSERT INTO products (name, description, price, stock_quantity)
SELECT 'Reactive Widget', 'WebFlux-powered product', 29.99, 100
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Reactive Widget');

INSERT INTO products (name, description, price, stock_quantity)
SELECT 'Reactive Gadget', 'Built with R2DBC', 49.99, 50
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Reactive Gadget');

INSERT INTO products (name, description, price, stock_quantity)
SELECT 'Reactive Doohickey', 'Non-blocking and fast', 19.99, 200
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Reactive Doohickey');

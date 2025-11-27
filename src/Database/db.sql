CREATE DATABASE coolmate_shop;
USE coolmate_shop;

-- 1. USERS
CREATE TABLE users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    full_name VARCHAR(100),
    email VARCHAR(150) UNIQUE,
    password VARCHAR(255),
    phone VARCHAR(20),
    role  // ROLE_USER hoặc ROLE_ADMIN
    status TINYINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. ADDRESSES
CREATE TABLE addresses (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT,
    full_name VARCHAR(100),
    address VARCHAR(255),
    phone VARCHAR(20),
    is_default TINYINT DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 3. CATEGORIES
CREATE TABLE categories (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100),
    status TINYINT DEFAULT 1
);

-- 4. PRODUCTS
CREATE TABLE products (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(150),
    description TEXT,
    price DECIMAL(10,2),
    discount_percent INT DEFAULT 0,
    discount_price DECIMAL(10,2) DEFAULT 0,
    category_id INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(id)
);

-- 5. PRODUCT IMAGES
CREATE TABLE product_images (
    id INT PRIMARY KEY AUTO_INCREMENT,
    product_id INT,
    image_url VARCHAR(255),
    FOREIGN KEY (product_id) REFERENCES products(id)
);

-- 6. SIZES
CREATE TABLE sizes (
    id INT PRIMARY KEY AUTO_INCREMENT,
    size_name VARCHAR(20) UNIQUE
);

-- 7. PRODUCT-SIZE (N-N)
CREATE TABLE product_sizes (
    product_id INT,
    size_id INT,
    stock INT DEFAULT 0,
    PRIMARY KEY (product_id, size_id),
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (size_id) REFERENCES sizes(id)
);

-- 8. VOUCHERS
CREATE TABLE vouchers (
    id INT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(50) UNIQUE,
    description VARCHAR(255),
    discount_type ENUM('PERCENT','AMOUNT') DEFAULT 'AMOUNT',
    discount_value DECIMAL(10,2),
    min_order_amount DECIMAL(10,2) DEFAULT 0,
    start_date DATE,
    end_date DATE,
    usage_limit INT DEFAULT 1,
    used_count INT DEFAULT 0,
    status TINYINT DEFAULT 1
);

-- 9. USER-VOUCHERS (N-N)
CREATE TABLE user_vouchers (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT,
    voucher_id INT,
    is_used TINYINT DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (voucher_id) REFERENCES vouchers(id)
);

-- 10. ORDERS (1 order chỉ dùng 1 voucher)
CREATE TABLE orders (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT,
    address_id INT,
    voucher_id INT,                   -- 1 voucher duy nhất
    discount_amount DECIMAL(10,2),    -- số tiền giảm từ voucher
    total DECIMAL(10,2),
    final_total DECIMAL(10,2),
    status VARCHAR(30) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (address_id) REFERENCES addresses(id),
    FOREIGN KEY (voucher_id) REFERENCES vouchers(id)
);

-- 11. ORDER ITEMS
CREATE TABLE order_items (
    id INT PRIMARY KEY AUTO_INCREMENT,
    order_id INT,
    product_id INT,
    size_id INT,
    quantity INT,
    price DECIMAL(10,2),               -- giá gốc lúc mua
    discount_price DECIMAL(10,2),      -- giá giảm theo sản phẩm
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (size_id) REFERENCES sizes(id)
);

-- 12. PAYMENTS
CREATE TABLE payments (
    id INT PRIMARY KEY AUTO_INCREMENT,
    order_id INT UNIQUE,
    amount DECIMAL(10,2),
    method VARCHAR(50),
    status VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id)
);

-- 13. PRODUCT REVIEWS
CREATE TABLE product_reviews (
    id INT PRIMARY KEY AUTO_INCREMENT,
    product_id INT NOT NULL,
    user_id INT NOT NULL,
    rating TINYINT NOT NULL,
    comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 14. COMMENTS (cho sản phẩm hoặc review)
CREATE TABLE comments (
    id INT PRIMARY KEY AUTO_INCREMENT,
    product_id INT NOT NULL,
    user_id INT NOT NULL,
    content TEXT NOT NULL,
    parent_id INT DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (parent_id) REFERENCES comments(id)
);
-- 15. CART (1 user có 1 giỏ)
CREATE TABLE carts (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT UNIQUE,                -- 1 user chỉ có 1 cart
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 16. CART ITEMS
CREATE TABLE cart_items (
    id INT PRIMARY KEY AUTO_INCREMENT,
    cart_id INT,
    product_id INT,
    size_id INT,
    quantity INT DEFAULT 1,
    FOREIGN KEY (cart_id) REFERENCES carts(id),
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (size_id) REFERENCES sizes(id)
);
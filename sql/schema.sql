CREATE DATABASE IF NOT EXISTS hotel_management CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE hotel_management;

CREATE TABLE IF NOT EXISTS admin (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL COMMENT '保存算法、迭代次数、盐和哈希，禁止明文',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS room (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    room_number VARCHAR(20) NOT NULL UNIQUE,
    room_type VARCHAR(50) NOT NULL,
    daily_price DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    CONSTRAINT ck_room_price CHECK (daily_price > 0),
    CONSTRAINT ck_room_status CHECK (status IN ('AVAILABLE', 'OCCUPIED'))
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS hotel_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_number VARCHAR(40) NOT NULL UNIQUE,
    room_id BIGINT NOT NULL,
    guest_name VARCHAR(100) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    id_number VARCHAR(30) NOT NULL,
    planned_check_in DATE NOT NULL,
    planned_check_out DATE NOT NULL,
    actual_check_in DATETIME NULL,
    actual_check_out DATETIME NULL,
    daily_price DECIMAL(10,2) NOT NULL,
    total_amount DECIMAL(12,2) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'RESERVED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_room FOREIGN KEY (room_id) REFERENCES room(id) ON DELETE RESTRICT,
    CONSTRAINT ck_order_dates CHECK (planned_check_out > planned_check_in),
    CONSTRAINT ck_actual_dates CHECK (actual_check_out IS NULL OR (actual_check_in IS NOT NULL AND actual_check_out >= actual_check_in)),
    CONSTRAINT ck_order_price CHECK (daily_price > 0),
    CONSTRAINT ck_order_total CHECK (total_amount IS NULL OR total_amount >= 0),
    CONSTRAINT ck_order_status CHECK (status IN ('RESERVED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED')),
    INDEX idx_room_dates (room_id, status, planned_check_in, planned_check_out),
    INDEX idx_guest_name (guest_name),
    INDEX idx_phone (phone),
    INDEX idx_id_number (id_number)
) ENGINE=InnoDB;

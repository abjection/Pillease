CREATE DATABASE IF NOT EXISTS pillease_db;
USE pillease_db;

DROP TABLE IF EXISTS medical_history;
DROP TABLE IF EXISTS medication_reminders;
DROP TABLE IF EXISTS daily_health_log;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    full_name VARCHAR(100) NOT NULL,
    age INT NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    gender VARCHAR(20),
    blood_type VARCHAR(10),
    emergency_contact VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE daily_health_log (
    log_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT,
    mood VARCHAR(50),
    temperature DECIMAL(4,1),
    heart_rate INT,
    symptoms TEXT,
    log_date DATE NOT NULL,
    log_time TIME NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE medication_reminders (
    reminder_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT,
    medication_name VARCHAR(200) NOT NULL,
    reminder_time TIME NOT NULL,
    frequency VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE medical_history (
    history_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT,
    record_name VARCHAR(200) NOT NULL,
    record_date VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE DATABASE IF NOT EXISTS im_register_login_demo;
USE im_register_login_demo;

CREATE TABLE user (
    id BIGINT AUTO_INCREMENT
        PRIMARY KEY,
    username VARCHAR(64),
    password VARCHAR(64),
    create_time DATE,
    user_email VARCHAR(64)
);

INSERT INTO user (id, username, password, create_time, user_email) VALUES ('10001', 'admin', 'admin', now(), 'admin@edu.com')
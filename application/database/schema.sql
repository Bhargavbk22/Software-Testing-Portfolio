-- Digital Lost and Found Management System - MySQL 8 reference schema
-- The Spring Boot app can create/update these tables itself (spring.jpa.hibernate.ddl-auto=update, the default).
-- Use this script to review the design or to create the schema manually (then run the app with DDL_AUTO=validate or none).
CREATE DATABASE IF NOT EXISTS lost_and_found CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE lost_and_found;

CREATE TABLE roles (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(30) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  full_name VARCHAR(100) NOT NULL,
  email VARCHAR(254) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  student_id VARCHAR(20) NOT NULL UNIQUE,
  department VARCHAR(100),
  phone VARCHAR(20),
  enabled BIT NOT NULL DEFAULT 1,
  created_at DATETIME(6)
) ENGINE=InnoDB;

CREATE TABLE user_roles (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY (user_id, role_id),
  CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles(id)
) ENGINE=InnoDB;

CREATE TABLE item_categories (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(80) NOT NULL UNIQUE,
  description VARCHAR(255)
) ENGINE=InnoDB;

CREATE TABLE lost_items (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  reporter_id BIGINT NOT NULL,
  name VARCHAR(100) NOT NULL,
  category_id BIGINT NOT NULL,
  description VARCHAR(2000) NOT NULL,
  lost_date DATE NOT NULL,
  lost_location VARCHAR(150) NOT NULL,
  color VARCHAR(50),
  brand VARCHAR(50),
  identifying_characteristics VARCHAR(1000),
  image_paths VARCHAR(1000),
  contact_info VARCHAR(150),
  verified BIT NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL,
  created_at DATETIME(6),
  updated_at DATETIME(6),
  CONSTRAINT fk_lost_reporter FOREIGN KEY (reporter_id) REFERENCES users(id),
  CONSTRAINT fk_lost_category FOREIGN KEY (category_id) REFERENCES item_categories(id),
  CONSTRAINT chk_lost_status CHECK (status IN ('REPORTED','UNDER_REVIEW','MATCHED','RESOLVED','CLOSED','REJECTED'))
) ENGINE=InnoDB;
CREATE INDEX idx_lost_status ON lost_items(status);
CREATE INDEX idx_lost_date ON lost_items(lost_date);

CREATE TABLE found_items (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  reporter_id BIGINT NOT NULL,
  name VARCHAR(100) NOT NULL,
  category_id BIGINT NOT NULL,
  description VARCHAR(2000) NOT NULL,
  found_date DATE NOT NULL,
  found_location VARCHAR(150) NOT NULL,
  color VARCHAR(50),
  brand VARCHAR(50),
  identifying_characteristics VARCHAR(1000),   -- private: moderators/admins and the reporter only
  image_paths VARCHAR(1000),
  handover_location VARCHAR(150),
  verified BIT NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL,
  created_at DATETIME(6),
  updated_at DATETIME(6),
  CONSTRAINT fk_found_reporter FOREIGN KEY (reporter_id) REFERENCES users(id),
  CONSTRAINT fk_found_category FOREIGN KEY (category_id) REFERENCES item_categories(id),
  CONSTRAINT chk_found_status CHECK (status IN ('REPORTED','UNDER_REVIEW','AVAILABLE','CLAIM_PENDING','RETURNED','REJECTED'))
) ENGINE=InnoDB;
CREATE INDEX idx_found_status ON found_items(status);
CREATE INDEX idx_found_date ON found_items(found_date);

CREATE TABLE item_matches (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  lost_item_id BIGINT NOT NULL,
  found_item_id BIGINT NOT NULL,
  score INT NOT NULL,
  status VARCHAR(20) NOT NULL,
  reviewed_by BIGINT,
  created_at DATETIME(6),
  CONSTRAINT uq_match UNIQUE (lost_item_id, found_item_id),
  CONSTRAINT fk_match_lost FOREIGN KEY (lost_item_id) REFERENCES lost_items(id),
  CONSTRAINT fk_match_found FOREIGN KEY (found_item_id) REFERENCES found_items(id),
  CONSTRAINT fk_match_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE claims (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  found_item_id BIGINT NOT NULL,
  claimant_id BIGINT NOT NULL,
  ownership_explanation VARCHAR(2000) NOT NULL,
  distinguishing_characteristics VARCHAR(1000) NOT NULL,
  additional_details VARCHAR(1000),
  status VARCHAR(30) NOT NULL,
  ownership_verified BIT NOT NULL DEFAULT 0,
  decision_reason VARCHAR(1000),
  reviewed_by BIGINT,
  created_at DATETIME(6),
  updated_at DATETIME(6),
  CONSTRAINT fk_claim_found FOREIGN KEY (found_item_id) REFERENCES found_items(id),
  CONSTRAINT fk_claim_user FOREIGN KEY (claimant_id) REFERENCES users(id),
  CONSTRAINT fk_claim_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(id),
  CONSTRAINT chk_claim_status CHECK (status IN ('PENDING','UNDER_REVIEW','APPROVED','REJECTED','CANCELLED','HANDOVER_SCHEDULED','RETURNED'))
) ENGINE=InnoDB;
CREATE INDEX idx_claim_status ON claims(status);

CREATE TABLE claim_evidence (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  claim_id BIGINT NOT NULL,
  description VARCHAR(1000) NOT NULL,
  created_at DATETIME(6),
  CONSTRAINT fk_evidence_claim FOREIGN KEY (claim_id) REFERENCES claims(id)
) ENGINE=InnoDB;

CREATE TABLE recoveries (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  claim_id BIGINT NOT NULL UNIQUE,
  scheduled_at DATETIME(6),
  handover_date DATETIME(6),
  location VARCHAR(150) NOT NULL,
  status VARCHAR(20) NOT NULL,
  handled_by BIGINT,
  confirmation_code VARCHAR(40),
  created_at DATETIME(6),
  CONSTRAINT fk_rec_claim FOREIGN KEY (claim_id) REFERENCES claims(id),
  CONSTRAINT fk_rec_admin FOREIGN KEY (handled_by) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE notifications (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  type VARCHAR(40) NOT NULL,
  message VARCHAR(500) NOT NULL,
  is_read BIT NOT NULL DEFAULT 0,
  related_type VARCHAR(20),
  related_id BIGINT,
  created_at DATETIME(6),
  CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;
CREATE INDEX idx_notif_user ON notifications(user_id);

CREATE TABLE activity_logs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT,
  action VARCHAR(60) NOT NULL,
  entity_type VARCHAR(20),
  entity_id BIGINT,
  details VARCHAR(500),
  suspicious BIT NOT NULL DEFAULT 0,
  created_at DATETIME(6),
  CONSTRAINT fk_log_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;
CREATE INDEX idx_log_suspicious ON activity_logs(suspicious);

CREATE TABLE status_history (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  entity_type VARCHAR(20) NOT NULL,
  entity_id BIGINT NOT NULL,
  from_status VARCHAR(30),
  to_status VARCHAR(30) NOT NULL,
  changed_by BIGINT,
  note VARCHAR(500),
  created_at DATETIME(6),
  CONSTRAINT fk_hist_user FOREIGN KEY (changed_by) REFERENCES users(id)
) ENGINE=InnoDB;
CREATE INDEX idx_history_entity ON status_history(entity_type, entity_id);

-- Reference data (the application also inserts these on start-up if missing)
INSERT IGNORE INTO roles(name) VALUES ('ROLE_USER'),('ROLE_MODERATOR'),('ROLE_ADMIN');
INSERT IGNORE INTO item_categories(name) VALUES ('Electronics'),('Documents and ID Cards'),('Bags and Accessories'),
 ('Books and Stationery'),('Wallets and Money'),('Keys'),('Clothing'),('Other');
-- Demo users/items are created by the application's DemoDataSeeder (passwords are BCrypt-hashed at runtime).

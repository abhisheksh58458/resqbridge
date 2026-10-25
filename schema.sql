-- ResQBridge MySQL schema + seed data (MySQL 5.7+ / 8.x)
--
-- Run once:   mysql -u root -p < schema.sql
-- WARNING:    re-running this script DROPS and recreates every table,
--             resetting all data (including users) to the seed values below.

CREATE DATABASE IF NOT EXISTS resqbridge
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE resqbridge;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS run_discrepancies, manifest_items, delivery_runs, chat_messages,
  hazard_reports, safe_zones, equipment, pending_accounts, audit_log, settings,
  inventory, alerts, drivers, donors, dispatches, camps, users;
SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------- users
-- Passwords are stored as SHA-256 hex digests, never as plain text.
-- To add a user:  INSERT INTO users (username, password_hash, display_name, user_role)
--                 VALUES ('newuser', SHA2('their-password', 256), 'Full Name', 'Admin');
CREATE TABLE users (
  id                 INT AUTO_INCREMENT PRIMARY KEY,
  username           VARCHAR(50)  NOT NULL UNIQUE,
  password_hash      CHAR(64)     NOT NULL,
  display_name       VARCHAR(100) NOT NULL,
  user_role          VARCHAR(30)  NOT NULL,          -- Admin | Driver | Camp Employee
  linked_driver_name VARCHAR(100) NULL,              -- for Driver accounts
  linked_camp_name   VARCHAR(100) NULL,              -- for Camp Employee accounts
  active             TINYINT(1)   NOT NULL DEFAULT 1,
  last_login         TIMESTAMP    NULL
);

INSERT INTO users (username, password_hash, display_name, user_role, linked_driver_name, linked_camp_name) VALUES
  ('admin',   SHA2('admin123', 256),  'Arjun Menon', 'Admin',         NULL,         NULL),
  ('driver1', SHA2('driver123', 256), 'Anil Kumar',  'Driver',        'Anil Kumar', NULL),
  ('camp1',   SHA2('camp123', 256),   'Priya Nair',  'Camp Employee', NULL,         'Thrissur Camp');

-- ---------------------------------------------------------------- camps
CREATE TABLE camps (
  id             INT AUTO_INCREMENT PRIMARY KEY,
  seq            INT          NOT NULL,
  name           VARCHAR(100) NOT NULL,
  supply_percent INT          NOT NULL,
  x_frac         DOUBLE       NOT NULL,
  y_frac         DOUBLE       NOT NULL,
  is_hub         TINYINT(1)   NOT NULL DEFAULT 0,
  operational    TINYINT(1)   NOT NULL DEFAULT 1
);

INSERT INTO camps (seq, name, supply_percent, x_frac, y_frac, is_hub, operational) VALUES
  (0, 'Kasaragod Camp',          75, 0.28, 0.10, 0, 1),
  (1, 'Kannur Camp',             62, 0.36, 0.22, 0, 1),
  (2, 'Kozhikode Camp',          80, 0.44, 0.34, 0, 1),
  (3, 'Thrissur Camp',           50, 0.50, 0.46, 0, 1),
  (4, 'Kochi Hub',               55, 0.46, 0.58, 1, 1),
  (5, 'Kollam Camp',             40, 0.40, 0.74, 0, 1),
  (6, 'Thiruvananthapuram Camp', 70, 0.48, 0.88, 0, 1);

-- ---------------------------------------------------------------- dispatches
CREATE TABLE dispatches (
  id       INT AUTO_INCREMENT PRIMARY KEY,
  seq      INT          NOT NULL,
  item     VARCHAR(150) NOT NULL,
  from_loc VARCHAR(100) NOT NULL,
  to_loc   VARCHAR(100) NOT NULL,
  time_ago VARCHAR(40)  NOT NULL,
  units    INT          NOT NULL DEFAULT 0
);

INSERT INTO dispatches (seq, item, from_loc, to_loc, time_ago, units) VALUES
  (0, 'Medical Supplies', 'Kochi Hub',       'Thrissur Camp', '2 mins ago',  40),
  (1, 'Food Supplies',    'Kozhikode Camp',  'Kannur Camp',   '15 mins ago', 120),
  (2, 'Water Bottles',    'Kollam Camp',     'Kochi Hub',     '32 mins ago', 300),
  (3, 'Tents & Blankets', 'Trivandrum Camp', 'Kollam Camp',   '45 mins ago', 60);

-- ---------------------------------------------------------------- donors
CREATE TABLE donors (
  id     INT AUTO_INCREMENT PRIMARY KEY,
  name   VARCHAR(150) NOT NULL,
  amount BIGINT       NOT NULL DEFAULT 0
);

INSERT INTO donors (name, amount) VALUES
  ('Kerala Foundation',  2500000),
  ('Helping Hands NGO',  1575000),
  ('People of Kerala',   1230000),
  ('United Volunteers',   845000);

-- ---------------------------------------------------------------- drivers
-- shift_start_epoch is seconds since 1970 (timezone-proof); NULL = not on shift.
CREATE TABLE drivers (
  id                INT AUTO_INCREMENT PRIMARY KEY,
  seq               INT          NOT NULL,
  name              VARCHAR(100) NOT NULL,
  vehicle           VARCHAR(100) NOT NULL,
  on_duty           TINYINT(1)   NOT NULL DEFAULT 0,
  destination       VARCHAR(100) NOT NULL DEFAULT '',
  reached           TINYINT(1)   NOT NULL DEFAULT 0,
  fuel_percent      INT          NOT NULL DEFAULT 80,
  maintenance_issue VARCHAR(300) NOT NULL DEFAULT '',
  shift_start_epoch BIGINT       NULL
);

INSERT INTO drivers (seq, name, vehicle, on_duty, destination, reached, shift_start_epoch) VALUES
  (0, 'Anil Kumar',    'Truck - KL07 A 1234', 1, 'Thrissur Camp', 0, UNIX_TIMESTAMP() - 3 * 3600),
  (1, 'Rajesh Menon',  'Van - KL01 C 5521',   1, 'Kannur Camp',   1, NULL),
  (2, 'Sunitha Nair',  'Truck - KL09 B 7788', 1, 'Kochi Hub',     0, NULL),
  (3, 'Vishnu Prasad', 'Van - KL04 D 3312',   0, '',              0, NULL),
  (4, 'Deepa Thomas',  'Truck - KL11 A 9090', 1, 'Kollam Camp',   1, NULL);

-- ---------------------------------------------------------------- alerts
CREATE TABLE alerts (
  id       INT AUTO_INCREMENT PRIMARY KEY,
  seq      INT          NOT NULL,
  title    VARCHAR(200) NOT NULL,
  detail   VARCHAR(500) NOT NULL,
  severity VARCHAR(20)  NOT NULL
);

INSERT INTO alerts (seq, title, detail, severity) VALUES
  (0, 'Low water supply', 'Kollam Camp water stock below 40%',        'Critical'),
  (1, 'Route delay',      'Kozhikode - Kannur route congested',       'Warning');

-- ---------------------------------------------------------------- inventory
CREATE TABLE inventory (
  id            INT AUTO_INCREMENT PRIMARY KEY,
  seq           INT         NOT NULL,
  category      VARCHAR(50) NOT NULL,
  percent_value INT         NOT NULL
);

INSERT INTO inventory (seq, category, percent_value) VALUES
  (0, 'Medical', 68), (1, 'Food', 82), (2, 'Water', 74), (3, 'Shelter', 87);

-- ---------------------------------------------------------------- audit log (append-only)
CREATE TABLE audit_log (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  time_text   VARCHAR(20) NOT NULL,
  description TEXT        NOT NULL,
  created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO audit_log (time_text, description) VALUES
  ('00:00:00', 'System started and default data loaded from MySQL.');

-- ---------------------------------------------------------------- pending staff accounts
CREATE TABLE pending_accounts (
  id             INT AUTO_INCREMENT PRIMARY KEY,
  seq            INT          NOT NULL,
  name           VARCHAR(100) NOT NULL,
  email          VARCHAR(150) NOT NULL,
  requested_role VARCHAR(50)  NOT NULL,
  date_requested VARCHAR(40)  NOT NULL
);

INSERT INTO pending_accounts (seq, name, email, requested_role, date_requested) VALUES
  (0, 'Meera Pillai',  'meera.pillai@example.com', 'Field Rescue Team', 'Today'),
  (1, 'Joseph Varkey', 'joseph.v@example.com',     'Volunteer',         'Yesterday');

-- ---------------------------------------------------------------- hazards / safe zones / equipment
CREATE TABLE hazard_reports (
  id        INT AUTO_INCREMENT PRIMARY KEY,
  seq       INT          NOT NULL,
  reporter  VARCHAR(100) NOT NULL,
  location  VARCHAR(200) NOT NULL,
  type      VARCHAR(50)  NOT NULL,
  severity  VARCHAR(20)  NOT NULL,
  time_text VARCHAR(20)  NOT NULL
);

CREATE TABLE safe_zones (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  seq         INT          NOT NULL,
  name        VARCHAR(150) NOT NULL,
  type        VARCHAR(80)  NOT NULL,
  distance_km DOUBLE       NOT NULL
);

INSERT INTO safe_zones (seq, name, type, distance_km) VALUES
  (0, 'Aluva Rest Stop',            'Rest Stop - Food',      12),
  (1, 'Edappally Secure Parking',   'Secure Parking',        20),
  (2, 'Perumbavoor Fuel Station',   'Fuel Station',           8),
  (3, 'Angamaly Police Checkpoint', 'Safe Zone - Checkpoint',15);

CREATE TABLE equipment (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  seq         INT         NOT NULL,
  name        VARCHAR(80) NOT NULL,
  total_count INT         NOT NULL,
  in_use      INT         NOT NULL DEFAULT 0
);

INSERT INTO equipment (seq, name, total_count, in_use) VALUES
  (0, 'Forklift', 2, 1), (1, 'Hand Truck', 6, 2), (2, 'Pallets', 40, 15);

-- ---------------------------------------------------------------- delivery runs
CREATE TABLE delivery_runs (
  id                 INT PRIMARY KEY,
  seq                INT          NOT NULL,
  driver_name        VARCHAR(100) NOT NULL,
  from_location      VARCHAR(100) NOT NULL,
  to_camp            VARCHAR(100) NOT NULL,
  status             VARCHAR(30)  NOT NULL,
  eta_minutes        INT          NOT NULL DEFAULT 0,
  check_in_time      VARCHAR(20)  NOT NULL DEFAULT '',
  check_out_time     VARCHAR(20)  NOT NULL DEFAULT '',
  proof_signed       TINYINT(1)   NOT NULL DEFAULT 0,
  proof_note         TEXT         NULL,
  storage_allocation VARCHAR(200) NOT NULL DEFAULT '',
  offload_team       VARCHAR(200) NOT NULL DEFAULT ''
);

CREATE TABLE manifest_items (
  id       INT AUTO_INCREMENT PRIMARY KEY,
  run_id   INT          NOT NULL,
  seq      INT          NOT NULL,
  name     VARCHAR(150) NOT NULL,
  qty      INT          NOT NULL,
  priority VARCHAR(20)  NOT NULL,
  FOREIGN KEY (run_id) REFERENCES delivery_runs(id) ON DELETE CASCADE
);

CREATE TABLE run_discrepancies (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  run_id      INT  NOT NULL,
  seq         INT  NOT NULL,
  description TEXT NOT NULL,
  FOREIGN KEY (run_id) REFERENCES delivery_runs(id) ON DELETE CASCADE
);

INSERT INTO delivery_runs (id, seq, driver_name, from_location, to_camp, status, eta_minutes) VALUES
  (1, 0, 'Anil Kumar',   'Kochi Hub',      'Thrissur Camp', 'En Route',        25),
  (2, 1, 'Rajesh Menon', 'Kozhikode Camp', 'Kannur Camp',   'Loading',         60),
  (3, 2, 'Deepa Thomas', 'Kollam Camp',    'Kochi Hub',     'Arrived at Gate',  0);

INSERT INTO manifest_items (run_id, seq, name, qty, priority) VALUES
  (1, 0, 'Medical Kits',  40,  'Critical'),
  (1, 1, 'Rice Bags',     100, 'Standard'),
  (2, 0, 'Food Packets',  120, 'Standard'),
  (3, 0, 'Water Cans',    300, 'Standard'),
  (3, 1, 'Baby Formula',  20,  'Critical');

-- ---------------------------------------------------------------- chat
CREATE TABLE chat_messages (
  id           INT AUTO_INCREMENT PRIMARY KEY,
  seq          INT          NOT NULL,
  channel      VARCHAR(150) NOT NULL,
  from_name    VARCHAR(100) NOT NULL,
  from_role    VARCHAR(50)  NOT NULL,
  message_text TEXT         NOT NULL,
  time_text    VARCHAR(20)  NOT NULL
);

INSERT INTO chat_messages (seq, channel, from_name, from_role, message_text, time_text) VALUES
  (0, 'driver:Anil Kumar',     'Dispatch', 'Admin', 'Route to Thrissur Camp is clear as of this morning. Drive safe.',       '09:00:00'),
  (1, 'camp:Thrissur Camp',    'Dispatch', 'Admin', 'Truck from Kochi Hub is loaded with medical kits, priority unload.',     '09:01:00');

-- ---------------------------------------------------------------- settings (key / value)
-- Keys are "<group>.<field>" and map directly onto the settings objects in the app.
CREATE TABLE settings (
  skey   VARCHAR(100) PRIMARY KEY,
  svalue TEXT         NOT NULL
);

INSERT INTO settings (skey, svalue) VALUES
  ('admin.name',  'Arjun Menon'),
  ('admin.email', 'arjun.menon@resqbridge.org'),
  ('admin.phone', '+91 98470 12345'),
  ('admin.role',  'Super Admin'),

  ('comms.pushEnabled',      'true'),
  ('comms.smsEnabled',       'true'),
  ('comms.emailEnabled',     'true'),
  ('comms.voiceEnabled',     'false'),
  ('comms.capSenderId',      'IN-KL-RESQBRIDGE'),
  ('comms.capCategory',      'Safety'),
  ('comms.capUrgency',       'Immediate'),
  ('comms.capSeverity',      'Severe'),
  ('comms.capCertainty',     'Likely'),
  ('comms.geoFenceRadiusKm', '15'),

  ('integration.weatherApiKey',       ''),
  ('integration.mappingApiKey',       ''),
  ('integration.seismicApiKey',       ''),
  ('integration.weatherConnected',    'true'),
  ('integration.mappingConnected',    'true'),
  ('integration.seismicConnected',    'false'),
  ('integration.bandwidthLimitMbps',  '5'),
  ('integration.syncIntervalMinutes', '15'),
  ('integration.offlineQueueEnabled', 'true'),

  ('security.mfaRequired',      'true'),
  ('security.mfaMethod',        'Authenticator App'),
  ('security.dataRetentionDays','180'),
  ('security.dataResidency',    'India (Mumbai region)'),
  ('security.maintenanceMode',  'false'),
  ('security.platformNotice',   ''),

  ('total_supplies_dispatched', '1245'),
  ('total_camps_in_network',    '14'),
  ('gate_max_capacity',         '3'),
  ('hold_incoming_trucks',      'false'),
  ('resource_categories',       'Emergency Vehicles|Medical Supplies|Shelter Capacity|Food & Water'),

  ('perm.Admin',             'View Dashboard|Edit Relief Camps|Manage Donors & Inventory|Broadcast Alerts|Manage Users & Roles'),
  ('perm.Field Rescue Team', 'View Dashboard|Edit Relief Camps'),
  ('perm.Volunteer',         'View Dashboard|Manage Donors & Inventory'),
  ('perm.Citizen',           'View Dashboard');

-- V002__users_and_audit.sql
-- Sign-in accounts, their roles, and the audit trail of privileged actions.

BEGIN;

DO $$
BEGIN
  IF (SELECT max(version) FROM schema_version) <> 1 THEN
    RAISE EXCEPTION 'Expected schema version 1';
  END IF;
END $$;

CREATE TABLE app_user (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  email VARCHAR(254) NOT NULL,
  full_name VARCHAR(150) NOT NULL,
  google_sub VARCHAR(255),
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  last_login_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_app_user_email UNIQUE (email),
  CONSTRAINT uq_app_user_google_sub UNIQUE (google_sub),
  CONSTRAINT ck_app_user_email_lower CHECK (email = lower(email)),
  CONSTRAINT ck_app_user_status CHECK (status IN ('ACTIVE', 'DEACTIVATED'))
);

CREATE TABLE user_role (
  user_id BIGINT NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
  role VARCHAR(30) NOT NULL,
  PRIMARY KEY (user_id, role),
  CONSTRAINT ck_user_role CHECK (role IN
    ('ADMIN', 'EXAM_COORDINATOR', 'HOD', 'FACULTY', 'INVIGILATOR', 'STUDENT'))
);

CREATE TABLE audit_log (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  actor_user_id BIGINT REFERENCES app_user (id) ON DELETE SET NULL,
  action VARCHAR(60) NOT NULL,
  entity_type VARCHAR(60) NOT NULL,
  entity_id VARCHAR(60),
  details TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT ck_audit_details_json CHECK (details IS JSON)
);
CREATE INDEX ix_audit_entity ON audit_log (entity_type, entity_id);
CREATE INDEX ix_audit_created ON audit_log (created_at DESC);

INSERT INTO schema_version (version, script, description)
VALUES (2, 'V002__users_and_audit.sql', 'Users, roles and audit log');

COMMIT;

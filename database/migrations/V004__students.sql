-- V004__students.sql
-- The student roster. A student exists whether or not they ever sign in.

BEGIN;

DO $$
BEGIN
  IF (SELECT max(version) FROM schema_version) <> 3 THEN
    RAISE EXCEPTION 'Expected schema version 3';
  END IF;
END $$;

CREATE TABLE student (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id BIGINT REFERENCES app_user (id) ON DELETE SET NULL,
  batch_id BIGINT NOT NULL REFERENCES batch (id) ON DELETE RESTRICT,
  roll_no VARCHAR(30) NOT NULL,
  full_name VARCHAR(150) NOT NULL,
  email VARCHAR(254) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_student_roll UNIQUE (roll_no),
  CONSTRAINT uq_student_email UNIQUE (email),
  CONSTRAINT uq_student_user UNIQUE (user_id),
  CONSTRAINT ck_student_email_lower CHECK (email = lower(email))
);
CREATE INDEX ix_student_batch ON student (batch_id);

INSERT INTO schema_version (version, script, description)
VALUES (4, 'V004__students.sql', 'Students');

COMMIT;

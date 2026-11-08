-- V005__halls.sql
-- Examination halls and their dated outages.

BEGIN;

DO $$
BEGIN
  IF (SELECT max(version) FROM schema_version) <> 4 THEN
    RAISE EXCEPTION 'Expected schema version 4';
  END IF;
END $$;

CREATE TABLE exam_hall (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  code VARCHAR(20) NOT NULL,
  name VARCHAR(120) NOT NULL,
  building VARCHAR(80) NOT NULL,
  floor INT,
  capacity INT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_exam_hall_code UNIQUE (code),
  CONSTRAINT ck_exam_hall_capacity CHECK (capacity > 0),
  CONSTRAINT ck_exam_hall_status CHECK (status IN ('AVAILABLE', 'MAINTENANCE', 'RETIRED'))
);

CREATE TABLE hall_unavailability (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  hall_id BIGINT NOT NULL REFERENCES exam_hall (id) ON DELETE CASCADE,
  from_date DATE NOT NULL,
  to_date DATE NOT NULL,
  reason VARCHAR(255) NOT NULL,
  created_by BIGINT REFERENCES app_user (id) ON DELETE SET NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT ck_hall_unavail_dates CHECK (to_date >= from_date)
);
CREATE INDEX ix_hall_unavail ON hall_unavailability (hall_id, from_date, to_date);

INSERT INTO schema_version (version, script, description)
VALUES (5, 'V005__halls.sql', 'Examination halls');

COMMIT;

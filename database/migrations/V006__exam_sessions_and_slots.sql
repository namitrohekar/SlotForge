-- V006__exam_sessions_and_slots.sql
-- Exam sessions hold the scheduling rules. Time slots are the colours of the conflict graph.

BEGIN;

DO $$
BEGIN
  IF (SELECT max(version) FROM schema_version) <> 5 THEN
    RAISE EXCEPTION 'Expected schema version 5';
  END IF;
END $$;

CREATE TABLE exam_session (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PLANNING',
  max_exams_per_student_per_day INT NOT NULL DEFAULT 1,
  heavy_gap_days INT NOT NULL DEFAULT 1,
  students_per_invigilator INT NOT NULL DEFAULT 30,
  max_duties_per_invigilator INT NOT NULL DEFAULT 8,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_exam_session_name UNIQUE (name),
  CONSTRAINT ck_exam_session_dates CHECK (end_date >= start_date),
  CONSTRAINT ck_exam_session_status CHECK (status IN ('PLANNING', 'PUBLISHED', 'COMPLETED', 'CANCELLED')),
  CONSTRAINT ck_exam_session_rules CHECK (
    max_exams_per_student_per_day > 0 AND heavy_gap_days >= 0
    AND students_per_invigilator > 0 AND max_duties_per_invigilator > 0)
);

CREATE TABLE time_slot (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  session_id BIGINT NOT NULL REFERENCES exam_session (id) ON DELETE CASCADE,
  slot_date DATE NOT NULL,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
  block_reason VARCHAR(255),
  CONSTRAINT ck_time_slot_order CHECK (end_time > start_time),
  CONSTRAINT ck_time_slot_status CHECK (status IN ('OPEN', 'BLOCKED')),
  CONSTRAINT uq_time_slot UNIQUE (session_id, slot_date, start_time),
  CONSTRAINT ex_time_slot_overlap EXCLUDE USING gist
    (session_id WITH =, tsrange(slot_date + start_time, slot_date + end_time) WITH &&)
);

INSERT INTO schema_version (version, script, description)
VALUES (6, 'V006__exam_sessions_and_slots.sql', 'Exam sessions and time slots');

COMMIT;

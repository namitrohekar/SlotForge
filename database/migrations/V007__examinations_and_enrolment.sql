-- V007__examinations_and_enrolment.sql
-- Examinations are the graph vertices. Enrolment is the only input to the conflict graph.

BEGIN;

DO $$
BEGIN
  IF (SELECT max(version) FROM schema_version) <> 6 THEN
    RAISE EXCEPTION 'Expected schema version 6';
  END IF;
END $$;

CREATE TABLE examination (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  session_id BIGINT NOT NULL REFERENCES exam_session (id) ON DELETE RESTRICT,
  subject_id BIGINT NOT NULL REFERENCES subject (id) ON DELETE RESTRICT,
  duration_minutes INT NOT NULL,
  earliest_date DATE,
  latest_date DATE,
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_examination UNIQUE (session_id, subject_id),
  CONSTRAINT ck_examination_duration CHECK (duration_minutes BETWEEN 15 AND 480),
  CONSTRAINT ck_examination_window CHECK (
    earliest_date IS NULL OR latest_date IS NULL OR earliest_date <= latest_date),
  CONSTRAINT ck_examination_status CHECK (status IN ('DRAFT', 'READY', 'COMPLETED', 'CANCELLED'))
);
CREATE INDEX ix_examination_subject ON examination (subject_id);

CREATE TABLE examination_enrolment (
  examination_id BIGINT NOT NULL REFERENCES examination (id) ON DELETE CASCADE,
  student_id BIGINT NOT NULL REFERENCES student (id) ON DELETE RESTRICT,
  PRIMARY KEY (examination_id, student_id)
);
CREATE INDEX ix_enrolment_student ON examination_enrolment (student_id);

INSERT INTO schema_version (version, script, description)
VALUES (7, 'V007__examinations_and_enrolment.sql', 'Examinations and enrolment');

COMMIT;

-- V008__runs_versions_and_schedule.sql
-- Scheduling runs, stored timetable versions, and the placement of each exam in a slot.
-- A version can only change while it is a DRAFT. The triggers enforce that in the database.

BEGIN;

DO $$
BEGIN
  IF (SELECT max(version) FROM schema_version) <> 7 THEN
    RAISE EXCEPTION 'Expected schema version 7';
  END IF;
END $$;

CREATE TABLE scheduling_run (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  session_id BIGINT NOT NULL REFERENCES exam_session (id) ON DELETE CASCADE,
  kind VARCHAR(20) NOT NULL,
  algorithm VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'RUNNING',
  parameters TEXT NOT NULL,
  report TEXT,
  exams_total INT,
  exams_scheduled INT,
  slots_used INT,
  search_nodes BIGINT,
  backtracks BIGINT,
  duration_ms BIGINT,
  started_by BIGINT NOT NULL REFERENCES app_user (id),
  started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  finished_at TIMESTAMPTZ,
  CONSTRAINT ck_run_kind CHECK (kind IN ('GENERATE', 'REGENERATE', 'REPAIR')),
  CONSTRAINT ck_run_algorithm CHECK (algorithm IN ('DSATUR', 'GREEDY')),
  CONSTRAINT ck_run_status CHECK (status IN ('RUNNING', 'SUCCEEDED', 'PARTIAL', 'FAILED')),
  CONSTRAINT ck_run_parameters_json CHECK (parameters IS JSON),
  CONSTRAINT ck_run_report_json CHECK (report IS JSON)
);
CREATE UNIQUE INDEX uq_run_one_running ON scheduling_run (session_id) WHERE status = 'RUNNING';
CREATE INDEX ix_run_session ON scheduling_run (session_id, started_at DESC);

CREATE TABLE schedule_version (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  session_id BIGINT NOT NULL REFERENCES exam_session (id) ON DELETE RESTRICT,
  version_no INT NOT NULL,
  source_run_id BIGINT REFERENCES scheduling_run (id) ON DELETE SET NULL,
  parent_version_id BIGINT REFERENCES schedule_version (id) ON DELETE SET NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  quality_score NUMERIC(5, 2),
  hard_violations INT NOT NULL DEFAULT 0,
  review_comment VARCHAR(500),
  created_by BIGINT NOT NULL REFERENCES app_user (id),
  reviewed_by BIGINT REFERENCES app_user (id),
  reviewed_at TIMESTAMPTZ,
  published_by BIGINT REFERENCES app_user (id),
  published_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  lock_version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT uq_version_no UNIQUE (session_id, version_no),
  CONSTRAINT ck_version_status CHECK (status IN ('DRAFT', 'REVIEWED', 'PUBLISHED', 'SUPERSEDED')),
  CONSTRAINT ck_version_score CHECK (quality_score BETWEEN 0 AND 100)
);
CREATE UNIQUE INDEX uq_version_one_published ON schedule_version (session_id) WHERE status = 'PUBLISHED';

CREATE TABLE exam_schedule (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  version_id BIGINT NOT NULL REFERENCES schedule_version (id) ON DELETE CASCADE,
  examination_id BIGINT NOT NULL REFERENCES examination (id) ON DELETE RESTRICT,
  time_slot_id BIGINT NOT NULL REFERENCES time_slot (id) ON DELETE RESTRICT,
  source VARCHAR(20) NOT NULL DEFAULT 'AUTO',
  locked BOOLEAN NOT NULL DEFAULT FALSE,
  CONSTRAINT uq_exam_once_per_version UNIQUE (version_id, examination_id),
  CONSTRAINT uq_exam_schedule_target UNIQUE (id, version_id, time_slot_id),
  CONSTRAINT uq_exam_schedule_exam_target UNIQUE (id, examination_id),
  CONSTRAINT ck_exam_schedule_source CHECK (source IN ('AUTO', 'MANUAL', 'REPAIR'))
);
CREATE INDEX ix_exam_schedule_slot ON exam_schedule (version_id, time_slot_id);
CREATE INDEX ix_exam_schedule_exam ON exam_schedule (examination_id);

-- Used by every table that belongs to a version through a version_id column.
CREATE FUNCTION assert_version_is_draft() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
  target_version BIGINT;
BEGIN
  IF TG_OP = 'DELETE' THEN
    target_version := OLD.version_id;
  ELSE
    target_version := NEW.version_id;
  END IF;

  IF EXISTS (SELECT 1 FROM schedule_version WHERE id = target_version AND status <> 'DRAFT') THEN
    RAISE EXCEPTION 'Schedule version % is frozen and cannot change', target_version;
  END IF;

  IF TG_OP = 'DELETE' THEN
    RETURN OLD;
  END IF;
  RETURN NEW;
END $$;

CREATE FUNCTION assert_version_can_be_deleted() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
  IF OLD.status <> 'DRAFT' THEN
    RAISE EXCEPTION 'Schedule version % is frozen and cannot be deleted', OLD.id;
  END IF;
  RETURN OLD;
END $$;

CREATE TRIGGER tg_version_delete BEFORE DELETE ON schedule_version
  FOR EACH ROW EXECUTE FUNCTION assert_version_can_be_deleted();

CREATE TRIGGER tg_exam_schedule_frozen BEFORE INSERT OR UPDATE OR DELETE ON exam_schedule
  FOR EACH ROW EXECUTE FUNCTION assert_version_is_draft();

INSERT INTO schema_version (version, script, description)
VALUES (8, 'V008__runs_versions_and_schedule.sql', 'Scheduling runs, versions and scheduled exams');

COMMIT;

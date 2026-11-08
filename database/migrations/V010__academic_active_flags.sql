-- V010__academic_active_flags.sql
-- Departments and programmes can be switched off without losing their history.
-- An inactive one is hidden from pickers, so nothing new attaches to it, while past
-- batches, subjects and timetables still show it. Existing rows stay active.

BEGIN;

DO $$
BEGIN
  IF (SELECT max(version) FROM schema_version) <> 9 THEN
    RAISE EXCEPTION 'Expected schema version 9';
  END IF;
END $$;

ALTER TABLE department ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE course ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;

INSERT INTO schema_version (version, script, description)
VALUES (10, 'V010__academic_active_flags.sql', 'Active switch for departments and programmes');

COMMIT;

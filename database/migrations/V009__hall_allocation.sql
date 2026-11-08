-- V009__hall_allocation.sql
-- Which halls an exam uses, and which hall each student goes to.
-- version_id and time_slot_id are copied so a plain UNIQUE can stop double-booking.
-- Composite foreign keys guarantee the copies always match their exam_schedule row.

BEGIN;

DO $$
BEGIN
  IF (SELECT max(version) FROM schema_version) <> 8 THEN
    RAISE EXCEPTION 'Expected schema version 8';
  END IF;
END $$;

CREATE TABLE hall_allocation (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  exam_schedule_id BIGINT NOT NULL,
  version_id BIGINT NOT NULL,
  time_slot_id BIGINT NOT NULL,
  hall_id BIGINT NOT NULL REFERENCES exam_hall (id) ON DELETE RESTRICT,
  seats_allocated INT NOT NULL,
  capacity_snapshot INT NOT NULL,
  CONSTRAINT fk_hall_alloc_schedule FOREIGN KEY (exam_schedule_id, version_id, time_slot_id)
    REFERENCES exam_schedule (id, version_id, time_slot_id) ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT uq_hall_no_double_booking UNIQUE (version_id, time_slot_id, hall_id),
  CONSTRAINT uq_hall_alloc_slot_target UNIQUE (id, version_id, time_slot_id),
  CONSTRAINT uq_hall_alloc_exam_target UNIQUE (id, exam_schedule_id),
  CONSTRAINT ck_hall_alloc_seats CHECK (seats_allocated > 0),
  CONSTRAINT ck_hall_alloc_capacity CHECK (seats_allocated <= capacity_snapshot)
);
CREATE INDEX ix_hall_alloc_exam ON hall_allocation (exam_schedule_id);
CREATE INDEX ix_hall_alloc_hall ON hall_allocation (hall_id);

CREATE TABLE student_hall_assignment (
  exam_schedule_id BIGINT NOT NULL,
  examination_id BIGINT NOT NULL,
  hall_allocation_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  PRIMARY KEY (exam_schedule_id, student_id),
  CONSTRAINT fk_sha_alloc FOREIGN KEY (hall_allocation_id, exam_schedule_id)
    REFERENCES hall_allocation (id, exam_schedule_id) ON DELETE CASCADE,
  CONSTRAINT fk_sha_schedule FOREIGN KEY (exam_schedule_id, examination_id)
    REFERENCES exam_schedule (id, examination_id) ON DELETE CASCADE,
  CONSTRAINT fk_sha_enrolment FOREIGN KEY (examination_id, student_id)
    REFERENCES examination_enrolment (examination_id, student_id) ON DELETE RESTRICT
);
CREATE INDEX ix_sha_alloc ON student_hall_assignment (hall_allocation_id);
CREATE INDEX ix_sha_student ON student_hall_assignment (student_id);
CREATE INDEX ix_sha_enrolment ON student_hall_assignment (examination_id, student_id);

-- This table has no version_id, so it finds the version through its exam_schedule row.
CREATE FUNCTION assert_placement_is_draft() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
  target_schedule BIGINT;
BEGIN
  IF TG_OP = 'DELETE' THEN
    target_schedule := OLD.exam_schedule_id;
  ELSE
    target_schedule := NEW.exam_schedule_id;
  END IF;

  IF EXISTS (
    SELECT 1
    FROM exam_schedule e
    JOIN schedule_version v ON v.id = e.version_id
    WHERE e.id = target_schedule AND v.status <> 'DRAFT'
  ) THEN
    RAISE EXCEPTION 'The version of exam schedule % is frozen and cannot change', target_schedule;
  END IF;

  IF TG_OP = 'DELETE' THEN
    RETURN OLD;
  END IF;
  RETURN NEW;
END $$;

CREATE TRIGGER tg_hall_allocation_frozen BEFORE INSERT OR UPDATE OR DELETE ON hall_allocation
  FOR EACH ROW EXECUTE FUNCTION assert_version_is_draft();

CREATE TRIGGER tg_student_hall_frozen BEFORE INSERT OR UPDATE OR DELETE ON student_hall_assignment
  FOR EACH ROW EXECUTE FUNCTION assert_placement_is_draft();

INSERT INTO schema_version (version, script, description)
VALUES (9, 'V009__hall_allocation.sql', 'Hall allocation and student placement');

COMMIT;

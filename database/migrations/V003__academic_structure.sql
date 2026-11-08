-- V003__academic_structure.sql
-- Departments, courses, curricula, batches, subjects and where subjects sit in a curriculum.

BEGIN;

DO $$
BEGIN
  IF (SELECT max(version) FROM schema_version) <> 2 THEN
    RAISE EXCEPTION 'Expected schema version 2';
  END IF;
END $$;

CREATE TABLE department (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  code VARCHAR(20) NOT NULL,
  name VARCHAR(150) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_department_code UNIQUE (code),
  CONSTRAINT uq_department_name UNIQUE (name)
);

CREATE TABLE course (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  department_id BIGINT NOT NULL REFERENCES department (id) ON DELETE RESTRICT,
  code VARCHAR(20) NOT NULL,
  name VARCHAR(150) NOT NULL,
  total_semesters INT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_course_code UNIQUE (code),
  CONSTRAINT ck_course_semesters CHECK (total_semesters BETWEEN 1 AND 12)
);
CREATE INDEX ix_course_department ON course (department_id);

CREATE TABLE curriculum (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  course_id BIGINT NOT NULL REFERENCES course (id) ON DELETE RESTRICT,
  name VARCHAR(80) NOT NULL,
  effective_from_year INT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_curriculum_course_name UNIQUE (course_id, name),
  CONSTRAINT ck_curriculum_year CHECK (effective_from_year BETWEEN 2000 AND 2100)
);

CREATE TABLE batch (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  curriculum_id BIGINT NOT NULL REFERENCES curriculum (id) ON DELETE RESTRICT,
  name VARCHAR(50) NOT NULL,
  admission_year INT NOT NULL,
  current_semester INT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_batch_curriculum_name UNIQUE (curriculum_id, name),
  CONSTRAINT ck_batch_year CHECK (admission_year BETWEEN 2000 AND 2100),
  CONSTRAINT ck_batch_semester CHECK (current_semester > 0)
);
CREATE INDEX ix_batch_curriculum ON batch (curriculum_id);

CREATE TABLE subject (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  department_id BIGINT NOT NULL REFERENCES department (id) ON DELETE RESTRICT,
  code VARCHAR(20) NOT NULL,
  name VARCHAR(150) NOT NULL,
  is_heavy BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_subject_code UNIQUE (code)
);
CREATE INDEX ix_subject_department ON subject (department_id);

CREATE TABLE curriculum_subject (
  curriculum_id BIGINT NOT NULL REFERENCES curriculum (id) ON DELETE CASCADE,
  subject_id BIGINT NOT NULL REFERENCES subject (id) ON DELETE RESTRICT,
  semester_no INT NOT NULL,
  is_elective BOOLEAN NOT NULL DEFAULT FALSE,
  PRIMARY KEY (curriculum_id, subject_id),
  CONSTRAINT ck_curriculum_subject_semester CHECK (semester_no > 0)
);
CREATE INDEX ix_curriculum_subject_subject ON curriculum_subject (subject_id);

INSERT INTO schema_version (version, script, description)
VALUES (3, 'V003__academic_structure.sql', 'Academic structure and curricula');

COMMIT;

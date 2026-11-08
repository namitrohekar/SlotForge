-- V001__schema_version.sql
--
-- Creates the schema_version record and enables the btree_gist extension.
-- Run the whole file in the pgAdmin Query Tool, on the slotforge database, as role slotforge.
-- It runs in one transaction, so if anything fails, nothing changes.
-- There is no earlier version to check. Running it twice fails safely, because the table exists.

BEGIN;

CREATE TABLE schema_version (
  version INT PRIMARY KEY,
  script VARCHAR(120) NOT NULL,
  description VARCHAR(255) NOT NULL,
  applied_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  applied_by VARCHAR(63) NOT NULL DEFAULT current_user
);

-- Needed later by the constraints that prevent overlapping time periods.
CREATE EXTENSION IF NOT EXISTS btree_gist;

INSERT INTO schema_version (version, script, description)
VALUES (1, 'V001__schema_version.sql', 'Schema version record and btree_gist extension');

COMMIT;

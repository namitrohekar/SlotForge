-- S004__vjti_batches.sql
--
-- Demo data: batches in the odd semester of 2026-27. First-year batches are in semester 1,
-- and the MCA batch admitted in 2025 is in semester 3.
-- Needs S003. Safe to run more than once.
-- Run the whole file in the pgAdmin Query Tool, on the slotforge database, as role slotforge.

BEGIN;

INSERT INTO batch (curriculum_id, name, admission_year, current_semester)
SELECT cu.id, v.name, v.year, v.semester
FROM (VALUES
  ('MCA', '2025-27', 2025, 3),
  ('MCA', '2026-28', 2026, 1),
  ('BT-CE', '2026-30', 2026, 1)
) AS v (course_code, name, year, semester)
JOIN course c ON c.code = v.course_code
JOIN curriculum cu ON cu.course_id = c.id AND cu.name = '2024 scheme'
ON CONFLICT ON CONSTRAINT uq_batch_curriculum_name DO NOTHING;

COMMIT;

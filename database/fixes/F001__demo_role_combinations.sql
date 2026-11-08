-- F001__demo_role_combinations.sql
--
-- Data fix only: no tables, columns or constraints change, so schema_version stays at 9.
-- Puts each S001 demo account back to exactly the one role S001 gives it, removing roles
-- added through the Users page before the role compatibility rules existed.
-- Account status is not touched. Safe to run more than once.
-- Run the whole file in the pgAdmin Query Tool, on the slotforge database, as role slotforge.

BEGIN;

WITH seeded (email, role) AS (
  VALUES
    ('admin@demo.slotforge.test', 'ADMIN'),
    ('coordinator@demo.slotforge.test', 'EXAM_COORDINATOR'),
    ('hod@demo.slotforge.test', 'HOD'),
    ('faculty@demo.slotforge.test', 'FACULTY'),
    ('invigilator@demo.slotforge.test', 'INVIGILATOR'),
    ('student@demo.slotforge.test', 'STUDENT')
)
DELETE FROM user_role ur
USING app_user u
WHERE ur.user_id = u.id
  AND u.email IN (SELECT email FROM seeded)
  AND (u.email, ur.role) NOT IN (SELECT email, role FROM seeded);

-- Restores the seeded role if it was removed.
INSERT INTO user_role (user_id, role)
SELECT u.id, s.role
FROM (VALUES
    ('admin@demo.slotforge.test', 'ADMIN'),
    ('coordinator@demo.slotforge.test', 'EXAM_COORDINATOR'),
    ('hod@demo.slotforge.test', 'HOD'),
    ('faculty@demo.slotforge.test', 'FACULTY'),
    ('invigilator@demo.slotforge.test', 'INVIGILATOR'),
    ('student@demo.slotforge.test', 'STUDENT')
) AS s (email, role)
JOIN app_user u ON u.email = s.email
ON CONFLICT (user_id, role) DO NOTHING;

COMMIT;

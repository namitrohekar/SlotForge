-- S001__demo_users.sql
-- One demo account per role, for the demo login. Needs V002. Safe to run more than once.
-- The .test domain is reserved, so these can never match a real Google account.

BEGIN;

INSERT INTO app_user (email, full_name) VALUES
  ('admin@demo.slotforge.test', 'Demo Administrator'),
  ('coordinator@demo.slotforge.test', 'Demo Coordinator'),
  ('hod@demo.slotforge.test', 'Demo Head of Department'),
  ('faculty@demo.slotforge.test', 'Demo Faculty'),
  ('invigilator@demo.slotforge.test', 'Demo Invigilator'),
  ('student@demo.slotforge.test', 'Demo Student')
ON CONFLICT (email) DO NOTHING;

INSERT INTO user_role (user_id, role)
SELECT u.id, r.role
FROM (VALUES
  ('admin@demo.slotforge.test', 'ADMIN'),
  ('coordinator@demo.slotforge.test', 'EXAM_COORDINATOR'),
  ('hod@demo.slotforge.test', 'HOD'),
  ('faculty@demo.slotforge.test', 'FACULTY'),
  ('invigilator@demo.slotforge.test', 'INVIGILATOR'),
  ('student@demo.slotforge.test', 'STUDENT')
) AS r (email, role)
JOIN app_user u ON u.email = r.email
ON CONFLICT (user_id, role) DO NOTHING;

COMMIT;

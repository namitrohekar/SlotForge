-- S002__vjti_departments_and_programmes.sql
--
-- Demo data: VJTI's twelve academic departments and a representative set of its programmes.
-- Department names follow VJTI's own list. Mathematics, Physics, and Humanities & Management
-- teach first-year and foundation subjects but run no programme of their own.
-- Programme lengths: B.Tech 8 semesters, Diploma 6, M.Tech and MCA 4.
-- Needs V003. Safe to run more than once: existing codes are left alone.
-- Run the whole file in the pgAdmin Query Tool, on the slotforge database, as role slotforge.

BEGIN;

INSERT INTO department (code, name) VALUES
  ('CIVIL', 'Civil & Environmental Engineering'),
  ('STRUCT', 'Structural Engineering'),
  ('CEIT', 'Computer Engineering & Information Technology'),
  ('MCA', 'Master in Computer Applications'),
  ('ELEC', 'Electrical Engineering'),
  ('MECH', 'Mechanical Engineering'),
  ('PROD', 'Production Engineering'),
  ('TEXT', 'Textile Engineering'),
  ('CHEM', 'Technical & Applied Chemistry'),
  ('MATH', 'Mathematics'),
  ('PHY', 'Physics'),
  ('HUM', 'Humanities & Management')
ON CONFLICT (code) DO NOTHING;

INSERT INTO course (department_id, code, name, total_semesters)
SELECT d.id, c.code, c.name, c.semesters
FROM (VALUES
  ('CEIT', 'BT-CE', 'B.Tech Computer Engineering', 8),
  ('CEIT', 'BT-IT', 'B.Tech Information Technology', 8),
  ('CIVIL', 'BT-CIVIL', 'B.Tech Civil Engineering', 8),
  ('ELEC', 'BT-EE', 'B.Tech Electrical Engineering', 8),
  ('ELEC', 'BT-ELN', 'B.Tech Electronics Engineering', 8),
  ('ELEC', 'BT-EXTC', 'B.Tech Electronics & Telecommunication Engineering', 8),
  ('MECH', 'BT-MECH', 'B.Tech Mechanical Engineering', 8),
  ('PROD', 'BT-PROD', 'B.Tech Production Engineering', 8),
  ('TEXT', 'BT-TEXT', 'B.Tech Textile Technology', 8),
  ('MCA', 'MCA', 'Master of Computer Applications', 4),
  ('CEIT', 'MT-CE', 'M.Tech Computer Engineering', 4),
  ('STRUCT', 'MT-STRUCT', 'M.Tech Structural Engineering', 4),
  ('ELEC', 'MT-CS', 'M.Tech Control Systems', 4),
  ('MECH', 'MT-MD', 'M.Tech Machine Design', 4),
  ('CIVIL', 'DIP-CIVIL', 'Diploma in Civil Engineering', 6),
  ('ELEC', 'DIP-EE', 'Diploma in Electrical Engineering', 6),
  ('MECH', 'DIP-MECH', 'Diploma in Mechanical Engineering', 6)
) AS c (department_code, code, name, semesters)
JOIN department d ON d.code = c.department_code
ON CONFLICT (code) DO NOTHING;

COMMIT;

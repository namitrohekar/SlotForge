-- S003__vjti_subjects_and_curricula.sql
--
-- Demo data: an MCA curriculum laid out over its four semesters, and the first semester of
-- B.Tech Computer Engineering. Subjects belong to the department that teaches them, so the
-- mathematics papers sit under Mathematics, as at VJTI. Subject codes are illustrative.
-- Heavy papers are the demanding ones that need a rest day between them for the same student.
-- Needs V003 and S002. Safe to run more than once.
-- Run the whole file in the pgAdmin Query Tool, on the slotforge database, as role slotforge.

BEGIN;

INSERT INTO subject (department_id, code, name, is_heavy)
SELECT d.id, s.code, s.name, s.heavy
FROM (VALUES
  ('MATH', 'MCA101', 'Mathematical Foundations for Computer Science', TRUE),
  ('MCA', 'MCA102', 'Data Structures and Algorithms', TRUE),
  ('MCA', 'MCA103', 'Object Oriented Programming with Java', FALSE),
  ('MCA', 'MCA104', 'Database Management Systems', FALSE),
  ('MCA', 'MCA105', 'Computer Networks', FALSE),
  ('MCA', 'MCA201', 'Design and Analysis of Algorithms', TRUE),
  ('MCA', 'MCA202', 'Operating Systems', FALSE),
  ('MCA', 'MCA203', 'Software Engineering', FALSE),
  ('MCA', 'MCA204', 'Web Technologies', FALSE),
  ('MATH', 'MCA205', 'Probability and Statistics', TRUE),
  ('MCA', 'MCA301', 'Machine Learning', TRUE),
  ('MCA', 'MCA302', 'Cloud Computing', FALSE),
  ('MCA', 'MCA303', 'Information Security', FALSE),
  ('MCA', 'MCA311', 'Big Data Analytics', FALSE),
  ('MCA', 'MCA312', 'Mobile Application Development', FALSE),
  ('MCA', 'MCA401', 'Industry Internship and Project', FALSE),
  ('MATH', 'MA101', 'Engineering Mathematics I', TRUE),
  ('PHY', 'PH101', 'Engineering Physics', FALSE),
  ('CHEM', 'CH101', 'Engineering Chemistry', FALSE),
  ('CEIT', 'CE101', 'Programming for Problem Solving', FALSE),
  ('HUM', 'HM101', 'Communication Skills', FALSE)
) AS s (department_code, code, name, heavy)
JOIN department d ON d.code = s.department_code
ON CONFLICT (code) DO NOTHING;

INSERT INTO curriculum (course_id, name, effective_from_year)
SELECT c.id, v.name, v.year
FROM (VALUES ('MCA', '2024 scheme', 2024), ('BT-CE', '2024 scheme', 2024)) AS v (course_code, name, year)
JOIN course c ON c.code = v.course_code
ON CONFLICT ON CONSTRAINT uq_curriculum_course_name DO NOTHING;

INSERT INTO curriculum_subject (curriculum_id, subject_id, semester_no, is_elective)
SELECT cu.id, s.id, p.semester, p.elective
FROM (VALUES
  ('MCA', 'MCA101', 1, FALSE), ('MCA', 'MCA102', 1, FALSE), ('MCA', 'MCA103', 1, FALSE),
  ('MCA', 'MCA104', 1, FALSE), ('MCA', 'MCA105', 1, FALSE),
  ('MCA', 'MCA201', 2, FALSE), ('MCA', 'MCA202', 2, FALSE), ('MCA', 'MCA203', 2, FALSE),
  ('MCA', 'MCA204', 2, FALSE), ('MCA', 'MCA205', 2, FALSE),
  ('MCA', 'MCA301', 3, FALSE), ('MCA', 'MCA302', 3, FALSE), ('MCA', 'MCA303', 3, FALSE),
  ('MCA', 'MCA311', 3, TRUE), ('MCA', 'MCA312', 3, TRUE),
  ('MCA', 'MCA401', 4, FALSE),
  ('BT-CE', 'MA101', 1, FALSE), ('BT-CE', 'PH101', 1, FALSE), ('BT-CE', 'CH101', 1, FALSE),
  ('BT-CE', 'CE101', 1, FALSE), ('BT-CE', 'HM101', 1, FALSE)
) AS p (course_code, subject_code, semester, elective)
JOIN course c ON c.code = p.course_code
JOIN curriculum cu ON cu.course_id = c.id AND cu.name = '2024 scheme'
JOIN subject s ON s.code = p.subject_code
ON CONFLICT (curriculum_id, subject_id) DO NOTHING;

COMMIT;

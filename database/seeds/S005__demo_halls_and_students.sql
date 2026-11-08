-- S005__demo_halls_and_students.sql
--
-- Demo data for running the scheduler: ten halls and 195 students across the three S004 batches.
-- Hall names, seat counts and student names are illustrative. Student emails use the reserved .test domain.
-- Needs S004. Safe to run more than once.
-- Run the whole file in the pgAdmin Query Tool, on the slotforge database, as role slotforge.

BEGIN;

INSERT INTO exam_hall (code, name, building, floor, capacity) VALUES
  ('MB-101', 'Main Building 101', 'Main Building', 1, 60),
  ('MB-102', 'Main Building 102', 'Main Building', 1, 60),
  ('MB-201', 'Main Building 201', 'Main Building', 2, 40),
  ('MB-202', 'Main Building 202', 'Main Building', 2, 40),
  ('CE-LH1', 'Computer Engineering Lecture Hall 1', 'Computer Engineering Building', 1, 80),
  ('CE-LH2', 'Computer Engineering Lecture Hall 2', 'Computer Engineering Building', 1, 80),
  ('MCA-301', 'MCA Classroom 301', 'MCA Department', 3, 45),
  ('MCA-302', 'MCA Classroom 302', 'MCA Department', 3, 45),
  ('EE-LH1', 'Electrical Lecture Hall 1', 'Electrical Building', 0, 100),
  ('AUD-1', 'Auditorium Examination Hall', 'Main Building', 0, 120)
ON CONFLICT (code) DO NOTHING;

WITH names AS (
  SELECT ARRAY['Aarav','Ishita','Rohan','Sneha','Vivaan','Ananya','Kunal','Priya','Aditya','Meera',
               'Siddharth','Pooja','Nikhil','Riya','Omkar','Tanvi','Yash','Shruti','Harsh','Neha'] AS first_names,
         ARRAY['Patil','Kulkarni','Deshmukh','Joshi','Sawant','Shinde','Iyer','Nair','Rao','Shah',
               'Mehta','Gupta','Sharma','Pawar','Kadam','Naik','Bhosale','Chavan'] AS last_names
), cohorts (course_code, batch_name, prefix, headcount) AS (
  VALUES ('MCA', '2025-27', '25MCA', 60),
         ('MCA', '2026-28', '26MCA', 60),
         ('BT-CE', '2026-30', '26BCE', 75)
)
INSERT INTO student (batch_id, roll_no, full_name, email)
SELECT b.id,
       co.prefix || lpad(n::text, 3, '0'),
       first_names[1 + (n - 1) % array_length(first_names, 1)] || ' ' || last_names[1 + (n * 7) % array_length(last_names, 1)],
       lower(co.prefix) || lpad(n::text, 3, '0') || '@demo.slotforge.test'
FROM cohorts co
JOIN course c ON c.code = co.course_code
JOIN curriculum cu ON cu.course_id = c.id AND cu.name = '2024 scheme'
JOIN batch b ON b.curriculum_id = cu.id AND b.name = co.batch_name
CROSS JOIN names
CROSS JOIN LATERAL generate_series(1, co.headcount) AS n
ON CONFLICT (roll_no) DO NOTHING;

COMMIT;

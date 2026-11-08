-- S003__verify.sql
--
-- SELECT-only: this changes nothing. Run on the slotforge database after S003.
-- Every row should say OK. Counts are "at least", because you may have added more by hand.

SELECT check_name,
       actual,
       expected,
       CASE WHEN actual >= expected THEN 'OK' ELSE 'CHECK' END AS status
FROM (
  VALUES
    ('subjects', (SELECT count(*) FROM subject), 21),
    ('heavy subjects', (SELECT count(*) FROM subject WHERE is_heavy), 6),
    ('curricula', (SELECT count(*) FROM curriculum), 2),
    ('MCA placements', (SELECT count(*) FROM curriculum_subject cs JOIN curriculum cu ON cu.id = cs.curriculum_id
                        JOIN course c ON c.id = cu.course_id WHERE c.code = 'MCA'), 16),
    ('MCA electives', (SELECT count(*) FROM curriculum_subject cs JOIN curriculum cu ON cu.id = cs.curriculum_id
                       JOIN course c ON c.id = cu.course_id WHERE c.code = 'MCA' AND cs.is_elective), 2),
    ('placements beyond their programme''s semesters',
     -(SELECT count(*) FROM curriculum_subject cs JOIN curriculum cu ON cu.id = cs.curriculum_id
       JOIN course c ON c.id = cu.course_id WHERE cs.semester_no > c.total_semesters), 0)
) AS checks (check_name, actual, expected);

-- The MCA syllabus, semester by semester, for a visual check.
SELECT cs.semester_no, s.code, s.name, cs.is_elective, s.is_heavy, d.code AS taught_by
FROM curriculum_subject cs
JOIN curriculum cu ON cu.id = cs.curriculum_id
JOIN course c ON c.id = cu.course_id
JOIN subject s ON s.id = cs.subject_id
JOIN department d ON d.id = s.department_id
WHERE c.code = 'MCA'
ORDER BY cs.semester_no, cs.is_elective, s.code;

-- S002__verify.sql
--
-- SELECT-only: this changes nothing. Run on the slotforge database after S002.
-- Every row should say OK. Counts are "at least", because you may have added more by hand.

SELECT check_name,
       actual,
       expected,
       CASE WHEN actual >= expected THEN 'OK' ELSE 'CHECK' END AS status
FROM (
  VALUES
    ('departments', (SELECT count(*) FROM department), 12),
    ('programmes', (SELECT count(*) FROM course), 17),
    ('B.Tech programmes with 8 semesters',
     (SELECT count(*) FROM course WHERE code LIKE 'BT-%' AND total_semesters = 8), 9),
    ('MCA under its own department',
     (SELECT count(*) FROM course c JOIN department d ON d.id = c.department_id
      WHERE c.code = 'MCA' AND d.code = 'MCA' AND c.total_semesters = 4), 1)
) AS checks (check_name, actual, expected);

-- Programmes per department, for a visual check. Mathematics, Physics and HUM show 0.
SELECT d.code, d.name, count(c.id) AS programmes
FROM department d
LEFT JOIN course c ON c.department_id = d.id
GROUP BY d.id, d.code, d.name
ORDER BY d.code;

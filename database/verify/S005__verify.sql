-- S005__verify.sql
--
-- SELECT-only: this changes nothing. Run on the slotforge database after S005.
-- Expected: at least 10 halls, then 60, 60 and 75 students in the three batches.

SELECT count(*) AS halls, sum(capacity) AS exam_seats FROM exam_hall;

SELECT c.code AS programme, b.name AS batch, count(s.id) AS students
FROM batch b
JOIN curriculum cu ON cu.id = b.curriculum_id
JOIN course c ON c.id = cu.course_id
LEFT JOIN student s ON s.batch_id = b.id
GROUP BY c.code, b.name
ORDER BY c.code, b.name;

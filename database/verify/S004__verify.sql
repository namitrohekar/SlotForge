-- S004__verify.sql
--
-- SELECT-only: this changes nothing. Run on the slotforge database after S004.
-- Expected: the three demo batches, each with within_programme = true.

SELECT c.code AS programme, b.name, b.admission_year, b.current_semester, c.total_semesters,
       b.current_semester <= c.total_semesters AS within_programme
FROM batch b
JOIN curriculum cu ON cu.id = b.curriculum_id
JOIN course c ON c.id = cu.course_id
ORDER BY c.code, b.name;

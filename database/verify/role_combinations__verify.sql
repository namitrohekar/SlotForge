-- role_combinations__verify.sql
--
-- SELECT-only: this changes nothing. Run on the slotforge database.
-- Lists every account whose roles break the compatibility rules in RoleRules.java.
-- Expected result: no rows.

SELECT u.id,
       u.email,
       u.status,
       string_agg(r.role, ', ' ORDER BY r.role) AS roles
FROM app_user u
JOIN user_role r ON r.user_id = u.id
GROUP BY u.id, u.email, u.status
HAVING (bool_or(r.role = 'STUDENT') AND count(*) > 1)
    OR count(*) FILTER (WHERE r.role IN ('FACULTY', 'HOD', 'INVIGILATOR')) > 1
    OR count(*) FILTER (WHERE r.role IN ('ADMIN', 'EXAM_COORDINATOR')) > 1
    OR (bool_or(r.role = 'HOD') AND bool_or(r.role IN ('ADMIN', 'EXAM_COORDINATOR')))
ORDER BY u.id;

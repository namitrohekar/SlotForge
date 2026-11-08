-- F002__verify.sql
--
-- SELECT-only: this changes nothing. Run on the slotforge database AFTER F002.
-- Every row should say OK.

SELECT check_name,
       actual,
       expected,
       CASE WHEN actual IS NOT DISTINCT FROM expected THEN 'OK' ELSE 'CHECK' END AS status
FROM (
  VALUES
    ('test@gmail.com is deactivated',
     (SELECT status::text FROM app_user WHERE id = 7 AND email = 'test@gmail.com'),
     'DEACTIVATED'),
    ('test@gmail.com holds no roles',
     (SELECT count(*)::text FROM user_role WHERE user_id = 7),
     '0'),
    ('the correction is in the audit log',
     (SELECT count(*)::text FROM audit_log WHERE entity_id = '7' AND action = 'USER_ROLES_CLEARED'),
     '1'),
    ('an active administrator remains',
     (SELECT (count(*) > 0)::text FROM app_user u JOIN user_role r ON r.user_id = u.id
      WHERE r.role = 'ADMIN' AND u.status = 'ACTIVE'),
     'true'),
    ('accounts breaking the role rules',
     (SELECT count(*)::text FROM (
        SELECT u.id
        FROM app_user u
        JOIN user_role r ON r.user_id = u.id
        GROUP BY u.id
        HAVING (bool_or(r.role = 'STUDENT') AND count(*) > 1)
            OR count(*) FILTER (WHERE r.role IN ('FACULTY', 'HOD', 'INVIGILATOR')) > 1
            OR count(*) FILTER (WHERE r.role IN ('ADMIN', 'EXAM_COORDINATOR')) > 1
            OR (bool_or(r.role = 'HOD') AND bool_or(r.role IN ('ADMIN', 'EXAM_COORDINATOR')))
      ) AS broken),
     '0')
) AS checks (check_name, actual, expected);

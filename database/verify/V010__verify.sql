-- V010__verify.sql
--
-- SELECT-only: this changes nothing.
-- Run after V010__academic_active_flags.sql, in the pgAdmin Query Tool on the slotforge database.
-- Every row should say OK.

SELECT check_name,
       actual,
       expected,
       CASE WHEN actual = expected THEN 'OK' ELSE 'CHECK' END AS status
FROM (
  VALUES
    ('schema version', (SELECT max(version)::text FROM schema_version), '10'),
    ('department.active',
     (SELECT data_type || ' ' || is_nullable || ' ' || column_default FROM information_schema.columns
      WHERE table_name = 'department' AND column_name = 'active'),
     'boolean NO true'),
    ('course.active',
     (SELECT data_type || ' ' || is_nullable || ' ' || column_default FROM information_schema.columns
      WHERE table_name = 'course' AND column_name = 'active'),
     'boolean NO true'),
    ('inactive departments right after V010', (SELECT count(*)::text FROM department WHERE NOT active), '0'),
    ('inactive programmes right after V010', (SELECT count(*)::text FROM course WHERE NOT active), '0')
) AS checks (check_name, actual, expected);

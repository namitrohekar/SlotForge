-- V002_to_V009__verify.sql
--
-- SELECT-only: this changes nothing.
-- Run after V002 to V009, in the pgAdmin Query Tool on the slotforge database.
-- Every row should say OK.

SELECT check_name,
       actual,
       expected,
       CASE WHEN passed THEN 'OK' ELSE 'CHECK' END AS status
FROM (
  VALUES
    (1, 'Latest schema version',
        (SELECT max(version)::text FROM schema_version),
        '9',
        (SELECT max(version) FROM schema_version) = 9),

    (2, 'Rows in schema_version',
        (SELECT count(*)::text FROM schema_version),
        '9',
        (SELECT count(*) FROM schema_version) = 9),

    (3, 'Tables in schema public',
        (SELECT count(*)::text FROM pg_tables WHERE schemaname = 'public'),
        '22',
        (SELECT count(*) FROM pg_tables WHERE schemaname = 'public') = 22),

    (4, 'Tables not owned by slotforge',
        (SELECT count(*)::text FROM pg_tables WHERE schemaname = 'public' AND tableowner <> 'slotforge'),
        '0',
        (SELECT count(*) FROM pg_tables WHERE schemaname = 'public' AND tableowner <> 'slotforge') = 0),

    (5, 'Missing tables',
        COALESCE((SELECT string_agg(name, ', ')
                  FROM unnest(ARRAY['app_user', 'user_role', 'audit_log', 'department', 'course', 'curriculum',
                                    'batch', 'subject', 'curriculum_subject', 'student', 'exam_hall',
                                    'hall_unavailability', 'exam_session', 'time_slot', 'examination',
                                    'examination_enrolment', 'scheduling_run', 'schedule_version', 'exam_schedule',
                                    'hall_allocation', 'student_hall_assignment']) AS name
                  WHERE to_regclass('public.' || name) IS NULL), 'none'),
        'none',
        NOT EXISTS (SELECT 1
                    FROM unnest(ARRAY['app_user', 'user_role', 'audit_log', 'department', 'course', 'curriculum',
                                      'batch', 'subject', 'curriculum_subject', 'student', 'exam_hall',
                                      'hall_unavailability', 'exam_session', 'time_slot', 'examination',
                                      'examination_enrolment', 'scheduling_run', 'schedule_version', 'exam_schedule',
                                      'hall_allocation', 'student_hall_assignment']) AS name
                    WHERE to_regclass('public.' || name) IS NULL)),

    (6, 'Key constraints present',
        (SELECT count(*)::text FROM pg_constraint
         WHERE conname IN ('ex_time_slot_overlap', 'uq_hall_no_double_booking', 'ck_hall_alloc_capacity',
                           'fk_hall_alloc_schedule', 'fk_sha_enrolment', 'fk_sha_schedule')),
        '6',
        (SELECT count(*) FROM pg_constraint
         WHERE conname IN ('ex_time_slot_overlap', 'uq_hall_no_double_booking', 'ck_hall_alloc_capacity',
                           'fk_hall_alloc_schedule', 'fk_sha_enrolment', 'fk_sha_schedule')) = 6),

    (7, 'Single-row rule indexes present',
        (SELECT count(*)::text FROM pg_indexes
         WHERE indexname IN ('uq_run_one_running', 'uq_version_one_published')),
        '2',
        (SELECT count(*) FROM pg_indexes
         WHERE indexname IN ('uq_run_one_running', 'uq_version_one_published')) = 2),

    (8, 'History protection triggers',
        (SELECT count(*)::text FROM pg_trigger WHERE NOT tgisinternal),
        '4',
        (SELECT count(*) FROM pg_trigger WHERE NOT tgisinternal) = 4)
) AS checks (step, check_name, actual, expected, passed)
ORDER BY step;

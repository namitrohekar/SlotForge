-- V001__verify.sql
--
-- SELECT-only: this changes nothing.
-- Run after V001__schema_version.sql, in the pgAdmin Query Tool on the slotforge database.
-- Every row should say OK.
-- An error saying schema_version does not exist means V001 did not apply.

SELECT check_name,
       actual,
       expected,
       CASE WHEN passed THEN 'OK' ELSE 'CHECK' END AS status
FROM (
  VALUES
    (1, 'Latest schema version',
        (SELECT max(version)::text FROM schema_version),
        '1',
        (SELECT max(version) FROM schema_version) = 1),

    (2, 'Rows in schema_version',
        (SELECT count(*)::text FROM schema_version),
        '1',
        (SELECT count(*) FROM schema_version) = 1),

    (3, 'Recorded script',
        (SELECT script::text FROM schema_version WHERE version = 1),
        'V001__schema_version.sql',
        (SELECT script::text FROM schema_version WHERE version = 1) = 'V001__schema_version.sql'),

    (4, 'Applied by',
        (SELECT applied_by::text FROM schema_version WHERE version = 1),
        'slotforge',
        (SELECT applied_by::text FROM schema_version WHERE version = 1) = 'slotforge'),

    (5, 'Table owner',
        (SELECT tableowner::text
         FROM pg_tables
         WHERE schemaname = 'public' AND tablename = 'schema_version'),
        'slotforge',
        (SELECT tableowner::text
         FROM pg_tables
         WHERE schemaname = 'public' AND tablename = 'schema_version') = 'slotforge'),

    (6, 'Primary keys on schema_version',
        (SELECT count(*)::text
         FROM pg_constraint
         WHERE conrelid = 'public.schema_version'::regclass AND contype = 'p'),
        '1',
        (SELECT count(*)
         FROM pg_constraint
         WHERE conrelid = 'public.schema_version'::regclass AND contype = 'p') = 1),

    (7, 'btree_gist version installed',
        COALESCE((SELECT extversion::text
                  FROM pg_extension
                  WHERE extname = 'btree_gist'), 'not installed'),
        '1.8',
        (SELECT extversion::text
         FROM pg_extension
         WHERE extname = 'btree_gist') = '1.8'),

    (8, 'Tables in schema public',
        (SELECT count(*)::text FROM pg_tables WHERE schemaname = 'public'),
        '1',
        (SELECT count(*) FROM pg_tables WHERE schemaname = 'public') = 1)
) AS checks (step, check_name, actual, expected, passed)
ORDER BY step;

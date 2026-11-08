-- V000__environment_check.sql
--
-- SELECT-only: this changes nothing.
-- Run in the pgAdmin Query Tool, connected to the slotforge database as role slotforge.
-- It is a single statement because the Query Tool shows only the last result.
-- Every row should say OK or INFO. A row saying CHECK needs attention before V001.

SELECT check_name,
       actual,
       expected,
       CASE
         WHEN passed IS NULL THEN 'INFO'
         WHEN passed THEN 'OK'
         ELSE 'CHECK'
       END AS status
FROM (
  VALUES
    (1, 'Connected role',
        current_user::text,
        'slotforge',
        current_user::text = 'slotforge'),

    (2, 'Connected database',
        current_database()::text,
        'slotforge',
        current_database()::text = 'slotforge'),

    (3, 'PostgreSQL version',
        current_setting('server_version'),
        '16 or later',
        current_setting('server_version_num')::int >= 160000),

    (4, 'Database owner',
        (SELECT pg_get_userbyid(datdba)::text
         FROM pg_database
         WHERE datname = current_database()),
        'slotforge',
        (SELECT pg_get_userbyid(datdba)::text
         FROM pg_database
         WHERE datname = current_database()) = 'slotforge'),

    (5, 'Can create tables in schema public',
        has_schema_privilege('public', 'CREATE')::text,
        'true',
        has_schema_privilege('public', 'CREATE')),

    (6, 'Can add extensions to this database',
        has_database_privilege(current_database()::text, 'CREATE')::text,
        'true',
        has_database_privilege(current_database()::text, 'CREATE')),

    (7, 'btree_gist extension available',
        COALESCE((SELECT default_version
                  FROM pg_available_extensions
                  WHERE name = 'btree_gist'), 'not available'),
        'a version number',
        EXISTS (SELECT 1
                FROM pg_available_extensions
                WHERE name = 'btree_gist')),

    (8, 'btree_gist needs no superuser',
        COALESCE((SELECT v.trusted::text
                  FROM pg_available_extension_versions v
                  JOIN pg_available_extensions e
                    ON e.name = v.name AND e.default_version = v.version
                  WHERE v.name = 'btree_gist'), 'unknown'),
        'true',
        EXISTS (SELECT 1
                FROM pg_available_extension_versions v
                JOIN pg_available_extensions e
                  ON e.name = v.name AND e.default_version = v.version
                WHERE v.name = 'btree_gist' AND v.trusted)),

    (9, 'Tables already in schema public',
        (SELECT count(*)::text FROM pg_tables WHERE schemaname = 'public'),
        '0',
        (SELECT count(*) FROM pg_tables WHERE schemaname = 'public') = 0),

    (10, 'Owner of slotforge_test',
        COALESCE((SELECT pg_get_userbyid(datdba)::text
                  FROM pg_database
                  WHERE datname = 'slotforge_test'), 'not created yet'),
        'slotforge',
        EXISTS (SELECT 1
                FROM pg_database
                WHERE datname = 'slotforge_test'
                  AND pg_get_userbyid(datdba) = 'slotforge')),

    (11, 'Connected role is a superuser',
        (SELECT rolsuper::text FROM pg_roles WHERE rolname = current_user),
        'for information',
        NULL),

    (12, 'Server time zone',
        current_setting('TimeZone'),
        'for information',
        NULL)
) AS checks (step, check_name, actual, expected, passed)
ORDER BY step;

-- F002__origin_check.sql
--
-- SELECT-only: this changes nothing. Run on the slotforge database BEFORE F002.
-- Shows where user 7 (test@gmail.com) came from and whether anything depends on it.
-- Run each query on its own and compare with the expected result.

-- 1. The account. Expected: last_login_at is null and linked_to_google is false,
--    so it has never been used to sign in.
SELECT id, email, full_name, status, google_sub IS NOT NULL AS linked_to_google,
       last_login_at, created_at, updated_at
FROM app_user
WHERE id = 7;

-- 2. Its history. Expected: USER_CREATED by admin@demo.slotforge.test, with details listing
--    the roles chosen in the Add user form, possibly followed by USER_ROLES_CHANGED rows.
SELECT a.created_at, a.action, a.details, actor.email AS done_by
FROM audit_log a
LEFT JOIN app_user actor ON actor.id = a.actor_user_id
WHERE a.entity_type = 'app_user' AND a.entity_id = '7'
ORDER BY a.created_at, a.id;

-- 3. What depends on it. Expected: 0 and 0. It is not on the student roster, and has done nothing.
SELECT (SELECT count(*) FROM student WHERE user_id = 7 OR email = 'test@gmail.com') AS student_roster_links,
       (SELECT count(*) FROM audit_log WHERE actor_user_id = 7) AS actions_by_this_account;

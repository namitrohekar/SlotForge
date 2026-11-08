-- F002__quarantine_test_user.sql
--
-- Data fix only: schema_version stays at 9. Run on the slotforge database AFTER F002__origin_check.sql.
--
-- User 7 (test@gmail.com, "test user 1") was created through Add user before role rules existed,
-- with all six roles ticked. It is a hand-made test account: a personal address, never signed in,
-- on no roster. No single intended role can be read from the data, so this script picks none.
-- It removes every role and deactivates the account, keeping the row and its audit history.
-- An administrator can later reactivate it and choose one valid set of roles explicitly.
--
-- Every fact above is checked first. If any differs, the script stops and changes nothing.

BEGIN;

DO $$
DECLARE
  account app_user%ROWTYPE;
  held TEXT;
BEGIN
  SELECT * INTO account FROM app_user WHERE id = 7 FOR UPDATE;
  IF NOT FOUND OR account.email <> 'test@gmail.com' THEN
    RAISE EXCEPTION 'User 7 is not test@gmail.com. Nothing changed.';
  END IF;
  IF account.last_login_at IS NOT NULL OR account.google_sub IS NOT NULL THEN
    RAISE EXCEPTION 'test@gmail.com has signed in, so it may be in real use. Nothing changed.';
  END IF;
  IF EXISTS (SELECT 1 FROM student WHERE user_id = 7) THEN
    RAISE EXCEPTION 'test@gmail.com is linked to a student roster row. Nothing changed.';
  END IF;

  SELECT string_agg(role, ',' ORDER BY role) INTO held FROM user_role WHERE user_id = 7;
  IF held IS DISTINCT FROM 'ADMIN,EXAM_COORDINATOR,FACULTY,HOD,INVIGILATOR,STUDENT' THEN
    RAISE EXCEPTION 'The roles of test@gmail.com have changed (now: %). Nothing changed.', held;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM app_user u JOIN user_role r ON r.user_id = u.id
    WHERE r.role = 'ADMIN' AND u.status = 'ACTIVE' AND u.id <> 7
  ) THEN
    RAISE EXCEPTION 'No other active administrator would remain. Nothing changed.';
  END IF;
END $$;

DELETE FROM user_role WHERE user_id = 7;

UPDATE app_user SET status = 'DEACTIVATED', updated_at = now() WHERE id = 7;

INSERT INTO audit_log (actor_user_id, action, entity_type, entity_id, details)
VALUES (NULL, 'USER_ROLES_CLEARED', 'app_user', '7',
  '{"reason":"Held all six roles, saved before role rules existed","removedRoles":["ADMIN","EXAM_COORDINATOR","FACULTY","HOD","INVIGILATOR","STUDENT"],"by":"F002__quarantine_test_user.sql"}');

COMMIT;

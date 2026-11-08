-- Empties slotforge_test so the real migration scripts can rebuild it.
-- The check below stops this from ever running against another database.

DO $$
BEGIN
  IF current_database() <> 'slotforge_test' THEN
    RAISE EXCEPTION 'Refusing to reset database %', current_database();
  END IF;
END $$;

DROP SCHEMA public CASCADE;
CREATE SCHEMA public;

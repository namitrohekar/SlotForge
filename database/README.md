# SlotForge Database

The PostgreSQL schema is managed by hand. Every change is a numbered SQL script in this folder, run in pgAdmin. The application never changes the schema: Hibernate runs with `ddl-auto=validate`, so it only checks it.

## Folders

| Path | Contents |
|---|---|
| `migrations/` | Schema changes, one numbered script each |
| `verify/` | SELECT-only checks, one per migration. They change nothing |
| `seeds/` | Demo data. Safe to run more than once |
| `SCHEMA_VERSIONS.md` | History of every script and when it was applied |

## Applying a migration

1. Open the pgAdmin Query Tool on the `slotforge` database, connected as role `slotforge`. Objects then belong to the role the application uses.
2. Open the script from `migrations/` and run the whole file.
3. Open the matching file from `verify/` and run it.
4. Confirm the result, so `SCHEMA_VERSIONS.md` can be updated.

Before a script that alters tables holding data, take a pgAdmin backup.

## Rules for scripts

- Numbered in the order they must run, such as `V007__examinations_and_enrolment.sql`.
- Each runs in one transaction, so a failure changes nothing.
- Each refuses to run unless the previous version is the latest.
- The last statement records the script in `schema_version`.
- An applied script is never edited. A correction is a new script.
- Scripts only move forward. There are no automatic rollbacks.

## Checking what is applied

```sql
SELECT version, script, applied_at, applied_by
FROM schema_version
ORDER BY version;
```

The application compares this with the version its code expects, and stops at startup with a message naming any missing script.

## Test database

Database tests use a separate database, `slotforge_test`, and can never run against `slotforge`. Create it once, while connected to the `postgres` database as a role that can create databases:

```sql
CREATE DATABASE slotforge_test OWNER slotforge;
```

Run that statement on its own. You never apply scripts to `slotforge_test` by hand, because the test suite rebuilds it from `migrations/` each time you run the database tests.

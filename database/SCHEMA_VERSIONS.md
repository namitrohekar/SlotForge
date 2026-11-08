# Schema Versions

The history of every migration applied to the `slotforge` database. The `schema_version` table in the database is the truth, and this file is its record in Git.

| Version | Script | Purpose | Applied on |
|---|---|---|---|
| 1 | `V001__schema_version.sql` | Schema version record and `btree_gist` extension | 2026-11-07 |
| 2 | `V002__users_and_audit.sql` | Users, roles and audit log | 2026-11-08 |
| 3 | `V003__academic_structure.sql` | Academic structure and curricula | 2026-11-08 |
| 4 | `V004__students.sql` | Students | 2026-11-08 |
| 5 | `V005__halls.sql` | Examination halls | 2026-11-08 |
| 6 | `V006__exam_sessions_and_slots.sql` | Exam sessions and time slots | 2026-11-08 |
| 7 | `V007__examinations_and_enrolment.sql` | Examinations and enrolment | 2026-11-08 |
| 8 | `V008__runs_versions_and_schedule.sql` | Scheduling runs, versions and scheduled exams | 2026-11-08 |
| 9 | `V009__hall_allocation.sql` | Hall allocation and student placement | 2026-11-08 |
| 10 | `V010__academic_active_flags.sql` | Active switch for departments and programmes (columns not used by the code yet) | 2026-11-08 |

Seed `S001__demo_users.sql` applied on 2026-11-08.

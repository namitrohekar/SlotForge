# SlotForge

Examination timetabling for a college. SlotForge places every paper so that no student has two at once, seats students in halls, assigns invigilators, and carries the timetable from draft to published under a named approver. When something changes afterwards, it repairs the timetable by moving as little as possible.

Built as an MCA mini project at VJTI, Mumbai.

## What it does

| Area | What you get |
|---|---|
| Timetable | DSATUR graph colouring with backtracking. Papers that share a student never share a slot |
| Rules | One paper per student per day, rest days between heavy papers, hall capacity, invigilator availability and duty limits. All shown on the session page |
| Halls | Large papers split across halls. Every student gets a hall |
| Invigilators | Duties shared evenly. Nobody is in two halls at once or on duty while away |
| Checks before building | Input that is certain to fail is reported in plain words before any solving |
| Explanation | "How it was built": the clash picture, the order papers were placed in and why, a quality score, and the same session solved by a simpler first-fit method |
| Approval | Coordinator generates and sends, the head of department or the Controller of Examinations approves, the coordinator publishes |
| Change | Record a hall outage or staff leave, press Repair, and only what must move moves. Move one paper by hand with the reason shown for every refused slot. "What changed" compares two versions |
| Output | Printable timetable notice, seating list per hall, and invigilation duty chart |
| People | Students see their papers and halls. Invigilators see their duties. Everyone can read published timetables and gets notices that concern them |
| Records | CSV import for students and enrolment, and a searchable audit log |

## How the engine works, briefly

1. Each paper is a dot. Two dots are joined when at least one student sits both papers.
2. Joined papers can never share a time slot. Giving every dot a slot with no joined pair in the same one is a clash-free timetable.
3. DSATUR always places next the paper with the fewest slots still open to it, because that is the paper that runs out of room if left for later.
4. Among the slots a paper may take, the one that keeps each student's papers furthest apart is chosen.
5. If the search gets stuck it backs up and tries another slot, up to a fixed number of steps, so the same input always gives the same timetable.

An independent set of SQL rules then checks the stored result, and database constraints make double-booking a hall or a person impossible.

## Stack

| Part | Technology |
|---|---|
| Backend | Java 21, Spring Boot 4, Spring Security, JPA with `ddl-auto=validate`, PostgreSQL 18 |
| Frontend | React 19, Vite, Tailwind CSS 4, React Router, Zustand, Axios |
| Tests | JUnit and AssertJ, Vitest and Testing Library |

## Running it

You need Java 21, Node 22 or newer, and PostgreSQL with a database named `slotforge`.

The SQL scripts that create the schema and demo data are kept in a `database/` folder that is not part of this repository. Apply the migration scripts in order, then the seed scripts.

1. Copy `backend/.env.example` to `backend/.env` and fill in the database values.
2. Start the backend from `backend/`:

   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
   ```

3. Start the frontend from `frontend/`:

   ```bash
   npm install
   npm run dev
   ```

4. Open `http://localhost:5173` and choose a demo account.

The `local` profile turns on demo sign-in, which only works from the same machine. Add the `google` profile and the Google values in `.env` for Google sign-in.

## Roles

| Role | What they do |
|---|---|
| Administrator | Manages accounts, appoints department heads, reads the audit log |
| Exam coordinator | Builds the session, generates, repairs, sends for approval, publishes |
| Head of department | Approves a timetable whose students are mostly from their department |
| Controller of Examinations | Approves a timetable that spans departments |
| Faculty, Invigilator | See their duties and record dates they are away |
| Student | Sees their papers and halls |

Whoever approves a timetable can never be the person who generates or publishes it.

## Tests

From `backend/`:

```bash
./mvnw test
```

```bash
./mvnw test -Pdb-tests
```

The second command runs the database tests against a separate `slotforge_test` database, which it rebuilds from the migration scripts.

From `frontend/`:

```bash
npm test
```

```bash
npm run lint
```

```bash
npm run build
```

## Layout

| Path | Contents |
|---|---|
| `backend/src/main/java/com/slotforge/planning` | The engine in plain Java: conflict graph, DSATUR, hall packing, invigilator allocation, preflight, quality, repair, first-fit baseline |
| `backend/.../schedule` | Stored timetable versions, the SQL validator, the approval workflow, the diff |
| `backend/.../exam`, `academic`, `student`, `hall`, `auth`, `audit`, `notification` | Everything the engine is fed with, and the people around it |
| `frontend/src/features` | One folder per feature: sessions, timetable, print sheets, dashboard, halls, students, users |
| `frontend/src/components` | Shared layout and controls |

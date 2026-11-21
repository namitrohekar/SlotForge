# SlotForge

Exam timetable scheduling for VJTI, built as an MCA mini project.

SlotForge takes the papers, students, halls and invigilators for an exam session and builds a timetable where no student has two papers at the same time. The timetable then goes for approval before it is published.

## Features

- Timetable generation using DSATUR graph colouring with backtracking
- Hall allocation, with large papers split across halls
- Invigilator allocation that respects leave and duty limits
- Approval flow: coordinator sends, head of department or controller approves, coordinator publishes
- Repair after a change, such as a hall becoming unavailable, moving as few papers as possible
- Moving a single paper by hand, with the reason shown for slots that are not allowed
- Comparison with a simple first-fit method on the same data
- Printable timetable, seating lists and duty chart
- Student and invigilator views, notifications, CSV import and an audit log
- Demo accounts for every role, plus Google sign-in limited to the college domain

## Tech stack

- Backend: Java 21, Spring Boot 4, Spring Security, PostgreSQL
- Frontend: React, Vite, Tailwind CSS

## Running locally

You need Java 21, Node 22 and PostgreSQL with a database called `slotforge`. The SQL scripts for the schema and demo data are kept outside this repository.

1. Copy `backend/.env.example` to `backend/.env` and fill in the database details.
2. Start the backend:

   ```bash
   cd backend
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
   ```

3. Start the frontend:

   ```bash
   cd frontend
   npm install
   npm run dev
   ```

4. Open http://localhost:5173 and pick a demo account.

## Deployment

The app is deployed as a single Docker container on Render, which serves both the frontend and the API. The database is on Neon. Both are on free plans.

1. Create a PostgreSQL 18 project on Neon and run the schema and seed scripts on it.
2. On Render, create a new Blueprint from this repository. It uses `render.yaml`.
3. Enter the database URL, username and password when asked, plus the site's own address and the Google client ID and secret.
4. In Google Cloud, add `https://<site address>/login/oauth2/code/google` as a redirect URI of the OAuth client.

The deployed site uses the `prod`, `demo` and `google` profiles, so anyone with the link can use the demo accounts. Keep only demo data in that database. Google sign-in works only for people already added on the Users page.

The free Render instance sleeps after 15 minutes without visits, so the first load can take about a minute.

## Tests

```bash
cd backend
./mvnw test
```

```bash
cd frontend
npm test
```

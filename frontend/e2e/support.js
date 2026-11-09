const health = {
  status: 'UP',
  database: 'slotforge',
  postgresVersion: '18.6',
  schemaVersion: 2,
  requiredSchemaVersion: 2,
}

const admin = { id: 1, fullName: 'Demo Admin', email: 'admin@demo.slotforge.test', roles: ['ADMIN'] }

export const demoUsers = [
  admin,
  { id: 2, fullName: 'Demo Coordinator', email: 'coordinator@demo.slotforge.test', roles: ['EXAM_COORDINATOR'] },
  { id: 3, fullName: 'Demo Head of Department', email: 'hod@demo.slotforge.test', roles: ['HOD'] },
  { id: 4, fullName: 'Demo Faculty', email: 'faculty@demo.slotforge.test', roles: ['FACULTY'] },
  { id: 5, fullName: 'Demo Invigilator', email: 'invigilator@demo.slotforge.test', roles: ['INVIGILATOR'] },
  { id: 6, fullName: 'Demo Student', email: 'student@demo.slotforge.test', roles: ['STUDENT'] },
]

const users = demoUsers.map((user, index) => ({
  ...user,
  status: index === 4 ? 'DEACTIVATED' : 'ACTIVE',
  lastLoginAt: index === 0 ? '2026-11-08T04:30:00Z' : null,
  createdAt: '2026-11-08T04:00:00Z',
}))

const listOf = (items) => ({ items, page: 0, size: 20, totalItems: items.length, totalPages: 1 })

const departments = [
  { id: 1, code: 'CEIT', name: 'Computer Engineering & Information Technology', courseCount: 3, subjectCount: 1 },
  { id: 2, code: 'ELEC', name: 'Electrical Engineering', courseCount: 2, subjectCount: 0 },
  { id: 3, code: 'MCA', name: 'Master in Computer Applications', courseCount: 1, subjectCount: 3 },
  { id: 4, code: 'MATH', name: 'Mathematics', courseCount: 0, subjectCount: 2 },
]

const course = (id, code, name, totalSemesters, departmentId) => {
  const department = departments.find((item) => item.id === departmentId)
  return {
    id,
    code,
    name,
    totalSemesters,
    departmentId,
    departmentCode: department.code,
    departmentName: department.name,
    curriculumCount: code === 'MCA' ? 1 : 0,
  }
}

const courses = [
  course(1, 'BT-CE', 'B.Tech Computer Engineering', 8, 1),
  course(2, 'BT-IT', 'B.Tech Information Technology', 8, 1),
  course(3, 'MT-CE', 'M.Tech Computer Engineering', 4, 1),
  course(4, 'BT-EXTC', 'B.Tech Electronics & Telecommunication Engineering', 8, 2),
  course(5, 'DIP-EE', 'Diploma in Electrical Engineering', 6, 2),
  course(6, 'MCA', 'Master of Computer Applications', 4, 3),
]

const subjects = [
  ['MCA101', 'Mathematical Foundations for Computer Science', true, 4],
  ['MCA102', 'Data Structures and Algorithms', true, 3],
  ['MCA103', 'Object Oriented Programming with Java', false, 3],
  ['MCA311', 'Big Data Analytics', false, 3],
  ['MA101', 'Engineering Mathematics I', true, 4],
  ['CE101', 'Programming for Problem Solving', false, 1],
].map(([code, name, heavy, departmentId], index) => {
  const department = departments.find((item) => item.id === departmentId)
  return { id: index + 1, code, name, heavy, departmentId, departmentCode: department.code, departmentName: department.name, curriculumCount: 1 }
})

const mcaCurriculum = {
  id: 1,
  name: '2024 scheme',
  effectiveFromYear: 2024,
  courseId: 6,
  courseCode: 'MCA',
  courseName: 'Master of Computer Applications',
  totalSemesters: 4,
  subjectCount: 4,
  batchCount: 1,
}

const batches = [
  { id: 1, name: '2025-27', admissionYear: 2025, currentSemester: 3, curriculumId: 1, curriculumName: '2024 scheme', courseId: 6, courseCode: 'MCA', courseName: 'Master of Computer Applications', totalSemesters: 4, studentCount: 60 },
  { id: 2, name: '2026-28', admissionYear: 2026, currentSemester: 1, curriculumId: 1, curriculumName: '2024 scheme', courseId: 6, courseCode: 'MCA', courseName: 'Master of Computer Applications', totalSemesters: 4, studentCount: 0 },
]

const halls = [
  { id: 1, code: 'AUD-1', name: 'Auditorium Examination Hall', building: 'Main Building', floor: 0, capacity: 120, status: 'AVAILABLE', allocationCount: 0 },
  { id: 2, code: 'MCA-301', name: 'MCA Classroom 301', building: 'MCA Department', floor: 3, capacity: 45, status: 'MAINTENANCE', allocationCount: 2 },
]

const students = [
  { id: 1, rollNo: '25MCA001', fullName: 'Aarav Patil', email: '25mca001@demo.slotforge.test', batchId: 1, batchName: '2025-27', courseCode: 'MCA', courseName: 'Master of Computer Applications', currentSemester: 3, enrolmentCount: 5 },
  { id: 2, rollNo: '26MCA001', fullName: 'Ishita Rao', email: '26mca001@demo.slotforge.test', batchId: 2, batchName: '2026-28', courseCode: 'MCA', courseName: 'Master of Computer Applications', currentSemester: 1, enrolmentCount: 0 },
]

const examSession = {
  id: 1,
  name: 'End Semester, November 2026',
  startDate: '2026-12-07',
  endDate: '2026-12-19',
  status: 'PLANNING',
  maxExamsPerStudentPerDay: 1,
  heavyGapDays: 1,
  slotCount: 3,
  examinationCount: 2,
  progress: {
    openSlots: 2,
    slotsTooShort: 0,
    shortestPaperMinutes: 180,
    readyPapers: 1,
    papersWithoutStudents: ['MCA311 Big Data Analytics'],
    latestVersionNo: 1,
    latestVersionStatus: 'DRAFT',
    inReviewStatus: 'DRAFT',
    latestProblems: 0,
    latestPlaced: 2,
    latestTotal: 3,
    approverPost: 'Head of MCA',
    approverName: 'Demo Head of Department',
    approverIds: [3],
  },
}

const slots = [
  { id: 1, date: '2026-12-07', start: '10:00:00', end: '13:00:00', status: 'OPEN', blockReason: null, usageCount: 0 },
  { id: 2, date: '2026-12-07', start: '14:00:00', end: '17:00:00', status: 'OPEN', blockReason: null, usageCount: 0 },
  { id: 3, date: '2026-12-08', start: '10:00:00', end: '13:00:00', status: 'BLOCKED', blockReason: 'Public holiday', usageCount: 0 },
]

const examinations = [
  { id: 1, subjectId: 1, subjectCode: 'MCA101', subjectName: 'Mathematical Foundations for Computer Science', departmentCode: 'MATH', heavy: true, durationMinutes: 180, earliestDate: null, latestDate: null, status: 'READY', enrolled: 60 },
  { id: 2, subjectId: 4, subjectCode: 'MCA311', subjectName: 'Big Data Analytics', departmentCode: 'MCA', heavy: false, durationMinutes: 90, earliestDate: null, latestDate: null, status: 'DRAFT', enrolled: 0 },
]

const version = {
  submittedAt: '2026-11-08T11:00:00Z',
  id: 1,
  versionNo: 1,
  status: 'DRAFT',
  createdAt: '2026-11-08T10:00:00Z',
  examsTotal: 3,
  examsScheduled: 2,
  slotsUsed: 2,
  searchNodes: 1240,
  backtracks: 4,
  durationMs: 86,
  reviewComment: null,
  reviewedBy: null,
  reviewedAt: null,
  publishedAt: null,
  unplaced: [
    { examId: 3, code: 'MCA301 Machine Learning', blockers: [{ constraint: 'STUDENT_CLASH', slots: 2, example: 'MCA101 Mathematical Foundations, 37 shared students' }] },
  ],
}

const entry = (examinationId, subjectCode, subjectName, slotId, date, start, end, conflicts) => ({
  examinationId,
  subjectCode,
  subjectName,
  heavy: examinationId === 1,
  departmentCode: 'MCA',
  durationMinutes: 180,
  slotId,
  date,
  start,
  end,
  students: 60,
  halls: [{ code: 'MB-101', name: 'Main Building 101', building: 'Main Building', seats: 60, capacity: 60, invigilators: ['Demo Invigilator', 'Demo Faculty'] }],
  conflicts,
})

const entries = [
  entry(1, 'MCA101', 'Mathematical Foundations for Computer Science', 1, '2026-12-07', '10:00:00', '13:00:00', [{ examinationId: 2, sharedStudents: 60 }]),
  entry(2, 'MCA102', 'Data Structures and Algorithms', 2, '2026-12-08', '14:00:00', '17:00:00', [{ examinationId: 1, sharedStudents: 60 }]),
]

const placements = [
  [1, 1, false],
  [2, 1, false],
  [3, 1, false],
  [4, 3, true],
].map(([subjectId, semesterNo, elective]) => {
  const subject = subjects.find((item) => item.id === subjectId)
  return { subjectId, code: subject.code, name: subject.name, heavy: subject.heavy, departmentCode: subject.departmentCode, semesterNo, elective }
})

export const routes = [
  '/',
  '/academics/departments',
  '/academics/courses',
  '/academics/subjects',
  '/academics/curricula',
  '/academics/curricula/1',
  '/academics/batches',
  '/sessions',
  '/sessions/1/slots',
  '/sessions/1/examinations',
  '/sessions/1/timetable',
  '/students',
  '/halls',
  '/users',
  '/system',
  '/design',
]

// Signed in as the demo administrator unless `signedIn` is false.
export async function stubApi(page, { signedIn = true } = {}) {
  await page.route('**/api/health', (route) => route.fulfill({ json: health }))
  await page.route(
    (url) => url.pathname === '/api/departments',
    (route) => route.fulfill({ json: listOf(departments) }),
  )
  await page.route(
    (url) => url.pathname === '/api/courses',
    (route) => route.fulfill({ json: listOf(courses) }),
  )
  await page.route(
    (url) => url.pathname === '/api/subjects',
    (route) => route.fulfill({ json: listOf(subjects) }),
  )
  await page.route(
    (url) => url.pathname === '/api/curricula',
    (route) => route.fulfill({ json: listOf([mcaCurriculum]) }),
  )
  await page.route('**/api/curricula/1', (route) => route.fulfill({ json: mcaCurriculum }))
  await page.route('**/api/curricula/1/subjects', (route) => route.fulfill({ json: placements }))
  await page.route(
    (url) => url.pathname === '/api/batches',
    (route) => route.fulfill({ json: listOf(batches) }),
  )
  await page.route(
    (url) => url.pathname === '/api/halls',
    (route) => route.fulfill({ json: listOf(halls) }),
  )
  await page.route(
    (url) => url.pathname === '/api/students',
    (route) => route.fulfill({ json: listOf(students) }),
  )
  await page.route(
    (url) => url.pathname === '/api/exam-sessions',
    (route) => route.fulfill({ json: listOf([examSession]) }),
  )
  await page.route('**/api/exam-sessions/1', (route) => route.fulfill({ json: examSession }))
  await page.route('**/api/exam-sessions/1/time-slots', (route) => route.fulfill({ json: slots }))
  await page.route(
    (url) => url.pathname === '/api/exam-sessions/1/examinations',
    (route) => route.fulfill({ json: listOf(examinations) }),
  )
  await page.route('**/api/exam-sessions/1/schedule-versions', (route) => route.fulfill({ json: [version] }))
  await page.route('**/api/schedule-versions/1/entries', (route) => route.fulfill({ json: entries }))
  await page.route('**/api/schedule-versions/1/violations', (route) => route.fulfill({ json: [] }))
  await page.route('**/api/schedule-versions/duties-at-risk', (route) => route.fulfill({ json: [] }))
  await page.route(
    (url) => url.pathname === '/api/users',
    (route) => route.fulfill({ json: { items: users, page: 0, size: 20, totalItems: users.length, totalPages: 1 } }),
  )
  await page.route('**/api/auth/options', (route) =>
    route.fulfill({ json: { googleEnabled: false, demoLoginEnabled: true } }),
  )
  await page.route('**/api/auth/demo-users', (route) => route.fulfill({ json: demoUsers }))
  await page.route('**/api/auth/me', (route) =>
    signedIn
      ? route.fulfill({ json: admin })
      : route.fulfill({ status: 401, contentType: 'application/problem+json', json: { status: 401, code: 'UNAUTHENTICATED' } }),
  )
}

import { createBrowserRouter, Navigate } from 'react-router'
import PageLoading from '../components/common/PageLoading.jsx'
import AppLayout from '../components/layout/AppLayout.jsx'
import { rolesFor } from './navigation.js'
import ProtectedRoute, { RequireRoles } from './ProtectedRoute.jsx'

const page = (load) => async () => ({ Component: (await load()).default })

// A page open only to the roles its menu item lists.
const guarded = (path, load) => ({
  element: <RequireRoles roles={rolesFor(`/${path}`)} />,
  children: [{ path, lazy: page(load) }],
})

export const router = createBrowserRouter([
  {
    path: '/login',
    HydrateFallback: PageLoading,
    lazy: page(() => import('../features/auth/pages/LoginPage.jsx')),
  },
  {
    Component: ProtectedRoute,
    HydrateFallback: PageLoading,
    children: [
      {
        path: '/',
        Component: AppLayout,
        children: [
          { index: true, lazy: page(() => import('../features/dashboard/pages/DashboardPage.jsx')) },
          {
            element: <RequireRoles roles={rolesFor('/sessions')} />,
            children: [
              { path: 'sessions', lazy: page(() => import('../features/exams/pages/SessionsPage.jsx')) },
              {
                path: 'sessions/:id',
                lazy: page(() => import('../features/exams/pages/SessionDetailPage.jsx')),
                children: [
                  { index: true, element: <Navigate to="slots" replace /> },
                  { path: 'slots', lazy: page(() => import('../features/exams/components/SlotsTab.jsx')) },
                  { path: 'examinations', lazy: page(() => import('../features/exams/components/ExaminationsTab.jsx')) },
                  { path: 'timetable', lazy: page(() => import('../features/schedules/components/TimetableTab.jsx')) },
                  { path: 'explained', lazy: page(() => import('../features/schedules/components/ExplainedTab.jsx')) },
                  { path: 'changes', lazy: page(() => import('../features/schedules/components/ChangesTab.jsx')) },
                ],
              },
            ],
          },
          {
            element: <RequireRoles roles={rolesFor('/academics')} />,
            children: [
              {
                path: 'academics',
                lazy: page(() => import('../features/academics/pages/AcademicsPage.jsx')),
                children: [
                  { index: true, element: <Navigate to="departments" replace /> },
                  { path: 'departments', lazy: page(() => import('../features/academics/components/DepartmentsTab.jsx')) },
                  { path: 'courses', lazy: page(() => import('../features/academics/components/CoursesTab.jsx')) },
                  { path: 'subjects', lazy: page(() => import('../features/academics/components/SubjectsTab.jsx')) },
                  { path: 'curricula', lazy: page(() => import('../features/academics/components/CurriculaTab.jsx')) },
                  { path: 'batches', lazy: page(() => import('../features/academics/components/BatchesTab.jsx')) },
                  { path: 'curricula/:id', lazy: page(() => import('../features/academics/pages/CurriculumDetailPage.jsx')) },
                ],
              },
            ],
          },
          guarded('timetables', () => import('../features/schedules/pages/TimetablesPage.jsx')),
          guarded('my/duties', () => import('../features/schedules/pages/MyDutiesPage.jsx')),
          guarded('students', () => import('../features/students/pages/StudentsPage.jsx')),
          guarded('halls', () => import('../features/halls/pages/HallsPage.jsx')),
          guarded('comparison', () => import('../features/schedules/pages/AlgorithmComparisonPage.jsx')),
          guarded('users', () => import('../features/users/pages/UsersPage.jsx')),
          guarded('system', () => import('../features/system/pages/SystemStatusPage.jsx')),
          guarded('audit', () => import('../features/audit/pages/AuditPage.jsx')),
          ...(import.meta.env.DEV
            ? [{ path: 'design', lazy: page(() => import('../features/design/pages/DesignGalleryPage.jsx')) }]
            : []),
          { path: '*', lazy: page(() => import('../features/auth/pages/NotFoundPage.jsx')) },
        ],
      },
      // Sheets for paper, so they sit outside the app's menu and bars.
      {
        path: '/print',
        children: [
          { path: 'notice/:sessionId', lazy: page(() => import('../features/schedules/print/NoticePrintPage.jsx')) },
          {
            element: <RequireRoles roles={rolesFor('/sessions')} />,
            children: [
              {
                path: 'sessions/:sessionId/versions/:versionId/seating',
                lazy: page(() => import('../features/schedules/print/SeatingPrintPage.jsx')),
              },
              {
                path: 'sessions/:sessionId/versions/:versionId/duties',
                lazy: page(() => import('../features/schedules/print/DutyChartPrintPage.jsx')),
              },
            ],
          },
        ],
      },
    ],
  },
])

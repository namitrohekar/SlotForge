import { CalendarDays, ClipboardList, DoorOpen, FileText, GraduationCap, LayoutDashboard, UserRound, Users } from 'lucide-react'
import { hasAnyRole } from '../utils/roles.js'

const PLANNERS = ['ADMIN', 'EXAM_COORDINATOR', 'HOD']

// The one list behind the sidebar, rail, tab bar, More sheet and route guards, so they cannot disagree.
// No roles means every signed-in user. The menu holds only what a role uses in the exam job.
export const navigation = [
  { label: 'Dashboard', to: '/', icon: LayoutDashboard },
  { label: 'Timetables', to: '/timetables', icon: FileText },
  // Heads and the Controller approve timetables and are never given duties, so the roles they imply do not count here.
  { label: 'My duties', to: '/my/duties', icon: ClipboardList, roles: ['FACULTY', 'INVIGILATOR'], heldOnly: true },
  { label: 'Sessions', to: '/sessions', icon: CalendarDays, roles: PLANNERS },
  { label: 'Academics', to: '/academics', icon: GraduationCap, roles: PLANNERS },
  { label: 'Students', to: '/students', icon: UserRound, roles: PLANNERS },
  { label: 'Halls', to: '/halls', icon: DoorOpen, roles: PLANNERS },
  { label: 'Users', to: '/users', icon: Users, roles: ['ADMIN'] },
  // Reached from a link on the dashboard, not from the menu.
  { label: 'Comparison', to: '/comparison', roles: PLANNERS, menu: false },
  { label: 'System', to: '/system', roles: ['ADMIN'], menu: false },
]

export const TAB_BAR_LIMIT = 4

export const rolesFor = (to) => navigation.find((item) => item.to === to)?.roles

const holds = (user, { roles, heldOnly }) => (heldOnly ? roles.some((role) => user?.roles.includes(role)) : hasAnyRole(user, roles))

export const visibleNavigation = (user) => navigation.filter((item) => item.menu !== false && holds(user, item))

export const activeNavIndex = (items, pathname) =>
  items.findIndex(({ to }) => (to === '/' ? pathname === '/' : pathname.startsWith(to)))

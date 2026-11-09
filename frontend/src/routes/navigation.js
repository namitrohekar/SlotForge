import { Activity, CalendarDays, ClipboardList, DoorOpen, GraduationCap, LayoutDashboard, Palette, UserRound, Users } from 'lucide-react'
import { hasAnyRole } from '../utils/roles.js'

// The one list behind the sidebar, rail, tab bar, More sheet and route guards, so they cannot disagree.
// No roles means every signed-in user.
export const navigation = [
  { label: 'Dashboard', to: '/', icon: LayoutDashboard },
  { label: 'My duties', to: '/my/duties', icon: ClipboardList, roles: ['FACULTY', 'INVIGILATOR'] },
  { label: 'Sessions', to: '/sessions', icon: CalendarDays, roles: ['ADMIN', 'EXAM_COORDINATOR', 'HOD', 'FACULTY'] },
  { label: 'Academics', to: '/academics', icon: GraduationCap, roles: ['ADMIN', 'EXAM_COORDINATOR', 'HOD', 'FACULTY'] },
  { label: 'Students', to: '/students', icon: UserRound, roles: ['ADMIN', 'EXAM_COORDINATOR', 'HOD'] },
  { label: 'Halls', to: '/halls', icon: DoorOpen, roles: ['ADMIN', 'EXAM_COORDINATOR', 'HOD', 'FACULTY', 'INVIGILATOR'] },
  { label: 'Users', to: '/users', icon: Users, roles: ['ADMIN'] },
  { label: 'System', to: '/system', icon: Activity, roles: ['ADMIN'] },
  ...(import.meta.env.DEV ? [{ label: 'Design', to: '/design', icon: Palette, roles: ['ADMIN'] }] : []),
]

export const TAB_BAR_LIMIT = 4

export const rolesFor = (to) => navigation.find((item) => item.to === to)?.roles

export const visibleNavigation = (user) => navigation.filter(({ roles }) => hasAnyRole(user, roles))

export const activeNavIndex = (items, pathname) =>
  items.findIndex(({ to }) => (to === '/' ? pathname === '/' : pathname.startsWith(to)))

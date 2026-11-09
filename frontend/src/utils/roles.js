export const ROLE_LABELS = {
  ADMIN: 'Administrator',
  EXAM_COORDINATOR: 'Exam coordinator',
  CONTROLLER: 'Controller of Examinations',
  HOD: 'Head of department',
  FACULTY: 'Faculty',
  INVIGILATOR: 'Invigilator',
  STUDENT: 'Student',
}

// Mirrors the backend role hierarchy. The backend still decides; this only shapes the interface.
const IMPLIED = { ADMIN: ['ADMIN', 'EXAM_COORDINATOR'], CONTROLLER: ['CONTROLLER', 'HOD', 'FACULTY'], HOD: ['HOD', 'FACULTY'] }

const STAFF = ['FACULTY', 'HOD', 'CONTROLLER', 'INVIGILATOR']
const ADMINISTRATION = ['ADMIN', 'EXAM_COORDINATOR']
const count = (roles, group) => roles.filter((role) => group.includes(role)).length

// Mirrors RoleRules.java, word for word, so the form explains what the server would refuse.
export function roleConflict(roles) {
  if (roles.includes('STUDENT') && roles.length > 1) {
    return 'A student account cannot also hold a staff or administrative role.'
  }
  if (count(roles, STAFF) > 1) {
    return 'Choose only one of Faculty, Head of department, Controller of Examinations or Invigilator. A head of department already has faculty access, and invigilator is for non-teaching staff.'
  }
  if (count(roles, ADMINISTRATION) > 1) {
    return 'Choose Administrator or Exam coordinator, not both. An administrator already has coordinator access.'
  }
  if ((roles.includes('HOD') || roles.includes('CONTROLLER')) && count(roles, ADMINISTRATION) > 0) {
    return 'Whoever approves timetables cannot also be an administrator or exam coordinator, who generate and publish them.'
  }
  return undefined
}

export const canAddRole = (role, roles) => roles.includes(role) || !roleConflict([...roles, role])

export function hasAnyRole(user, roles) {
  if (!roles) return true
  if (!user) return false
  const held = new Set(user.roles.flatMap((role) => IMPLIED[role] ?? [role]))
  return roles.some((role) => held.has(role))
}

export const roleSummary = (roles) => roles.map((role) => ROLE_LABELS[role] ?? role).join(', ')

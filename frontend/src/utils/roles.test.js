import { describe, expect, it } from 'vitest'
import { rolesFor, visibleNavigation } from '../routes/navigation.js'
import { canAddRole, hasAnyRole, roleConflict, roleSummary } from './roles.js'

const as = (...roles) => ({ roles })

describe('roles', () => {
  it('lets an administrator do what a coordinator can, but not review as a head of department', () => {
    expect(hasAnyRole(as('ADMIN'), ['EXAM_COORDINATOR'])).toBe(true)
    expect(hasAnyRole(as('ADMIN'), ['HOD'])).toBe(false)
    expect(hasAnyRole(as('EXAM_COORDINATOR'), ['ADMIN'])).toBe(false)
  })

  it('treats a missing role list as open to every signed-in user, and nobody signed out', () => {
    expect(hasAnyRole(as('STUDENT'), undefined)).toBe(true)
    expect(hasAnyRole(null, ['STUDENT'])).toBe(false)
  })

  it('names roles in plain words', () => {
    expect(roleSummary(['HOD', 'FACULTY'])).toBe('Head of department, Faculty')
  })

  it('gives each role a menu of only what it uses', () => {
    const labels = (user) => visibleNavigation(user).map(({ label }) => label)

    expect(labels(as('STUDENT'))).toEqual(['Dashboard', 'Timetables'])
    expect(labels(as('INVIGILATOR'))).toEqual(['Dashboard', 'Timetables', 'My duties'])
    expect(labels(as('FACULTY'))).toEqual(['Dashboard', 'Timetables', 'My duties'])
    expect(labels(as('EXAM_COORDINATOR'))).toEqual(['Dashboard', 'Timetables', 'Sessions', 'Academics', 'Students', 'Halls'])
    expect(labels(as('ADMIN'))).toContain('Users')
  })

  it('keeps My duties away from approvers, who are never given a duty', () => {
    const labels = (user) => visibleNavigation(user).map(({ label }) => label)

    expect(labels(as('HOD'))).not.toContain('My duties')
    expect(labels(as('CONTROLLER'))).not.toContain('My duties')
    expect(labels(as('HOD'))).toContain('Sessions')
  })

  it('keeps System and Comparison out of the menu but still guarded', () => {
    const labels = visibleNavigation(as('ADMIN')).map(({ label }) => label)

    expect(labels).not.toContain('System')
    expect(labels).not.toContain('Comparison')
    expect(rolesFor('/system')).toEqual(['ADMIN'])
  })

  it('gives a head of department faculty access without the Faculty role', () => {
    expect(hasAnyRole(as('HOD'), ['FACULTY'])).toBe(true)
    expect(hasAnyRole(as('FACULTY'), ['HOD'])).toBe(false)
  })

  it('gives the Controller of Examinations reviewer access, but not administration', () => {
    expect(hasAnyRole(as('CONTROLLER'), ['HOD'])).toBe(true)
    expect(hasAnyRole(as('CONTROLLER'), ['EXAM_COORDINATOR'])).toBe(false)
    expect(roleConflict(['CONTROLLER', 'ADMIN'])).toBeDefined()
    expect(roleConflict(['CONTROLLER', 'HOD'])).toBeDefined()
  })

  it('refuses the same role combinations as the server', () => {
    expect(roleConflict(['STUDENT', 'ADMIN'])).toMatch(/student/)
    expect(roleConflict(['HOD', 'STUDENT'])).toMatch(/student/)
    expect(roleConflict(['ADMIN', 'EXAM_COORDINATOR', 'HOD', 'FACULTY', 'INVIGILATOR', 'STUDENT'])).toBeDefined()
    expect(roleConflict(['HOD', 'FACULTY'])).toBeDefined()
    expect(roleConflict(['HOD', 'EXAM_COORDINATOR'])).toBeDefined()
    expect(roleConflict(['EXAM_COORDINATOR', 'FACULTY'])).toBeUndefined()
    expect(roleConflict(['ADMIN', 'INVIGILATOR'])).toBeUndefined()
  })

  it('lets a wrongly saved role be removed, but not a clashing one added', () => {
    expect(canAddRole('STUDENT', ['HOD'])).toBe(false)
    expect(canAddRole('STUDENT', ['HOD', 'STUDENT'])).toBe(true)
  })
})

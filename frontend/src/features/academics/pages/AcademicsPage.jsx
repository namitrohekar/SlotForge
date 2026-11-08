import { Outlet } from 'react-router'
import PageHeader from '../../../components/layout/PageHeader.jsx'
import SectionNav from '../../../components/layout/SectionNav.jsx'

const sections = [
  { to: 'departments', label: 'Departments' },
  { to: 'courses', label: 'Programmes' },
  { to: 'subjects', label: 'Subjects' },
  { to: 'curricula', label: 'Curricula' },
  { to: 'batches', label: 'Batches' },
]

// Each section is its own address, so its search, filters and page survive reload and can be shared.
export default function AcademicsPage() {
  return (
    <>
      <PageHeader
        title="Academic structure"
        description="Departments, the programmes they run, the subjects they teach, and each programme’s syllabus, semester by semester."
      />
      <SectionNav label="Academic structure" sections={sections} />
      <Outlet />
    </>
  )
}

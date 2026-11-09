package com.slotforge.auth.security;

/** @PreAuthorize rules shared by feature controllers. ADMIN inherits coordinator, HOD inherits faculty. */
public final class Access {

    /** Teaching and administrative staff: may read reference data. */
    public static final String STAFF = "hasAnyRole('ADMIN', 'EXAM_COORDINATOR', 'HOD', 'FACULTY')";

    /** Exam coordinators and administrators: may change reference data. */
    public static final String COORDINATOR = "hasRole('EXAM_COORDINATOR')";

    /** Heads of department approve timetables. Administrators deliberately cannot. */
    public static final String HOD = "hasRole('HOD')";

    /** May see the student roster. */
    public static final String STUDENT_RECORDS = "hasAnyRole('ADMIN', 'EXAM_COORDINATOR', 'HOD')";

    /** Staff who can be given invigilation duties. A head of department inherits this through faculty. */
    public static final String INVIGILATORS = "hasAnyRole('FACULTY', 'INVIGILATOR')";

    /** May see halls: all staff, including non-teaching invigilators. */
    public static final String HALL_READERS = "hasAnyRole('ADMIN', 'EXAM_COORDINATOR', 'HOD', 'FACULTY', 'INVIGILATOR')";

    private Access() {
    }
}

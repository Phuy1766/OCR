package vn.edu.congvan.outbound.entity;

/** Cấp duyệt VB đi. */
public enum ApprovalLevel {
    /** Trưởng phòng duyệt VB của phòng mình. */
    DEPARTMENT_HEAD,
    /** Lãnh đạo / cấp phó duyệt cuối — chốt approved_version_id (BR-07). */
    UNIT_LEADER
}

package vn.edu.congvan.masterdata.entity;

/**
 * Phạm vi mật của sổ đăng ký.
 * NORMAL: chứa VB không mật. SECRET: sổ riêng cho VB mật (BR-03).
 */
public enum ConfidentialityScope {
    NORMAL,
    SECRET
}

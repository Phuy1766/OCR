package vn.edu.congvan.inbound.service;

/** Tải lên 1 file qua multipart — dùng cho service layer. */
public record UploadedFile(String filename, String mimeType, byte[] content) {}

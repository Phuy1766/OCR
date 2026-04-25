package vn.edu.congvan.masterdata.dto;

import java.util.UUID;

/**
 * Kết quả cấp số từ {@code DocumentNumberService}.
 *
 * @param bookId  ID sổ đã cấp số
 * @param year    năm
 * @param number  số đã cấp (unique trong phạm vi bookId+year)
 */
public record ReservedNumber(UUID bookId, int year, long number) {}

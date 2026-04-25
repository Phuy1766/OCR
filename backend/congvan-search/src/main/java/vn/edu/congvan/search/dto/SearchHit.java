package vn.edu.congvan.search.dto;

import java.time.LocalDate;
import java.util.UUID;

/** 1 hit kết quả tìm kiếm. */
public record SearchHit(
        UUID documentId,
        String direction,
        String status,
        String subject,
        String summary,
        String externalReferenceNumber,
        String externalIssuer,
        Long bookNumber,
        Integer bookYear,
        LocalDate receivedDate,
        LocalDate issuedDate,
        UUID organizationId,
        UUID departmentId,
        double score,
        /** HTML highlight {@code <mark>...</mark>} từ ts_headline. */
        String headline,
        /** Nguồn match: METADATA hoặc OCR (text từ kết quả OCR đã accept). */
        String matchSource) {}

package vn.edu.congvan.search.dto;

import java.util.List;

public record SearchResponse(
        List<SearchHit> hits,
        long totalElements,
        int page,
        int size,
        boolean fuzzyFallback) {}

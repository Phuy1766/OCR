package vn.edu.congvan.search.service;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import vn.edu.congvan.auth.security.AuthPrincipal;
import vn.edu.congvan.search.dto.SearchHit;
import vn.edu.congvan.search.dto.SearchResponse;

/**
 * Full-text search tiếng Việt qua PostgreSQL FTS config "vietnamese"
 * (V2: unaccent + simple). Match cả metadata document + raw_text OCR đã accept.
 *
 * <p>Thuật toán:
 * <ol>
 *   <li>Build {@code websearch_to_tsquery('vietnamese', :q)} từ query
 *   <li>Match {@code documents.search_vector} (METADATA) UNION
 *       match {@code ocr_results.search_vector} JOIN documents (OCR)
 *   <li>Filter theo direction/status/book/date/scope
 *   <li>Order theo {@code ts_rank} DESC
 *   <li>{@code ts_headline} cho highlight HTML
 *   <li>Fuzzy fallback (pg_trgm) nếu FTS 0 hits — query subject ILIKE
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final JdbcTemplate jdbc;

    public SearchResponse search(SearchCriteria criteria, AuthPrincipal actor, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = safePage * safeSize;

        String query = criteria.query() == null ? "" : criteria.query().trim();
        if (query.isBlank()) {
            // Không có query → list toàn bộ filtered, sort theo created_at desc
            return listOnly(criteria, actor, safePage, safeSize, offset);
        }

        // Try FTS first
        FtsResult ftsResult = ftsSearch(query, criteria, actor, safeSize, offset);
        if (ftsResult.total > 0) {
            return new SearchResponse(ftsResult.hits, ftsResult.total, safePage, safeSize, false);
        }

        // Fuzzy fallback với pg_trgm + ILIKE
        FtsResult fuzzy = fuzzyFallback(query, criteria, actor, safeSize, offset);
        return new SearchResponse(fuzzy.hits, fuzzy.total, safePage, safeSize, true);
    }

    // ---------- FTS query (UNION metadata + OCR) ----------
    private FtsResult ftsSearch(String query, SearchCriteria c, AuthPrincipal actor,
                                int limit, int offset) {
        StringBuilder sql = new StringBuilder();
        sql.append("WITH q AS (SELECT websearch_to_tsquery('vietnamese', ?) AS tsq), ");
        sql.append("base AS ( ");

        // Branch 1: match METADATA
        sql.append("  SELECT d.id AS doc_id, ts_rank(d.search_vector, q.tsq) AS score, ");
        sql.append("         'METADATA' AS source, ");
        sql.append("         ts_headline('vietnamese', ");
        sql.append("              coalesce(d.subject,'') || ' — ' || coalesce(d.summary,''), ");
        sql.append("              q.tsq, 'StartSel=<mark>,StopSel=</mark>,MaxWords=30,MinWords=10') AS headline ");
        sql.append("  FROM documents d, q ");
        sql.append("  WHERE d.is_deleted = false AND d.search_vector @@ q.tsq ");
        sql.append("  UNION ALL ");

        // Branch 2: match OCR raw_text (chỉ accepted)
        sql.append("  SELECT d.id AS doc_id, ts_rank(o.search_vector, q.tsq) * 0.7 AS score, ");
        sql.append("         'OCR' AS source, ");
        sql.append("         ts_headline('vietnamese', o.raw_text, q.tsq, ");
        sql.append("              'StartSel=<mark>,StopSel=</mark>,MaxWords=30,MinWords=10') AS headline ");
        sql.append("  FROM documents d ");
        sql.append("  JOIN ocr_jobs j ON j.document_id = d.id ");
        sql.append("  JOIN ocr_results o ON o.job_id = j.id ");
        sql.append("  CROSS JOIN q ");
        sql.append("  WHERE d.is_deleted = false ");
        sql.append("    AND o.is_accepted = true ");
        sql.append("    AND o.search_vector @@ q.tsq ");
        sql.append(") ");

        // Top 1 source per document (ưu tiên METADATA do score cao hơn)
        sql.append("SELECT b.doc_id, b.score, b.source, b.headline, ");
        sql.append("       d.direction, d.status, d.subject, d.summary, ");
        sql.append("       d.external_reference_number, d.external_issuer, ");
        sql.append("       d.book_number, d.book_year, d.received_date, d.issued_date, ");
        sql.append("       d.organization_id, d.department_id ");
        sql.append("FROM ( ");
        sql.append("  SELECT DISTINCT ON (b.doc_id) b.* ");
        sql.append("  FROM base b ORDER BY b.doc_id, b.score DESC ");
        sql.append(") b JOIN documents d ON d.id = b.doc_id WHERE d.is_deleted = false ");

        List<Object> params = new ArrayList<>();
        params.add(query);
        appendFilters(sql, params, c, actor);

        sql.append(" ORDER BY b.score DESC, d.created_at DESC ");
        sql.append(" LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        List<SearchHit> hits = jdbc.query(sql.toString(), this::mapHit, params.toArray());

        // Total count: cùng base CTE nhưng count distinct doc_id
        StringBuilder countSql = new StringBuilder();
        countSql.append("WITH q AS (SELECT websearch_to_tsquery('vietnamese', ?) AS tsq) ");
        countSql.append("SELECT COUNT(DISTINCT d.id) FROM documents d, q ");
        countSql.append("WHERE d.is_deleted = false AND ( ");
        countSql.append("  d.search_vector @@ q.tsq ");
        countSql.append("  OR EXISTS (SELECT 1 FROM ocr_jobs j JOIN ocr_results o ON o.job_id = j.id ");
        countSql.append("             WHERE j.document_id = d.id AND o.is_accepted = true ");
        countSql.append("               AND o.search_vector @@ q.tsq) ");
        countSql.append(")");
        List<Object> countParams = new ArrayList<>();
        countParams.add(query);
        appendFilters(countSql, countParams, c, actor);
        Long total = jdbc.queryForObject(countSql.toString(), Long.class, countParams.toArray());

        return new FtsResult(hits, total == null ? 0 : total);
    }

    // ---------- Fuzzy fallback ----------
    private FtsResult fuzzyFallback(String query, SearchCriteria c, AuthPrincipal actor,
                                    int limit, int offset) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT d.id AS doc_id, ");
        sql.append("       similarity(d.subject, ?) AS score, ");
        sql.append("       'FUZZY' AS source, ");
        sql.append("       d.subject AS headline, ");
        sql.append("       d.direction, d.status, d.subject, d.summary, ");
        sql.append("       d.external_reference_number, d.external_issuer, ");
        sql.append("       d.book_number, d.book_year, d.received_date, d.issued_date, ");
        sql.append("       d.organization_id, d.department_id ");
        sql.append("FROM documents d ");
        sql.append("WHERE d.is_deleted = false AND ( ");
        sql.append("  d.subject ILIKE ? OR d.external_reference_number ILIKE ? ");
        sql.append("  OR similarity(d.subject, ?) > 0.3) ");

        List<Object> params = new ArrayList<>();
        String like = "%" + query + "%";
        params.add(query);   // similarity
        params.add(like);    // subject ILIKE
        params.add(like);    // ref ILIKE
        params.add(query);   // similarity threshold

        appendFilters(sql, params, c, actor);

        sql.append(" ORDER BY similarity(d.subject, ?) DESC, d.created_at DESC ");
        sql.append(" LIMIT ? OFFSET ?");
        params.add(query);
        params.add(limit);
        params.add(offset);

        List<SearchHit> hits = jdbc.query(sql.toString(), this::mapHit, params.toArray());

        StringBuilder countSql = new StringBuilder();
        countSql.append("SELECT COUNT(*) FROM documents d ");
        countSql.append("WHERE d.is_deleted = false AND ( ");
        countSql.append("  d.subject ILIKE ? OR d.external_reference_number ILIKE ? ");
        countSql.append("  OR similarity(d.subject, ?) > 0.3) ");
        List<Object> countParams = new ArrayList<>();
        countParams.add(like);
        countParams.add(like);
        countParams.add(query);
        appendFilters(countSql, countParams, c, actor);
        Long total = jdbc.queryForObject(countSql.toString(), Long.class, countParams.toArray());
        return new FtsResult(hits, total == null ? 0 : total);
    }

    // ---------- List-only (query rỗng) ----------
    private SearchResponse listOnly(SearchCriteria c, AuthPrincipal actor,
                                    int page, int size, int offset) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT d.id AS doc_id, 0::float AS score, 'LIST' AS source, ");
        sql.append("       coalesce(d.subject,'') AS headline, ");
        sql.append("       d.direction, d.status, d.subject, d.summary, ");
        sql.append("       d.external_reference_number, d.external_issuer, ");
        sql.append("       d.book_number, d.book_year, d.received_date, d.issued_date, ");
        sql.append("       d.organization_id, d.department_id ");
        sql.append("FROM documents d WHERE d.is_deleted = false ");
        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, c, actor);
        sql.append(" ORDER BY d.created_at DESC LIMIT ? OFFSET ?");
        params.add(size);
        params.add(offset);
        List<SearchHit> hits = jdbc.query(sql.toString(), this::mapHit, params.toArray());

        StringBuilder countSql = new StringBuilder();
        countSql.append("SELECT COUNT(*) FROM documents d WHERE d.is_deleted = false ");
        List<Object> countParams = new ArrayList<>();
        appendFilters(countSql, countParams, c, actor);
        Long total = jdbc.queryForObject(countSql.toString(), Long.class, countParams.toArray());
        return new SearchResponse(hits, total == null ? 0 : total, page, size, false);
    }

    // ---------- Filters chung ----------
    private void appendFilters(
            StringBuilder sql, List<Object> params, SearchCriteria c, AuthPrincipal actor) {
        if (c.direction() != null) {
            sql.append(" AND d.direction = ?");
            params.add(c.direction());
        }
        if (c.status() != null) {
            sql.append(" AND d.status = ?");
            params.add(c.status());
        }
        if (c.organizationId() != null) {
            sql.append(" AND d.organization_id = ?");
            params.add(c.organizationId());
        }
        if (c.bookId() != null) {
            sql.append(" AND d.book_id = ?");
            params.add(c.bookId());
        }
        if (c.confidentialityLevelId() != null) {
            sql.append(" AND d.confidentiality_level_id = ?");
            params.add(c.confidentialityLevelId());
        }
        if (c.priorityLevelId() != null) {
            sql.append(" AND d.priority_level_id = ?");
            params.add(c.priorityLevelId());
        }
        if (c.fromDate() != null) {
            sql.append(" AND coalesce(d.received_date, d.issued_date, d.created_at::date) >= ?");
            params.add(Date.valueOf(c.fromDate()));
        }
        if (c.toDate() != null) {
            sql.append(" AND coalesce(d.received_date, d.issued_date, d.created_at::date) <= ?");
            params.add(Date.valueOf(c.toDate()));
        }

        // Permission scope
        if (actor != null) {
            var perms = actor.permissions();
            boolean canReadAll = perms.contains("INBOUND:READ_ALL")
                    || perms.contains("OUTBOUND:READ_ALL");
            boolean canReadDept = perms.contains("INBOUND:READ_DEPT")
                    || perms.contains("OUTBOUND:READ_DEPT");
            boolean canReadOwn = perms.contains("INBOUND:READ_OWN")
                    || perms.contains("OUTBOUND:READ_OWN");

            if (!canReadAll) {
                if (canReadDept && actor.userId() != null) {
                    // Phase 8: chưa resolve user dept tree, dùng created_by hoặc handler
                    // Phase tương lai sẽ join với user_roles + departments tree
                    sql.append(" AND (d.created_by = ? OR d.current_handler_user_id = ?) ");
                    params.add(actor.userId());
                    params.add(actor.userId());
                } else if (canReadOwn && actor.userId() != null) {
                    sql.append(" AND (d.created_by = ? OR d.current_handler_user_id = ?) ");
                    params.add(actor.userId());
                    params.add(actor.userId());
                } else {
                    // Không có permission → return zero rows
                    sql.append(" AND 1 = 0 ");
                }
            }
        }
    }

    private SearchHit mapHit(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        return new SearchHit(
                rs.getObject("doc_id", UUID.class),
                rs.getString("direction"),
                rs.getString("status"),
                rs.getString("subject"),
                rs.getString("summary"),
                rs.getString("external_reference_number"),
                rs.getString("external_issuer"),
                (Long) rs.getObject("book_number"),
                (Integer) rs.getObject("book_year"),
                toLocalDate(rs.getDate("received_date")),
                toLocalDate(rs.getDate("issued_date")),
                rs.getObject("organization_id", UUID.class),
                rs.getObject("department_id", UUID.class),
                rs.getDouble("score"),
                rs.getString("headline"),
                rs.getString("source"));
    }

    private static LocalDate toLocalDate(Date d) {
        return d == null ? null : d.toLocalDate();
    }

    private record FtsResult(List<SearchHit> hits, long total) {}
}

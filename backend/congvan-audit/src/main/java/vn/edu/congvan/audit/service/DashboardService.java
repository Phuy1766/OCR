package vn.edu.congvan.audit.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.congvan.audit.dto.DashboardStatsDto;
import vn.edu.congvan.audit.dto.DashboardStatsDto.Counts;
import vn.edu.congvan.audit.dto.DashboardStatsDto.MyTask;
import vn.edu.congvan.audit.dto.DashboardStatsDto.RecentDocument;
import vn.edu.congvan.auth.security.AuthPrincipal;

/** Tổng hợp số liệu cho trang dashboard. Truy vấn JDBC trực tiếp cho nhanh. */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final JdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public DashboardStatsDto getStats(AuthPrincipal actor) {
        UUID userId = actor == null ? null : actor.userId();

        long inboundThisWeek = jdbc.queryForObject(
                "SELECT COUNT(*) FROM documents WHERE direction = 'INBOUND' "
                        + "AND is_deleted = false "
                        + "AND created_at >= NOW() - INTERVAL '7 days'",
                Long.class);

        long outboundIssued = jdbc.queryForObject(
                "SELECT COUNT(*) FROM documents WHERE direction = 'OUTBOUND' "
                        + "AND is_deleted = false "
                        + "AND status IN ('ISSUED','SENT')",
                Long.class);

        long myActiveAssignments = userId == null ? 0L : jdbc.queryForObject(
                "SELECT COUNT(*) FROM assignments "
                        + "WHERE assigned_to_user_id = ? AND status = 'ACTIVE'",
                Long.class, userId);

        long unreadNotifications = userId == null ? 0L : jdbc.queryForObject(
                "SELECT COUNT(*) FROM notifications "
                        + "WHERE recipient_user_id = ? AND read_at IS NULL",
                Long.class, userId);

        Counts counts = new Counts(
                inboundThisWeek, outboundIssued, myActiveAssignments, unreadNotifications);

        List<RecentDocument> recentInbound = jdbc.query(
                "SELECT id, direction, status, subject, book_number, book_year, created_at "
                        + "FROM documents WHERE direction = 'INBOUND' AND is_deleted = false "
                        + "ORDER BY created_at DESC LIMIT 5",
                this::mapDocument);

        List<RecentDocument> recentOutbound = jdbc.query(
                "SELECT id, direction, status, subject, book_number, book_year, created_at "
                        + "FROM documents WHERE direction = 'OUTBOUND' AND is_deleted = false "
                        + "ORDER BY created_at DESC LIMIT 5",
                this::mapDocument);

        List<MyTask> myPendingTasks = userId == null ? List.of() : jdbc.query(
                "SELECT a.id AS assignment_id, a.document_id, d.subject, "
                        + "       to_char(a.due_date, 'YYYY-MM-DD') AS due_date "
                        + "FROM assignments a "
                        + "JOIN documents d ON d.id = a.document_id "
                        + "WHERE a.assigned_to_user_id = ? AND a.status = 'ACTIVE' "
                        + "ORDER BY a.due_date NULLS LAST, a.assigned_at DESC LIMIT 5",
                (rs, i) -> new MyTask(
                        rs.getObject("assignment_id", UUID.class),
                        rs.getObject("document_id", UUID.class),
                        rs.getString("subject"),
                        rs.getString("due_date")),
                userId);

        Map<String, Long> statusBreakdown = new HashMap<>();
        jdbc.query(
                "SELECT status, COUNT(*) AS cnt FROM documents "
                        + "WHERE is_deleted = false GROUP BY status",
                rs -> {
                    statusBreakdown.put(rs.getString("status"), rs.getLong("cnt"));
                });

        return new DashboardStatsDto(
                counts, recentInbound, recentOutbound, myPendingTasks, statusBreakdown);
    }

    private RecentDocument mapDocument(ResultSet rs, int row) throws SQLException {
        return new RecentDocument(
                rs.getObject("id", UUID.class),
                rs.getString("direction"),
                rs.getString("status"),
                rs.getString("subject"),
                (Long) rs.getObject("book_number"),
                (Integer) rs.getObject("book_year"),
                rs.getObject("created_at", OffsetDateTime.class));
    }
}

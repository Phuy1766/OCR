package vn.edu.congvan.audit.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Dashboard stats trả qua {@code GET /api/dashboard/stats}. */
public record DashboardStatsDto(
        Counts counts,
        List<RecentDocument> recentInbound,
        List<RecentDocument> recentOutbound,
        List<MyTask> myPendingTasks,
        Map<String, Long> statusBreakdown) {

    public record Counts(
            long inboundThisWeek,
            long outboundIssued,
            long myActiveAssignments,
            long unreadNotifications) {}

    public record RecentDocument(
            UUID id,
            String direction,
            String status,
            String subject,
            Long bookNumber,
            Integer bookYear,
            OffsetDateTime createdAt) {}

    public record MyTask(
            UUID assignmentId,
            UUID documentId,
            String subject,
            String dueDate) {}
}

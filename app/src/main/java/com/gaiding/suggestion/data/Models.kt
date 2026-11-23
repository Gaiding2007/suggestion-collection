package com.gaiding.suggestion.data

import org.json.JSONObject
import java.time.OffsetDateTime

enum class Status(val wire: String, val label: String) {
    Pending("pending", "待处理"),
    Accepted("accepted", "已接纳"),
    Rejected("rejected", "已驳回"),
    Resolved("resolved", "已解决"),
    Pushed("pushed", "已推送");

    companion object {
        fun of(wire: String?): Status = entries.firstOrNull { it.wire == wire } ?: Pending
    }
}

data class Profile(
    val id: String,
    val email: String,
    val displayName: String?,
    val isAdmin: Boolean,
) {
    val label: String get() = displayName?.takeIf { it.isNotBlank() } ?: email.substringBefore("@")

    companion object {
        fun from(o: JSONObject) = Profile(
            id = o.optString("id"),
            email = o.optString("email"),
            displayName = o.stringOrNull("display_name"),
            isAdmin = o.optBoolean("is_admin", false),
        )
    }
}

data class Project(
    val id: String,
    val name: String,
    val description: String?,
    val createdAt: Long,
    val archived: Boolean,
) {
    companion object {
        fun from(o: JSONObject) = Project(
            id = o.optString("id"),
            name = o.optString("name"),
            description = o.stringOrNull("description"),
            createdAt = parseTime(o.stringOrNull("created_at")) ?: 0L,
            archived = o.optBoolean("is_archived", false),
        )
    }
}

data class Suggestion(
    val id: String,
    val projectId: String,
    val authorId: String?,
    val title: String,
    val content: String?,
    val status: Status,
    val rejectReason: String?,
    val version: String?,
    val pushNote: String?,
    val createdAt: Long,
    val acceptedAt: Long?,
    val rejectedAt: Long?,
    val resolvedAt: Long?,
    val pushedAt: Long?,
) {
    companion object {
        fun from(o: JSONObject) = Suggestion(
            id = o.optString("id"),
            projectId = o.optString("project_id"),
            authorId = o.stringOrNull("author_id"),
            title = o.optString("title"),
            content = o.stringOrNull("content"),
            status = Status.of(o.stringOrNull("status")),
            rejectReason = o.stringOrNull("reject_reason"),
            version = o.stringOrNull("pushed_version"),
            pushNote = o.stringOrNull("pushed_note"),
            createdAt = parseTime(o.stringOrNull("created_at")) ?: 0L,
            acceptedAt = parseTime(o.stringOrNull("accepted_at")),
            rejectedAt = parseTime(o.stringOrNull("rejected_at")),
            resolvedAt = parseTime(o.stringOrNull("resolved_at")),
            pushedAt = parseTime(o.stringOrNull("pushed_at")),
        )
    }
}

data class AdminSuggestion(
    val suggestion: Suggestion,
    val projectName: String,
    val authorLabel: String,
)

data class AdminBundle(
    val projects: List<Project>,
    val suggestions: List<Suggestion>,
    val authors: Map<String, String>,
    val users: List<Profile>,
    val access: Set<String>,
) {
    fun toRows(): List<AdminSuggestion> {
        val projectNames = projects.associate { it.id to it.name }
        return suggestions.map { s ->
            AdminSuggestion(
                suggestion = s,
                projectName = projectNames[s.projectId] ?: "未知项目",
                authorLabel = authors[s.id] ?: "未知",
            )
        }
    }

    fun hasAccess(projectId: String, userId: String): Boolean = access.contains("$projectId|$userId")
}

internal fun JSONObject.stringOrNull(key: String): String? {
    if (!has(key) || isNull(key)) return null
    return optString(key).takeIf { it.isNotBlank() }
}

internal fun parseTime(raw: String?): Long? {
    if (raw.isNullOrBlank()) return null
    return try {
        OffsetDateTime.parse(raw).toInstant().toEpochMilli()
    } catch (_: Exception) {
        null
    }
}

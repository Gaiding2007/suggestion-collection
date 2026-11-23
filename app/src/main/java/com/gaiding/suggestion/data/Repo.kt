package com.gaiding.suggestion.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/**
 * 所有业务查询都走 Supabase 的 PostgREST 接口，权限由数据库的 RLS 策略兜底。
 * 客户端只做展示层的过滤，不安全的数据接口已经由数据库挡住。
 */
object Repo {

    private suspend fun authed(
        method: String,
        path: String,
        body: JSONObject? = null,
        prefer: String? = null,
    ): String {
        var token = Auth.accessToken()
        return try {
            Supa.raw(method, path, body, token, prefer)
        } catch (e: Supa.ApiException) {
            if (e.status != 401) throw e
            token = Auth.accessToken(force = true)
            Supa.raw(method, path, body, token, prefer)
        }
    }

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

    suspend fun myProfile(): Profile {
        val uid = SessionStore.userId
        if (uid.isNullOrBlank()) throw Supa.ApiException(401, "登录信息不完整，请重新登录")
        val list = Supa.arr(authed("GET", "/rest/v1/profiles?select=*&id=eq.$uid&limit=1")).objects()
        if (list.isEmpty()) throw IllegalStateException("账号资料还没建好，请联系管理员检查数据库")
        return Profile.from(list.first())
    }

    suspend fun projects(): List<Project> =
        Supa.arr(authed("GET", "/rest/v1/projects?select=*&order=created_at.asc"))
            .objects().map { Project.from(it) }

    suspend fun suggestionCounts(): Map<String, Int> {
        val arr = Supa.arr(authed("GET", "/rest/v1/suggestions?select=project_id"))
        return arr.objects().groupingBy { it.optString("project_id") }.eachCount()
    }

    suspend fun suggestionsOfProject(projectId: String): List<Suggestion> =
        Supa.arr(
            authed("GET", "/rest/v1/suggestions?select=*&project_id=eq.$projectId&order=created_at.asc")
        ).objects().map { Suggestion.from(it) }

    suspend fun adminBundle(): AdminBundle {
        val projects = projects()
        val suggestions = Supa.arr(
            authed("GET", "/rest/v1/suggestions?select=*&order=created_at.asc")
        ).objects().map { Suggestion.from(it) }

        val authorPairs = suggestions.mapNotNull { s -> s.authorId?.let { s.id to it } }.toMap()

        val users = Supa.arr(
            authed("GET", "/rest/v1/profiles?select=*&order=created_at.asc")
        ).objects().map { Profile.from(it) }

        val labelById = users.associate { it.id to it.label }
        val authors = authorPairs.mapValues { (_, uid) -> labelById[uid] ?: "已注销用户" }

        val access = Supa.arr(
            authed("GET", "/rest/v1/project_access?select=project_id,user_id")
        ).objects().map { "${it.optString("project_id")}|${it.optString("user_id")}" }.toSet()

        return AdminBundle(
            projects = projects,
            suggestions = suggestions,
            authors = authors,
            users = users,
            access = access,
        )
    }

    suspend fun createProject(name: String, description: String?) {
        val body = JSONObject()
            .put("name", name.trim())
            .put("description", description?.trim()?.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
            .put("created_by", SessionStore.userId ?: JSONObject.NULL)
        authed("POST", "/rest/v1/projects", body, prefer = "return=minimal")
    }

    suspend fun createSuggestion(projectId: String, title: String, content: String?) {
        val body = JSONObject()
            .put("p_project_id", projectId)
            .put("p_title", title.trim())
            .put("p_content", content?.trim()?.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
        authed("POST", "/rest/v1/rpc/create_suggestion", body, prefer = "return=minimal")
    }

    /**
     * 彻底删掉一个项目：项目里的建议、谁能进的授权靠外键 on delete cascade 一起走。
     *
     * 用 return=representation 要回被删掉的行：数据库的 RLS 把删除挡下来时，
     * PostgREST 不会报错，只会「什么都没删」并返回 204，客户端就以为成功了。
     * 这里靠返回的行数判断，0 行就当失败抛出去，别让界面骗人。
     */
    suspend fun deleteProject(id: String) {
        val text = authed(
            "DELETE",
            "/rest/v1/projects?id=eq.$id",
            null,
            prefer = "return=representation",
        )
        if (Supa.arr(text).length() == 0) {
            throw Supa.ApiException(
                403,
                "项目没被删掉：数据库没有同意删除。多半是删除规则还没同步" +
                    "（去 Supabase 的 SQL Editor 把 supabase/schema.sql 再执行一次），" +
                    "或者当前账号不是管理员。",
            )
        }
    }

    private suspend fun patchSuggestion(id: String, fields: JSONObject) {
        authed("PATCH", "/rest/v1/suggestions?id=eq.$id", fields, prefer = "return=minimal")
    }

    private fun now(): String = Instant.now().toString()

    suspend fun accept(id: String) = patchSuggestion(
        id,
        JSONObject()
            .put("status", Status.Accepted.wire)
            .put("accepted_at", now())
            // 从「已驳回」改成「已接纳」时，把旧的驳回痕迹擦掉，免得界面自相矛盾
            .put("reject_reason", JSONObject.NULL)
            .put("rejected_at", JSONObject.NULL)
            .put("handled_by", SessionStore.userId ?: JSONObject.NULL),
    )

    /** 提交人改自己还没被处理的建议（数据库里只能改标题和内容，见 update_suggestion 函数）。 */
    suspend fun updateSuggestion(id: String, title: String, content: String?) {
        val body = JSONObject()
            .put("p_id", id)
            .put("p_title", title.trim())
            .put("p_content", content?.trim()?.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
        authed("POST", "/rest/v1/rpc/update_suggestion", body, prefer = "return=minimal")
    }

    suspend fun reject(id: String, reason: String) = patchSuggestion(
        id,
        JSONObject()
            .put("status", Status.Rejected.wire)
            .put("reject_reason", reason.trim())
            .put("rejected_at", now())
            .put("handled_by", SessionStore.userId ?: JSONObject.NULL),
    )

    suspend fun resolve(id: String) = patchSuggestion(
        id,
        JSONObject()
            .put("status", Status.Resolved.wire)
            .put("resolved_at", now())
            .put("handled_by", SessionStore.userId ?: JSONObject.NULL),
    )

    suspend fun push(id: String, version: String, note: String?) = patchSuggestion(
        id,
        JSONObject()
            .put("status", Status.Pushed.wire)
            .put("pushed_version", version.trim())
            .put("pushed_note", note?.trim()?.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
            .put("pushed_at", now())
            .put("handled_by", SessionStore.userId ?: JSONObject.NULL),
    )

    suspend fun grantAccess(projectId: String, userId: String) {
        val body = JSONObject().put("project_id", projectId).put("user_id", userId)
        authed("POST", "/rest/v1/project_access", body, prefer = "return=minimal,resolution=ignore-duplicates")
    }

    suspend fun revokeAccess(projectId: String, userId: String) {
        authed(
            "DELETE",
            "/rest/v1/project_access?project_id=eq.$projectId&user_id=eq.$userId",
            prefer = "return=minimal",
        )
    }
}

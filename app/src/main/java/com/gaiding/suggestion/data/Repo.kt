package com.gaiding.suggestion.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * 所有业务数据都走自己服务器上的接口（deploy/server/server.js）。
 *
 * 权限判断在服务端：普通用户只能拿到被授权的项目和建议，管理员拿全量。
 * 客户端仍然只做展示层的过滤。
 */
object Repo {

    private suspend fun authed(method: String, path: String, body: JSONObject? = null): String {
        var token = Auth.accessToken()
        return try {
            Supa.raw(method, path, body, token)
        } catch (e: Supa.ApiException) {
            if (e.status != 401) throw e
            token = Auth.accessToken(force = true)
            Supa.raw(method, path, body, token)
        }
    }

    /** 把 {"key": [...]} 里的数组取成 List<JSONObject>。 */
    private fun JSONObject.list(key: String): List<JSONObject> {
        val arr: JSONArray = optJSONArray(key) ?: return emptyList()
        return (0 until arr.length()).mapNotNull { runCatching { arr.getJSONObject(it) }.getOrNull() }
    }

    /** 一次拿全首页需要的东西：项目、可见建议，管理员还会多拿到成员和授权表。 */
    private suspend fun home(): JSONObject = Supa.obj(authed("GET", "/home"))

    suspend fun myProfile(): Profile {
        val user = Supa.obj(authed("GET", "/me")).optJSONObject("user")
            ?: throw Supa.ApiException(401, "账号资料没拿到，请重新登录")
        return Profile.from(user)
    }

    suspend fun projects(): List<Project> = home().list("projects").map { Project.from(it) }

    suspend fun suggestionCounts(): Map<String, Int> =
        home().list("suggestions").groupingBy { it.optString("project_id") }.eachCount()

    suspend fun suggestionsOfProject(projectId: String): List<Suggestion> =
        home().list("suggestions")
            .filter { it.optString("project_id") == projectId }
            .map { Suggestion.from(it) }

    suspend fun adminBundle(): AdminBundle {
        val raw = home()
        val projects = raw.list("projects").map { Project.from(it) }
        val suggestions = raw.list("suggestions").map { Suggestion.from(it) }
        val users = raw.list("users").map { Profile.from(it) }
        val access = raw.list("access")
            .map { "${it.optString("project_id")}|${it.optString("user_id")}" }
            .toSet()

        val labelById = users.associate { it.id to it.label }
        val authors = suggestions.mapNotNull { s ->
            s.authorId?.let { uid -> s.id to (labelById[uid] ?: "已注销用户") }
        }.toMap()

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
        authed("POST", "/projects", body)
    }

    /** 彻底删掉一个项目：项目里的建议、授权由数据库外键 on delete cascade 一起走。 */
    suspend fun deleteProject(id: String) {
        val ok = Supa.obj(authed("DELETE", "/projects/$id")).optBoolean("ok", false)
        if (!ok) throw Supa.ApiException(500, "服务端说没删掉，刷新再看一眼")
    }

    suspend fun createSuggestion(
        projectId: String,
        title: String,
        content: String?,
        images: List<String> = emptyList(),
    ) {
        val body = JSONObject()
            .put("project_id", projectId)
            .put("title", title.trim())
            .put("content", content?.trim()?.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
            .put("images", JSONArray(images))
        authed("POST", "/suggestions", body)
    }

    /** 提交人改自己还没被处理的建议（服务端校验作者和时间点）。images 传 null 表示不动图片。 */
    suspend fun updateSuggestion(
        id: String,
        title: String,
        content: String?,
        images: List<String>? = null,
    ) {
        val body = JSONObject()
            .put("title", title.trim())
            .put("content", content?.trim()?.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
        if (images != null) body.put("images", JSONArray(images))
        authed("PATCH", "/suggestions/$id", body)
    }

    /** 先把图传上去拿到路径，再随建议一起提交。 */
    suspend fun uploadImage(jpegBytes: ByteArray): String {
        val body = JSONObject().put(
            "data",
            android.util.Base64.encodeToString(jpegBytes, android.util.Base64.NO_WRAP),
        )
        val path = Supa.obj(authed("POST", "/upload", body)).optString("path")
        if (path.isBlank()) throw Supa.ApiException(500, "图片没传上去，请重试")
        return path
    }

    /** 提交人删掉自己还没被处理的建议。 */
    suspend fun deleteSuggestion(id: String) {
        authed("DELETE", "/suggestions/$id")
    }

    private suspend fun act(id: String, action: String, body: JSONObject = JSONObject()) {
        authed("POST", "/suggestions/$id/$action", body)
    }

    suspend fun accept(id: String) = act(id, "accept")

    suspend fun reject(id: String, reason: String) =
        act(id, "reject", JSONObject().put("reason", reason.trim()))

    suspend fun resolve(id: String) = act(id, "resolve")

    suspend fun push(id: String, version: String, note: String?) =
        act(
            id,
            "push",
            JSONObject()
                .put("version", version.trim())
                .put("note", note?.trim()?.takeIf { it.isNotBlank() } ?: JSONObject.NULL),
        )

    /**
     * 把「已解决 / 已推送 / 已驳回」里的建议退回「待办」（= 已接纳）：
     * 推送信息、驳回理由、解决时间全部清掉，让它回到可以重新处理的状态。
     */
    suspend fun reopen(id: String) = act(id, "reopen")

    suspend fun grantAccess(projectId: String, userId: String) {
        authed(
            "PUT",
            "/projects/$projectId/access",
            JSONObject().put("user_id", userId).put("granted", true),
        )
    }

    suspend fun revokeAccess(projectId: String, userId: String) {
        authed(
            "PUT",
            "/projects/$projectId/access",
            JSONObject().put("user_id", userId).put("granted", false),
        )
    }
}

package com.gaiding.suggestion.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 打开 App 时自动查一次有没有新版本。
 *
 * 版本清单就是下载页用的那个 /versions.json（{updated, items:[{version, code, file, size, notes}]}），
 * 里面每条带 code（= versionCode）。本地 versionCode 比清单里最大的一条小，就提示去下载页。
 *
 * 这里只做「提示 + 打开浏览器」，不自己下载安装——Android 装 App 必须由用户点确认，
 * 老老实实跳到下载页比申请「安装未知应用」权限更省事，也更好解释。
 */
data class UpdateInfo(
    val version: String,
    val code: Int,
    val sizeBytes: Long,
    val notes: String,
    val file: String,
) {
    val sizeText: String
        get() = if (sizeBytes > 0) "%.1f MB".format(sizeBytes / 1048576.0) else ""
}

object Update {
    private const val MANIFEST = "/versions.json"

    /** 当前装着的 App 的 versionCode。 */
    fun currentCode(context: Context): Int = try {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            info.versionCode
        }
    } catch (_: Exception) {
        0
    }

    /** 拉清单，返回清单里 code 最大的那条；网络不行就返回 null（不打扰用户）。 */
    suspend fun check(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        val root = Supa.obj(Supa.raw("GET", MANIFEST, base = Supa.SITE))
        val items = root.optJSONArray("items") ?: return@withContext null
        var best: UpdateInfo? = null
        for (i in 0 until items.length()) {
            val o = items.optJSONObject(i) ?: continue
            val version = o.optString("version").trim()
            val code = o.optInt("code", 0)
            if (version.isEmpty() || code <= 0) continue
            val current = best
            if (current == null || code > current.code) {
                best = UpdateInfo(
                    version = version,
                    code = code,
                    sizeBytes = o.optLong("size", 0L),
                    notes = o.optString("notes").trim(),
                    file = o.optString("file").trim(),
                )
            }
        }
        best
    }

    /** 打开下载页（页面上有安装步骤和最新的 APK）。 */
    fun openDownloadPage(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(Supa.SITE)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            // 设备上连浏览器都没有，就算了，用户自己知道地址
        }
    }
}

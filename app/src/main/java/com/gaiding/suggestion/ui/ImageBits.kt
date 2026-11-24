package com.gaiding.suggestion.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gaiding.suggestion.data.Supa
import com.gaiding.suggestion.ui.theme.Brand
import com.gaiding.suggestion.ui.theme.InkFaint
import com.gaiding.suggestion.ui.theme.LineSoft
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * 建议里的图片（0.0.9 新增）。
 *
 * 图片存自己服务器上的 /uploads/ 目录，由 nginx 直接返回；这里只做三件事：
 * 1. 选图后压到长边 1200、JPEG 质量 82 再上传（省流量，手机拍的照片一张能压到几百 KB）
 * 2. 内存里缓存一下，列表滚动时不重复下载
 * 3. 卡片里显示缩略图，点开看大图
 */
object ImageStore {

    /** 缩略图用长边 720，足够 96dp 的格子显示，也省内存。 */
    private const val THUMB_SIDE = 720

    private val cache = object : android.util.LruCache<String, ImageBitmap>(24) {}

    fun cached(path: String): ImageBitmap? = cache.get(path)

    suspend fun load(path: String, fullSize: Boolean = false): ImageBitmap? {
        cache.get(path)?.let { if (!fullSize) return it }
        return try {
            val bytes = Supa.imageBytes(path)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            val side = if (fullSize) 1600 else THUMB_SIDE
            val scaled = downscale(bitmap, side)
            val image = scaled.asImageBitmap()
            cache.put(path, image)
            image
        } catch (_: Exception) {
            null
        }
    }

    private fun downscale(src: Bitmap, maxSide: Int): Bitmap {
        val longest = maxOf(src.width, src.height)
        if (longest <= maxSide) return src
        val ratio = maxSide.toFloat() / longest
        val w = (src.width * ratio).toInt().coerceAtLeast(1)
        val h = (src.height * ratio).toInt().coerceAtLeast(1)
        val out = Bitmap.createScaledBitmap(src, w, h, true)
        if (out !== src) src.recycle()
        return out
    }
}

/**
 * 把相册里选的图读出来：按 EXIF 转过方向、缩到长边 1200、压成 JPEG。
 * 参数只限制结果大小，原图多大都不怕（先读尺寸再决定采样率）。
 */
fun prepareImageBytes(context: Context, uri: Uri): ByteArray {
    val raw = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        ?: throw IllegalStateException("这张图读不出来，换一张试试")

    // 先在内存里算采样率，避免超大图把内存撑爆
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(raw, 0, raw.size, bounds)
    var sample = 1
    val longest = maxOf(bounds.outWidth, bounds.outHeight)
    while (longest / (sample * 2) >= 1200) sample *= 2

    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    val bitmap = BitmapFactory.decodeByteArray(raw, 0, raw.size, opts)
        ?: throw IllegalStateException("这张图打不开，换一张试试")

    val rotated = applyExifRotation(raw, bitmap)
    val ratio = 1200f / maxOf(rotated.width, rotated.height)
    val scaled = if (ratio < 1f) {
        Bitmap.createScaledBitmap(
            rotated,
            (rotated.width * ratio).toInt().coerceAtLeast(1),
            (rotated.height * ratio).toInt().coerceAtLeast(1),
            true,
        )
    } else {
        rotated
    }

    val out = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, 82, out)
    scaled.recycle()
    if (scaled !== rotated) rotated.recycle()
    return out.toByteArray()
}

private fun applyExifRotation(raw: ByteArray, bitmap: Bitmap): Bitmap {
    val orientation = try {
        ExifInterface(ByteArrayInputStream(raw))
            .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    } catch (_: Exception) {
        ExifInterface.ORIENTATION_NORMAL
    }
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        else -> return bitmap
    }
    val out = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    if (out !== bitmap) bitmap.recycle()
    return out
}

/** 一张远程图：加载中显示浅灰方块，失败显示「图片没加载出来」。 */
@Composable
fun RemoteImage(
    path: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val bitmap by produceState<ImageBitmap?>(initialValue = ImageStore.cached(path), path) {
        if (value == null) value = ImageStore.load(path)
    }
    Box(modifier.background(LineSoft), contentAlignment = Alignment.Center) {
        val image = bitmap
        if (image == null) {
            Text("图片加载中…", style = MaterialTheme.typography.labelSmall, color = InkFaint)
        } else {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** 卡片里的图片行：最多三张 88dp 的方块，超过就正常横排（最多就三张）。 */
@Composable
fun ImageStrip(paths: List<String>, onOpen: (String) -> Unit) {
    if (paths.isEmpty()) return
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        paths.forEach { path ->
            RemoteImage(
                path = path,
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onOpen(path) },
            )
        }
    }
}

/** 点开看大图：全屏、深色底、点任意处关掉。 */
@Composable
fun ImageViewer(path: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xEE10131A))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            val bitmap by produceState<ImageBitmap?>(initialValue = ImageStore.cached(path), path) {
                value = ImageStore.load(path, fullSize = true)
            }
            val image = bitmap
            if (image == null) {
                LoadingBlock()
            } else {
                Image(
                    bitmap = image,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = "点一下关闭",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFB9BFD0),
                )
            }
        }
    }
}

/** 编辑框里已选图片的小格子，右上角一个 ✕ 能删掉。 */
@Composable
fun EditableImageTile(path: String, onRemove: () -> Unit) {
    Box(
        Modifier
            .size(88.dp)
            .clip(RoundedCornerShape(12.dp)),
    ) {
        RemoteImage(path = path, modifier = Modifier.matchParentSize())
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(Color(0xCC10131A))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "×",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/** 编辑框底部「加一张图」的按钮（带张数提示和上传中状态）。 */
@Composable
fun AddImageButton(
    count: Int,
    limit: Int,
    uploading: Boolean,
    onPick: () -> Unit,
) {
    val enabled = !uploading && count < limit
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .clickable(enabled = enabled, onClick = onPick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (uploading) "正在上传…" else "+ 加图片",
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) Brand else InkFaint,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = "$count/$limit",
            style = MaterialTheme.typography.labelSmall,
            color = InkFaint,
        )
    }
}
